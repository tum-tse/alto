package de.tum.bgu.msm.longDistance.tripGeneration;


import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.LDModelGermany;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.sp.*;
import de.tum.bgu.msm.longDistance.data.trips.*;
import de.tum.bgu.msm.longDistance.data.zoneSystem.AreaTypeGermany;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Germany Model
 * Module to simulate long-distance travel
 * Author: Ana Moreno, Technische Universität München (TUM), ana.moreno@tum.de
 * Date: 11 December 2020
 * Version 1
 * Adapted from Ontario Provincial Model
 */

public class DomesticTripGenerationGermany {

    private DataSet dataSet;

    static Logger logger = Logger.getLogger(DomesticTripGenerationGermany.class);
    private ResourceBundle rb;
    private JSONObject prop;

    private TableDataSet tripGenerationCoefficients;

    private AtomicInteger atomicInteger = new AtomicInteger(0);



    public DomesticTripGenerationGermany(JSONObject prop, String inputFolder, String outputFolder) {
        this.rb = rb;
        this.prop = prop;

        tripGenerationCoefficients = Util.readCSVfile(inputFolder + JsonUtilMto.getStringProp(prop,"trip_generation.domestic.coef_file"));
        tripGenerationCoefficients.buildIndex(tripGenerationCoefficients.getColumnPosition("factor"));
        tripGenerationCoefficients.buildStringIndex(tripGenerationCoefficients.getColumnPosition("factorName"));


    }



    public void load(DataSet dataSet){

        this.dataSet = dataSet;
    }


