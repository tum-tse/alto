package de.tum.bgu.msm.longDistance.destinationChoice;

//import com.pb.common.datafile.TableDataSet;
//import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.common.datafile.TableDataSet;
import de.tum.bgu.msm.common.matrix.Matrix;
import de.tum.bgu.msm.longDistance.LDModelGermany;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.trips.*;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneGermany;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneType;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneTypeGermany;
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

public class DaytripDestinationChoiceGermany implements DestinationChoiceModule {
    private ResourceBundle rb;
    private static Logger logger = Logger.getLogger(DaytripDestinationChoiceGermany.class);
    public static int choice_set_size;
    public static int longDistanceThreshold;
    public static float scaleFactor;
    private TableDataSet coefficients;
    protected Matrix autoDist;
    private Map<Type, Map<ZoneType, Map<Purpose, Double>>> calibrationDaytripDcMatrix;
    private int[] destinations;
    private DataSet dataSet;
    private boolean calibrationDaytripDC;

    public DaytripDestinationChoiceGermany(JSONObject prop, String inputFolder) {
        coefficients = Util.readCSVfile(inputFolder + JsonUtilMto.getStringProp(prop, "destination_choice.daytrip.coef_file"));
        coefficients.buildStringIndex(1);

        choice_set_size = JsonUtilMto.getIntProp(prop, "destination_choice.choice_set_size");
        longDistanceThreshold = JsonUtilMto.getIntProp(prop, "threshold_long_distance");
        scaleFactor = JsonUtilMto.getFloatProp(prop, "synthetic_population.scale_factor");
        this.calibrationDaytripDcMatrix = new HashMap<>();
        calibrationDaytripDC = JsonUtilMto.getBooleanProp(prop,"destination_choice.calibration.daytrip");
        logger.info("Daytrip DC set up");

    }

    public void load(DataSet dataSet) {

        autoDist = dataSet.getDistanceMatrix().get(ModeGermany.AUTO);
        destinations = dataSet.getZones().keySet().stream().mapToInt(Integer::intValue).toArray();
        this.dataSet = dataSet;


        this.calibrationDaytripDcMatrix.put(TypeGermany.DAYTRIP, new HashMap<>());
        this.calibrationDaytripDcMatrix.get(TypeGermany.DAYTRIP).putIfAbsent(ZoneTypeGermany.GERMANY,new HashMap<>());
        for (Purpose purpose : PurposeGermany.values()){
            this.calibrationDaytripDcMatrix.get(TypeGermany.DAYTRIP).get(ZoneTypeGermany.GERMANY).putIfAbsent(purpose,1.0);
        }


        logger.info("Daytrip DC loaded");

    }

    @Override
    public int selectDestination(LongDistanceTrip trip) {
        LongDistanceTripGermany t = (LongDistanceTripGermany) trip;
        return selectDestination(t, dataSet);
    }


    //given a trip, calculate the utility of each destination
    public int selectDestination(LongDistanceTripGermany trip, DataSet dataSet) {

        int[] alternatives = dataSet.getZones().keySet().stream().mapToInt(u ->u).toArray(); //select all the zones

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
        ZoneGermany destinationZone = (ZoneGermany) dataSet.getZones().get(destination);
        boolean populatedZone = !destinationZone.getEmptyZone();
        boolean isOverseas = destinationZone.getZoneType().equals(ZoneTypeGermany.EXTOVERSEAS);

        if(isOverseas){
            return Double.NEGATIVE_INFINITY;
        }
        else if (distance > longDistanceThreshold && populatedZone) {

            double population = destinationZone.getPopulation();
            if(population<=0){
                population=0;
            }

            double employment = destinationZone.getEmployment();
            if(employment<=0){
                employment=0;
            }

            double touristsAtHotel = destinationZone.getTouristsAtHotel();
            if(touristsAtHotel<=0){
                touristsAtHotel=0;
            }

            Purpose tripPurpose = trip.getTripPurpose();
            TypeGermany tripState = (TypeGermany) trip.getTripState();
            //Coefficients
            String coefficientColumn = tripState + "." + tripPurpose;
            double b_distance_log = coefficients.getStringIndexedValueAt("log_distance", coefficientColumn);
            double b_popEmployment = coefficients.getStringIndexedValueAt("popEmployment", coefficientColumn);
            double b_touristAtHotel = coefficients.getStringIndexedValueAt("guest", coefficientColumn);
            double k_calibration = coefficients.getStringIndexedValueAt("k_calibration", coefficientColumn);

            //log conversions
            double log_distance = distance > 0 ? Math.log10(distance) : 0;

            if (calibrationDaytripDC) {
                k_calibration = k_calibration * calibrationDaytripDcMatrix.get(tripState).get(ZoneTypeGermany.GERMANY).get(tripPurpose);
            }

            double u = b_distance_log * k_calibration * log_distance +
                       b_touristAtHotel * Math.pow(touristsAtHotel / 1000, 0.01) +  //touristsAtHotel in thousands
                       b_popEmployment * Math.pow((population + employment) / 1000000, 0.01);

            return u;
        } else {
            return Double.NEGATIVE_INFINITY;
        }
    }

    public Map<Type, Map<ZoneType, Map<Purpose, Double>>> getDomesticDcCalibration() {
        return calibrationDaytripDcMatrix;
    }

    public void updateDaytripDcCalibration(Map<Type, Map<ZoneType, Map<Purpose, Double>>> updatedMatrix) {
        for (Purpose purpose : PurposeGermany.values()){
            double newValue = calibrationDaytripDcMatrix.get(TypeGermany.DAYTRIP).get(ZoneTypeGermany.GERMANY).get(purpose) * updatedMatrix.get(TypeGermany.DAYTRIP).get(ZoneTypeGermany.GERMANY).get(purpose);
            calibrationDaytripDcMatrix.get(TypeGermany.DAYTRIP).get(ZoneTypeGermany.GERMANY).put(purpose, newValue);
            System.out.println("k-factor: " + TypeGermany.DAYTRIP + "\t" + ZoneTypeGermany.GERMANY + "\t" + purpose + "\t" + calibrationDaytripDcMatrix.get(TypeGermany.DAYTRIP).get(ZoneTypeGermany.GERMANY).get(purpose));
        }
    }

    @Deprecated
    public Matrix getAutoDist() {
        return autoDist;
    }
}

