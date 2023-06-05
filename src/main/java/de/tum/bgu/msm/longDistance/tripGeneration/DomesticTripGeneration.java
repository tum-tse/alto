package de.tum.bgu.msm.longDistance.tripGeneration;


import de.tum.bgu.msm.common.datafile.TableDataSet;
import de.tum.bgu.msm.common.matrix.Matrix;
import de.tum.bgu.msm.*;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.LDModelOntario;
import de.tum.bgu.msm.longDistance.accessibilityAnalysis.AccessibilityAnalysis;
import de.tum.bgu.msm.longDistance.data.sp.Household;
import de.tum.bgu.msm.longDistance.data.sp.HouseholdOntario;
import de.tum.bgu.msm.longDistance.data.sp.Person;

import de.tum.bgu.msm.longDistance.data.sp.PersonOntario;
import de.tum.bgu.msm.longDistance.data.trips.*;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.Logger;

/**
 * Created by Carlos Llorca on 7/4/2016.
 * Technical Universty of Munich
 * <p>
 * Class to generate trips for the synthetic population
 * <p>
 * works for domestic trips
 */

public class DomesticTripGeneration{

    private DataSet dataSet;

    static Logger logger = Logger.getLogger(DomesticTripGeneration.class);
    private ResourceBundle rb;
    private JSONObject prop;

    private TableDataSet tripGenerationCoefficients;
    private TableDataSet travelPartyProbabilities;

    //private SyntheticPopulationReader synPop;

    private float alphaAccess;
    private float betaAccess;
    private AtomicInteger atomicInteger = new AtomicInteger(0);



    public DomesticTripGeneration(JSONObject prop, String inputFolder, String outputFolder) {
        this.rb = rb;
        this.prop = prop;

        //this.synPop = synPop;

        //String tripGenCoefficientsFilename = rb.getString("domestic.coefs");
        tripGenerationCoefficients = Util.readCSVfile(inputFolder + JsonUtilMto.getStringProp(prop,"trip_generation.domestic.coef_file"));
        tripGenerationCoefficients.buildIndex(tripGenerationCoefficients.getColumnPosition("factor"));
        tripGenerationCoefficients.buildStringIndex(tripGenerationCoefficients.getColumnPosition("factorName"));

        //String travelPartyProbabilitiesFilename = rb.getString("domestic.parties");

        travelPartyProbabilities = Util.readCSVfile(inputFolder +JsonUtilMto.getStringProp(prop,"trip_generation.domestic.party_file"));
        travelPartyProbabilities.buildIndex(travelPartyProbabilities.getColumnPosition("travelParty"));


        //alphaAccess = (float) ResourceUtil.getDoubleProperty(rb, "domestic.access.alpha");
        //betaAccess = (float) ResourceUtil.getDoubleProperty(rb, "domestic.access.beta");

        alphaAccess = JsonUtilMto.getFloatProp(prop,"trip_generation.domestic.accessibility.alpha");
        betaAccess = JsonUtilMto.getFloatProp(prop,"trip_generation.domestic.accessibility.beta");

    }



    public void load(DataSet dataSet){

        this.dataSet = dataSet;

        List<String> fromZones;
        List<String> toZones;
        //accessibility in Canada to Canada Trips - assing short-distance accessibility to zones
        fromZones = Arrays.asList("ONTARIO");
        toZones = Arrays.asList("ONTARIO", "EXTCANADA");
        AccessibilityAnalysis.calculateAccessibility(dataSet, fromZones, toZones, alphaAccess , betaAccess);
    }


