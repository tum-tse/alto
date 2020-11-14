package de.tum.bgu.msm.longDistance.data.zoneSystem;



/**
 * Created by carlloga on 8/10/2016.
 */
public class ZoneGermany implements Zone {
    private int id;
    private int population = 0;
    private int employment = 0;
    private int households = 0;
    private ZoneTypeGermany zoneType;
    private double accessibility;
    private int combinedZoneId;
    private long staticAttraction;
    private int closestAirport = 0;
    private int closestHub = 0;
    private int closestMainAirport = 0;


    public ZoneGermany(int id, int population, int employment, ZoneTypeGermany zoneType, int combinedZoneId) {
        this.id = id;
        this.population = population;
        this.employment = employment;
        this.zoneType = zoneType;
        this.combinedZoneId = combinedZoneId;
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

    public int getCombinedZoneId() {
        return combinedZoneId;
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
}

