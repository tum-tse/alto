package de.tum.bgu.msm.longDistance.tripGeneration;

import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.LDModelGermany;
import de.tum.bgu.msm.longDistance.LDModelOntario;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.sp.Person;
import de.tum.bgu.msm.longDistance.data.sp.PersonOntario;
import de.tum.bgu.msm.longDistance.data.trips.*;
import de.tum.bgu.msm.longDistance.destinationChoice.IntOutboundDestinationChoice;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Germany Model
 * Module to simulate long-distance travel
 * Author: Ana Moreno, Technische Universität München (TUM), ana.moreno@tum.de
 * Date: 17 December 2020
 * Version 1
 * Adapted from Ontario Provincial Model
 */
public class InternationalTripGenerationGermany {

    private static Logger logger = Logger.getLogger(InternationalTripGenerationGermany.class);
    private final JSONObject prop;
    private Map<Purpose, Map<Type, Double>> sumProbabilities;
    private Map<Purpose, Map<Type, Map<Integer, Double>>> probabilityMatrix;
    private TableDataSet travelPartyProbabilities;
    private TableDataSet internationalTripRates;


    private TableDataSet originCombinedZones;

    private DataSet dataSet;
    private IntOutboundDestinationChoice intOutboundDestinationChoiceInstance;
    private AtomicInteger atomicInteger;


    public InternationalTripGenerationGermany(JSONObject prop, String inputFolder, String outputFolder) {
//        this.synPop = synPop;
//        this.rb = rb;
        this.prop = prop;
        this.intOutboundDestinationChoiceInstance = new IntOutboundDestinationChoice(prop);


        //String internationalTriprates = rb.getString("int.trips");
        internationalTripRates = Util.readCSVfile(inputFolder + JsonUtilMto.getStringProp(prop,"trip_generation.international.rates_file"));
        internationalTripRates.buildIndex(internationalTripRates.getColumnPosition("tripState"));

        //String intTravelPartyProbabilitiesFilename = rb.getString("int.parties");;
        travelPartyProbabilities = Util.readCSVfile(inputFolder + JsonUtilMto.getStringProp(prop,"trip_generation.international.party_file"));
        travelPartyProbabilities.buildIndex(travelPartyProbabilities.getColumnPosition("travelParty"));

        sumProbabilities = new HashMap<>();
        probabilityMatrix = new HashMap<>();
    }


    public void load(DataSet dataSet){

        this.dataSet = dataSet;
        //method to calculate the accessibility to US as a measure of the probability of starting and international trip
//        List<String> fromZones;
//        List<String> toZones;
//        fromZones = Arrays.asList("ONTARIO");
//        float alpha = (float) ResourceUtil.getDoubleProperty(rb, "int.access.alpha");
//        float beta = (float) ResourceUtil.getDoubleProperty(rb, "int.access.beta");
//        toZones = Arrays.asList("EXTUS");
//        zonalData.calculateAccessibility(zonalData.getZoneList(), fromZones, toZones, alpha , beta);


        intOutboundDestinationChoiceInstance.load(dataSet);
        originCombinedZones = intOutboundDestinationChoiceInstance.getOrigCombinedZones();


    }

    //method to run the trip generation
    public ArrayList<LongDistanceTripOntario> run() {

      atomicInteger = new AtomicInteger(dataSet.getAllTrips().size() + 1);



        ArrayList<LongDistanceTripOntario> trips = new ArrayList<>();

        //initialize probMatrices
        for (Purpose purpose : PurposeOntario.values()){
            sumProbabilities.put(purpose, new HashMap<>());
            probabilityMatrix.put(purpose, new HashMap<>());
            for (Type type : TypeOntario.values()){
                sumProbabilities.get(purpose).put(type, 0.);
                probabilityMatrix.get(purpose).put(type, new HashMap<>());
            }
        }

        //normalize p(travel) per purpose/state by sum of the probability for each person
        sumProbs();

        //run trip generation
        for (Purpose tripPurpose : PurposeOntario.values()) {
            for (TypeOntario tripState : TypeOntario.values()) {
                int tripCount = 0;
                //get the total number of trips to generate
                int numberOfTrips = (int)(internationalTripRates.getIndexedValueAt(TypeOntario.getIndex(tripState), tripPurpose.toString().toLowerCase())*probabilityMatrix.get(tripPurpose).get(tripState).size());
                //select the travellers - repeat more than once because the two random numbers can be in the interval of 1 person
                for (int iteration = 0; iteration < 5; iteration++){
                    int n = numberOfTrips - tripCount;
                    double[] randomChoice = new double[n];
                    for (int k = 0; k < randomChoice.length; k++) {
                        randomChoice[k] = LDModelGermany.rand.nextDouble()*sumProbabilities.get(tripPurpose).get(tripState);
                    }
                    //sort the matrix for faster lookup
                    Arrays.sort(randomChoice);
                    //look up for the n travellers
                    int p = 0;
                    Iterator<Map.Entry<Integer, Double>> iterator = probabilityMatrix.get(tripPurpose).get(tripState).entrySet().iterator();
                    Map.Entry<Integer, Double> next = iterator.next();
                    double cumulative = next.getValue();

                    for (double randomNumber : randomChoice){
                        while (randomNumber > cumulative && p < probabilityMatrix.get(tripPurpose).get(tripState).size() - 1) {
                            p++;
                            next = iterator.next();
                            cumulative += next.getValue();
                        }
                        PersonOntario pers = (PersonOntario) dataSet.getPersonFromId(next.getKey());

                        if (!pers.isDaytrip() && !pers.isAway() && !pers.isInOutTrip() && pers.getAge() > 17 && tripCount < numberOfTrips) {

                            LongDistanceTripOntario trip = createIntLongDistanceTrip(pers, tripPurpose,tripState, travelPartyProbabilities);
                            trips.add(trip);
                            tripCount++;
                        }
                    }
                    if (numberOfTrips - tripCount == 0){
                        //logger.info("Number of iterations: " + iteration);
                        break;
                    }
                }
                //logger.info(tripCount + " international trips generated in Ontario, with purpose " + tripPurpose + " and state " + tripState);
            }
        }

        probabilityMatrix.clear();
        sumProbabilities.clear();
        return trips;
    }