    public ArrayList<LongDistanceTripOntario> run() {
        ArrayList<LongDistanceTripOntario> trips = new ArrayList<>();

        //initialize utility vectors
        Map<Type, Double> expUtilities = new HashMap<>();
        double doNotTravelExpUtility = 1; //base case - or do not travel

        //this option may give randomness to the results
        //synPop.getHouseholds().forEach(hhold -> {
          for (Household hhold : dataSet.getHouseholds().values()) {

            //pick and shuffle the members of the household
            ArrayList<Person> membersList = new ArrayList<>(Arrays.asList(((HouseholdOntario) hhold).getPersonsOfThisHousehold()));
            Collections.shuffle(membersList, LDModelOntario.rand);

            for (Person person : membersList) {
                //array to store 3 x 3 trip probabilities for later use in international

                PersonOntario pers = (PersonOntario) person;

                Map<Purpose, Map<Type ,Double>> tgProbabilities = new HashMap<>();

                if (!pers.isAway() && !pers.isDaytrip() && !pers.isInOutTrip() && pers.getAge() > 17) {

                    for (Purpose tripPurpose : PurposeOntario.values()) {
                        tgProbabilities.put(tripPurpose, new HashMap<>());

                        for (Type tripState : TypeOntario.values()) {
                            //expUtilities[tripStates.indexOf(tripState)] = Math.exp(estimateMlogitUtility(personDescription, tripPurpose, tripState, tripGenerationCoefficients));
                            expUtilities.put(tripState, Math.exp(calculateUtility(pers, tripPurpose.toString(), tripState.toString())));
                        }

                        double denominator = expUtilities.values().stream().mapToDouble(Double::doubleValue).sum() + doNotTravelExpUtility;
                        Map<Type, Double> probabilities = new HashMap<>();
                        expUtilities.forEach((x,y)-> probabilities.put(x,y/denominator));


                        //store the probability for later int trips
                        for (Type tripState : TypeOntario.values()) {
                            tgProbabilities.get(tripPurpose).put(tripState, probabilities.get(tripState));
                        }
                        //select the trip state
                        double randomNumber1 = LDModelOntario.rand.nextDouble();
                        int tripStateChoice = 3;
                        Type tripState = null;

                        if (randomNumber1 < probabilities.get(TypeOntario.AWAY)) {
                            tripState = TypeOntario.AWAY;
                            pers.setAway(true);
                        } else if (randomNumber1 < probabilities.get(TypeOntario.AWAY) + probabilities.get(TypeOntario.DAYTRIP)) {
                            tripState = TypeOntario.DAYTRIP;
                            pers.setDaytrip(true);
                        } else if (randomNumber1 < probabilities.get(TypeOntario.AWAY) + probabilities.get(TypeOntario.DAYTRIP) + probabilities.get(TypeOntario.INOUT)) {
                            tripState = TypeOntario.INOUT;
                            pers.setInOutTrip(true);
                        }

                        if (tripState != null) {
                            LongDistanceTripOntario trip = createLongDistanceTrip(pers, tripPurpose, tripState, probabilities,  travelPartyProbabilities);
                            trips.add(trip);
                            //tripCount++;
                        }
                    }
                    //assign probabilities to the person
                    pers.setTravelProbabilities(tgProbabilities);
                }


            }

        };
        return trips;
    }

    public double calculateUtility(PersonOntario pers, String tripPurpose, String tripState) {

        double accessibility = pers.getHousehold().getZone().getAccessibility();

        int winter = JsonUtilMto.getBooleanProp(prop, "summer" )? 0:1;


        //read coefficients
        String coefficientColumn = tripState + "." + tripPurpose;

        double intercept = tripGenerationCoefficients.getStringIndexedValueAt("(intercept)", coefficientColumn);
        double b_young = tripGenerationCoefficients.getStringIndexedValueAt("Young", coefficientColumn);
        double b_retired = tripGenerationCoefficients.getStringIndexedValueAt("Retired", coefficientColumn);
        double b_female = tripGenerationCoefficients.getStringIndexedValueAt("Female", coefficientColumn);
        double b_adultsHh = tripGenerationCoefficients.getStringIndexedValueAt("adultsInHousehold", coefficientColumn);
        double b_kidsHh = tripGenerationCoefficients.getStringIndexedValueAt("kidsInHousehold", coefficientColumn);
        double b_highSchool = tripGenerationCoefficients.getStringIndexedValueAt("HighSchool", coefficientColumn);
        double b_postSecondary = tripGenerationCoefficients.getStringIndexedValueAt("PostSecondary", coefficientColumn);
        double b_university = tripGenerationCoefficients.getStringIndexedValueAt("University", coefficientColumn);
        double b_employed = tripGenerationCoefficients.getStringIndexedValueAt("Employed", coefficientColumn);
        double b_income2 = tripGenerationCoefficients.getStringIndexedValueAt("income2", coefficientColumn);
        double b_income3 = tripGenerationCoefficients.getStringIndexedValueAt("income3", coefficientColumn);
        double b_income4 = tripGenerationCoefficients.getStringIndexedValueAt("income4", coefficientColumn);
        double b_accessibility = tripGenerationCoefficients.getStringIndexedValueAt("accessibility", coefficientColumn);
        double b_winter = tripGenerationCoefficients.getStringIndexedValueAt("winter", coefficientColumn);

        return intercept +
                b_young * Boolean.compare(pers.isYoung(), false) +
                b_retired * Boolean.compare(pers.isRetired(), false) +
                b_female * Boolean.compare(pers.isFemale(), false) +
                b_adultsHh * pers.getAdultsHh() +
                b_kidsHh * pers.getKidsHh() +
                b_highSchool * Boolean.compare(pers.isHighSchool(), false) +
                b_postSecondary * Boolean.compare(pers.isPostSecondary(), false) +
                b_university * Boolean.compare(pers.isUniversity(), false) +
                b_employed * Boolean.compare(pers.isEmployed(), false) +
                b_income2 * Boolean.compare(pers.isIncome2(), false) +
                b_income3 * Boolean.compare(pers.isIncome3(), false) +
                b_income4 * Boolean.compare(pers.isIncome4(), false) +
                b_accessibility * accessibility +
                b_winter * winter;

    }

