package de.tum.bgu.msm.longDistance.data.airport;

import org.apache.log4j.Logger;

public class AirportDataManager {

    private final static Logger logger = Logger.getLogger(AirportDataManager.class);

    private final AirportData airportData;

    public AirportDataManager(AirportData airportData){
        this.airportData = airportData;
    }

    public Airport getAirportFromId(int airportId){
        return airportData.getAirport(airportId);
    }

    public Flight getFlightFromId(int flightId){
        return airportData.getFlight(flightId);
    }

    public AirLeg getAirRouteFromId(int routeId){
        return airportData.getAirRoute(routeId);
    }


}
