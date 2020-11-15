package de.tum.bgu.msm.longDistance.airportAnalysis;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.ModelComponent;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.airport.*;
import de.tum.bgu.msm.longDistance.data.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneGermany;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneTypeGermany;
import de.tum.bgu.msm.longDistance.io.writer.OmxMatrixWriter;
import omx.OmxFile;
import omx.OmxLookup;
import omx.OmxMatrix;
import omx.hdf5.OmxConstants;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import javax.xml.crypto.Data;
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
    private String fileNameOmxFirstLeg;
    private String fileNameOmxSecondLeg;
    private String fileNameOmxTransfer;
    private String fileNameOmxAccess;
    private String fileNameOmxEgress;
    private String fileNameOmxTotal;


    private Map<Airport, Map<Airport, Map<String, Integer>>> connectedAirports = new HashMap<>();


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
        fileNameOmxFirstLeg = JsonUtilMto.getStringProp(prop, "airport.omxOutputfirstLeg_file");
        fileNameOmxSecondLeg = JsonUtilMto.getStringProp(prop, "airport.omxOutputsecondLeg_file");
        fileNameOmxTransfer = JsonUtilMto.getStringProp(prop, "airport.omxOutputtransfer_file");
        fileNameOmxAccess = JsonUtilMto.getStringProp(prop, "airport.omxOutputaccess_file");
        fileNameOmxEgress = JsonUtilMto.getStringProp(prop, "airport.omxOutputegress_file");
        fileNameOmxTotal = JsonUtilMto.getStringProp(prop, "airport.omxOutputtotal_file");
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


        /*int numberOfZones = 4;
        Matrix firstLegTime = new Matrix(fileNameOmxFirstLeg, fileNameOmxFirstLeg, numberOfZones, numberOfZones);
        Matrix secondLegTime = new Matrix(fileNameOmxSecondLeg, fileNameOmxSecondLeg, numberOfZones, numberOfZones);
        Matrix transferTime = new Matrix(fileNameOmxTransfer, fileNameOmxTransfer, numberOfZones, numberOfZones);
        Matrix accessTime = new Matrix(fileNameOmxAccess, fileNameOmxAccess, numberOfZones, numberOfZones);
        Matrix egressTime = new Matrix(fileNameOmxEgress, fileNameOmxEgress, numberOfZones, numberOfZones);
        Matrix totalTime = new Matrix(fileNameOmxTotal, fileNameOmxTotal, numberOfZones, numberOfZones);*/

 int numberOfZones = dataSet.getZones().size();
        Matrix firstLegTime = new Matrix(fileNameOmxFirstLeg, fileNameOmxFirstLeg, numberOfZones, numberOfZones);
        Matrix secondLegTime = new Matrix(fileNameOmxSecondLeg, fileNameOmxSecondLeg, numberOfZones, numberOfZones);
        Matrix transferTime = new Matrix(fileNameOmxTransfer, fileNameOmxTransfer, numberOfZones, numberOfZones);
        Matrix accessTime = new Matrix(fileNameOmxAccess, fileNameOmxAccess, numberOfZones, numberOfZones);
        Matrix egressTime = new Matrix(fileNameOmxEgress, fileNameOmxEgress, numberOfZones, numberOfZones);
        Matrix totalTime = new Matrix(fileNameOmxTotal, fileNameOmxTotal, numberOfZones, numberOfZones);


        for (Map.Entry<Integer, Zone> originMap : dataSet.getZones().entrySet()){

        //int[] example = new int[]{1,5, 6644, 8223};
        //int[] example2 = new int[]{1,5, 6644, 8223};
        //int rowOr = 1;
        //for (int originZoneId : example){
            ZoneGermany originZone = (ZoneGermany) originMap.getValue();
            int originZoneId = originMap.getKey();
            //ZoneGermany originZone = (ZoneGermany) dataSet.getZones().get(originZoneId);
            //int rowDes = 1;

            //for (int destinationZoneId : example2){
            for (Map.Entry<Integer, Zone> destinationMap : dataSet.getZones().entrySet()){

                ZoneGermany destinationZone = (ZoneGermany) destinationMap.getValue();
                int destinationZoneId = destinationMap.getKey();
                //ZoneGermany destinationZone = (ZoneGermany) dataSet.getZones().get(destinationZoneId);


                float[] travelTimes = new float[6];

                if (originZone.getId() == destinationZone.getId()){
                    //same zone - no flights allowed
                    travelTimes = assignIntrazonalTrip();

                } else {
                    if (originZone.getClosestAirportId() == destinationZone.getClosestAirportId()){
                        //same airport - no flights allowed
                        travelTimes = assignIntrazonalTrip();

                    } else {
                        //not in the catchment area of the same airport
                        Airport originAirport = dataSet.getAirportFromId(originZone.getClosestAirportId());
                        Airport destinationAirport = dataSet.getAirportFromId(destinationZone.getClosestAirportId());

                        if (checkIfDirectFlightExists(originAirport, destinationAirport)){

                            if (connectedAirports.get(originAirport).get(destinationAirport).get("stops") == 0) {
                                //direct flight
                                travelTimes = getTravelTimes(dataSet, originZoneId, destinationZoneId, originAirport, destinationAirport);

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
                                    travelTimes = getTravelTimes(dataSet, originZoneId, destinationZoneId, originAirport, destinationAirport);
                                } else if (minTravelTime == travelTimeFromHubOrigin) {
                                    //the fastest is to travel from the origin hub
                                    travelTimes = getTravelTimes(dataSet, originZoneId, destinationZoneId, originHub, destinationAirport);
                                } else if (minTravelTime == travelTimeFromHubDestination) {
                                    //the fastest is to travel to the destination hub
                                    travelTimes = getTravelTimes(dataSet, originZoneId, destinationZoneId, originAirport, destinationHub);
                                } else {
                                    //the fastest is to travel to the destination hub
                                    travelTimes = getTravelTimes(dataSet, originZoneId, destinationZoneId, originHub, destinationHub);
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
                                    travelTimes = getTravelTimes(dataSet, originZoneId, destinationZoneId, originMain, destinationMain);
                                } else if (minTravelTime == travelTimeFromHubOrigin) {
                                    travelTimes = getTravelTimes(dataSet, originZoneId, destinationZoneId, originHub, destinationAirport);
                                } else if (minTravelTime == travelTimeFromHubDestination) {
                                    travelTimes = getTravelTimes(dataSet, originZoneId, destinationZoneId, originAirport, destinationHub);
                                } else if (minTravelTime == travelTimeBetweenHubs) {
                                    travelTimes = getTravelTimes(dataSet, originZoneId, destinationZoneId, originHub, destinationHub);
                                } else if (minTravelTime == travelTimeBetweenAirportAndMain) {
                                    travelTimes = getTravelTimes(dataSet, originZoneId, destinationZoneId, originAirport, destinationMain);
                                } else {
                                    travelTimes = getTravelTimes(dataSet, originZoneId, destinationZoneId, originMain, destinationMain);
                                }
                            } else {
                                travelTimes = assignIntrazonalTrip();
                            }
                        }
                    }
                }
                /*firstLegTime.setValueAt(rowOr, rowDes, travelTimes[0]);
                secondLegTime.setValueAt(rowOr, rowDes, travelTimes[1]);
                transferTime.setValueAt(rowOr, rowDes, travelTimes[2]);
                accessTime.setValueAt(rowOr, rowDes, travelTimes[3]);
                egressTime.setValueAt(rowOr, rowDes, travelTimes[4]);*/
                firstLegTime.setValueAt(originZoneId, destinationZoneId, travelTimes[0]);
                secondLegTime.setValueAt(originZoneId, destinationZoneId, travelTimes[1]);
                transferTime.setValueAt(originZoneId, destinationZoneId, travelTimes[2]);
                accessTime.setValueAt(originZoneId, destinationZoneId, travelTimes[3]);
                egressTime.setValueAt(originZoneId, destinationZoneId, travelTimes[4]);
                totalTime.setValueAt(originZoneId, destinationZoneId, travelTimes[5]);
                //rowDes++;
            }
            //rowOr++;
        }
        //String fileName, DataSet dataSet, Matrix matrix, String matrixName
        printSkim(fileNameOmxFirstLeg, dataSet, firstLegTime, "time");
        printSkim(fileNameOmxSecondLeg, dataSet, secondLegTime, "time");
        printSkim(fileNameOmxTransfer, dataSet, transferTime, "time");
        printSkim(fileNameOmxAccess, dataSet, accessTime, "time");
        printSkim(fileNameOmxEgress, dataSet, egressTime, "time");
        printSkim(fileNameOmxTotal, dataSet, totalTime, "time");
    }


    private float[] assignIntrazonalTrip(){
        float[] times = new float[6];
        times[0] = 100000000;
        times[1] = 0;
        times[2] = 0;
        times[3] = 0;
        times[4] = 0;
        times[5] = 0;
        return times;

    }

    private float getTotalTravelTime(DataSet dataSet, int originZoneId, int destinationZoneId, Airport originAirport, Airport destinationAirport) {
        float travelTimeBetweenAirports = dataSet.getFligthFromId(connectedAirports.get(originAirport).get(destinationAirport).get("flightId")).getTime() ;
        if (originZoneId < 11868) {
            travelTimeBetweenAirports = travelTimeBetweenAirports +
                    dataSet.getAutoTravelTime(originZoneId, originAirport.getZone().getId());
        }
        if (destinationZoneId < 11868) {
            travelTimeBetweenAirports = travelTimeBetweenAirports +
                    dataSet.getAutoTravelTime(destinationAirport.getZone().getId(), destinationZoneId);
        }
        return travelTimeBetweenAirports;
    }


    private float[] getTravelTimes(DataSet dataSet, int originZoneId, int destinationZoneId, Airport originAirport, Airport destinationAirport){
        float[] times = new float[6];
        if (originAirport.getId() != destinationAirport.getId()) {
            List<AirLeg> legs = dataSet.getFligthFromId(connectedAirports.get(originAirport).get(destinationAirport).get("flightId")).getLegs();
            times[0] = legs.get(0).getTime();
            times[1] = 0;
            times[2] = 0;
            if (legs.size() > 1) {
                times[1] = legs.get(1).getTime();
                ;
                times[2] = transferTimes.getIndexedValueAt(legs.get(0).getDestination().getId(), "transferTime");
            }
            if (originZoneId < 11868) {
                times[3] = dataSet.getAutoTravelTime(originZoneId, originAirport.getZone().getId());
            } else {
                times[3] = 0;
            }
            if (destinationZoneId < 11868) {
                times[4] = dataSet.getAutoTravelTime(destinationAirport.getZone().getId(), destinationZoneId);
            } else {
                times[4] = 0;
            }
            times[5] = times[0] + times[1] + times[2] + times[3] + times[4];
        } else {
            times = assignIntrazonalTrip();
        }
        return times;
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



    private void printSkim(String fileName, DataSet dataSet, Matrix matrix, String matrixName) {

        try {

            int dimension = dataSet.getZones().size();
            //int dimension = 4;
            OmxMatrixWriter.createOmxFile(fileName, dimension);
            OmxMatrixWriter.createOmxSkimMatrix(matrix,fileName, matrixName);


        } catch (ClassCastException e) {
            logger.info("Currently it is not possible to print out a matrix from an object which is not SkimTravelTime");
        }
    }
}
