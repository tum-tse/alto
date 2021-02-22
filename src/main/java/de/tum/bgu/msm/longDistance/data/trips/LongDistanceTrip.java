package de.tum.bgu.msm.longDistance.data.trips;

import java.util.Map;

public interface LongDistanceTrip {

    Mode getMode();

    Type getTripState();

    Purpose getTripPurpose();

    Map<Pollutant, Float> getEmissions();

    Float getCO2emissions();
}
