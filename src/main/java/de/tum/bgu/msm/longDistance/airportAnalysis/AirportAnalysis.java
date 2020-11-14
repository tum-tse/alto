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

    private Map<Airport, Map<Airport, Map<String, Float>>> connectedAirports = new HashMap<>();


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
        calculateAirSkims(dataSet);
        writeAirports(dataSet, fileNameAirports);
        writeLegs(dataSet, fileNameLegs);
        writeFlights(dataSet, fileNameFlights);

    }

    private void calculateAirSkims(DataSet dataSet) {

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
                    Map<String, Float> attributes = new HashMap<>();
                    Map<Airport, Map<String, Float>> airportDestination = new HashMap<>();
                    attributes.put("time", time);
                    attributes.put("cost", leg.getCost());
                    airportDestination.put(destination, attributes);
                    connectedAirports.put(origin, airportDestination);
                } else {
                    if (!connectedAirports.get(origin).containsKey(destination)) {
                        Map<Airport, Map<String, Float>> destinationAttributes = connectedAirports.get(origin);
                        Map<String, Float> attributes = new HashMap<>();
                        attributes.put("time", time);
                        attributes.put("cost", leg.getCost());
                        destinationAttributes.put(destination, attributes);
                        connectedAirports.put(origin, destinationAttributes);
                    } else {
                        Map<Airport, Map<String, Float>> destinationAttributes = connectedAirports.get(origin);
                        Map<String, Float> attributes = destinationAttributes.get(destination);
                        attributes.put("time", time);
                        attributes.put("cost", leg.getCost());
                        destinationAttributes.put(destination, attributes);
                        connectedAirports.put(origin, destinationAttributes);
                    }
                }
                origin.getFlights().add(flight);
                airportsWithFlightsMap.put(id++, origin);
            }
        }
    }

    private void calculateMultiStopConnections(DataSet dataSet) {

        Map<Integer, Airport> airportsDataSet= dataSet.getAirports();
        List<Airport> airports = dataSet.getMainAirports();
        List<Airport> germanHubs = dataSet.getGermanHubs();
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
                        }
                        if (minHubId > 0) {
                            Airport hub = dataSet.getAirportFromId(minHubId);
                            List<AirLeg> legs = new ArrayList<>();
                            AirLeg leg1 = new AirLeg(atomicIntegerLeg.getAndIncrement(), origin, hub);
                            leg1.setTime(connectedAirports.get(origin).get(hub).get("time"));
                            leg1.setCost(connectedAirports.get(origin).get(hub).get("cost"));
                            AirLeg leg2 = new AirLeg(atomicIntegerLeg.getAndIncrement(), hub, destination);
                            leg2.setTime(connectedAirports.get(hub).get(destination).get("time"));
                            leg2.setCost(connectedAirports.get(hub).get(destination).get("cost"));
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
                time1 = connectedAirports.get(origin).get(originHub).get("time") +
                        transferTimes.getIndexedValueAt(originHub.getId(), "transferTime") +
                        connectedAirports.get(originHub).get(destination).get("time");
            }
        }
        if (checkIfDirectFlightExists(origin, destinationHub)) {
            if (checkIfDirectFlightExists(destinationHub, destination)) {
                time2 = connectedAirports.get(origin).get(destinationHub).get("time") +
                        transferTimes.getIndexedValueAt(destinationHub.getId(), "transferTime") +
                        connectedAirports.get(destinationHub).get(destination).get("time");
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
                time = connectedAirports.get(origin).get(hub).get("time") +
                        transferTimes.getIndexedValueAt(hub.getId(), "transferTime") +
                        connectedAirports.get(hub).get(destination).get("time");
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
        pw.println("zone,airport,main,hub,distanceAirport,distanceMain,distanceHub");
        List <Airport> airports = dataSet.getMainAirports();
        for (Map.Entry<Integer, Zone> zoneId : dataSet.getZones().entrySet()){
            ZoneGermany zone = (ZoneGermany) zoneId.getValue();
            Airport airport = dataSet.getAirportFromId(zone.getClosestAirportId());
            Airport main = dataSet.getAirportFromId(zone.getClosestMainAirportId());
            Airport hub = dataSet.getAirportFromId(zone.getClosestHubId());
            pw.print(zone.getId());
            pw.print(",");
            pw.print(airport.getName());
            pw.print(",");
            pw.print(main.getName());
            pw.print(",");
            pw.print(hub.getName());
            pw.print(",");
            if (!airport.getZone().getZoneType().equals(ZoneTypeGermany.EXTOVERSEAS)) {
                pw.print(dataSet.getAutoTravelDistance(zone.getId(), airport.getZone().getId()));
                pw.print(",");
                pw.print(dataSet.getAutoTravelDistance(zone.getId(), main.getZone().getId()));
                pw.print(",");
                pw.println(dataSet.getAutoTravelDistance(zone.getId(), hub.getZone().getId()));
            } else {
                pw.print(0);
                pw.print(",");
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
                        if (dataSet.getAutoTravelDistance(zone.getId(), airport.getZone().getId()) < distMin) {
                            distMin = dataSet.getAutoTravelDistance(zone.getId(), airport.getZone().getId());
                            idClosestAirport = airport.getId();
                        }
                        if (airport.getAirportDestinationType().equals(AirportDestinationType.DOMESTIC_AND_INTERNATIONAL)) {
                            if (dataSet.getAutoTravelDistance(zone.getId(), airport.getZone().getId()) < distMainAirport) {
                                distMainAirport = dataSet.getAutoTravelDistance(zone.getId(), airport.getZone().getId());
                                idClosestMainAirport = airport.getId();
                            }
                        }
                        if (airport.getAirportType().equals(AirportType.HUB)) {
                            if (dataSet.getAutoTravelDistance(zone.getId(), airport.getZone().getId()) < distHub) {
                                distHub = dataSet.getAutoTravelDistance(zone.getId(), airport.getZone().getId());
                                idClosestHub = airport.getId();
                            }
                        }
                    }
                }

            } else if (zone.getZoneType().equals(ZoneTypeGermany.EXTEU)) {

                for (Airport airport : mainAirports) {
                    if (!airport.getZone().getZoneType().equals(ZoneTypeGermany.EXTOVERSEAS)) {
                        if (dataSet.getAutoTravelDistance(zone.getId(), airport.getZone().getId()) < distMin) {
                            distMin = dataSet.getAutoTravelDistance(zone.getId(), airport.getZone().getId());
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
