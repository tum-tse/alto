package de.tum.bgu.msm.longDistance.data;

import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.longDistance.data.airport.AirLeg;
import de.tum.bgu.msm.longDistance.data.airport.Airport;
import de.tum.bgu.msm.longDistance.data.airport.AirportType;
import de.tum.bgu.msm.longDistance.data.airport.Flight;
import de.tum.bgu.msm.longDistance.data.trips.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.data.trips.LongDistanceTripOntario;
import de.tum.bgu.msm.longDistance.data.trips.Mode;
import de.tum.bgu.msm.longDistance.data.sp.Household;
import de.tum.bgu.msm.longDistance.data.sp.Person;
import de.tum.bgu.msm.longDistance.data.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneTypeGermany;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneTypeOntario;
import org.apache.log4j.Logger;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by carlloga on 8/1/2017.
 */
public class DataSet {

    private static Logger logger = Logger.getLogger(DataSet.class);


    //ZONAL DATA
    private Map<Integer, Zone> zones = new HashMap<>();

    //SKIMS GA-zones
    private Matrix autoTravelTime;
    private Matrix autoTravelDistance;

    //SKIMS level-2 zones
    private Map<Mode, Matrix> travelTimeMatrix;;
    private Map<Mode, Matrix> priceMatrix;
    private Map<Mode, Matrix> transferMatrix;
    private Map<Mode, Matrix> distanceMatrix;

    public Map<Mode, Matrix> getTravelTimeMatrix() {
        return travelTimeMatrix;
    }

    public void setTravelTimeMatrix(Map<Mode, Matrix> travelTimeMatrix) {
        this.travelTimeMatrix = travelTimeMatrix;
    }

    public Map<Mode, Matrix> getPriceMatrix() {
        return priceMatrix;
    }

    public void setPriceMatrix(Map<Mode, Matrix> priceMatrix) {
        this.priceMatrix = priceMatrix;
    }

    public Map<Mode, Matrix> getTransferMatrix() {
        return transferMatrix;
    }

    public void setTransferMatrix(Map<Mode, Matrix> transferMatrix) {
        this.transferMatrix = transferMatrix;
    }

    public Map<Mode, Matrix> getFrequencyMatrix() {
        return frequencyMatrix;
    }

    public void setFrequencyMatrix(Map<Mode, Matrix> frequencyMatrix) {
        this.frequencyMatrix = frequencyMatrix;
    }

    private Map<Mode, Matrix> frequencyMatrix;

    //SYNYHETIC POPULATION
    private Map<Integer, Person> persons = new HashMap<>();
    private Map<Integer, Household> households = new HashMap<>();

    //TRIPS
    private ArrayList<LongDistanceTrip> allTrips = new ArrayList<>();

    public Map<Integer, Zone> getZones() {
        return zones;
    }

    public void setZones(Map<Integer, Zone> zones) {
        this.zones = zones;
    }

    public List<Zone> getExternalZones() {
        return zones.values().stream().filter(zone -> !zone.getZoneType().equals(ZoneTypeOntario.ONTARIO)).collect(Collectors.toList());
    }

    public void setAutoTravelTime(Matrix autoTravelTime) {
        this.autoTravelTime = autoTravelTime;
    }

    public void setAutoTravelDistance(Matrix autoTravelDistance) {
        this.autoTravelDistance = autoTravelDistance;
    }

    public Map<Integer, Person> getPersons() {
        return persons;
    }

    public void setPersons(Map<Integer, Person> persons) {
        this.persons = persons;
    }

    public Map<Integer, Household> getHouseholds() {
        return households;
    }

    public void setHouseholds(Map<Integer, Household> households) {
        this.households = households;
    }

    public float getAutoTravelTime(int orig, int dest) {
            return autoTravelTime.getValueAt(orig, dest);
    }

    public float getAutoTravelDistance(int orig, int dest) {
            return autoTravelDistance.getValueAt(orig, dest);
    }

    public Person getPersonFromId(int personId) {
        return persons.get(personId);
    }

    public Household getHouseholdFromId(int hhId) {
        return households.get(hhId);
    }

    public ArrayList<LongDistanceTrip> getAllTrips() {
        return allTrips;
    }

    public Map<Mode, Matrix> getDistanceMatrix() {
        return distanceMatrix;
    }

    public void setDistanceMatrix(Map<Mode, Matrix> distanceMatrix) {
        this.distanceMatrix = distanceMatrix;
    }


    //airports
    private Map<Integer, Airport> airports = new ConcurrentHashMap();
    private Map<Integer, Flight> flights  = new ConcurrentHashMap();
    private Map<Integer, AirLeg> airLegs  = new ConcurrentHashMap();
    private Map<Integer, Airport> airportsWithFlights  = new ConcurrentHashMap();

    public Map<Integer, Airport> getAirportsWithFlights() {
        return airportsWithFlights;
    }

    public void setAirportsWithFlights(Map<Integer, Airport> airportsWithFlights) {
        this.airportsWithFlights = airportsWithFlights;
    }

    public Map<Integer, Airport> getAirports(){return airports;}
    public Map<Integer, Flight> getFlights(){return flights;}
    public Map<Integer, AirLeg> getAirLegs(){return airLegs;}

    public Airport getAirportFromId(int airportId) {
        return airports.get(airportId);
    }

    public AirLeg getAirLegFromId(int airportId) {
        return airLegs.get(airportId);
    }

    public Flight getFligthFromId(int flightId){return flights.get(flightId);}

    public void setAirports(Map<Integer, Airport> airports) {
        this.airports = airports;
    }

    public void setFlights(Map<Integer, Flight> flights) {
        this.flights = flights;
    }

    public void setAirLegs(Map<Integer, AirLeg> legs) {
        this.airLegs = legs;
    }

    public List<Airport> getMainAirports() {
        return airports.values().stream().filter(airport -> !airport.getAirportType().equals(AirportType.FEEDER_ZONE)).collect(Collectors.toList());
    }

    public List<Airport> getGermanHubs() {
        return airports.values().stream().filter(airport -> airport.getAirportType().equals(AirportType.HUB) && airport.getZone().getZoneType().equals(ZoneTypeGermany.GERMANY) ).collect(Collectors.toList());
    }

    public List<Airport> getGermanAirports(){
        return airports.values().stream().filter(airport -> airport.getZone().getZoneType().equals(ZoneTypeGermany.GERMANY)).collect(Collectors.toList());
    }

    public List<Airport> getOverseasAirports(){
        return airports.values().stream().filter(airport -> airport.getZone().getZoneType().equals(ZoneTypeGermany.EXTOVERSEAS)).collect(Collectors.toList());
    }


}