    //this method is no longer used
    public float[] readPersonSocioDemographics(PersonOntario pers) {
        float personDescription[] = new float[15];
        //change size to 15 if "winter" is added
        //intercept always = 1
        personDescription[0] = 1;
        //Young = 1 if age is under 25
        if (pers.getAge() < 25) {
            personDescription[1] = 1;
        } else {
            personDescription[1] = 0;
        }
        //Retired = 1 if age is over 64
        if (pers.getAge() > 64) {
            personDescription[2] = 1;
        } else {
            personDescription[2] = 0;
        }
        //Gender =  1 if is female
        if (pers.getGender() == 'F') {
            personDescription[3] = 1;
        } else {
            personDescription[3] = 0;
        }
        //household sizes
        personDescription[4] = pers.getAdultsHh();
        personDescription[5] = pers.getKidsHh();
        //High School
        if (pers.getEducation() == 2) {
            personDescription[6] = 1;
        } else {
            personDescription[6] = 0;
        }
        // Post Secondary
        if (pers.getEducation() > 2 & pers.getEducation() < 6) {
            personDescription[7] = 1;
        } else {
            personDescription[7] = 0;
        }
        //University
        if (pers.getEducation() > 5 & pers.getEducation() < 9) {
            personDescription[8] = 1;
        } else {
            personDescription[8] = 0;
        }
        //Employed = 0 if unemployed
        if (pers.getWorkStatus() > 2) {
            personDescription[9] = 0;
        } else {
            personDescription[9] = 1;
        }
        if (pers.getIncome() >= 100000) {
            //is in income group 4
            personDescription[10] = 0;
            personDescription[11] = 0;
            personDescription[12] = 1;
        } else if (pers.getIncome() >= 70000) {
            //is in income gorup 3
            personDescription[10] = 0;
            personDescription[11] = 1;
            personDescription[12] = 0;
        } else if (pers.getIncome() >= 50000) {
            //is in income group 2
            personDescription[10] = 1;
            personDescription[11] = 0;
            personDescription[12] = 0;
        } else {
            personDescription[10] = 0;
            personDescription[11] = 0;
            personDescription[12] = 0;
        }

        personDescription[13] = (float) pers.getHousehold().getZone().getAccessibility();

        //variable is winter
        if (JsonUtilMto.getBooleanProp(prop, "winter" )) {
            personDescription[14] = 1;
        } else {
            personDescription[14] = 0;
        }

        return personDescription;
    }

    //this method is no longer used
    public static double estimateMlogitUtility(float[] personDescription, String tripPurpose, String tripState, TableDataSet tripGenerationCoefficients) {
        double utility = 0;
        // set sum of utilities of the 4 alternatives

        //j is an index for tripStates
        // 0 = away
        // 1 = daytrip
        // 2 = inout trip
        //the next loop calculate the utilities for the 3 states (stay at home has utility 0)

        String coefficientColumn = tripState + "." + tripPurpose;
        for (int var = 0; var < personDescription.length; var++) {
            //var is an index for the variables of person and coefficients of the model
            utility += tripGenerationCoefficients.getIndexedValueAt(var + 1, coefficientColumn) * personDescription[var];
        }
        return utility;
    }


