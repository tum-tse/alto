package de.tum.bgu.msm.longDistance.trafficAssignment;

import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public class TollTravelDisutilityFactory implements TravelDisutilityFactory {
    @Override
    public TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
        return new TollTravelDisutility(timeCalculator);
    }
}
