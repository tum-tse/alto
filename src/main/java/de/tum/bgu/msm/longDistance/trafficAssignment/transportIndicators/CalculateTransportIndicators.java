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
import java.io.FileReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CalculateTransportIndicators {

    private static Logger logger = Logger.getLogger(CalculateTransportIndicators.class);

    public static void main(String[] args) {


        String networkFile = args[0];

        String eventsFile = args[1];


        EventsManager eventsManager = EventsUtils.createEventsManager();
        TransportIndicatorsEventHandler eventHandler = new TransportIndicatorsEventHandler(NetworkUtils.readNetwork(networkFile));

        eventsManager.addHandler(eventHandler);
        new MatsimEventsReader(eventsManager).readFile(eventsFile);
        final Map<Id<Vehicle>, Double> vehicleDelayMap = eventHandler.getVehicleDelayMap();

        double delay = 0;
        for (double thisDelay : vehicleDelayMap.values()) {
            delay += thisDelay;
        }

        logger.info("The total delay is " + delay/3600 + " hours.");
        logger.info("The number of vehicles is " + vehicleDelayMap.size() + ".");
        logger.info("The average delay is " +  delay/60/vehicleDelayMap.size() + " minutes.");


        final Map<TransportIndicatorsEventHandler.CountVehicleType, Map<String, Double>> vktByVehicleAndRoadType = eventHandler.getVktByVehicleAndRoadType();

        vktByVehicleAndRoadType.keySet().forEach(vehicleType -> {
            vktByVehicleAndRoadType.get(vehicleType).keySet().forEach(roadType -> {
                double vkt = vktByVehicleAndRoadType.get(vehicleType).get(roadType);
                logger.info("vehicle_type," + vehicleType + ",road_type," + roadType + ",vkt," + vkt);
            });
        });

    }
}
