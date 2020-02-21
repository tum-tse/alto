package de.tum.bgu.msm.longDistance.tripGeneration;

import de.tum.bgu.msm.longDistance.DataSet;
import de.tum.bgu.msm.longDistance.data.trips.LongDistanceTrip;

import java.util.ArrayList;

public interface TripGenerationModule {

    void load(DataSet dataSet);

    ArrayList<LongDistanceTrip> run();
}
