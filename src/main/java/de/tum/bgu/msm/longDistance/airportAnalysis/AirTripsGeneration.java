package de.tum.bgu.msm.longDistance.airportAnalysis;

import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.ModelComponent;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.airport.*;
import de.tum.bgu.msm.longDistance.data.trips.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.data.trips.LongDistanceTripGermany;
import de.tum.bgu.msm.longDistance.data.trips.ModeGermany;
import de.tum.bgu.msm.longDistance.data.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneGermany;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneTypeGermany;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class AirTripsGeneration implements ModelComponent {

    private static final Logger logger = Logger.getLogger(AirTripsGeneration.class);
    private TableDataSet airportsInput;
    private TableDataSet flightsInput;
    private TableDataSet airportDistance;
    private TableDataSet transferTimes;
    private float detourFactorEU;
    private float detourFactorOVERSEAS;
    private float cruiseSpeed;
    private int ascendingTime;
    private int descendingTime;

    private AtomicInteger atomicIntegerFlight = new AtomicInteger(0);
    private AtomicInteger atomicIntegerLeg = new AtomicInteger(0);
    private AtomicInteger atomicIntegerAirport = new AtomicInteger(0);

    private Map<Airport, Map<Airport, Map<String, Integer>>> connectedAirports = new HashMap<>();
    private int boardingTime_sec;
    private int postprocessTime_sec;


    public AirTripsGeneration() {
    }


    @Override
    public void setup(JSONObject prop, String inputFolder, String outputFolder) {
        airportsInput = Util.readCSVfile(inputFolder + JsonUtilMto.getStringProp(prop, "airport.airportList_file"));
        flightsInput = Util.readCSVfile(inputFolder + JsonUtilMto.getStringProp(prop,"airport.flightList_file"));
        airportDistance = Util.readCSVfile(inputFolder + JsonUtilMto.getStringProp(prop, "airport.distance_file"));
        airportDistance.buildStringIndex(airportDistance.getColumnPosition("ID"));
        detourFactorEU = JsonUtilMto.getFloatProp(prop, "airport.detour_factor_EU");
        detourFactorOVERSEAS = JsonUtilMto.getFloatProp(prop, "airport.detour_factor_OVERSEAS");
        cruiseSpeed = JsonUtilMto.getFloatProp(prop,"airport.cruise_speed");
        ascendingTime = JsonUtilMto.getIntProp(prop, "airport.ascending_time") * 60;
        descendingTime = JsonUtilMto.getIntProp(prop, "airport.descending_time") * 60;
        boardingTime_sec = JsonUtilMto.getIntProp(prop, "airport.boardingTime") * 60;
        postprocessTime_sec = JsonUtilMto.getIntProp(prop, "airport.postProcessTime") * 60;
        transferTimes = Util.readCSVfile(inputFolder + JsonUtilMto.getStringProp(prop,"airport.transferTime_file"));
        transferTimes.buildIndex(transferTimes.getColumnPosition("id_hub"));
        logger.info("Airport analysis set up");
    }

    @Override
    public void load(DataSet dataSet) {
        generateAirports(dataSet);
        calculateDirectConnections(dataSet);
        calculateMultiStopConnections(dataSet);
        calculateMainAirportAndHubForZone(dataSet);
        putParametersInDataset(dataSet);
    }

    private void putParametersInDataset(DataSet dataSet) {
        Map<Airport, Float> transferTimes_sec = new HashMap<>();
        for (int i = 1; i <= transferTimes.getRowCount(); i++){
            transferTimes_sec.put(dataSet.getAirportFromId((int)transferTimes.getValueAt(i,"id_hub")),transferTimes.getValueAt(i,"transferTime"));
        }
        dataSet.setTransferTimeAirport(transferTimes_sec);
        dataSet.setboardingTime_sec(boardingTime_sec);
        dataSet.setpostprocessTime_sec(postprocessTime_sec);
    }


    @Override
    public void run(DataSet dataSet, int nThreads) {
        obtainAirRoutesForTrips(dataSet);
    }

    private void obtainAirRoutesForTrips(DataSet dataSet){
        ArrayList<LongDistanceTrip> trips = dataSet.getAllTrips();
        logger.info("Running Air Trip Generation Model for " + trips.size() + " trips");
        AtomicInteger counter = new AtomicInteger(0);
        trips.parallelStream().forEach(t -> {
            if (! ((LongDistanceTripGermany)t).isInternational()) {
                obtainAirRoute(dataSet, t);
            }
        });
        logger.info("Finished Assigning Air Route Model");
    }


    private void obtainAirRoute (DataSet dataSet, LongDistanceTrip trip) {

        Map<String, Float> airRoute = new HashMap<>();
        ZoneGermany originZone = ((LongDistanceTripGermany) trip).getOrigZone();
        int originZoneId = originZone.getId();
        ZoneGermany destinationZone = (ZoneGermany) ((LongDistanceTripGermany) trip).getDestZone();
        int destinationZoneId = destinationZone.getId();

        if (originZone.getId() == destinationZone.getId()){
            //same zone - no flights allowed
            airRoute = assignIntrazonalTrip();

        } else {
            if (originZone.getClosestAirportId() == destinationZone.getClosestAirportId()){
                //same airport - no flights allowed
                airRoute = assignIntrazonalTrip();

            } else {
                //not in the catchment area of the same airport
                Airport originAirport = dataSet.getAirportFromId(originZone.getClosestAirportId());
                Airport destinationAirport = dataSet.getAirportFromId(destinationZone.getClosestAirportId());

                if (checkIfDirectFlightExists(originAirport, destinationAirport)){

                    if (connectedAirports.get(originAirport).get(destinationAirport).get("stops") == 0) {
                        //direct flight
                        airRoute = assignRoute(dataSet, originAirport, destinationAirport, originZoneId, destinationZoneId);

                    } else  {
                        //no direct flight: Need to check if it is faster to drive to the hub (only the hub because the airport is already connected)
                        Airport originHub = dataSet.getAirportFromId(originZone.getClosestHubId());
                        Airport destinationHub = dataSet.getAirportFromId(destinationZone.getClosestHubId());

                        float travelTimeBetweenAirports = getTotalTravelTime(dataSet, originZoneId, destinationZoneId, originAirport, destinationAirport);
                        float travelTimeFromHubOrigin = Float.MAX_VALUE;
                        float travelTimeFromHubDestination = Float.MAX_VALUE;
                        float travelTimeBetweenHubs = Float.MAX_VALUE;

                        if (checkIfDirectFlightExists(originHub, destinationAirport)) {
                            travelTimeFromHubOrigin = getTotalTravelTime(dataSet, originZoneId, destinationZoneId, originHub, destinationAirport);
                        }
                        if (checkIfDirectFlightExists(originAirport, destinationHub)) {
                            travelTimeFromHubDestination = getTotalTravelTime(dataSet, originZoneId, destinationZoneId, originAirport, destinationHub);
                        }
                        if(checkIfDirectFlightExists(originHub, destinationHub)){
                            travelTimeBetweenHubs = getTotalTravelTime(dataSet, originZoneId, destinationZoneId, originHub, destinationHub);
                        }

                        float minTravelTime = Math.min(Math.min(Math.min(travelTimeBetweenAirports, travelTimeFromHubOrigin), travelTimeFromHubDestination), travelTimeBetweenHubs);

                        if (minTravelTime == travelTimeBetweenAirports) {
                            //the fastest is to have 1 stop and use the airport
                            airRoute = assignRoute(dataSet, originAirport, destinationAirport, originZoneId, destinationZoneId);
                        } else if (minTravelTime == travelTimeFromHubOrigin) {
                            //the fastest is to travel from the origin hub
                            airRoute = assignRoute(dataSet, originHub, destinationAirport, originZoneId, destinationZoneId);
                        } else if (minTravelTime == travelTimeFromHubDestination) {
                            //the fastest is to travel to the destination hub
                            airRoute = assignRoute(dataSet, originAirport, destinationHub, originZoneId, destinationZoneId);
                        } else {
                            //the fastest is to travel to the destination hub
                            airRoute = assignRoute(dataSet, originHub, destinationHub, originZoneId, destinationZoneId);
                        }
                    }
                } else { //no connection between the airports
                    //check if the main airport is connected and repeat with hubs
                    //no direct flight: Need to check if it is faster to drive to the hub (only the hub because the airport is already connected)
                    Airport originMain = dataSet.getAirportFromId(originZone.getClosestMainAirportId());
                    Airport destinationMain = dataSet.getAirportFromId(destinationZone.getClosestMainAirportId());
                    Airport originHub = dataSet.getAirportFromId(originZone.getClosestHubId());
                    Airport destinationHub = dataSet.getAirportFromId(destinationZone.getClosestHubId());

                    float travelTimeBetweenMainAndAirport = Float.MAX_VALUE;
                    float travelTimeBetweenAirportAndMain = Float.MAX_VALUE;
                    float travelTimeBetweenMainAirports = Float.MAX_VALUE;
                    float travelTimeFromHubOrigin = Float.MAX_VALUE;
                    float travelTimeFromHubDestination = Float.MAX_VALUE;
                    float travelTimeBetweenHubs = Float.MAX_VALUE;

                    if (checkIfDirectFlightExists(originMain, destinationMain)) {
                        travelTimeBetweenMainAirports = getTotalTravelTime(dataSet, originZoneId, destinationZoneId, originMain, destinationMain);
                    }
                    if (checkIfDirectFlightExists(originAirport, destinationMain)) {
                        travelTimeBetweenAirportAndMain = getTotalTravelTime(dataSet, originZoneId, destinationZoneId, originAirport, destinationMain);
                    }
                    if (checkIfDirectFlightExists(originMain, destinationAirport)) {
                        travelTimeBetweenMainAndAirport = getTotalTravelTime(dataSet, originZoneId, destinationZoneId, originMain, destinationAirport);
                    }
                    if (checkIfDirectFlightExists(originHub, destinationAirport)) {
                        travelTimeFromHubOrigin = getTotalTravelTime(dataSet, originZoneId, destinationZoneId, originHub, destinationAirport);
                    }
                    if (checkIfDirectFlightExists(originAirport, destinationHub)) {
                        travelTimeFromHubDestination = getTotalTravelTime(dataSet, originZoneId, destinationZoneId, originAirport, destinationHub);
                    }
                    if (checkIfDirectFlightExists(originHub, destinationHub)) {
                        travelTimeBetweenHubs = getTotalTravelTime(dataSet, originZoneId, destinationZoneId, originHub, destinationHub);
                    }

                    float minTravelTime = Math.min(Math.min(Math.min(Math.min(Math.min(travelTimeBetweenMainAndAirport, travelTimeFromHubOrigin), travelTimeFromHubDestination),
                            travelTimeBetweenHubs), travelTimeBetweenMainAirports), travelTimeBetweenAirportAndMain);

                    if (minTravelTime < Float.MAX_VALUE) {
                        if (minTravelTime == travelTimeBetweenMainAndAirport) {
                            airRoute = assignRoute(dataSet, originMain, destinationMain, originZoneId, destinationZoneId);
                        } else if (minTravelTime == travelTimeFromHubOrigin) {
                            airRoute = assignRoute(dataSet, originHub, destinationAirport, originZoneId, destinationZoneId);
                        } else if (minTravelTime == travelTimeFromHubDestination) {
                            airRoute = assignRoute(dataSet, originAirport, destinationHub, originZoneId, destinationZoneId);
                        } else if (minTravelTime == travelTimeBetweenHubs) {
                            airRoute = assignRoute(dataSet, originHub, destinationHub, originZoneId, destinationZoneId);
                        } else if (minTravelTime == travelTimeBetweenAirportAndMain) {
                            airRoute = assignRoute(dataSet, originAirport, destinationMain, originZoneId, destinationZoneId);
                        } else {
                            airRoute = assignRoute(dataSet, originMain, destinationMain, originZoneId, destinationZoneId);
                        }
                    } else {
                        airRoute = assignIntrazonalTrip();
                    }
                }
            }
        }
        ((LongDistanceTripGermany) trip).getAdditionalAttributes().putAll(airRoute);
    }


    private  Map<String, Float>  assignIntrazonalTrip(){
        Map<String, Float> airRouteAttributes = new HashMap<>();
        airRouteAttributes.put("originAirport", null); //6
        airRouteAttributes.put("destinationAirport", null); //7
        airRouteAttributes.put("transferAirport", null); //0
        return airRouteAttributes;

    }

    private float getTotalTravelTime(DataSet dataSet, int originZoneId, int destinationZoneId, Airport originAirport, Airport destinationAirport) {
        float travelTimeBetweenAirports = dataSet.getFligthFromId(connectedAirports.get(originAirport).get(destinationAirport).get("flightId")).getTime() ;
        if (originZoneId < 11868) {
            travelTimeBetweenAirports = travelTimeBetweenAirports +
                    dataSet.getTravelTimeMatrix().get(ModeGermany.AUTO).getValueAt(originZoneId, originAirport.getZone().getId());
        }
        if (destinationZoneId < 11868) {
            travelTimeBetweenAirports = travelTimeBetweenAirports +
                    dataSet.getTravelTimeMatrix().get(ModeGermany.AUTO).getValueAt(destinationAirport.getZone().getId(), destinationZoneId);
        }
        return travelTimeBetweenAirports;
    }


    private Map<String, Float> assignRoute(DataSet dataSet, Airport originAirport, Airport destinationAirport, int originZoneId, int destinationZoneId){

        Map<String, Float> airRoute = new HashMap<>();
        if (originAirport.getId() != destinationAirport.getId()) {
            List<AirLeg> legs = dataSet.getFligthFromId(connectedAirports.get(originAirport).get(destinationAirport).get("flightId")).getLegs();
            airRoute.put("originAirport", (float) originAirport.getId());
            airRoute.put("destinationAirport", (float) destinationAirport.getId());
            double time_sec = 0;
            double distance_m = 0;
            if (legs.size() > 1) {
                Airport transferAirport = legs.get(0).getDestination();
                airRoute.put("transferAirport", (float) transferAirport.getId());
                time_sec = time_sec + transferTimes.getIndexedValueAt(transferAirport.getId(), "transferTime");
            }
            for (AirLeg leg : legs) {
                time_sec = time_sec + leg.getTime();
                distance_m = distance_m + leg.getDistance();
            }
            time_sec = time_sec + boardingTime_sec + postprocessTime_sec;
            time_sec = time_sec + dataSet.getTravelTimeMatrix().get(ModeGermany.AUTO).getValueAt(originZoneId, originAirport.getId());
            time_sec = time_sec + dataSet.getTravelTimeMatrix().get(ModeGermany.AUTO).getValueAt(destinationAirport.getId(), destinationZoneId);
            distance_m = distance_m + dataSet.getDistanceMatrix().get(ModeGermany.AUTO).getValueAt(originZoneId, originAirport.getId());
            distance_m = distance_m + dataSet.getDistanceMatrix().get(ModeGermany.AUTO).getValueAt(destinationAirport.getId(), destinationZoneId);
            airRoute.put("totalTime_sec", (float) time_sec);
            airRoute.put("totalDistance_sec", (float) distance_m);
        } else {
            airRoute = assignIntrazonalTrip();
        }
        return airRoute;
    }


    private void calculateDirectConnections(DataSet dataSet) {
        Map<Airport, Map<Airport, Float>>  legs = obtainOpenflightLegs(dataSet);
        generateLegs(dataSet, legs);
        Map<Integer, Airport> airportsWithFlightsMap = new HashMap<>();
        Map<Integer, Flight> flightMap = new HashMap<>();
        int id = 0;

        for (AirLeg leg: dataSet.getAirLegs().values()){
            Airport origin = leg.getOrigin();
            if (dataSet.getGermanAirports().contains(origin)) {
                Airport destination = leg.getDestination();
                List<AirLeg> connection = new ArrayList<>();
                connection.add(leg);
                Flight flight = new Flight(atomicIntegerFlight.getAndIncrement(), origin, destination, connection);
                float time = leg.getTime();
                flight.setTime(time);
                flight.setCost(leg.getCost());
                if (!connectedAirports.containsKey(origin)) {
                    Map<String, Integer> attributes = new HashMap<>();
                    Map<Airport, Map<String, Integer>> airportDestination = new HashMap<>();
                    attributes.put("stops", 0);
                    attributes.put("flightId", flight.getId());
                    airportDestination.put(destination, attributes);
                    connectedAirports.put(origin, airportDestination);
                } else {
                    if (!connectedAirports.get(origin).containsKey(destination)) {
                        Map<Airport, Map<String, Integer>> destinationAttributes = connectedAirports.get(origin);
                        Map<String, Integer> attributes = new HashMap<>();
                        attributes.put("stops", 0);
                        attributes.put("flightId", flight.getId());
                        destinationAttributes.put(destination, attributes);
                        connectedAirports.put(origin, destinationAttributes);
                    } else {
                        Map<Airport, Map<String, Integer>> destinationAttributes = connectedAirports.get(origin);
                        Map<String, Integer> attributes = destinationAttributes.get(destination);
                        attributes.put("stops", 0);
                        attributes.put("flightId", flight.getId());
                        destinationAttributes.put(destination, attributes);
                        connectedAirports.put(origin, destinationAttributes);
                    }
                }
                origin.getFlights().add(flight);
                airportsWithFlightsMap.put(id++, origin);
                flightMap.put(flight.getId(), flight);
            }
        }
        dataSet.setFlights(flightMap);
        dataSet.setConnectedAirports(connectedAirports);
    }

    private void calculateMultiStopConnections(DataSet dataSet) {

        Map<Integer, Airport> airportsDataSet= dataSet.getAirports();
        List<Airport> airports = dataSet.getMainAirports();
        List<Airport> germanHubs = dataSet.getGermanHubs();
        Map<Integer, Flight> flightMap = dataSet.getFlights();
        for (Airport origin : dataSet.getGermanAirports()){
            for (Airport destination : airports) {
                if (origin.getId() != destination.getId()) {
                    if (!checkIfDirectFlightExists(origin, destination)) {
                        float time = 1000000;
                        int minHubId = 0;
                        for (Airport hub : germanHubs) {
                            float timeHub = routeThroughHub(origin, destination, hub, dataSet);
                            if (timeHub < time) {
                                minHubId = hub.getId();
                                time = timeHub;

                            }
                        }
                        if (minHubId > 0) {
                            Airport hub = dataSet.getAirportFromId(minHubId);
                            List<AirLeg> legs = new ArrayList<>();
                            AirLeg legOriginToHub = dataSet.getFligthFromId(connectedAirports.get(origin).get(hub).get("flightId")).getLegs().get(0);
                            AirLeg legHubToDestination = dataSet.getFligthFromId(connectedAirports.get(hub).get(destination).get("flightId")).getLegs().get(0);
                            legs.add(legOriginToHub);
                            legs.add(legHubToDestination);
                            Flight flight = new Flight(atomicIntegerFlight.getAndIncrement(), origin, destination, legs);
                            flight.setTime(time);
                            flight.setCost(legOriginToHub.getCost() + legHubToDestination.getCost());
                            origin.getFlights().add(flight);
                            Map<Airport, Map<String, Integer>> destinationAttributes = connectedAirports.get(origin);
                            Map<String, Integer> attributes = new HashMap<>();
                            attributes.put("stops", 1);
                            attributes.put("flightId", flight.getId());
                            destinationAttributes.put(destination, attributes);
                            connectedAirports.put(origin, destinationAttributes);
                            flightMap.put(flight.getId(), flight);
                        }
                    }
                }
            }
        }
        dataSet.setFlights(flightMap);
        dataSet.setConnectedAirports(connectedAirports);
    }


    private boolean checkIfDirectFlightExists(Airport origin, Airport destination){
        boolean exists = true;
        if (!connectedAirports.containsKey(origin)) {
            exists = false;
        } else {
            if (!connectedAirports.get(origin).containsKey(destination)) {
                exists = false;
            } else {
                exists = true;
            }
        }
        return exists;
    }


    private float routeThroughHub(Airport origin, Airport destination, Airport hub, DataSet dataSet) {
        float time = 1000001f; //10 hours

        if (checkIfDirectFlightExists(origin, hub)) {
            if (checkIfDirectFlightExists(hub, destination)) {
                time = dataSet.getFligthFromId(connectedAirports.get(origin).get(hub).get("flightId")).getTime() +
                        transferTimes.getIndexedValueAt(hub.getId(), "transferTime") +
                        dataSet.getFligthFromId(connectedAirports.get(hub).get(destination).get("flightId")).getTime();
            }
        }

        return time;
    }


    private void generateAirports(DataSet dataSet){

        Map<Integer, Airport> airportMap = new HashMap<>();
        Map<Integer, Zone> zoneMap = dataSet.getZones();

        for (int row = 1; row <= airportsInput.getRowCount(); row++){

            int id = (int) airportsInput.getValueAt(row, "id");
            String name = airportsInput.getStringValueAt(row, "iata");
            int idFlights = (int) airportsInput.getValueAt(row, "id_openFlight");
            int zoneId = (int) airportsInput.getValueAt(row, "TAZ_id_world");
            int idMain = (int) airportsInput.getValueAt(row, "id_main");
            int idHub = (int) airportsInput.getValueAt(row, "id_hub");

            AirportType airportType = AirportType.HUB;
            if (id == idHub){
                airportType = AirportType.HUB;
            } else {
                if (id == idMain){
                    airportType = AirportType.MAIN;
                } else {
                    airportType = AirportType.FEEDER_ZONE;
                }
            }
            ZoneGermany zoneGermany = (ZoneGermany) zoneMap.get(zoneId);
            Airport airport = new Airport(id, name, zoneGermany, airportType);
            airport.setMainAirportId(idMain);
            airport.setHubAirportId(idHub);
            airport.setIdOpenFlight(idFlights);
            airportMap.put(id, airport);
        }
        dataSet.setAirports(airportMap);
    }


    private Map<Airport, Map<Airport, Float>> obtainOpenflightLegs(DataSet dataSet) {

        Map<Integer, Flight> flightMap = new HashMap<>();
        Map<Integer, AirLeg> legMap = new HashMap<>();
        Map<Integer, Airport> airportMap = dataSet.getAirports();
        Map<Airport, Map<Airport, Float>> legsFromGermany = new HashMap<>();


        for (int row = 1; row <= flightsInput.getRowCount(); row++){

            int id = (int) flightsInput.getValueAt(row,"id");
            int originId = (int) flightsInput.getValueAt(row,"from_id");
            int destinationId = (int) flightsInput.getValueAt(row,"to_id");
            Airport originAirport = dataSet.getAirports().get(originId);
            Airport destinationAirport = dataSet.getAirports().get(destinationId);

            Airport originMainAirport = identifyAirportForLeg(originAirport, airportMap);
            Airport destinationMainAirport = identifyAirportForLeg(destinationAirport, airportMap);

            if (legsFromGermany.containsKey(originMainAirport)) {
                Map<Airport, Float> destinations = legsFromGermany.get(originMainAirport);
                if (destinations == null){
                    destinations = new HashMap<Airport, Float>();
                    legsFromGermany.put(originMainAirport, destinations);
                }
                destinations.put(destinationMainAirport, 1f);

            } else {
                Map<Airport, Float> destinations = new HashMap<>();
                destinations.put(destinationMainAirport, 1f);
                legsFromGermany.put(originMainAirport, destinations);
            }
        }

        logger.info("finished direct flights");
        return legsFromGermany;
    }

    private void generateLegs(DataSet dataSet, Map<Airport, Map<Airport, Float>> legsFromGermany) {

        Map<Integer, AirLeg> airLegMap = new HashMap<>();
        int idLeg = 0;

        for (Map.Entry<Airport, Map<Airport, Float>> origin : legsFromGermany.entrySet()){
            Map<Airport, Float> destinations = origin.getValue();
            for (Map.Entry<Airport, Float> destination : destinations.entrySet()){
                AirLeg airLeg = new AirLeg(atomicIntegerLeg.getAndIncrement(),origin.getKey(), destination.getKey());
                float distance = airportDistance.getStringIndexedValueAt(origin.getKey().getName(), destination.getKey().getName());
                float cost = estimateAirCost(distance);
                float time = estimateAirTime(distance);
                airLeg.setCost(cost);
                airLeg.setTime(time);
                airLeg.setDistance(distance);
                airLegMap.put(airLeg.getId(), airLeg);
            }
        }
        dataSet.setAirLegs(airLegMap);

        logger.info("finished air legs");
    }

    private float estimateAirTime(float distance) {
        float detour = detourFactorEU;
        if (distance > 1000000){
            detour = detourFactorOVERSEAS;
        }
        return  distance * detour / cruiseSpeed * 3600 / 1000 + ascendingTime + descendingTime;
    }

    private float estimateAirCost(float distance) {
        float costPerKm = 0;
        distance = distance / 1000;
        if (distance < 350){
            costPerKm = 0.3f;
        } else if (distance < 600) {
            costPerKm = 0.25f;
        } else if (distance < 1000){
            costPerKm = 0.2f;
        } else {
            costPerKm = 0.05f;
        }

        return costPerKm * distance;
    }


    private Airport identifyAirportForLeg(Airport airport, Map<Integer, Airport> airportMap){
        Airport mainAirport;
        if (airport.getAirportType().equals(AirportType.FEEDER_ZONE)){
            mainAirport = airportMap.get(airport.getMainAirportId());
        } else {
            mainAirport = airport;
        }
        return mainAirport;
    }


    private void calculateMainAirportAndHubForZone(DataSet dataSet) {
        //calculate the closest airport to each zone

        List<Airport> mainAirports = dataSet.getMainAirports();
        List<Airport> germanAirports = dataSet.getGermanAirports();
        List<Airport> overseasAirports = dataSet.getOverseasAirports();

        for (Airport airport : mainAirports){
            if (airport.getZone().getZoneType().equals(ZoneTypeGermany.GERMANY)){
                int nationalFlights = 0;
                int internationalFlights = 0;
                Set<Flight> flights = airport.getFlights();
                for (Flight flight : flights){
                    if (germanAirports.contains(flight.getDestination())){
                        nationalFlights++;
                    } else {
                        internationalFlights++;
                    }
                }
                if (nationalFlights == 0){
                    airport.setAirportDestinationType(AirportDestinationType.ONLY_INTERNATIONAL);
                } else {
                    airport.setAirportDestinationType(AirportDestinationType.DOMESTIC_AND_INTERNATIONAL);
                }
            } else {
                airport.setAirportDestinationType(AirportDestinationType.ONLY_INTERNATIONAL);
            }
        }


        for (Map.Entry<Integer, Zone> zoneId : dataSet.getZones().entrySet()){
            ZoneGermany zone = (ZoneGermany) zoneId.getValue();
            int idClosestAirport = 0;
            float distMin = 100000000000000f;
            int idClosestMainAirport = 0;
            float distMainAirport = 100000000000000f;
            int idClosestHub = 0;
            float distHub = 100000000000000f;

            if (zone.getZoneType().equals(ZoneTypeGermany.GERMANY)) {

                for (Airport airport : germanAirports) {
                    if (!airport.getZone().getZoneType().equals(ZoneTypeGermany.EXTOVERSEAS)) {
                        if (dataSet.getTravelTimeMatrix().get(ModeGermany.AUTO).getValueAt(zone.getId(), airport.getZone().getId()) < distMin) {
                            distMin = dataSet.getTravelTimeMatrix().get(ModeGermany.AUTO).getValueAt(zone.getId(), airport.getZone().getId());
                            idClosestAirport = airport.getId();
                        }
                        if (airport.getAirportDestinationType().equals(AirportDestinationType.DOMESTIC_AND_INTERNATIONAL)) {
                            if (dataSet.getTravelTimeMatrix().get(ModeGermany.AUTO).getValueAt(zone.getId(), airport.getZone().getId()) < distMainAirport) {
                                distMainAirport = dataSet.getTravelTimeMatrix().get(ModeGermany.AUTO).getValueAt(zone.getId(), airport.getZone().getId());
                                idClosestMainAirport = airport.getId();
                            }
                        }
                        if (airport.getAirportType().equals(AirportType.HUB)) {
                            if (dataSet.getTravelTimeMatrix().get(ModeGermany.AUTO).getValueAt(zone.getId(), airport.getZone().getId()) < distHub) {
                                distHub = dataSet.getTravelTimeMatrix().get(ModeGermany.AUTO).getValueAt(zone.getId(), airport.getZone().getId());
                                idClosestHub = airport.getId();
                            }
                        }
                    }
                }

            } else if (zone.getZoneType().equals(ZoneTypeGermany.EXTEU)) {

                for (Airport airport : mainAirports) {
                    if (!airport.getZone().getZoneType().equals(ZoneTypeGermany.EXTOVERSEAS)) {
                        if (dataSet.getTravelTimeMatrix().get(ModeGermany.AUTO).getValueAt(zone.getId(), airport.getZone().getId()) < distMin) {
                            distMin = dataSet.getTravelTimeMatrix().get(ModeGermany.AUTO).getValueAt(zone.getId(), airport.getZone().getId());
                            idClosestAirport = airport.getId();
                            idClosestMainAirport = airport.getId();
                            idClosestHub = airport.getId();
                        }
                    }
                }

            } else {
                //overseas
                for (Airport airport : overseasAirports) {
                    if (airport.getZone().getId() == zone.getId()) {
                        idClosestAirport = airport.getId();
                        idClosestMainAirport = airport.getId();
                        idClosestHub = airport.getId();
                    }
                }
            }
            //this happens because there are zones with all times equal to infinity (200 or so)
            //TODO clean the zone system to avoid zones that are not connected to the network
            //set the airports to Frankfurt FRA
            if (idClosestAirport == 0) {

                idClosestAirport = 13;
            }
            if (idClosestMainAirport == 0) {
                idClosestMainAirport = 13;
            }
            if (idClosestHub == 0){
                idClosestHub = 13;
            }

            zone.setClosestAirportId(idClosestAirport);
            zone.setClosestMainAirportId(idClosestMainAirport);
            zone.setClosestHubId(idClosestHub);
        }

    }

}
