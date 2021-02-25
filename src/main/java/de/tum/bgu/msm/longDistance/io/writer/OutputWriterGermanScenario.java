package de.tum.bgu.msm.longDistance.io.writer;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;
import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.trips.*;
import de.tum.bgu.msm.longDistance.data.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.io.writer.charts.Histogram;
import de.tum.bgu.msm.longDistance.io.writer.charts.PieChart;
import de.tum.bgu.msm.longDistance.scaling.PotentialTravelersSelectionGermany;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

public class OutputWriterGermanScenario implements OutputWriter {


    private DataSet dataSet;
    private String outputFile;
    private String outputFolder;
    private float increaseAirCost;
    private float airDistanceThreshold;
    private String outputFileScenarioSettings;
    private TableDataSet scenarios;
    private static Logger logger = Logger.getLogger(OutputWriterGermanScenario.class);
    private boolean writeTrips;
    private boolean runSubpopulations;


    @Override
    public void setup(JSONObject prop, String inputFolder, String outputFolderInput) {
        outputFolder = outputFolderInput;
        outputFile = JsonUtilMto.getStringProp(prop, "output.trip_file");
        scenarios = Util.readCSVfile(inputFolder + JsonUtilMto.getStringProp(prop,"scenarioPolicy.scenarios"));
        writeTrips = JsonUtilMto.getBooleanProp(prop,"output.write_trips");
        runSubpopulations = JsonUtilMto.getBooleanProp(prop, "synthetic_population.runSubpopulations");
    }

    @Override
    public void load(DataSet dataSet) {
        this.dataSet = dataSet;
    }

    @Override
    public void run(DataSet dataSet, int nThreads) {

        if (writeTrips) {
            int scenario = dataSet.getScenario();
            String outputFolderScenario = outputFolder + "/" + scenarios.getStringValueAt(scenario, "scenario");
            String fileName = outputFolderScenario + "_" + outputFile;
            if (runSubpopulations){
                fileName = outputFolderScenario + "_p" + dataSet.getPopulationSection() + "_" + outputFile;
            }
            PrintWriter pw = Util.openFileForSequentialWriting(fileName, false);
            pw.println(LongDistanceTripGermany.getHeader());
            for (LongDistanceTrip tr : dataSet.getTripsofPotentialTravellers()) {
                pw.println(tr.toString());
            }
            pw.close();
        }

        //summarizeTripLengthFrequencyDistribution(dataSet, outputFolder);
        summarizeModalShares();
        //writeSummaryByScenarioBySubpopulation(scenario);
/*        for (PurposeGermany purpose : PurposeGermany.values()){
            writeCharts(dataSet, purpose, outputFolder);
        }
        generatePieCharts();*/
    }


    public void writeSummaryByScenarioBySubpopulation(int scenarioId){

        ArrayList<LongDistanceTrip> trips = dataSet.getTripsofPotentialTravellers();
        Map<Mode, Integer> modalCountByMode = new HashMap<>();
        Map<Mode, Float> co2EmissionsByMode = new HashMap<>();
        Map<Mode, Map<Integer, Integer>> distanceByMode = new HashMap<>();
        for (Mode m : ModeGermany.values()){
            modalCountByMode.putIfAbsent(m,0);
            Stream<LongDistanceTrip> tripsByMode = trips.stream().filter(x -> x.getMode().equals(m));
            int updatedTrips = modalCountByMode.get(m) +
                    (int) tripsByMode.count();
            modalCountByMode.put(m, updatedTrips);
            co2EmissionsByMode.putIfAbsent(m,0.f);
            float updatedEmissions = co2EmissionsByMode.get(m) +
                    (float) tripsByMode.mapToDouble(x->((LongDistanceTripGermany)x).getEmissions().get(Pollutant.CO2)).sum();
            co2EmissionsByMode.put(m, updatedEmissions);
            int countPreviousDistance = 0;
            for (int distanceBin: dataSet.getDistanceBins()){
                int countDistanceShorterThan = (int) tripsByMode.filter(x->((LongDistanceTripGermany)x).getDistanceByMode() <= distanceBin).count();
                distanceByMode.putIfAbsent(m, new HashMap<>());
                distanceByMode.get(m).putIfAbsent(distanceBin,0);
                int updatedTripDistance = distanceByMode.get(m).get(distanceBin) +
                        countDistanceShorterThan - countPreviousDistance;
                distanceByMode.get(m).put(distanceBin, updatedTripDistance);
                countPreviousDistance = countDistanceShorterThan;
            }
        }
        //dataSet.getModalCountByModeByScenario().put(scenarioId, modalCountByMode);
        //dataSet.getCo2EmissionsByModeByScenario().put(scenarioId, co2EmissionsByMode);
        //dataSet.getDistanceByModeByScenario().put(scenarioId, distanceByMode);
    }

