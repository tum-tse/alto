package de.tum.bgu.msm.longDistance.data.airport;

import de.tum.bgu.msm.longDistance.data.Id;
import org.apache.log4j.Logger;

public class AirLeg implements Id {

    private static final Logger logger = Logger.getLogger(de.tum.bgu.msm.longDistance.data.airport.Airport.class);

    private final int id;
    private final Airport origin;
    private final Airport destination;
    private float distance;
    private float time;
    private float cost;


    public AirLeg(int id, Airport origin, Airport destination) {
        this.id = id;
        this.origin = origin;
        this.destination = destination;
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

    public float getFrequency() {
        return distance;
    }

    public void setFrequency(float distance) {
        this.distance = distance;
    }

    public float getTime() {
        return time;
    }

    public void setTime(float time) {
        this.time = time;
    }

    public float getCost() {
        return cost;
    }

    public void setCost(float cost) {
        this.cost = cost;
    }
}
