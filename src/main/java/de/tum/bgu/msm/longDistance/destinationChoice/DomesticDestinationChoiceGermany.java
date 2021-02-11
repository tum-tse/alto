package de.tum.bgu.msm.longDistance.destinationChoice;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.LDModelGermany;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.trips.*;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneGermany;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.*;

/**
 * Germany wide travel demand model
 * Class to read synthetic population
 * Author: Ana Moreno, Technical University of Munich (TUM), ana.moreno@tum.de
 * Date: 8 December 2020
 * Version 1
 * Adapted from Destination Choice Model from Joe, Created by Joe on 26/10/2016.
 */

public class DomesticDestinationChoiceGermany implements DestinationChoiceModule {
    private ResourceBundle rb;
    private static Logger logger = Logger.getLogger(DomesticDestinationChoiceGermany.class);
    public static int choice_set_size;
    public static int longDistanceThreshold;
    public static float scaleFactor;
    private TableDataSet coefficients;
    protected Matrix autoDist;
    private boolean calibration;
    private Map<Purpose, Map<Type, Double>> calibrationDomesticDcMatrix;
    private int[] destinations;
    private DataSet dataSet;
    private boolean calibrationDomesticDC;

    public DomesticDestinationChoiceGermany(JSONObject prop, String inputFolder) {
        coefficients = Util.readCSVfile(inputFolder + JsonUtilMto.getStringProp(prop, "destination_choice.domestic.coef_file"));
        coefficients.buildStringIndex(1);

        choice_set_size = JsonUtilMto.getIntProp(prop, "destination_choice.choice_set_size");
        longDistanceThreshold = JsonUtilMto.getIntProp(prop, "threshold_long_distance");
        scaleFactor = JsonUtilMto.getFloatProp(prop, "synthetic_population.scale_factor");
        //calibration = ResourceUtil.getBooleanProperty(rb,"dc.calibration",false);
        calibration = JsonUtilMto.getBooleanProp(prop, "destination_choice.calibration");
        this.calibrationDomesticDcMatrix = new HashMap<>();
        calibrationDomesticDC = JsonUtilMto.getBooleanProp(prop,"destination_choice.calibration");
        logger.info("Domestic DC set up");

    }

    public void load(DataSet dataSet) {

        autoDist = dataSet.getDistanceMatrix().get(ModeGermany.AUTO);
        destinations = dataSet.getZones().keySet().stream().mapToInt(Integer::intValue).toArray();
        this.dataSet = dataSet;

        for(Purpose purpose : PurposeGermany.values()){
            this.calibrationDomesticDcMatrix.put(purpose, new HashMap<>());
            for (Type tripState : TypeGermany.values()){
                this.calibrationDomesticDcMatrix.get(purpose).putIfAbsent(tripState, 1.0);
            }
        }
        logger.info("Domestic DC loaded");

    }

    @Override
    public int selectDestination(LongDistanceTrip trip) {
        LongDistanceTripGermany t = (LongDistanceTripGermany) trip;
        return selectDestination(t, dataSet);
    }


    //given a trip, calculate the utility of each destination
    public int selectDestination(LongDistanceTripGermany trip, DataSet dataSet) {


        Purpose tripPurpose = trip.getTripPurpose();
        int choiceSet = choice_set_size;
        if (choice_set_size == 10000){
            choiceSet = dataSet.getZones().keySet().size();
        }
        int[] alternatives = new int[choiceSet];
        if (choice_set_size == 10000){
            alternatives = dataSet.getZones().keySet().stream().mapToInt(u ->u).toArray(); //select all the zones
        } else {
            alternatives = selectRandomDestinations(trip.getOrigZone().getId());
        }
        double[] expUtilities = Arrays.stream(alternatives)
                //calculate exp(Ui) for each destination
                .mapToDouble(a -> Math.exp(calculateUtility(trip, a))).toArray();
        //calculate the probability for each trip, based on the destination utilities
        double probability_denominator = Arrays.stream(expUtilities).sum();

        //calculate the probability for each trip, based on the destination utilities
        double[] probabilities = Arrays.stream(expUtilities).map(u -> u / probability_denominator).toArray();

        //choose one destination, weighted at random by the probabilities
        return Util.selectGermany(probabilities, alternatives);
        //return new EnumeratedIntegerDistribution(alternatives, probabilities).sample();

    }

    private int[] selectRandomDestinations(int origin) {
        int[] alternatives = new int[choice_set_size];
        int chosen = 0;
        while(chosen < choice_set_size){
            int r = (int) (destinations.length * LDModelGermany.rand.nextFloat());
            int destination = destinations[r];
            if (autoDist.getValueAt(origin, destination) > longDistanceThreshold * 1000 ){
                alternatives[chosen] = destination;
                chosen++;
            }
        }
        return alternatives;
    }

    private double calculateUtility(LongDistanceTripGermany trip, int destination) {
        // Method to calculate utility of all possible destinations for LongDistanceTrip trip

        int origin = trip.getOrigZone().getId();
        float distance = autoDist.getValueAt(origin, destination) / 1000; //to convert meters to km

        if (distance > longDistanceThreshold) {

            ZoneGermany destinationZone = (ZoneGermany) dataSet.getZones().get(destination);
            double population = destinationZone.getPopulation() * scaleFactor;
            double employment = destinationZone.getEmployment() * scaleFactor;
            double hotels = destinationZone.getHotels();

            Purpose tripPurpose = trip.getTripPurpose();
            TypeGermany tripState = (TypeGermany) trip.getTripState();
            //Coefficients
            String coefficientColumn = tripState + "." + tripPurpose;
            double b_distance_log = coefficients.getStringIndexedValueAt("log_distance", coefficientColumn);
            double b_popEmployment = coefficients.getStringIndexedValueAt("popEmployment", coefficientColumn);
            double b_hotel = coefficients.getStringIndexedValueAt("hotel", coefficientColumn);
            double k_calibration = coefficients.getStringIndexedValueAt("k_calibration", coefficientColumn);

            //log conversions
            double log_distance = distance > 0 ? Math.log10(distance) : 0;

            if (calibrationDomesticDC) {
                k_calibration = k_calibration * calibrationDomesticDcMatrix.get(tripPurpose).get(tripState);
            }

            double u =
                    b_distance_log * k_calibration * log_distance +
                            b_hotel * hotels / 1000 +  //hotels in thousands
                            b_popEmployment * (population + employment) / 1000000; //population and employment in millions

            return u;
        } else {
            return Double.NEGATIVE_INFINITY;
        }
    }

    public Map<Purpose, Map<Type, Double>> getDomesticDcCalibration() {
        return calibrationDomesticDcMatrix;
    }

    public void updateDomesticDcCalibration(Map<Purpose, Map<Type, Double>> updatedMatrix) {

        for (Purpose purpose : PurposeGermany.values()){
            for (Type tripState : TypeGermany.values()){
                double newValue = calibrationDomesticDcMatrix.get(purpose).get(tripState) * updatedMatrix.get(purpose).get(tripState);
                calibrationDomesticDcMatrix.get(purpose).put(tripState, newValue);
                System.out.println("k-factor: " + purpose + "\t" + tripState + "\t" + calibrationDomesticDcMatrix.get(purpose).get(tripState));
            }
        }
    }

    @Deprecated
    public Matrix getAutoDist() {
        return autoDist;
    }
}

