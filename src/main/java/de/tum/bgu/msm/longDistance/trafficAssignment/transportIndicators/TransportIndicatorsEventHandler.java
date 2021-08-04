package de.tum.bgu.msm.longDistance.trafficAssignment.transportIndicators;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.mobsim.jdeqsim.Vehicle;

import java.util.HashMap;
import java.util.Map;


public class TransportIndicatorsEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {

    Map<Id<Vehicle>, Double> vehicleEnterLinkMap = new HashMap<>();
    Map<Id<Vehicle>, Double> vehicleDelayMap = new HashMap<>();
//    Map<Id<Vehicle>, Double> vehicleTotalTime = new HashMap<>();
//    Map<Id<Vehicle>, Double> vehicleFreeFlowTime = new HashMap<>();
    Map<CountVehicleType, Map<String, Double>> vktByVehicleAndRoadType = new HashMap<>();


    enum CountVehicleType {
        car_sd,car_ld, truck;
    }

    private final int LAST_HOUR = 49;
    private final Network network;
    private static Logger logger = Logger.getLogger(TransportIndicatorsEventHandler.class);

    private int thisIteration;

    public TransportIndicatorsEventHandler(Network network) {
        this.network = network;
        vktByVehicleAndRoadType.put(CountVehicleType.car_ld, new HashMap<>());
        vktByVehicleAndRoadType.put(CountVehicleType.car_sd, new HashMap<>());
        vktByVehicleAndRoadType.put(CountVehicleType.truck, new HashMap<>());
    }



    private int getHourFromTime(double time_s) {
        return (int) (time_s / 3600) > (LAST_HOUR - 1) ? LAST_HOUR : (int) Math.floor(time_s / 3600);
    }



    @Override
    public void handleEvent(LinkEnterEvent linkEnterEvent) {
        Id vehicleId = linkEnterEvent.getVehicleId();
        vehicleEnterLinkMap.put(vehicleId, linkEnterEvent.getTime());


    }

    @Override
    public void handleEvent(LinkLeaveEvent linkLeaveEvent) {
        Id linkId = linkLeaveEvent.getLinkId();
        Id vehicleId = linkLeaveEvent.getVehicleId();
        try{
            double travelTime =  linkLeaveEvent.getTime() - vehicleEnterLinkMap.get(vehicleId);
            vehicleEnterLinkMap.remove(vehicleId);
            int hour = getHourFromTime(linkLeaveEvent.getTime());
            final Link link = network.getLinks().get(linkId);
            double delay = travelTime - link.getLength() / link.getFreespeed();
            vehicleDelayMap.putIfAbsent(vehicleId, 0.);
            vehicleDelayMap.put(vehicleId, delay + vehicleDelayMap.get(vehicleId));

            CountVehicleType vehicleType = getTypeFromId(linkLeaveEvent.getVehicleId().toString());

            String linkType = link.getAttributes().getAttribute("type").toString();
            Map<String, Double> vktByRoadType = vktByVehicleAndRoadType.get(vehicleType);
            vktByRoadType.putIfAbsent(linkType, 0.);
            vktByRoadType.put(linkType, link.getLength() + vktByRoadType.get(linkType));
        } catch(NullPointerException e){
            //seems not critical, vehicle start in a link!
            //logger.info("The vehicle is already in a link");
        }

    }

    @Override
    public void reset(int iteration) {
        this.thisIteration = iteration;
        logger.info("Reset event handler at iteration " + thisIteration);
    }


    private static CountVehicleType getTypeFromId(String vehicleId){
        //todo review this
        if(vehicleId.contains("truck")){
            return CountVehicleType.truck;
        } else if (vehicleId.contains("ld")) {
            return CountVehicleType.car_ld;
        } else {
            return CountVehicleType.car_sd;
        }
    }


    public Map<Id<Vehicle>, Double> getVehicleDelayMap(){
        return vehicleDelayMap;
    }

    public Map<CountVehicleType, Map<String, Double>> getVktByVehicleAndRoadType() {
        return vktByVehicleAndRoadType;
    }
}