    public ArrayList<LongDistanceTripGermany> run() {
        ArrayList<LongDistanceTripGermany> trips = new ArrayList<>();

        //initialize utility vectors
        Map<Type, Double> expUtilities = new HashMap<>();
        double doNotTravelExpUtility = 1; //base case - or do not travel

        //this option may give randomness to the results
        //synPop.getHouseholds().forEach(hhold -> {
          for (Household hhold : dataSet.getHouseholds().values()) {

            //pick and shuffle the members of the household
            ArrayList<Person> membersList = new ArrayList<>(Arrays.asList(((HouseholdGermany) hhold).getPersonsOfThisHousehold()));
            Collections.shuffle(membersList, LDModelGermany.rand);

            for (Person person : membersList) {
                //array to store 3 x 3 trip probabilities for later use in international

                PersonGermany pers = (PersonGermany) person;

                Map<Purpose, Map<Type ,Double>> tgProbabilities = new HashMap<>();

                if (!pers.isAway() && !pers.isDaytrip() && !pers.isInOutTrip() && pers.getAge() > 17) {

                    for (Purpose tripPurpose : PurposeGermany.values()) {
                        tgProbabilities.put(tripPurpose, new HashMap<>());

                        for (Type tripState : TypeGermany.values()) {
                            //expUtilities[tripStates.indexOf(tripState)] = Math.exp(estimateMlogitUtility(personDescription, tripPurpose, tripState, tripGenerationCoefficients));
                            expUtilities.put(tripState, Math.exp(calculateUtility(pers, tripPurpose.toString(), tripState.toString())));
                        }

                        double denominator = expUtilities.values().stream().mapToDouble(Double::doubleValue).sum() + doNotTravelExpUtility;
                        Map<Type, Double> probabilities = new HashMap<>();
                        expUtilities.forEach((x,y)-> probabilities.put(x,y/denominator));


                        //store the probability for later int trips
                        for (Type tripState : TypeGermany.values()) {
                            tgProbabilities.get(tripPurpose).put(tripState, probabilities.get(tripState));
                        }
                        //select the trip state
                        double randomNumber1 = LDModelGermany.rand.nextDouble();
                        int tripStateChoice = 3;
                        Type tripState = null;

                        if (randomNumber1 < probabilities.get(TypeGermany.AWAY)) {
                            tripState = TypeGermany.AWAY;
                            pers.setAway(true);
                        } else if (randomNumber1 < probabilities.get(TypeGermany.AWAY) + probabilities.get(TypeGermany.DAYTRIP)) {
                            tripState = TypeGermany.DAYTRIP;
                            pers.setDaytrip(true);
                        } else if (randomNumber1 < probabilities.get(TypeGermany.AWAY) + probabilities.get(TypeGermany.DAYTRIP) + probabilities.get(TypeGermany.OVERNIGHT)) {
                            tripState = TypeGermany.OVERNIGHT;
                            pers.setInOutTrip(true);
                        }

                        if (tripState != null) {
                            LongDistanceTripGermany trip = createLongDistanceTrip(pers, tripPurpose, tripState);
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

    public double calculateUtility(PersonGermany pers, String tripPurpose, String tripState) {


        int holiday = JsonUtilMto.getBooleanProp(prop, "holiday" )? 0:1;


        //read coefficients
        String coefficientColumn = tripState + "." + tripPurpose;

        double intercept = tripGenerationCoefficients.getStringIndexedValueAt("(intercept)", coefficientColumn);
        double b_autos = tripGenerationCoefficients.getStringIndexedValueAt("autos", coefficientColumn);
        double b_econStMedium = tripGenerationCoefficients.getStringIndexedValueAt("economicStatusMedium", coefficientColumn);
        double b_econStHigh = tripGenerationCoefficients.getStringIndexedValueAt("economicStatusHigh", coefficientColumn);
        double b_econStVeryHigh = tripGenerationCoefficients.getStringIndexedValueAt("economicStatusVeryHigh", coefficientColumn);
        double b_adult18_60 = tripGenerationCoefficients.getStringIndexedValueAt("adult18_60", coefficientColumn);
        double b_olderThan60 = tripGenerationCoefficients.getStringIndexedValueAt("olderThan60", coefficientColumn);
        double b_male = tripGenerationCoefficients.getStringIndexedValueAt("male", coefficientColumn);
        double b_holiday = tripGenerationCoefficients.getStringIndexedValueAt("holiday", coefficientColumn);
        double b_employed = tripGenerationCoefficients.getStringIndexedValueAt("employed", coefficientColumn);
        double b_student = tripGenerationCoefficients.getStringIndexedValueAt("student", coefficientColumn);
        double b_distanceLog = tripGenerationCoefficients.getStringIndexedValueAt("log_distance", coefficientColumn);
        double b_midSizeCity = tripGenerationCoefficients.getStringIndexedValueAt("urban", coefficientColumn);
        double b_ruralOrTown = tripGenerationCoefficients.getStringIndexedValueAt("rural", coefficientColumn);

        HouseholdGermany hh = pers.getHousehold();

        return intercept +
                b_autos * hh.getHhAutos() +
                b_econStMedium * Boolean.compare(hh.getEconomicStatus().equals(EconomicStatus.MEDIUM), false) +
                b_econStHigh * Boolean.compare(hh.getEconomicStatus().equals(EconomicStatus.HIGH), false) +
                b_econStVeryHigh * Boolean.compare(hh.getEconomicStatus().equals(EconomicStatus.VERYHIGH), false) +
                b_adult18_60 * Boolean.compare(pers.getAge() < 60 && pers.getAge() >= 18, false) +
                b_olderThan60 * Boolean.compare(pers.getAge() > 60, false) +
                b_male * Boolean.compare(pers.isFemale(), true) +
                b_holiday * holiday +
                b_employed * Boolean.compare(pers.isEmployed(), false) +
                b_student * Boolean.compare(pers.isStudent(), false) +
                b_distanceLog * Math.log(hh.getZone().getTimeToLongDistanceRail() / 60) + //convert travel time in minutes
                b_midSizeCity * Boolean.compare(hh.getZone().getAreatype().equals(AreaTypeGermany.MEDIUM_SIZED_CITY), false) +
                b_ruralOrTown * Boolean.compare(hh.getZone().getAreatype().equals(AreaTypeGermany.RURAL), false) +
                b_ruralOrTown * Boolean.compare(hh.getZone().getAreatype().equals(AreaTypeGermany.TOWN), false);


    }


    private LongDistanceTripGermany createLongDistanceTrip(PersonGermany pers, Purpose tripPurpose, Type tripState) {


        LongDistanceTripGermany trip = new LongDistanceTripGermany(atomicInteger.getAndIncrement(), pers, false, tripPurpose, tripState,
                pers.getHousehold().getZone());

        if (  Util.isPowerOfFour(atomicInteger.get())){
            logger.info("Domestic trips: " + atomicInteger.get());
        }


        return trip;

    }


}





