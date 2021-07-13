package de.tum.bgu.msm.longDistance.trafficAssignment;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

public class TollTravelDisutility implements TravelDisutility {

    public static final double minSpeed_ms = 1.;
    public static final String avoidToll = "avoidTOLL";

    final TravelTime tt;
    private final double FACTOR = 100.;

    public TollTravelDisutility(TravelTime tt) {
        this.tt = tt;
    }

    @Override
    public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
        if ((boolean)(person.getAttributes().getAttribute(avoidToll)) && hasToll(link)){
            return tt.getLinkTravelTime(link, time, person, vehicle) * FACTOR;
        } else {
            return tt.getLinkTravelTime(link, time, person, vehicle);
        }
    }

    public static boolean hasToll(Link link) {
        if (link.getAttributes().getAttribute("type").toString().equalsIgnoreCase("motorway")){
            return true;
        } else {
            return false;
        }
    }

    @Override
    public double getLinkMinimumTravelDisutility(Link link) {
        return link.getLength() / link.getFreespeed();
    }
}
