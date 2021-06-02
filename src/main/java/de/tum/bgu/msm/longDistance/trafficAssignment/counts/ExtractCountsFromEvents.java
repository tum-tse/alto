package de.tum.bgu.msm.longDistance.trafficAssignment.counts;

import de.tum.bgu.msm.Util;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ExtractCountsFromEvents {


    public static void main(String[] args) throws IOException {


        String inputLinksFile = args[0];
        String networkFile = args[1];
        String eventsFile = args[2];
        String countsFile = args[3];

        Set<Id> linkIds = new HashSet<>();

        BufferedReader br = new BufferedReader(new FileReader(inputLinksFile));
        int idIndex = Util.findPositionInArray("linkId", br.readLine().split(","));

        String line;

        while((line = br.readLine())!= null) {
            linkIds.add(Id.createLinkId(line.split(",")[idIndex]));
        }


        EventsManager eventsManager = EventsUtils.createEventsManager();
        CountEventHandler countEventHandler = new CountEventHandler();

        for (Id linkId :linkIds) {
            countEventHandler.addLinkById(linkId);
        }

        eventsManager.addHandler(countEventHandler);
        new MatsimEventsReader(eventsManager).readFile(eventsFile);
        countEventHandler.printOutCounts(countsFile, networkFile);
    }


}

