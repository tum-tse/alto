package de.tum.bgu.msm.longDistance.data.airport;

import de.tum.bgu.msm.longDistance.data.Id;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Set;

public class Flight implements Id {

    private static final Logger logger = Logger.getLogger(de.tum.bgu.msm.longDistance.data.airport.Airport.class);

    private final int id;
    private final Airport origin;
    private final Airport destination;
    private final List< AirLeg> legs;
    private float time;


    public Flight(int id, Airport origin, Airport destination, List<AirLeg> legs) {
        this.id = id;
        this.origin = origin;
        this.destination = destination;
        this.legs = legs;
    }

    public Airport getOrigin() {
        return origin;
    }

    public Airport getDestination() {
        return destination;
    }

    @Override
    public int getId() {
        return id;
    }

    public List<AirLeg> getLegs() {
        return legs;
    }

    public float getTime() {
        return time;
    }

    public void setTime(float time) {
        this.time = time;
    }
}