    private LongDistanceTripOntario createIntLongDistanceTrip(PersonOntario pers, Purpose tripPurpose, Type tripState, TableDataSet travelPartyProbabilities ){

        TypeOntario type = (TypeOntario) tripState;

        switch (type) {
            case AWAY :
                pers.setAway(true);
            case DAYTRIP:
                pers.setDaytrip(true);
            case INOUT:
                pers.setInOutTrip(true);
        }

        ArrayList<Person> adultsHhTravelParty = DomesticTripGeneration.addAdultsHhTravelParty(pers, tripPurpose.toString(), travelPartyProbabilities);
        ArrayList<Person> kidsHhTravelParty = DomesticTripGeneration.addKidsHhTravelParty(pers, tripPurpose.toString(), travelPartyProbabilities);
        ArrayList<Person> hhTravelParty = new ArrayList<>();
        hhTravelParty.addAll(adultsHhTravelParty);
        hhTravelParty.addAll(kidsHhTravelParty);
        int nonHhTravelPartySize = DomesticTripGeneration.addNonHhTravelPartySize(tripPurpose.toString(), travelPartyProbabilities);

        int tripDuration;
        if (pers.isDaytrip()) tripDuration = 0;
        else {
            tripDuration = 1;
        }

        LongDistanceTripOntario trip =  new LongDistanceTripOntario(atomicInteger.getAndIncrement(), pers, true, tripPurpose, tripState, pers.getHousehold().getZone(), tripDuration,
                nonHhTravelPartySize);
        trip.setHhTravelParty(hhTravelParty);

        if ( Util.isPowerOfFour(atomicInteger.get())){
            logger.info("Domestic trips: " + atomicInteger.get());
        }

        return trip;


    }


    public void sumProbs(){
        List <Person> persons = new ArrayList<>(dataSet.getPersons().values());

        //make random list of persons
        Collections.shuffle(persons, LDModelGermany.rand);
        double exponent = 2;

        int p = 0;
        for (Person person : persons) {
            PersonOntario pers = (PersonOntario) person;

            //IntStream.range(0, synPop.getPersons().size()).parallel().forEach(p -> {
            //Person pers = synPop.getPersonFromId(p);
            if (pers.getTravelProbabilities() != null) {
                for (Purpose tripPurpose : PurposeOntario.values()) {
                    for (Type tripState : TypeOntario.values()) {

                        if (pers.isAway() || pers.isDaytrip() || pers.isInOutTrip() || pers.getAge() < 18) {
                            probabilityMatrix.get(tripPurpose).get(tripState).put(pers.getPersonId(),  0.);
                            //cannot be an adult travelling
                        } else {
                            //probabilityMatrix[i][j][p] = pers.getTravelProbabilities()[i][j];
                            //correct here the probability by the accessibility to US - using access at level 2 zone
                            probabilityMatrix.get(tripPurpose).get(tripState).put(pers.getPersonId(), pers.getTravelProbabilities().get(tripPurpose).get(tripState) *
                                    Math.pow(originCombinedZones.getIndexedValueAt(pers.getHousehold().getZone().getCombinedZoneId(), "usAccess"),exponent));
                        }
                        double newValue = sumProbabilities.get(tripPurpose).get(tripState) + probabilityMatrix.get(tripPurpose).get(tripState).get(pers.getPersonId());
                        sumProbabilities.get(tripPurpose).put(tripState, newValue);
                    }
                }
            }
            p++;

        }

        //});
        //logger.info("  sum of probabilities done for " + p + " persons");

    }

}
