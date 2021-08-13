package de.tum.bgu.msm.longDistance.trafficAssignment.transportIndicators;

import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.trafficAssignment.counts.CountEventHandler;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.mobsim.jdeqsim.Vehicle;
import org.matsim.core.network.NetworkUtils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CalculateTransportIndicators {

    private static Logger logger = Logger.getLogger(CalculateTransportIndicators.class);

    public static void main(String[] args) throws FileNotFoundException {


        String networkFile = args[0];

        String eventsFile = args[1];

        String outputFile = args[2];


        EventsManager eventsManager = EventsUtils.createEventsManager();
        TransportIndicatorsEventHandler eventHandler = new TransportIndicatorsEventHandler(NetworkUtils.readNetwork(networkFile));

        eventsManager.addHandler(eventHandler);
        new MatsimEventsReader(eventsManager).readFile(eventsFile);
        final Map<Id<Vehicle>, Double> vehicleDelayMap = eventHandler.getVehicleDelayMap();

        PrintWriter printWriter = new PrintWriter(outputFile);
        printWriter.println("indicator,vehicle_type,osm_type,value");

        double delay = 0;
        for (double thisDelay : vehicleDelayMap.values()) {
            delay += thisDelay;
        }

        printWriter.println("total_delay,all,all," + delay);
        printWriter.println("total_vehicles,all,all," + vehicleDelayMap.size());


        final Map<TransportIndicatorsEventHandler.CountVehicleType, Map<String, Map<Id<Vehicle>, Double>>> delayByVehicleAndRoadType = eventHandler.getDelayByVehicleAndRoadType();
        delayByVehicleAndRoadType.keySet().forEach(vehicleType -> {
            delayByVehicleAndRoadType.get(vehicleType).keySet().forEach(roadType -> {
                final Map<Id<Vehicle>, Double> delayMap = delayByVehicleAndRoadType.get(vehicleType).get(roadType);

                double delayThisType = 0;
                for (double thisDelay : delayMap.values()) {
                    delayThisType += thisDelay;
                }
                printWriter.println("delay," + vehicleType + "," + roadType + "," + delayThisType);
                printWriter.println("vehicles," + vehicleType + "," + roadType + "," + delayMap.size());
            });
        });


        final Map<TransportIndicatorsEventHandler.CountVehicleType, Map<String, Double>> vktByVehicleAndRoadType = eventHandler.getVktByVehicleAndRoadType();

        vktByVehicleAndRoadType.keySet().forEach(vehicleType -> {
            vktByVehicleAndRoadType.get(vehicleType).keySet().forEach(roadType -> {
                double vkt = vktByVehicleAndRoadType.get(vehicleType).get(roadType);
                printWriter.println("vkt," + vehicleType + "," + roadType + "," + vkt);


            });
        });

        printWriter.close();

    }
}