    private static void summarizeTripLengthFrequencyDistribution(DataSet dataSet, String outputFolder) {
        String outputSubDirectory = outputFolder;

        for (PurposeGermany purpose : PurposeGermany.values()) {
            List<Double> travelTimes = new ArrayList<>();
            List<Double> travelDistances = new ArrayList<>();
            List<Double> travelDistancesAway = new ArrayList<>();
            List<Double> travelDistancesOvernight = new ArrayList<>();
            List<Double> travelDistancesDaytrip = new ArrayList<>();
            Map<Integer, List<Double>> distancesByZone = new HashMap<>();
            Multiset<Zone> tripsByZone = HashMultiset.create();

            for (LongDistanceTrip t : dataSet.getTripsofPotentialTravellers()) {
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


            Histogram.createFrequencyHistogram(outputFolder + "tripTimeDistribution" + purpose, travelTimesArray, "Travel Time Distribution " + purpose, "Auto Travel Time (h)", "Frequency", 6, 0, 6);

            Histogram.createFrequencyHistogram(outputFolder + "tripDistanceDistribution" + purpose, travelDistancesArray, "Travel Distance Distribution " + purpose, "Distance (km)", "Frequency", 100, 0, 1000);

            Histogram.createFrequencyHistogram(outputFolder + "tripDistanceDistributionDaytrip" + purpose, travelDistancesDaytripArray, "Travel Distance Distribution for daytrips " + purpose, "Distance (km)", "Frequency", 100, 0, 1000);

            Histogram.createFrequencyHistogram(outputFolder + "tripDistanceDistributionOvernighttrip" + purpose, travelDistancesOvernightArray, "Travel Distance Distribution for overnighttrips " + purpose, "Distance (km)", "Frequency", 100, 0, 1000);

            Histogram.createFrequencyHistogram(outputFolder + "tripDistanceDistributionAwaytrip" + purpose, travelDistancesAwayArray, "Travel Distance Distribution for away " + purpose, "Distance (km)", "Frequency", 100, 0, 1000);
        }

    }

    private void summarizeTripGeneration() {

        Map<Purpose, List<LongDistanceTrip>> tripsByPurpose = (Map<Purpose, List<LongDistanceTrip>>) dataSet.getTripsofPotentialTravellers().stream()
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
        PieChart.createPieChart(outputFolder + "tripGeneration", purposes, "Generated Trips By Purpose ");

        Map<Type, List<LongDistanceTrip>> tripsByPurpose1 = (Map<Type, List<LongDistanceTrip>>) dataSet.getTripsofPotentialTravellers().stream()
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
                    PieChart.createPieChart(outputFolder + "tripGeneration" + statePurpose, purposesTree, "Trip Generation " + statePurpose);
                }
        );
    }

    private void summarizeModalShares() {
        int scenario = dataSet.getScenario();
        for (Purpose purpose1 : PurposeGermany.values()){
            Map<Type, List<LongDistanceTrip>> tripsByPurpose2 = (Map<Type, List<LongDistanceTrip>>) dataSet.getTripsofPotentialTravellers().stream()
                    .filter(trip -> {
                        try {
                            PurposeGermany tripState = (PurposeGermany) ((LongDistanceTripGermany)trip).getTripPurpose();
                            ModeGermany mode = (ModeGermany) ((LongDistanceTripGermany)trip).getMode();
                            if (tripState.equals(purpose1) && mode!=null) {
                                return true;
                            } else {
                                //TODO. Check for the ODs that have no available mode and remove them from the trip list
                                //we are excluding the trips that have null as mode
                                return false;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            return false;
                        }
                    })
                    .collect(Collectors.groupingBy(LongDistanceTrip::getTripState));
            tripsByPurpose2.forEach((type, trips) -> {
                Map<Type, Map<Purpose, Map<Mode, Integer>>> modes = dataSet.getModalCountByModeByScenario().get(scenario);
                Map<Type, Map<Purpose, Map<Mode, Float>>> emissions = dataSet.getCo2EmissionsByModeByScenario().get(scenario);
                Map<Type, Map<Purpose, Map<Mode, Map<Integer, Integer>>>> modesByDistance = dataSet.getModalCountByModeByScenarioByDistance().get(scenario);
                Map<Type, Map<Purpose, Map<Mode, Map<Integer, Float>>>> emissionsByDistance = dataSet.getCo2EmissionsByModeByScenarioByDistance().get(scenario);
                final long totalTrips = trips.size();
                trips.parallelStream()
                        //group number of trips by mode
                        .collect(Collectors.groupingBy(LongDistanceTrip::getMode, Collectors.counting()))
                        //calculate and add trips to data set table
                        .forEach((mode, count) -> {
                                    int previousValue = modes.get(type).get(purpose1).get(mode);
                                    modes.get(type).get(purpose1).put(mode, previousValue + (int) (double) count);
                                }
                        );
                trips.parallelStream()
                        //group number of trips by mode
                        .collect(Collectors.groupingBy(LongDistanceTrip::getMode,
                                Collectors.summarizingDouble(LongDistanceTrip::getCO2emissions)))
                        .forEach((mode, value) ->{
                                    float previousValue = emissions.get(type).get(purpose1).get(mode);
                                    emissions.get(type).get(purpose1).put(mode, previousValue + (float)value.getSum());
                                }
                        );
                dataSet.getModalCountByModeByScenario().put(scenario, modes);
                dataSet.getCo2EmissionsByModeByScenario().put(scenario, emissions);
                //PieChart.createPieChart(outputFolder  + "modeChoice_" + purposeMode + "_" + purpose1, modes, "Mode Choice of " + purposeMode + " " + purpose1);
                }
            );
        }
    }
}