    private LongDistanceTripOntario createLongDistanceTrip(PersonOntario pers, Purpose tripPurpose, Type tripState, Map<Type, Double> probability, TableDataSet travelPartyProbabilities) {

        ArrayList<Person> adultsHhTravelParty = addAdultsHhTravelParty(pers, tripPurpose.toString(), travelPartyProbabilities);
        ArrayList<Person> kidsHhTravelParty = addKidsHhTravelParty(pers, tripPurpose.toString(), travelPartyProbabilities);
        ArrayList<Person> hhTravelParty = new ArrayList<>();
        hhTravelParty.addAll(adultsHhTravelParty);
        hhTravelParty.addAll(kidsHhTravelParty);
        int nonHhTravelPartySize = addNonHhTravelPartySize(tripPurpose.toString(), travelPartyProbabilities);
        int tripDuration;
        if (pers.isDaytrip()) tripDuration = 0;
        else {
            tripDuration = estimateSimpleTripDuration(tripState);
        }

        LongDistanceTripOntario trip = new LongDistanceTripOntario(atomicInteger.getAndIncrement(), pers, false, tripPurpose, tripState,
                pers.getHousehold().getZone(), tripDuration, nonHhTravelPartySize);
        trip.setHhTravelParty(hhTravelParty);

        if (  Util.isPowerOfFour(atomicInteger.get())){
            logger.info("Domestic trips: " + atomicInteger.get());
        }


        return trip;

    }

    @Deprecated
    public static int estimateTripDuration(double[] probability) {
        int tripDuration = 1;
        double randomChoice4 = LDModelOntario.rand.nextDouble();
        while (tripDuration < 30 && randomChoice4 < probability[0] / (probability[0] + probability[2])) {
            randomChoice4 = LDModelOntario.rand.nextDouble();
            tripDuration++;
        }
        return tripDuration;
    }

    public static int estimateSimpleTripDuration(Type tripState) {
        int tripDuration = tripState.equals(TypeOntario.DAYTRIP) ? 0 : 1;
        return tripDuration;
    }


    public static ArrayList<Person> addAdultsHhTravelParty(PersonOntario pers, String tripPurpose, TableDataSet travelPartyProbabilities) {

        ArrayList<Person> hhTravelParty = new ArrayList<>();
        int hhmember = 0;
        hhTravelParty.add(0, pers);
        double randomChoice2 = LDModelOntario.rand.nextDouble();
        HouseholdOntario hhold = pers.getHousehold();
        for (PersonOntario pers2 : hhold.getPersonsOfThisHousehold()) {
            if (pers2 != pers && !pers2.isAway() && !pers2.isDaytrip() && !pers2.isInOutTrip() && pers2.getAge() > 17) {
                String column = tripPurpose + "." + Math.min(pers.getAdultsHh(), 5);
                double probability2 = travelPartyProbabilities.getIndexedValueAt(Math.min(hhmember + 1, 5), column);
                if (randomChoice2 < probability2) {
                    if (pers.isAway()) pers2.setAway(true);
                    else if (pers.isDaytrip()) pers2.setDaytrip(true);
                    else if (pers.isInOutTrip()) pers2.setInOutTrip(true);
                    hhmember++;
                    hhTravelParty.add(hhmember, pers2);
                }
            }
        }
        return hhTravelParty;
    }

    public static ArrayList<Person> addKidsHhTravelParty(PersonOntario pers, String tripPurpose, TableDataSet travelPartyProbabilities) {
        ArrayList<Person> hhTravelParty = new ArrayList<>();
        int hhmember = 0;
        double randomChoice2 = LDModelOntario.rand.nextDouble();
        HouseholdOntario hhold = pers.getHousehold();
        for (PersonOntario pers2 : hhold.getPersonsOfThisHousehold()) {
            if (pers2 != pers && !pers2.isAway() && !pers2.isDaytrip() && !pers2.isInOutTrip() && pers2.getAge() < 18) {
                String column = "kids." + tripPurpose + "." + Math.min(pers.getKidsHh(), 5);
                double probability2 = travelPartyProbabilities.getIndexedValueAt(Math.min(hhmember + 1, 5), column);
                if (randomChoice2 < probability2) {
                    if (pers.isAway()) pers2.setAway(true);
                    else if (pers.isDaytrip()) pers2.setDaytrip(true);
                    else if (pers.isInOutTrip()) pers2.setInOutTrip(true);
                    hhTravelParty.add(hhmember, pers2);
                    hhmember++;
                }
            }
        }
        return hhTravelParty;
    }

    public static int addNonHhTravelPartySize(String tripPurpose, TableDataSet travelPartyProbabilities) {
        // methods selects party size for travel groups that are composed of non-household members
        // note that additional travelers on this trip are not specified in the synthetic population (simplified approach)
        double randomChoice3 = LDModelOntario.rand.nextDouble();
        int k = 0;
        String column = tripPurpose + ".nonHh";
        while (randomChoice3 < travelPartyProbabilities.getIndexedValueAt(k + 1, column) && k < 10)
            k++;
        return k;
    }


}





