package de.tum.bgu.msm.longDistance.io;

import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.trips.*;
import de.tum.bgu.msm.longDistance.data.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneGermany;
import de.tum.bgu.msm.longDistance.io.writer.charts.Histogram;
import de.tum.bgu.msm.longDistance.io.writer.charts.PieChart;
import de.tum.bgu.msm.longDistance.io.writer.charts.ScatterPlot;
import org.json.simple.JSONObject;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.math.Stats;
import org.locationtech.jts.geom.Geometry;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * Germany Model
 * Class to write outputs
 * Author: Ana Moreno, Technical University of Munich (TUM), ana.moreno@tum.de
 * Date: 8 December 2020
 * Version 1
 * Adapted from Ontario
 * Version 1
 *
 */

public class OutputWriterGermany implements OutputWriter {


    private DataSet dataSet;
    private String outputFile;
    private String outputFolder;


    @Override
    public void setup(JSONObject prop, String inputFolder, String outputFolderInput) {
        outputFile = outputFolder + JsonUtilMto.getStringProp(prop, "output.trip_file");
        outputFolder = outputFolderInput;
    }

    @Override
    public void load(DataSet dataSet) {
        this.dataSet = dataSet;
    }

    @Override
    public void run(DataSet dataSet, int nThreads) {
        PrintWriter pw = Util.openFileForSequentialWriting(outputFile, false);
        pw.println(LongDistanceTripGermany.getHeader());
        for (LongDistanceTrip tr : dataSet.getAllTrips()) {
            pw.println(tr.toString());
        }
        pw.close();
        for (PurposeGermany purpose : PurposeGermany.values()){
            writeCharts(dataSet, purpose, outputFolder);
        }

    }

    private static void writeCharts(DataSet dataSet, PurposeGermany purpose, String outputFolder) {
        String outputSubDirectory = outputFolder;

        List<Double> travelTimes = new ArrayList<>();
        List<Double> travelDistances = new ArrayList<>();
        Map<Integer, List<Double>> distancesByZone = new HashMap<>();
        Multiset<Zone> tripsByZone = HashMultiset.create();

        for (LongDistanceTrip t : dataSet.getAllTrips()) {
            LongDistanceTripGermany trip = (LongDistanceTripGermany) t;
            final int tripOrigin = trip.getOrigZone().getId();
            if (trip.getTripPurpose() == purpose && trip.getDestZone() != null) {
                final int tripDestination = trip.getDestZone().getId();
                travelTimes.add((double) dataSet.getAutoTravelTime(tripOrigin, tripDestination));
                double travelDistance = dataSet.getAutoTravelDistance(tripOrigin, tripDestination);
                travelDistances.add(travelDistance);
                tripsByZone.add(dataSet.getZones().get(tripOrigin));
                if(distancesByZone.containsKey(tripOrigin)){
                    distancesByZone.get(tripOrigin).add(travelDistance);
                } else {
                    List<Double> values = new ArrayList<>();
                    values.add(travelDistance);
                    distancesByZone.put(tripOrigin, values);
                }
            }
        }

        double[] travelTimesArray = new double[travelTimes.size()];
        int i = 0;
        for (Double value : travelTimes) {
            travelTimesArray[i] = value / 3600; //convert seconds to hours
            i++;
        }

        double[] travelDistancesArray = new double[travelTimes.size()];
        i= 0;
        for(Double value: travelDistances) {
            travelDistancesArray[i] = value / 1000; //convert meters to km
            i++;
        }
        Histogram.createFrequencyHistogram(outputFolder + "tripTimeDistribution"+ purpose, travelTimesArray, "Travel Time Distribution " + purpose, "Time", "Frequency", 12, 0, 12);

        Histogram.createFrequencyHistogram(outputFolder + "tripDistanceDistribution"+ purpose, travelDistancesArray, "Travel Distance Distribution " + purpose, "Distance", "Frequency", 120, 0, 1200);




        Map<Purpose, List<LongDistanceTrip>> tripsByPurpose = (Map<Purpose, List<LongDistanceTrip>>) dataSet.getAllTrips().stream()
                .collect(Collectors.groupingBy(LongDistanceTrip::getTripPurpose));

        tripsByPurpose.forEach((purposeMode, trips) -> {
                    TreeMultiset<Comparable> modes = TreeMultiset.create();
                    final long totalTrips = trips.size();
                    trips.parallelStream()
                            //group number of trips by mode
                            .collect(Collectors.groupingBy(LongDistanceTrip::getMode, Collectors.counting()))
                            //calculate and add share to data set table
                            .forEach((mode, count) -> {
                                modes.add((Comparable) mode, (int) (((double) count / totalTrips) * 100.));
                                    }
                            );
                    PieChart.createPieChart(outputFolder + purpose, modes, "Mode Choice " + purpose);
                }
        );
    }
}
