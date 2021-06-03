package de.tum.bgu.msm.longDistance.timeOfDay;

import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.LDModelGermany;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.trips.*;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.pb.common.datafile.TableDataSet;

public class TimeOfDayChoiceGermany implements TimeOfDayChoice {

    private static Logger logger = Logger.getLogger(TimeOfDayChoiceGermany.class);
    private TableDataSet ToD_Germany;
    private int[] departureTimesInMin;


    @Override
    public void setup(JSONObject prop, String inputFolder, String outputFolder) {

        ToD_Germany = Util.readCSVfile(inputFolder + JsonUtilMto.getStringProp(prop, "timeOfDay_choice.domestic.germany.coef_file"));
        departureTimesInMin = ToD_Germany.getColumnAsInt(1);
        logger.info("Domestic Time of Day set up");
    }

    @Override
    public void load(DataSet dataSet) {
    }

    @Override
    public void run(DataSet dataSet, int nThreads) {

        ArrayList<LongDistanceTrip> trips = dataSet.getTripsofPotentialTravellers();
        logger.info("Running time-of-day choice for " + trips.size() + " trips");

        trips.parallelStream().forEach(t -> {
            if (t.getTripState().equals(TypeGermany.AWAY)) {
                //away
            } else {
                calculateDepartureTime(t);
            }
        });
        logger.info("Finished time-of-day choice");
    }

    private void calculateDepartureTime(LongDistanceTrip tripToCast) {

        LongDistanceTripGermany trip = (LongDistanceTripGermany) tripToCast;
        if (trip.getTripState().equals(TypeGermany.DAYTRIP)) {
            if (trip.getMode() != null) {
                //daytrip
                String coefficientColumn;
                coefficientColumn = "depart." + trip.getMode() + "." + trip.getTripPurpose() + ".day.outbound";
                trip.setDepartureTimesInMin(Util.selectGermany(ToD_Germany.getColumnAsDouble(coefficientColumn), departureTimesInMin));

                coefficientColumn = "depart." + trip.getMode() + "." + trip.getTripPurpose() + ".day.inbound";
                trip.setDepartureTimeInHoursSecondSegment(Util.selectGermany(ToD_Germany.getColumnAsDouble(coefficientColumn), departureTimesInMin));

            }
        } else if (trip.getTripState().equals(TypeGermany.OVERNIGHT)) {
            if (trip.getMode() != null) {
                //overnight trip outbound
                boolean outbound;
                if (LDModelGermany.rand.nextFloat() < 0.5) {
                    outbound = true;
                    String coefficientColumn = "depart." + trip.getMode() + "." + trip.getTripPurpose() + ".OV.outbound";
                    trip.setDepartureTimesInMin(Util.selectGermany(ToD_Germany.getColumnAsDouble(coefficientColumn), departureTimesInMin));
                    trip.setReturnOvernightTrip(false);
                } else {
                    outbound = false;
                    String coefficientColumn = "depart." + trip.getMode() + "." + trip.getTripPurpose() + ".OV.inbound";
                    trip.setDepartureTimesInMin(Util.selectGermany(ToD_Germany.getColumnAsDouble(coefficientColumn), departureTimesInMin));
                    trip.setReturnOvernightTrip(true);
                }
            } else { // AWAY
                int arrivalTime = 0;
                trip.setDepartureTimesInMin(arrivalTime);
                trip.setReturnOvernightTrip(true);
            }
        }
    }

    public double[] multiply(double[] array1, double[] array2) {
        double[] array = new double[array1.length];
        for (int i = 0; i < array.length; i++) {
            array[i] = array1[i] * array2[i];
        }
        return array;
    }

    private double[] getOneMinusArray(double[] correctionFactorDayTripOutbound) {
        double[] array = new double[correctionFactorDayTripOutbound.length];
        for (int i = 0; i < correctionFactorDayTripOutbound.length; i++) {
            array[i] = 1 - correctionFactorDayTripOutbound[i];
        }
        return array;
    }

}
