package de.tum.bgu.msm.longDistance.timeOfDay;

import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.DataSet;
import de.tum.bgu.msm.longDistance.LDModel;
import de.tum.bgu.msm.longDistance.data.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.ModelComponent;
import de.tum.bgu.msm.longDistance.data.ModeOntario;
import de.tum.bgu.msm.longDistance.data.PurposeOntario;
import de.tum.bgu.msm.longDistance.data.TypeOntario;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TimeOfDayChoice implements ModelComponent {

    private static Logger logger = Logger.getLogger(TimeOfDayChoice.class);
    private int[] departureTimesInHours; //float to use fractions of hour if needed
    private double[] correctionFactorDayTripOutbound;
    private double[] correctionFactorDayTripInbound;
    private Map<String, double[]> probabilities;


    @Override
    public void setup(JSONObject prop, String inputFolder, String outputFolder) {

        departureTimesInHours = JsonUtilMto.getArrayIntProp(prop, "departure_time.intervals");
        correctionFactorDayTripOutbound = JsonUtilMto.getArrayDoubleProp(prop, "departure_time.correction_daytrip");
        correctionFactorDayTripInbound = getOneMinusArray(correctionFactorDayTripOutbound);
        //overnight trips
        probabilities = new HashMap<>();
        probabilities.put("departure.air.all", JsonUtilMto.getArrayDoubleProp(prop, "departure_time.departure_air"));
        probabilities.put("departure.auto.business", JsonUtilMto.getArrayDoubleProp(prop, "departure_time.departure_auto_business"));
        probabilities.put("departure.auto.other", JsonUtilMto.getArrayDoubleProp(prop, "departure_time.departure_auto_other"));

        probabilities.put("arrival.air.all", JsonUtilMto.getArrayDoubleProp(prop, "departure_time.arrival_air"));
        probabilities.put("arrival.auto.business", JsonUtilMto.getArrayDoubleProp(prop, "departure_time.arrival_auto_business"));
        probabilities.put("arrival.auto.other", JsonUtilMto.getArrayDoubleProp(prop, "departure_time.arrival_auto_other"));

    }

    @Override
    public void load(DataSet dataSet) {
    }

    @Override
    public void run(DataSet dataSet, int nThreads) {

        ArrayList<LongDistanceTrip> trips = dataSet.getAllTrips();
        logger.info("Running time-of-day choice for " + trips.size() + " trips");

        trips.parallelStream().forEach(trip -> {
            int departureTime;
            if (trip.getTripState().equals(TypeOntario.AWAY)){
                //away
            } else {
                calculateDepartureTime(trip, convertMode(trip), convertPurpose(trip));
            }
        });
        logger.info("Finished time-of-day choice");
    }

    private void calculateDepartureTime(LongDistanceTrip trip, String mode, String purpose) {

        if( trip.getTripState().equals(TypeOntario.AWAY)){
            //daytrip
            trip.setDepartureTimeInHours(Util.select(multiply(probabilities.get("departure." + mode + "." + purpose),correctionFactorDayTripOutbound), departureTimesInHours));
            trip.setDepartureTimeInHoursSecondSegment(Util.select(multiply(probabilities.get("departure." + mode + "." + purpose),correctionFactorDayTripInbound), departureTimesInHours));

        } else {
            //overnight trip inbound or outbound
            if (LDModel.rand.nextBoolean()) {
                trip.setDepartureTimeInHours(Util.select(probabilities.get("departure." + mode + "." + purpose), departureTimesInHours));
                trip.setReturnOvernightTrip(false);
            } else {
                int arrivalTime = Util.select(probabilities.get("arrival." + mode + "." + purpose), departureTimesInHours) - Math.round(trip.getTravelTimeLevel2())/60;
                trip.setDepartureTimeInHours(arrivalTime);
                trip.setReturnOvernightTrip(true);
            }

        }
    }

    private String convertPurpose(LongDistanceTrip trip) {
        String purpose = "all";
    if (trip.getMode().equals(ModeOntario.AUTO) || trip.getMode().equals(ModeOntario.BUS)) {
            switch ((PurposeOntario) trip.getTripPurpose()) {
                case LEISURE:
                case VISIT:
                    purpose = "other";
                    break;
                case BUSINESS:
                    purpose = "business";
                    break;
            }
        }
        return purpose;

    }

    private String convertMode(LongDistanceTrip trip) {
        String mode = "";
            switch ((ModeOntario) trip.getMode()) {
                case AUTO:
                case BUS:
                    mode = "auto";
                    break;
                case AIR:
                case RAIL:
                    mode = "air";
                    break;
            }

        return mode;

    }



    public double[] multiply(double[] array1, double[] array2){
        double[] array = new double[array1.length];
        for (int i=0; i< array.length; i++){
            array[i] = array1[i]*array2[i];
        }
        return array;
    }

    private double[] getOneMinusArray(double[] correctionFactorDayTripOutbound) {
        double[] array = new double[correctionFactorDayTripOutbound.length];
        for (int i = 0; i < correctionFactorDayTripOutbound.length; i++){
            array[i]  = 1 - correctionFactorDayTripOutbound[i];
        }
        return array;
    }

}
