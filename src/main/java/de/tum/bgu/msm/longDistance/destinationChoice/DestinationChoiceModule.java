package de.tum.bgu.msm.longDistance.destinationChoice;

import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.trips.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.data.trips.LongDistanceTripOntario;

public interface DestinationChoiceModule {
    void load(DataSet dataSet);

    int selectDestination(LongDistanceTrip trip);
}
