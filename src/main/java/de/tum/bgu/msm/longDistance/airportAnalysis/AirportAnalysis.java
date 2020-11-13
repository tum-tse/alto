package de.tum.bgu.msm.longDistance.airportAnalysis;

import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.ModelComponent;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.airport.*;
import de.tum.bgu.msm.longDistance.data.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneGermany;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneTypeGermany;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public final class AirportAnalysis implements ModelComponent {

    private static final Logger logger = Logger.getLogger(AirportAnalysis.class);
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

    private String fileNameAirports;
    private String fileNameLegs;
    private String fileNameFlights;

    private Map<Airport, Map<Airport, Float>> connectedAirports = new HashMap<>();


    public AirportAnalysis() {
    }


    @Override
    public void setup(JSONObject prop, String inputFolder, String outputFolder) {
        airportsInput = Util.readCSVfile(JsonUtilMto.getStringProp(prop, "airport.airportList_file"));
        flightsInput = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"airport.flightList_file"));
        airportDistance = Util.readCSVfile(JsonUtilMto.getStringProp(prop, "airport.distance_file"));
        airportDistance.buildStringIndex(airportDistance.getColumnPosition("ID"));
        detourFactorEU = JsonUtilMto.getFloatProp(prop, "airport.detour_factor_EU");
        detourFactorOVERSEAS = JsonUtilMto.getFloatProp(prop, "airport.detour_factor_OVERSEAS");
        cruiseSpeed = JsonUtilMto.getFloatProp(prop,"airport.cruise_speed");
        ascendingTime = JsonUtilMto.getIntProp(prop, "airport.ascending_time");
        descendingTime = JsonUtilMto.getIntProp(prop, "airport.descending_time");
        fileNameAirports = JsonUtilMto.getStringProp(prop, "airport.airportOutput_file");
        fileNameLegs = JsonUtilMto.getStringProp(prop, "airport.legOutput_file");
        fileNameFlights = JsonUtilMto.getStringProp(prop, "airport.flightsOutput_file");
        transferTimes = Util.readCSVfile(JsonUtilMto.getStringProp(prop, "airport.transferTime_file"));
        transferTimes.buildIndex(transferTimes.getColumnPosition("id_hub"));
        logger.info("Airport analysis set up");
    }

    @Override
    public void load(DataSet dataSet) {
        generateAirports(dataSet);
    }



    @Override
    public void run(DataSet dataSet, int nThreads) {

        calculateDirectConnections(dataSet);
        calculateMultiStopConnections(dataSet);
        calculateMainAirportAndHubForZone(dataSet);
        //need to read the matrix with distance between airports
        //need to loop for direct flights and one change plus going to hub
        writeAirports(dataSet, fileNameAirports);
        writeLegs(dataSet, fileNameLegs);
        writeFlights(dataSet, fileNameFlights);

    }

    private void calculateDirectConnections(DataSet dataSet) {
        Map<Airport, Map<Airport, Float>>  legs = obtainOpenflightLegs(dataSet);
        generateLegs(dataSet, legs);
       Map<Integer, Airport> airportsWithFlightsMap = new HashMap<>();
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
                if (!connectedAirports.containsKey(origin)) {
                    HashMap<Airport, Float> con = new HashMap<>();
                    con.put(destination, time);
                    connectedAirports.put(origin, con);
                } else {

                    // private Map<Airport, Map<Airport, Float>> connectedAirports = new HashMap<>();
                   Map<Airport, Float> con = connectedAirports.get(origin);
                   con.put(destination, time);
                   connectedAirports.put(origin, con);
                }
                origin.getFlights().add(flight);
                airportsWithFlightsMap.put(id++, origin);
            }
        }
    }

    private void calculateMultiStopConnections(DataSet dataSet) {

        Map<Integer, Airport> airportsDataSet= dataSet.getAirports();
        List<Airport> airports = dataSet.getMainAirports();
        List<Airport> germanHubs = dataSet.getHubs();
        for (Airport origin : dataSet.getGermanAirports()){
            for (Airport destination : airports) {
                if (origin.getId() != destination.getId()) {
                    if (!checkIfDirectFlightExists(origin, destination)) {
                        float time = 1000000;
                        int minHubId = 0;
                        for (Airport hub : germanHubs) {
                            float timeHub = routeThroughHub(origin, destination, hub);
                            if (timeHub < time) {
                                minHubId = hub.getId();
                                time = timeHub;

                            }
                        /*Airport originHub = dataSet.getAirportFromId(origin.getHubAirportId());
                        Airport destinationHub = dataSet.getAirportFromId(destination.getHubAirportId());
                        routeIfFlightRoutedFromOriginHubOrDestinationHub(origin, destination, originHub, destinationHub);*/
                        }
                        if (minHubId > 0) {
                            Airport hub = dataSet.getAirportFromId(minHubId);
                            List<AirLeg> legs = new ArrayList<>();
                            AirLeg leg1 = new AirLeg(atomicIntegerLeg.getAndIncrement(), origin, hub);
                            leg1.setTime(connectedAirports.get(origin).get(hub));
                            AirLeg leg2 = new AirLeg(atomicIntegerLeg.getAndIncrement(), hub, destination);
                            leg2.setTime(connectedAirports.get(hub).get(destination));
                            legs.add(leg1);
                            legs.add(leg2);
                            Flight flight = new Flight(atomicIntegerFlight.getAndIncrement(), origin, destination, legs);
                            flight.setTime(time);
                            origin.getFlights().add(flight);
                        }
                    }
                }

            }
        }
        //dataSet.setAirports();
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


    private void routeIfFlightRoutedFromOriginHubOrDestinationHub(Airport origin, Airport destination, Airport originHub, Airport destinationHub) {
        float time1 = 10 * 60f; //10 hours
        float time2 = 10 * 60f; //10 hours
        if (checkIfDirectFlightExists(origin, originHub)) {
            if (checkIfDirectFlightExists(originHub, destination)) {
                logger.info("boo at " + originHub.getId() + " of airport " + origin.getId());
                time1 = connectedAirports.get(origin).get(originHub) +
                        transferTimes.getIndexedValueAt(originHub.getId(), "transferTime") +
                        connectedAirports.get(originHub).get(destination);
            }
        }
        if (checkIfDirectFlightExists(origin, destinationHub)) {
            if (checkIfDirectFlightExists(destinationHub, destination)) {
                time2 = connectedAirports.get(origin).get(destinationHub) +
                        transferTimes.getIndexedValueAt(destinationHub.getId(), "transferTime") +
                        connectedAirports.get(destinationHub).get(destination);
            }
        }
        if (time1 == time2 & time2 == 10 * 60f){

        } else if (time1 < time2){
            List<AirLeg> legs = new ArrayList<>();
            legs.add(new AirLeg(atomicIntegerLeg.getAndIncrement(), origin, originHub));
            legs.add(new AirLeg(atomicIntegerLeg.getAndIncrement(), originHub, destination));
            Flight flight = new Flight(atomicIntegerFlight.getAndIncrement(), origin, destination, legs);
            flight.setTime(time1);
            origin.getFlights().add(flight);
        } else {
            List<AirLeg> legs = new ArrayList<>();
            legs.add(new AirLeg(atomicIntegerLeg.getAndIncrement(), origin, destinationHub));
            legs.add(new AirLeg(atomicIntegerLeg.getAndIncrement(), destinationHub, destination));
            Flight flight = new Flight(atomicIntegerFlight.getAndIncrement(), origin, destination, legs);
            flight.setTime(time2);
            origin.getFlights().add(flight);
        }

    }


    private float routeThroughHub(Airport origin, Airport destination, Airport hub) {
        float time = 1000001f; //10 hours

        if (checkIfDirectFlightExists(origin, hub)) {
            if (checkIfDirectFlightExists(hub, destination)) {
                time = connectedAirports.get(origin).get(hub) +
                        transferTimes.getIndexedValueAt(hub.getId(), "transferTime") +
                        connectedAirports.get(hub).get(destination);
            }
        }

        return time;
    }



    private void writeFlights(DataSet dataSet, String fileName) {
        PrintWriter pw = Util.openFileForSequentialWriting(fileName, false);
        pw.println("idAirport,originAirport,idFlight,destinationAirport,totalTime,numberLegs,idLeg,originLeg,destinationLeg,costLeg,timeLeg");
        for (Airport airport : dataSet.getGermanAirports()){
            int airportId = airport.getId();
            String airportName = airport.getName();
            int numberOfFlights = airport.getFlights().size();
            for (Flight flight : airport.getFlights()) {
                int numberOfRoutes = flight.getLegs().size();
                for (AirLeg leg : flight.getLegs()) {
                    pw.print(airportId);
                    pw.print(",");
                    pw.print(airportName);
                    pw.print(",");
                    pw.print(flight.getId());
                    pw.print(",");
                    pw.print(flight.getDestination().getName());
                    pw.print(",");
                    pw.print(flight.getTime());
                    pw.print(",");
                    pw.print(numberOfRoutes);
                    pw.print(",");
                    pw.print(leg.getId());
                    pw.print(",");
                    pw.print(leg.getOrigin().getName());
                    pw.print(",");
                    pw.print(leg.getDestination().getName());
                    pw.print(",");
                    pw.print(leg.getCost());
                    pw.print(",");
                    pw.println(leg.getTime());

                }
            }
        }
        pw.close();
    }



    private void writeLegs(DataSet dataSet, String fileName) {
        PrintWriter pw = Util.openFileForSequentialWriting(fileName, false);
        pw.println("id,originAirport,destinationAirport,distance,time_min,cost_eur");
        for (Map.Entry<Integer, AirLeg> legId : dataSet.getAirLegs().entrySet()){
            AirLeg leg = legId.getValue();
            pw.print(leg.getId());
            pw.print(",");
            pw.print(leg.getOrigin().getName());
            pw.print(",");
            pw.print(leg.getDestination().getName());
            pw.print(",");
            pw.print(leg.getFrequency());
            pw.print(",");
            pw.print(leg.getTime());
            pw.print(",");
            pw.println(leg.getCost());

        }
        pw.close();
    }

    private void writeAirports(DataSet dataSet, String fileName) {

        PrintWriter pw = Util.openFileForSequentialWriting(fileName, false);
        pw.println("zone,airport,hub,distanceAirport,distanceHub");
        for (Map.Entry<Integer, Zone> zoneId : dataSet.getZones().entrySet()){
            ZoneGermany zone = (ZoneGermany) zoneId.getValue();
            pw.print(zone.getId());
            pw.print(",");
            pw.print(zone.getClosestAirport());
            pw.print(",");
            pw.print(zone.getClosestHub());
            pw.print(",");
            if (zone.getId() < 11865) {
                pw.print(dataSet.getAutoTravelDistance(zone.getId(), zone.getClosestAirport()));
                pw.print(",");
                pw.println(dataSet.getAutoTravelDistance(zone.getId(), zone.getClosestHub()));
            } else {
                pw.print(0);
                pw.print(",");
                pw.println(0);
            }
        }
        pw.close();

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
                airLeg.setFrequency(distance);
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
        return  distance * detour / cruiseSpeed * 60 / 1000 + ascendingTime + descendingTime;
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

        for (Map.Entry<Integer, Zone> zoneId : dataSet.getZones().entrySet()){
            ZoneGermany zone = (ZoneGermany) zoneId.getValue();
            if (zone.getId() < 11865) {
                int idClosestAirport = 0;
                float distMin = 100000000000000f;
                int idClosestHub = 0;
                float distHub = 100000000000000f;
                List<Airport> mainAirports = dataSet.getMainAirports();
                for (Airport airport : mainAirports) {
                    if (airport.getZone().getId() < 11865) {
                        if (dataSet.getAutoTravelDistance(zone.getId(), airport.getZone().getId()) < distMin) {
                            distMin = dataSet.getAutoTravelDistance(zone.getId(), airport.getZone().getId());
                            idClosestAirport = airport.getId();
                            if (!airport.getAirportType().equals(AirportType.HUB)) {
                                distHub = distMin;
                                idClosestHub = idClosestAirport;
                            }
                        }
                    }
                }
                for (Airport hub : dataSet.getHubs()){
                    if (hub.getZone().getId() < 11718) {
                        if (dataSet.getAutoTravelDistance(zone.getId(), hub.getZone().getId()) < distMin) {
                            distMin = dataSet.getAutoTravelDistance(zone.getId(), hub.getZone().getId());
                            idClosestAirport = hub.getId();
                            if (!hub.getAirportType().equals(AirportType.HUB)) {
                                distHub = distMin;
                                idClosestHub = idClosestAirport;
                            }
                        }
                    }
                }
                zone.setClosestAirport(idClosestAirport);
                zone.setClosestHub(idClosestHub);
            } else {
                zone.setClosestAirport(zone.getId());
                zone.setClosestHub(zone.getId());
            }
        }



    }

    private void addRoute(){

    }


}
