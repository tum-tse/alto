package de.tum.bgu.msm.longDistance.data.zoneSystem;


import java.awt.geom.Area;

/**
 *
 * Germany Model
 * Class to store zones
 * Author: Ana Moreno, Technical University of Munich (TUM), ana.moreno@tum.de
 * Date: 8 December 2020
 * Version 1
 * Adapted from Ontario. Added features for airport analysis
 * Version 1
 *
 */
public class ZoneGermany implements Zone {
    private int id;
    private int population = 0;
    private int employment = 0;
    private int households = 0;
    private ZoneTypeGermany zoneType;
    private double accessibility;
    private long staticAttraction;
    private int closestAirport = 0;
    private int closestHub = 0;
    private int closestMainAirport = 0;
    private int area;
    private AreaTypeGermany areatype;
    private int hotels = 0;
    private float timeToLongDistanceRail = 0;



    private boolean emptyZone = false;

    public ZoneGermany(int id, int population, int employment, ZoneTypeGermany zoneType, int area,
                       AreaTypeGermany areaType, boolean emptyZone) {
        this.id = id;
        this.population = population;
        this.employment = employment;
        this.zoneType = zoneType;
        this.area = area;
        this.areatype = areaType;
        this.emptyZone = emptyZone;
    }

    @Override
    public int getId() {
        return id;
    }

    public int getPopulation() {
        return population;
    }

    public int getEmployment() {
        return employment;
    }

    public int getHouseholds() {
        return households;
    }

    @Override
    public ZoneTypeGermany getZoneType() {
        return zoneType;
    }

    public double getAccessibility() {
        return accessibility;
    }

    public void addPopulation (int population){
        this.population += population;
}

    public void addHouseholds (int households){
        this.households += households;
    }

    public void setEmployment(int employment) {
        this.employment = employment;
    }

    public void addEmployment(int employment) {
        this.employment += employment;
    }

    public void setAccessibility(double accessibility) {
        this.accessibility = accessibility;
    }

    public long getStaticAttraction() {
        return staticAttraction;
    }

    public void setStaticAttraction(long staticAttraction) {
        this.staticAttraction = staticAttraction;
    }

    public int getClosestAirportId() {
        return closestAirport;
    }

    public void setClosestAirportId(int closestAirport) {
        this.closestAirport = closestAirport;
    }

    public int getClosestHubId() {
        return closestHub;
    }

    public void setClosestHubId(int closestHub) {
        this.closestHub = closestHub;
    }

    public int getClosestMainAirportId() {
        return closestMainAirport;
    }

    public void setClosestMainAirportId(int closestMainAirport) {
        this.closestMainAirport = closestMainAirport;
    }

    public int getArea() {
        return area;
    }

    public AreaTypeGermany getAreatype() {
        return areatype;
    }

    public int getHotels() {
        return hotels;
    }

    public void setHotels(int hotels) {
        this.hotels = hotels;
    }

    public float getTimeToLongDistanceRail() {
        return timeToLongDistanceRail;
    }

    public void setTimeToLongDistanceRail(float timeToLongDistanceRail) {
        this.timeToLongDistanceRail = timeToLongDistanceRail;
    }

    public boolean getEmptyZone() {
        return emptyZone;
    }

    public void setEmptyZone(boolean emptyZone) {
        this.emptyZone = emptyZone;
    }
}

