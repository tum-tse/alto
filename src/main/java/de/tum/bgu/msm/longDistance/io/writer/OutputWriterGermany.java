package de.tum.bgu.msm.longDistance.io.writer;

import com.google.common.collect.TreeMultiset;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.LDModelGermany;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.trips.*;
import de.tum.bgu.msm.longDistance.data.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneTypeGermany;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Germany Model
 * Class to write outputs
 * Author: Ana Moreno, Technical University of Munich (TUM), ana.moreno@tum.de
 * Date: 8 December 2020
 * Version 1
 * Adapted from Ontario
 * Version 1
 */

public class OutputWriterGermany implements OutputWriter {


    private DataSet dataSet;
    private String outputFile;
    private String outputFolder;
    private String outputSubFolder;
    private float increaseAirCost;
    private boolean increaseCostScenario;
    private boolean limitAirDistanceScenario;
    private int airDistanceThreshold;
    private String outputFileScenarioSettings;
    private String outputFileName;
    static Logger logger = Logger.getLogger(OutputWriterGermany.class);

    @Override
    public void setup(JSONObject prop, String inputFolder, String outputFolderInput) {
        outputFolder = outputFolderInput;
        //outputFile = outputFolder + dataSet.getPopulationSection() + JsonUtilMto.getStringProp(prop, "output.trip_file") ;
        outputFileScenarioSettings = outputFolder + "scenarioSettings.csv";
        //increaseCostScenario = JsonUtilMto.getBooleanProp(prop,"scenarioPolicy.boolean_increaseCost");
        //increaseAirCost = JsonUtilMto.getIntProp(prop,"scenarioPolicy.costIncreasePercentage");
        //limitAirDistanceScenario = JsonUtilMto.getBooleanProp(prop,"scenarioPolicy.boolean_limitDistance");
        //airDistanceThreshold = JsonUtilMto.getIntProp(prop,"scenarioPolicy.distanceThreshold");
    }

    @Override
    public void load(DataSet dataSet) {
        this.dataSet = dataSet;
    }

    @Override
    public void run(DataSet dataSet, int nThreads) {
        this.outputFileName = dataSet.getScenarioSettings().getStringValueAt(dataSet.getScenario(), "remark");
        this.outputFile = outputFolder + "/" + outputFileName + ".csv";

        PrintWriter pw = Util.openFileForSequentialWriting(outputFile, false);
        pw.println(LongDistanceTripGermany.getHeader());
        for (LongDistanceTrip tr : dataSet.getAllTrips()) {
            LongDistanceTripGermany trip = (LongDistanceTripGermany) tr;
            if (!trip.getTripState().equals(TypeGermany.AWAY)) {
                //if (!trip.getDestZoneType().equals(ZoneTypeGermany.EXTOVERSEAS)){
                pw.println(trip.toString());
                //}
            }
        }
        pw.close();
        logger.info("---------------------" + outputFileName + "completed" + "---------------------");

    }

