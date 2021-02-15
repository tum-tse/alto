package de.tum.bgu.msm.longDistance.data.airport;

import de.tum.bgu.msm.longDistance.data.Id;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneGermany;
import org.apache.log4j.Logger;

import java.util.LinkedHashSet;
import java.util.Set;

public class Airport implements Id {

    private static final Logger logger = Logger.getLogger(de.tum.bgu.msm.longDistance.data.airport.Airport.class);

    private final int id;
    private final String name;
    private final ZoneGermany zone;
    private final AirportType airportType;

    private Set<Flight> flights = new LinkedHashSet<>();
    private Set<AirLeg> routes = new LinkedHashSet<>();
    private int mainAirportId;
    private int hubAirportId;
    private int idOpenFlight;
    private int transferTime;
    private AirportDestinationType airportDestinationType;


    public Airport(int id, String name, ZoneGermany zone, AirportType airportType) {
        this.id = id;
        this.name = name;
        this.zone = zone;
        this.airportType = airportType;
    }

    public String getName() {
        return name;
    }

    public ZoneGermany getZone() {
        return zone;
    }

    public AirportType getAirportType() {
        return airportType;
    }

    public Set<Flight> getFlights() {
        return flights;
    }

    public void setFlights(Set<Flight> flights) {
        this.flights = flights;
    }

    public Set<AirLeg> getRoutes() {
        return routes;
    }

    public void setRoutes(Set<AirLeg> routes) {
        this.routes = routes;
    }

    public int getMainAirportId() {
        return mainAirportId;
    }

    public void setMainAirportId(int mainAirportId) {
        this.mainAirportId = mainAirportId;
    }

    public int getHubAirportId() {
        return hubAirportId;
    }

    public void setHubAirportId(int hubAirportId) {
        this.hubAirportId = hubAirportId;
    }

    @Override
    public int getId() {
        return id;
    }

    public int getIdOpenFlight() {
        return idOpenFlight;
    }

    public void setIdOpenFlight(int idOpenFlight) {
        this.idOpenFlight = idOpenFlight;
    }

    public int getTransferTime() {
        return transferTime;
    }

    public void setTransferTime(int transferTime) {
        this.transferTime = transferTime;
    }

    public AirportDestinationType getAirportDestinationType() {
        return airportDestinationType;
    }

    public void setAirportDestinationType(AirportDestinationType airportDestinationType) {
        this.airportDestinationType = airportDestinationType;
    }
}
