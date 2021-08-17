package de.tum.bgu.msm.longDistance.trafficAssignment.counts;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkUtils;
import org.matsim.vehicles.Vehicle;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;


public class CountEventHandlerWithToll implements LinkEnterEventHandler {

    private Map<Id<Person>, DriverType> driverTypeMap;

    public void setPersonTypes(Map<Id<Person>, DriverType> driverTypeMap) {
        this.driverTypeMap = driverTypeMap;
    }

    private enum CountVehicleType {
        car_sd, car_ld, truck;
    }

    private final int LAST_HOUR = 49;

    private static Logger logger = Logger.getLogger(CountEventHandlerWithToll.class);

    private int thisIteration;

    private Map<Id, Map<Integer, Map<CountVehicleType, Map<DriverType, Integer>>>> listOfSelectedLinks = new HashMap<>();

    private int getHourFromTime(double time_s) {
        return (int) (time_s / 3600) > (LAST_HOUR - 1) ? LAST_HOUR : (int) Math.floor(time_s / 3600);
    }

    public void addLinkById(Id linkId) {
        Map<Integer, Map<CountVehicleType, Map<DriverType, Integer>>> countsByHour = new HashMap<>();
        for (int i = 0; i < LAST_HOUR + 1; i++) {
            Map<CountVehicleType, Map<DriverType, Integer>> countsByVehicleType = new HashMap<>();
            for (CountVehicleType vehicleType : CountVehicleType.values()) {
                Map<DriverType, Integer> countsByDriverType = new HashMap<>();
                for (DriverType driverType : DriverType.values()) {
                    countsByDriverType.put(driverType, 0);
                }
                countsByVehicleType.put(vehicleType, countsByDriverType);
            }
            countsByHour.put(i, countsByVehicleType);
        }
        listOfSelectedLinks.put(linkId, countsByHour);
    }


    @Override
    public void handleEvent(LinkEnterEvent linkEnterEvent) {
        Id id = linkEnterEvent.getLinkId();
        int hour = getHourFromTime(linkEnterEvent.getTime());
        CountVehicleType vehicleType = getTypeFromId(linkEnterEvent.getVehicleId().toString());
        final Id<Vehicle> vehicleId = linkEnterEvent.getVehicleId();
        String transformedVehicleId = vehicleId.toString().replace("_truck", "");
        DriverType driverType = driverTypeMap.get(Id.createPersonId(transformedVehicleId)); //assumes person and vehicle having same id
        if (driverType == null){
            System.out.println("?");
        }
        if (listOfSelectedLinks.containsKey(id)) {
            listOfSelectedLinks.get(id).get(hour).get(vehicleType).put(driverType, listOfSelectedLinks.get(id).get(hour).get(vehicleType).get(driverType) + 1);
        }
    }

    @Override
    public void reset(int iteration) {
        this.thisIteration = iteration;
        for (Id id : listOfSelectedLinks.keySet()) {
            addLinkById(id);
        }
        logger.info("Reset event handler at iteration " + thisIteration);
    }


    public void printOutCounts(String countsFile, String networkFile) throws IOException {
        PrintWriter pw = new PrintWriter(new FileWriter(countsFile));

        Network network = NetworkUtils.readNetwork(networkFile);

        pw.print("link,hour,length,type,vehicle_type,driver_type,count");
        pw.println();

        for (Id id : listOfSelectedLinks.keySet()) {
            Map<Integer, Map<CountVehicleType, Map<DriverType, Integer>>> countsByHour = listOfSelectedLinks.get(id);
            for (int hour : countsByHour.keySet()) {
                final Map<CountVehicleType, Map<DriverType, Integer>> countsByVehicleAndDriverType = countsByHour.get(hour);
                for (CountVehicleType vehicleType : countsByVehicleAndDriverType.keySet()) {
                    for (DriverType driverType : countsByVehicleAndDriverType.get(vehicleType).keySet()) {
                        pw.print(id.toString());
                        pw.print(",");
                        pw.print(hour);
                        pw.print(",");
                        Link link = network.getLinks().get(id);
                        pw.print(link.getLength());
                        pw.print(",");
                        pw.print(link.getAttributes().getAttribute("type"));
                        pw.print(",");
                        pw.print(vehicleType);
                        pw.print(",");
                        pw.print(driverType);
                        pw.print(",");
                        pw.print(countsByVehicleAndDriverType.get(vehicleType).get(driverType));
                        pw.println();
                    }
                }
            }
        }
        pw.close();
    }

    private static CountVehicleType getTypeFromId(String vehicleId) {
        //todo review this
        if (vehicleId.contains("truck")) {
            return CountVehicleType.truck;
        } else if (vehicleId.contains("ld")) {
            return CountVehicleType.car_ld;
        } else {
            return CountVehicleType.car_sd;
        }
    }

}
