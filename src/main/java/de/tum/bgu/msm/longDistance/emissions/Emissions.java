package de.tum.bgu.msm.longDistance.emissions;

import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.ModelComponent;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.trips.*;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;


public class Emissions implements ModelComponent {

    static Logger logger = Logger.getLogger(Emissions.class);
    private TableDataSet coefficients;
    private int[] distanceBins;

    @Override
    public void setup(JSONObject prop, String inputFolder, String outputFolder) {
        coefficients = Util.readCSVfile(inputFolder+JsonUtilMto.getStringProp(prop, "emissions.coef_file"));
        coefficients.buildStringIndex(2);
        logger.info("Domestic DC set up");
    }

    @Override
    public void load(DataSet dataSet) {

    }

    @Override
    public void run(DataSet dataSet, int nThreads) {

        distanceBins = dataSet.getDistanceBins();
        ArrayList<LongDistanceTrip> trips = dataSet.getTripsofPotentialTravellers();
        logger.info("Running emission calculator for " + trips.size() + " trips");

        trips.parallelStream().forEach(tripFromArray -> {
            LongDistanceTripGermany trip = (LongDistanceTripGermany) tripFromArray;
            calculateEmissions(trip);
            if ((ModeGermany) trip.getMode() != null) {
                updateTripsByDistance(dataSet, trip);
            }
        });
        logger.info("Finished emission calculator");

    }

    private void calculateEmissions(LongDistanceTripGermany t) {
        ModeGermany mode = (ModeGermany) t.getMode();
        if (mode != null) {
            float distance = t.getAutoTravelDistance() / 1000; // convert to km
            HashMap<Pollutant, Float> emissions = new HashMap<>();
            for (Pollutant pollutant : Pollutant.values()) {
                float emissionPerTrip = 0;
                if (t.getTripState().equals(TypeGermany.AWAY)) {
                    emissionPerTrip = 0; // have not travelled
                } else {
                    String columnModePollutant = mode.toString() + "." + pollutant.toString();
                    float emissionFactor = (float) (coefficients.getStringIndexedValueAt("alpha", columnModePollutant) *
                            Math.pow(distance, coefficients.getStringIndexedValueAt("beta", columnModePollutant)));
                    if (t.getTripState().equals(TypeGermany.OVERNIGHT)) {
                        emissionPerTrip = emissionFactor * distance;
                    } else if (t.getTripState().equals(TypeGermany.DAYTRIP)) {
                        emissionPerTrip = emissionFactor * distance * 2; //also account for the emissions of the return trip
                    }
                }
                emissions.put(pollutant, emissionPerTrip);
            }
            t.setEmissions(emissions);
        } else {
            HashMap<Pollutant, Float> emissions = new HashMap<>();
            for (Pollutant pollutant : Pollutant.values()) {
                float emissionPerTrip = 0;
                emissions.put(pollutant, emissionPerTrip);
            }
            t.setEmissions(emissions);
        }
    }


    private void updateTripsByDistance(DataSet dataSet, LongDistanceTrip t){
        double autoDistance = dataSet.getDistanceMatrix().get(t.getMode()).getValueAt(((LongDistanceTripGermany) t).getOrigZone().getId(), ((LongDistanceTripGermany) t).getDestZone().getId()) / 1000;
        boolean conditionNotMet = true;
        int distanceT = 0;
        while (conditionNotMet && distanceT < distanceBins.length){
            if (autoDistance > distanceBins[distanceT]){
                distanceT++;
            } else {
                conditionNotMet = false;
            }
        }
        distanceT = Math.min(distanceT, distanceBins.length - 1);
        float emissionsByDistance = dataSet.getCo2EmissionsByModeByScenarioByDistance().get(dataSet.getScenario()).get(t.getTripState()).get(t.getTripPurpose()).get(t.getMode()).get(distanceBins[distanceT]);
        dataSet.getCo2EmissionsByModeByScenarioByDistance().get(dataSet.getScenario()).get(t.getTripState()).get(t.getTripPurpose()).get(t.getMode()).put(distanceBins[distanceT], emissionsByDistance + t.getCO2emissions());
    }
}
