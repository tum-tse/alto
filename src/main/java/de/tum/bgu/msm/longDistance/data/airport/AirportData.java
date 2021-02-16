package de.tum.bgu.msm.longDistance.data.airport;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AirportData {

    private final Map<Integer, Airport> airports = new ConcurrentHashMap<>();
    private final Map<Integer, Flight> flights = new ConcurrentHashMap<>();
    private final Map<Integer, AirLeg> airRoutes = new ConcurrentHashMap<>();

    public Airport getAirport(int airportId){return airports.get(airportId);}
    public Flight getFlight(int airportId){return flights.get(airportId);}
    public AirLeg getAirRoute(int airportId){return airRoutes.get(airportId);}


}