    private static void writeCharts(DataSet dataSet, PurposeGermany purpose, String outputFolder) {
        String outputSubDirectory = outputFolder;

        List<Double> travelTimes = new ArrayList<>();
        List<Double> travelDistances = new ArrayList<>();
        List<Double> travelDistancesAway = new ArrayList<>();
        List<Double> travelDistancesOvernight = new ArrayList<>();
        List<Double> travelDistancesDaytrip = new ArrayList<>();
        Map<Integer, List<Double>> distancesByZone = new HashMap<>();
        Multiset<Zone> tripsByZone = HashMultiset.create();

        for (LongDistanceTrip t : dataSet.getAllTrips()) {
            LongDistanceTripGermany trip = (LongDistanceTripGermany) t;
            final int tripOrigin = trip.getOrigZone().getId();
            if (trip.getTripPurpose() == purpose && trip.getDestZone() != null) {
                final int tripDestination = trip.getDestZone().getId();
                travelTimes.add((double) dataSet.getTravelTimeMatrix().get(ModeGermany.AUTO).getValueAt(tripOrigin, tripDestination));
                double travelDistance = dataSet.getDistanceMatrix().get(ModeGermany.AUTO).getValueAt(tripOrigin, tripDestination);
                travelDistances.add(travelDistance);
                if (trip.getTripState().equals(TypeGermany.DAYTRIP)) {
                    travelDistancesDaytrip.add(travelDistance);
                } else if (trip.getTripState().equals(TypeGermany.OVERNIGHT)) {
                    travelDistancesOvernight.add(travelDistance);
                } else if (trip.getTripState().equals(TypeGermany.AWAY)) {
                    travelDistancesAway.add(travelDistance);
                }
                tripsByZone.add(dataSet.getZones().get(tripOrigin));
                if (distancesByZone.containsKey(tripOrigin)) {
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
        double[] travelDistancesOvernightArray = new double[travelTimes.size()];
        double[] travelDistancesDaytripArray = new double[travelTimes.size()];
        double[] travelDistancesAwayArray = new double[travelTimes.size()];
        i = 0;
        for (Double value : travelDistances) {
            travelDistancesArray[i] = value / 1000; //convert meters to km
            travelDistancesOvernightArray[i] = value / 1000; //convert meters to km
            travelDistancesDaytripArray[i] = value / 1000; //convert meters to km
            travelDistancesAwayArray[i] = value / 1000; //convert meters to km
            i++;
        }
        //Histogram.createFrequencyHistogram(outputFolder + "tripTimeDistribution"+ purpose, travelTimesArray, "Travel Time Distribution " + purpose, "Auto Travel Time (h)", "Frequency", 6, 0, 6);

        // Histogram.createFrequencyHistogram(outputFolder + "tripDistanceDistribution"+ purpose, travelDistancesArray, "Travel Distance Distribution " + purpose, "Distance (km)", "Frequency", 100, 0, 1000);

        //.createFrequencyHistogram(outputFolder + "tripDistanceDistributionDaytrip"+ purpose, travelDistancesDaytripArray, "Travel Distance Distribution for daytrips " + purpose, "Distance (km)", "Frequency", 100, 0, 1000);

        //Histogram.createFrequencyHistogram(outputFolder + "tripDistanceDistributionOvernighttrip"+ purpose, travelDistancesOvernightArray, "Travel Distance Distribution for overnighttrips " + purpose, "Distance (km)", "Frequency", 100, 0, 1000);

        //Histogram.createFrequencyHistogram(outputFolder + "tripDistanceDistributionAwaytrip"+ purpose, travelDistancesAwayArray, "Travel Distance Distribution for away " + purpose, "Distance (km)", "Frequency", 100, 0, 1000);

    }

    private void generatePieCharts() {

        Map<Purpose, List<LongDistanceTrip>> tripsByPurpose = (Map<Purpose, List<LongDistanceTrip>>) dataSet.getAllTrips().stream()
                .filter(trip -> {
                    try {
                        TypeGermany tripState = (TypeGermany) ((LongDistanceTripGermany) trip).getTripState();
                        if (tripState.equals(TypeGermany.DAYTRIP)) {
                            return true;
                        } else {
                            return false;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                })
                .collect(Collectors.groupingBy(LongDistanceTrip::getTripPurpose));

        TreeMultiset<Comparable> purposes = TreeMultiset.create();
        for (Purpose purpose1 : PurposeGermany.values()) {
            purposes.add((Comparable) purpose1, tripsByPurpose.get(purpose1).size());
        }
        //PieChart.createPieChart(outputFolder + "tripGeneration", purposes, "Generated Trips By Purpose ");

        Map<Type, List<LongDistanceTrip>> tripsByPurpose1 = (Map<Type, List<LongDistanceTrip>>) dataSet.getAllTrips().stream()
                .collect(Collectors.groupingBy(LongDistanceTrip::getTripState));

        tripsByPurpose1.forEach((statePurpose, trips) -> {
                    TreeMultiset<Comparable> purposesTree = TreeMultiset.create();
                    final long totalTrips = trips.size();
                    trips.parallelStream()
                            //group number of trips by mode
                            .collect(Collectors.groupingBy(LongDistanceTrip::getTripPurpose, Collectors.counting()))
                            //calculate and add share to data set table
                            .forEach((purpose1, count) -> {
                                        purposesTree.add((Comparable) purpose1, (int) (((double) count / totalTrips) * 100.));
                                    }
                            );
                    //PieChart.createPieChart(outputFolder  + "tripGeneration" + statePurpose, purposesTree, "Trip Generation " + statePurpose);
                }
        );

        for (Purpose purpose1 : PurposeGermany.values()) {
            Map<Type, List<LongDistanceTrip>> tripsByPurpose2 = (Map<Type, List<LongDistanceTrip>>) dataSet.getAllTrips().stream()
                    .filter(trip -> {
                        try {
                            PurposeGermany tripState = (PurposeGermany) ((LongDistanceTripGermany) trip).getTripPurpose();
                            if (tripState.equals(purpose1)) {
                                return true;
                            } else {
                                return false;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            return false;
                        }
                    })
                    .collect(Collectors.groupingBy(LongDistanceTrip::getTripState));
            tripsByPurpose2.forEach((purposeMode, trips) -> {
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
                        // PieChart.createPieChart(outputFolder  + "modeChoice_" + purposeMode + "_" + purpose1, modes, "Mode Choice of " + purposeMode + " " + purpose1);
                    }
            );
        }
    }
}
