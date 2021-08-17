package de.tum.bgu.msm.longDistance.trafficAssignment.counts;

import de.tum.bgu.msm.Util;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ExtractCountsFromEventsWithToll {


    public static void main(String[] args) throws IOException {


        String inputLinksFile = args[0];
        String networkFile = args[1];
        String eventsFile = args[2];
        String countsFile = args[3];
        String personFile = args[4];

        Set<Id> linkIds = new HashSet<>();

        BufferedReader br = new BufferedReader(new FileReader(inputLinksFile));
        int idIndex = Util.findPositionInArray("linkId", br.readLine().split(","));

        String line;

        while((line = br.readLine())!= null) {
            linkIds.add(Id.createLinkId(line.split(",")[idIndex]));
        }


        BufferedReader br2 = new BufferedReader(new FileReader(personFile));

        String header = br2.readLine();
        boolean existsTollVariable = true;
        int personIdIndex = Util.findPositionInArray("person", header.split(";"));
        int avoidTollIndex = 0;
        try{
            avoidTollIndex = Util.findPositionInArray("avoidTOLL", header.split(";"));
        } catch (Exception e){
            existsTollVariable = false;
        }

        Map<Id<Person>, DriverType> driverTypeMap = new HashMap<>();

        while((line = br2.readLine())!= null) {
            Id<Person> personId = Id.createPersonId(line.split(";")[personIdIndex]);
            DriverType driverType;
            if (existsTollVariable){
                boolean avoidTollValue;
                try{
                    avoidTollValue = Boolean.parseBoolean(line.split(";")[avoidTollIndex]);
                } catch (IndexOutOfBoundsException e){
                    avoidTollValue = false;
                }
                if (avoidTollValue){
                    driverType = DriverType.avoid_toll;
                } else {
                    driverType = DriverType.accept_toll;
                }
            } else {
                driverType = DriverType.accept_toll;
            }
            driverTypeMap.put(personId, driverType);
        }


        EventsManager eventsManager = EventsUtils.createEventsManager();
        CountEventHandlerWithToll countEventHandler = new CountEventHandlerWithToll();

        for (Id linkId :linkIds) {
            countEventHandler.addLinkById(linkId);
        }
        countEventHandler.setPersonTypes(driverTypeMap);

        eventsManager.addHandler(countEventHandler);
        new MatsimEventsReader(eventsManager).readFile(eventsFile);
        countEventHandler.printOutCounts(countsFile, networkFile);
    }


}

