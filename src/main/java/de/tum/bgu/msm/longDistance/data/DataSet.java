package de.tum.bgu.msm.longDistance.data;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.longDistance.data.airport.AirLeg;
import de.tum.bgu.msm.longDistance.data.airport.Airport;
import de.tum.bgu.msm.longDistance.data.airport.AirportType;
import de.tum.bgu.msm.longDistance.data.airport.Flight;
import de.tum.bgu.msm.longDistance.data.grids.Grid;
import de.tum.bgu.msm.longDistance.data.trips.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.data.trips.Mode;
import de.tum.bgu.msm.longDistance.data.sp.Household;
import de.tum.bgu.msm.longDistance.data.sp.Person;
import de.tum.bgu.msm.longDistance.data.trips.Purpose;
import de.tum.bgu.msm.longDistance.data.trips.Type;
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
    private Map<Mode, Matrix> travelTimeMatrix;
    private Map<Mode, Matrix> priceMatrix;
    private Map<Mode, Matrix> transferMatrix;
    private Map<Mode, Matrix> distanceMatrix;
    private Map<Mode, Matrix> tollDistanceMatrix;

    private Map<Mode, Matrix> railAccessDistMatrix; //A
    private Map<Mode, Matrix> railEgressDistMatrix; //A
    private Map<Mode, Matrix> railAccessTimeMatrix; //A
    private Map<Mode, Matrix> railEgressTimeMatrix; //A



    private Matrix airAccessAirportZone;
    private Matrix airEgressAirportZone;

    private double numberOfSubpopulations;
    private int numberOfScenarios;
    private int scenario;
    private int[] distanceBins;
    private Map<Integer, Airport> germanMain;
    private Map<Integer, Airport> overseasAirport;
    private Map<Airport, Map<Airport, Map<String, Integer>>> connectedAirports;
    private int boardingTime_sec;
    private int postprocessTime_sec;


    private Map<Mode, Map<Zone, Map<String, Map<Integer, Double>>>> accessibilityByModeZone;

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

    //Grids
    private Map<Integer, List> grids = new HashMap<>();

    public Map<Integer, List> getGrids() {
        return grids;
    }

    public void setGrids(Map<Integer, List> grids) {
        this.grids = grids;
    }

    //SYNYHETIC POPULATION
    private Map<Integer, Person> persons = new LinkedHashMap<>();
    private Map<Integer, Household> households = new LinkedHashMap<>();
    private int populationSection;
    private Map<Integer, Person> potentialTravellers = new LinkedHashMap<>();
    private Map<Integer, Household> potentialHouseholdTravellers = new LinkedHashMap<>();
    private String householdSubpopulationFileName;
    private String personSubpopulationFileName;

    //TRIPS
    private ArrayList<LongDistanceTrip> allTrips = new ArrayList<>();
    private ArrayList<LongDistanceTrip> tripsofPotentialTravellers = new ArrayList<>();

    private TableDataSet scenarioSettings;
    private Map<Integer, Map<Type, Map<Purpose, Map<Mode, Integer>>>> modalCountByModeByScenario = new LinkedHashMap<>();
    private Map<Integer, Map<Type, Map<Purpose, Map<Mode, Float>>>> co2EmissionsByModeByScenario = new LinkedHashMap<>();

    private Map<Integer, Map<Type, Map<Purpose, Map<Mode, Map<Integer, Integer>>>>> modalCountByModeByScenarioByDistance = new LinkedHashMap<>();
    private Map<Integer, Map<Type, Map<Purpose, Map<Mode, Map<Integer, Float>>>>> co2EmissionsByModeByScenarioByDistance = new LinkedHashMap<>();

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

    //Alona Scenario 1
    public void setRailAccessDistMatrix(Map<Mode, Matrix> railAccessDistMatrix) { this.railAccessDistMatrix = railAccessDistMatrix;}
    public void setRailEgressDistMatrix(Map<Mode, Matrix> railEgressDistMatrix) { this.railEgressDistMatrix = railEgressDistMatrix;}

    public Map<Mode, Matrix> getRailEgressDistMatrix() { return railEgressDistMatrix;}
    public Map<Mode, Matrix> getRailAccessDistMatrix() { return railAccessDistMatrix;}

    public void setRailAccessTimeMatrix(Map<Mode, Matrix> railAccessTimeMatrix) { this.railAccessTimeMatrix = railAccessTimeMatrix;}
    public void setRailEgressTimeMatrix(Map<Mode, Matrix> railEgressTimeMatrix) { this.railEgressTimeMatrix = railEgressTimeMatrix;}

    public Map<Mode, Matrix> getRailEgressTimeMatrix() { return railEgressTimeMatrix;}
    public Map<Mode, Matrix> getRailAccessTimeMatrix() { return railAccessTimeMatrix;}

    ///

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

    public int getPopulationSection() {
        return populationSection;
    }

    public void setPopulationSection(int populationSection) {
        this.populationSection = populationSection;
    }

    public Map<Integer, Person> getPotentialTravellers() {
        return potentialTravellers;
    }

    public void setPotentialTravelers(Map<Integer, Person> potentialTravellers) {
        this.potentialTravellers = potentialTravellers;
    }

    public Map<Integer, Household> getPotentialHouseholdTravellers() {
        return potentialHouseholdTravellers;
    }

    public void setHouseholdsPotentialTravelers(Map<Integer, Household> potentialHouseholdTravellers) {
        this.potentialHouseholdTravellers = potentialHouseholdTravellers;
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

    public ArrayList<LongDistanceTrip> getTripsofPotentialTravellers() {
        return tripsofPotentialTravellers;
    }

    public Map<Mode, Matrix> getDistanceMatrix() {
        return distanceMatrix;
    }

    public void setDistanceMatrix(Map<Mode, Matrix> distanceMatrix) {
        this.distanceMatrix = distanceMatrix;
    }

    public Map<Mode, Matrix> getTollDistanceMatrix() {
        return tollDistanceMatrix;
    }

    public void setTollDistanceMatrix(Map<Mode, Matrix> tollDistanceMatrix) {
        this.tollDistanceMatrix = tollDistanceMatrix;
    }


    public Map<Integer, Map<Type, Map<Purpose, Map<Mode, Integer>>>> getModalCountByModeByScenario() {
        return modalCountByModeByScenario;
    }

    public void setModalCountByModeByScenario(Map<Integer, Map<Type, Map<Purpose, Map<Mode, Integer>>>> modalCountByModeByScenario) {
        this.modalCountByModeByScenario = modalCountByModeByScenario;
    }

    public Map<Integer, Map<Type, Map<Purpose, Map<Mode, Float>>>> getCo2EmissionsByModeByScenario() {
        return co2EmissionsByModeByScenario;
    }

    public void setCo2EmissionsByModeByScenario(Map<Integer, Map<Type, Map<Purpose, Map<Mode, Float>>>> co2EmissionsByModeByScenario) {
        this.co2EmissionsByModeByScenario = co2EmissionsByModeByScenario;
    }

    public Map<Integer, Map<Type, Map<Purpose, Map<Mode, Map<Integer, Integer>>>>> getModalCountByModeByScenarioByDistance() {
        return modalCountByModeByScenarioByDistance;
    }

    public void setModalCountByModeByScenarioByDistance(Map<Integer, Map<Type, Map<Purpose, Map<Mode, Map<Integer, Integer>>>>> modalCountByModeByScenarioByDistance) {
        this.modalCountByModeByScenarioByDistance = modalCountByModeByScenarioByDistance;
    }

    public Map<Integer, Map<Type, Map<Purpose, Map<Mode, Map<Integer, Float>>>>> getCo2EmissionsByModeByScenarioByDistance() {
        return co2EmissionsByModeByScenarioByDistance;
    }

    public void setCo2EmissionsByModeByScenarioByDistance(Map<Integer, Map<Type, Map<Purpose, Map<Mode, Map<Integer, Float>>>>> co2EmissionsByModeByScenarioByDistance) {
        this.co2EmissionsByModeByScenarioByDistance = co2EmissionsByModeByScenarioByDistance;
    }

    public Matrix getAirAccessAirportZone() {
        return airAccessAirportZone;
    }

    public void setAirAccessAirportZone(Matrix airAccessAirportZone) {
        this.airAccessAirportZone = airAccessAirportZone;
    }

    public Matrix getAirEgressAirportZone() {
        return airEgressAirportZone;
    }

    public void setAirEgressAirportZone(Matrix airEgressAirportZone) {
        this.airEgressAirportZone = airEgressAirportZone;
    }

    public TableDataSet getScenarioSettings() {
        return scenarioSettings;
    }

    public void setScenarioSettings(TableDataSet scenarioSettings) {
        this.scenarioSettings = scenarioSettings;
    }

    public String getHouseholdSubpopulationFileName() {
        return householdSubpopulationFileName;
    }

    public void setHouseholdSubpopulationFileName(String householdSubpopulationFileName) {
        this.householdSubpopulationFileName = householdSubpopulationFileName;
    }

    public String getPersonSubpopulationFileName() {
        return personSubpopulationFileName;
    }

    public void setPersonSubpopulationFileName(String personSubpopulationFileName) {
        this.personSubpopulationFileName = personSubpopulationFileName;
    }

    //airports
    private Map<Integer, Airport> airports = new ConcurrentHashMap();
    private Map<Integer, Flight> flights  = new ConcurrentHashMap();
    private Map<Integer, AirLeg> airLegs  = new ConcurrentHashMap();
    private Map<Integer, Airport> airportsWithFlights  = new ConcurrentHashMap();
    private Map<Airport, Float> transferTimeAirport = new HashMap<>();

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

    public List<Airport> getEuropeAirports(){
        return airports.values().stream().filter(airport -> airport.getZone().getZoneType().equals(ZoneTypeGermany.GERMANY) && airport.getZone().getZoneType().equals(ZoneTypeGermany.EXTEU)).collect(Collectors.toList());
    }

    public List<Airport> getOverseasAirports(){
        return airports.values().stream().filter(airport -> airport.getZone().getZoneType().equals(ZoneTypeGermany.EXTOVERSEAS)).collect(Collectors.toList());
    }

    public Map<Airport, Float> getTransferTimeAirport() {
        return transferTimeAirport;
    }

    public void setTransferTimeAirport(Map<Airport, Float> transferTimeAirport) {
        this.transferTimeAirport = transferTimeAirport;
    }

    public void setNumberOfSubpopulations(double numberOfSubpopulations) {
        this.numberOfSubpopulations = numberOfSubpopulations;
    }

    public double getNumberOfSubpopulations() {
        return numberOfSubpopulations;
    }

    public void setNumberOfScenarios(int numberOfScenarios) {
        this.numberOfScenarios = numberOfScenarios;
    }

    public int getNumberOfScenarios() {
        return numberOfScenarios;
    }

    public void setDistanceBins(int[] distanceBins) {
        this.distanceBins = distanceBins;
    }

    public int[] getDistanceBins() {
        return distanceBins;
    }

    public int getScenario() {
        return scenario;
    }

    public void setScenario(int scenario) {
        this.scenario = scenario;
    }

    public void setConnectedAirports(Map<Airport, Map<Airport, Map<String, Integer>>> connectedAirports) {
        this.connectedAirports = connectedAirports;
    }

    public Map<Airport, Map<Airport, Map<String, Integer>>> getConnectedAirports() {
        return connectedAirports;
    }


    public int getBoardingTime_sec() {
        return boardingTime_sec;
    }

    public int getPostprocessTime_sec() {
        return postprocessTime_sec;
    }

    public void setboardingTime_sec(int boardingTime_sec) {
        this.boardingTime_sec = boardingTime_sec;
    }

    public void setpostprocessTime_sec(int postprocessTime_sec) {
        this.postprocessTime_sec = postprocessTime_sec;
    }

    public Map<Mode, Map<Zone, Map<String, Map<Integer, Double>>>> getAccessibilityByModeZone() {
        return accessibilityByModeZone;
    }

    public void setAccessibilityByModeZone(Map<Mode, Map<Zone, Map<String, Map<Integer, Double>>>> accessibilityByModeZone) {
        this.accessibilityByModeZone = accessibilityByModeZone;
    }
}
