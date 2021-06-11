package de.tum.bgu.msm.longDistance.trafficAssignment;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

public class TollTravelDisutility implements TravelDisutility {


    final TravelTime tt;
    public TollTravelDisutility(TravelTime tt) {
        this.tt = tt;
    }

    @Override
    public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
        if ((boolean)(person.getAttributes().getAttribute("TOLL")) && hasToll(link)){
            return Double.POSITIVE_INFINITY;
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
