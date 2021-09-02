package de.tum.bgu.msm.longDistance.accessibilityAnalysis;

import com.google.common.math.LongMath;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.airport.AirLeg;
import de.tum.bgu.msm.longDistance.data.airport.Airport;
import de.tum.bgu.msm.longDistance.data.trips.*;
import de.tum.bgu.msm.longDistance.data.zoneSystem.*;
import de.tum.bgu.msm.longDistance.io.reader.SkimsReaderOntario;
import org.apache.log4j.Logger;
import org.ejml.data.Matrix;
import org.json.simple.JSONObject;

import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by Ana Moreno on 17/08/2021.
 */
public class AccessibilityAnalysisGermany {


    static Logger logger = Logger.getLogger(AccessibilityAnalysisGermany.class);
    private DataSet dataSet;
    private AtomicInteger atomicInteger;

    private boolean runScenario1;
    private float shuttleBusCostPerKm;
    private float shuttleBusCostBase;

    private boolean runScenario2;
    private float busCostFactor;

    private boolean runScenario3;
    private boolean runTollScenario;
    private float toll;
    private boolean tollOnBundesstrasse;

    private TableDataSet coefficients;
    private TableDataSet costsPerKm;

    Map<Mode, Map<Zone, Map<String, Map<Integer, Double>>>> accessibilityByModeZone = new ConcurrentHashMap<>();

    private final ConcurrentMap<Mode, Matrix> matricesByMode = new ConcurrentHashMap<>();

    private float alphaAuto;
    private float betaAuto;
    private String outputFolder;
    private String outputFileName;

    public AccessibilityAnalysisGermany(){

    }




    public void setup(JSONObject prop, String inputFolder, String outputFolder) {

        coefficients = Util.readCSVfile(inputFolder + JsonUtilMto.getStringProp(prop, "accessibility.coef_file"));
        coefficients.buildIndex(1);
        costsPerKm = Util.readCSVfile(inputFolder + JsonUtilMto.getStringProp(prop, "mode_choice.costPerKm_file"));
        costsPerKm.buildStringIndex(2);
        outputFileName = JsonUtilMto.getStringProp(prop, "accessibility.accessibility_file") ;

        runScenario1 = JsonUtilMto.getBooleanProp(prop, "scenarioPolicy.shuttleBusToRail.run");
        shuttleBusCostPerKm = JsonUtilMto.getFloatProp(prop, "scenarioPolicy.shuttleBusToRail.costPerKm");
        shuttleBusCostBase = JsonUtilMto.getFloatProp(prop, "scenarioPolicy.shuttleBusToRail.costBase");

        runScenario2 = JsonUtilMto.getBooleanProp(prop, "scenarioPolicy.BusSpeedImprovement.run");
        busCostFactor = JsonUtilMto.getFloatProp(prop, "scenarioPolicy.BusSpeedImprovement.busCostFactor");

        runScenario3 = JsonUtilMto.getBooleanProp(prop, "scenarioPolicy.DeutschlandTakt_InVehTransferTimesReduction.run");
        runTollScenario = JsonUtilMto.getBooleanProp(prop, "scenarioPolicy.tollScenario.run");

        toll = 0;
        if (runTollScenario) {
            toll = JsonUtilMto.getFloatProp(prop, "scenarioPolicy.tollScenario.toll_km");
            tollOnBundesstrasse = JsonUtilMto.getBooleanProp(prop, "scenarioPolicy.tollScenario.appliedInBundesstrasse");
        }
        this.outputFolder = outputFolder;
        atomicInteger = new AtomicInteger(0);
        logger.info("Accessibility analysis set up");
    }

    public void load(DataSet dataSet) {
        this.dataSet = dataSet;

    }

    public void run(DataSet dataSet, int i) {

        Set<Mode> modes = new HashSet<>();
        if (runScenario1){
            modes.add(ModeGermany.RAIL_SHUTTLE);
        } else if (runScenario2){
            modes.add(ModeGermany.BUS);
        } else if (runScenario3){
            modes.add(ModeGermany.RAIL);
        } else if (runTollScenario){
            modes.add(ModeGermany.AUTO);
            modes.add(ModeGermany.AUTO_noTOLL);
        } else {
            modes.add(ModeGermany.AUTO);
            modes.add(ModeGermany.BUS);
            modes.add(ModeGermany.RAIL);
            modes.add(ModeGermany.AIR);
        }
        for (Mode mode : modes) {
            int assignedJobs = 0;
            String outputFile = outputFolder + dataSet.getPopulationSection() + "_" + mode.toString() + "_" + outputFileName;
            PrintWriter pw = Util.openFileForSequentialWriting(outputFile, false);
            String header = "zone";
            for (int row = 1; row < coefficients.getRowCount(); row++) {
                header = header + "," + (int) coefficients.getValueAt(1, "vot_other") + "_" + row;
                header = header + "," + (int) coefficients.getValueAt(1, "vot_business") + "_" + row;
            }
            pw.println(header);
            for (Zone origin : dataSet.getZones().values()) {
                if (origin.getId() < 11818) {
                    Map<String, Map<Integer, Double>> accessibilityMap = new HashMap<>();
                    String values = String.valueOf(origin.getId());
                    accessibilityMap.putIfAbsent("vot_other", new HashMap<>());
                    accessibilityMap.put("vot_business", new HashMap<>());
                    for (int row = 1; row < coefficients.getRowCount(); row++) {
                        accessibilityMap.get("vot_other").putIfAbsent(row, 0.);
                        accessibilityMap.get("vot_business").put(row, 0.);
                    }
                    for (Zone destination : dataSet.getZones().values()) {
                        if (destination.getId() < 11818) {
                            Map<Integer, Double> impedances = calculateImpedance(origin, destination, mode);
                            for (int row = 1; row < coefficients.getRowCount(); row++) {
                                double impedance = impedances.get((int) coefficients.getValueAt(1, "vot_other"));
                                double impedanceBusiness = impedances.get((int) coefficients.getValueAt(1, "vot_business"));
                                double autoImpedance = Math.exp(coefficients.getValueAt(row, "beta") * impedance);
                                double autoImpedanceBusiness = Math.exp(coefficients.getValueAt(row, "beta") * impedanceBusiness);
                                double accessibilityZone = Math.pow(((ZoneGermany) destination).getPopulation(), coefficients.getValueAt(row, "alpha")) * autoImpedance;
                                double accessibilityBusinessZone = Math.pow(((ZoneGermany) destination).getPopulation(), coefficients.getValueAt(row, "alpha")) * autoImpedanceBusiness;
                                accessibilityMap.get("vot_other").put(row, accessibilityMap.get("vot_other").get(row) + accessibilityZone);
                                accessibilityMap.get("vot_business").put(row, accessibilityMap.get("vot_business").get(row) + accessibilityBusinessZone);
/*                                if (row == 1){
                                   logger.info(" Destination " + destination.getId() + ". Accessibility zone: " + accessibilityZone + ". AccessibilityBus: " + accessibilityBusinessZone);
                                }*/
                            }
                        }
                    }

                    for (int row = 1; row < coefficients.getRowCount(); row++) {
                        values = values + "," + accessibilityMap.get("vot_other").get(row);
                        values = values + "," + accessibilityMap.get("vot_business").get(row);
                    }
                    pw.println(values);
                    accessibilityByModeZone.putIfAbsent(mode, new HashMap<>());
                    accessibilityByModeZone.get(mode).putIfAbsent(origin, accessibilityMap);
                    assignedJobs++;
                    if (LongMath.isPowerOfTwo(assignedJobs)) {
                        logger.info("   Calculated " + assignedJobs + " zones.");
                    }
                }
            }
        /*                String outputFile = outputFolder + dataSet.getPopulationSection() + "_" + mode.toString() +  "_" + outputFileName;
        PrintWriter pw = Util.openFileForSequentialWriting(outputFile, false);
        String header = "zone";
        Zone firstZone = dataSet.getZones().values().stream().findFirst().orElse(null);
        for (String vot : accessibilityByModeZone.get(mode).get(firstZone).keySet()) {
            for (int combination : accessibilityByModeZone.get(mode).get(firstZone).get("vot_other").keySet()) {
                header = header + "," + vot + "_" + combination;
            }
        }

        pw.println(header);
        for (Zone zone : accessibilityByModeZone.get(mode).keySet()) {
            String values = String.valueOf(zone.getId());
            for (String vot : accessibilityByModeZone.get(mode).get(zone).keySet()) {
                for (int combination : accessibilityByModeZone.get(mode).get(zone).get(vot).keySet()) {
                    values = values + "," + accessibilityByModeZone.get(mode).get(zone).get(vot).get(combination);
                }
            }
            pw.println(values);
        }*/
            pw.close();
            dataSet.setAccessibilityByModeZone(accessibilityByModeZone);
        }
    }


    private Map<Integer, Double> calculateImpedance(Zone originZone, Zone destinationZone, Mode m) {

        int origin = originZone.getId();
        int destination = destinationZone.getId();
        double time = 1000000000 / 3600;
        double timeAccess = 0;
        double timeEgress = 0;
        double timeTotal = 0;
        double distance = 1000000000 / 1000; //convert to km
        double tollDistance = 0;
        double distanceAccess = 0;
        double distanceEgress = 0;
        Map<Integer, Double> impedances = new HashMap<>();



        if (m.equals(ModeGermany.RAIL)) {
            timeAccess = dataSet.getRailAccessTimeMatrix().get(ModeGermany.RAIL).getValueAt(origin, destination) / 3600;
            timeEgress = dataSet.getRailEgressTimeMatrix().get(ModeGermany.RAIL).getValueAt(origin, destination) / 3600;

            distance = dataSet.getDistanceMatrix().get(m).getValueAt(origin, destination) / 1000;

            time = dataSet.getTravelTimeMatrix().get(m).getValueAt(origin, destination) / 3600;
            timeTotal = time + timeAccess + timeEgress;

        } else if (m.equals(ModeGermany.RAIL_SHUTTLE)) {
            if (runScenario1) {
                timeAccess = dataSet.getRailAccessTimeMatrix().get(ModeGermany.RAIL_SHUTTLE).getValueAt(origin, destination) / 3600;
                timeEgress = dataSet.getRailEgressTimeMatrix().get(ModeGermany.RAIL_SHUTTLE).getValueAt(origin, destination) / 3600;

                //distanceAccess = dataSet.getRailAccessDistMatrix().get(ModeGermany.RAIL_SHUTTLE).getValueAt(origin, destination) / 1000;
                //distanceEgress = dataSet.getRailEgressDistMatrix().get(ModeGermany.RAIL_SHUTTLE).getValueAt(origin, destination) / 1000;
                distance = dataSet.getDistanceMatrix().get(m).getValueAt(origin, destination) / 1000;

                time = dataSet.getTravelTimeMatrix().get(m).getValueAt(origin, destination) / 3600;
                timeTotal = time + timeAccess + timeEgress;
            }
        } else {
            time = dataSet.getTravelTimeMatrix().get(m).getValueAt(origin, destination) / 3600;
            timeTotal = time;
            distance = dataSet.getDistanceMatrix().get(m).getValueAt(origin, destination) / 1000; //convert to km
            if (m.equals(ModeGermany.AUTO) || m.equals(ModeGermany.AUTO_noTOLL)) {
                tollDistance = dataSet.getTollDistanceMatrix().get(m).getValueAt(origin, destination) / 1000;
            }
        }

        if (time < 1000000000 / 3600) {
                double cost = 0;
                double costAccess = 0;
                double costEgress = 0;
                double costTotal = 0;
                if (distance != 0) {
                    cost = costsPerKm.getStringIndexedValueAt("alpha", m.toString()) *
                            Math.pow(distance, costsPerKm.getStringIndexedValueAt("beta", m.toString()))
                            * distance + tollDistance * toll; //* distance;
                    costTotal = cost;

                } else {
                    //todo technically the distance and cost cannot be zero. However, this happens when there is no segment by main mode (only access + egress)
                    cost = 0;
                    costTotal = cost;
                }

                if (runScenario2) {
                    if (m.equals(ModeGermany.BUS)) {
                        cost = cost * busCostFactor;
                        costTotal = cost;
                    }
                }

                if (runScenario1) {
                    if (m.equals(ModeGermany.RAIL_SHUTTLE)) {
                        distanceAccess = dataSet.getRailAccessDistMatrix().get(ModeGermany.RAIL_SHUTTLE).getValueAt(origin, destination) / 1000;
                        costAccess = distanceAccess * shuttleBusCostPerKm + shuttleBusCostBase;
                        distanceEgress = dataSet.getRailEgressDistMatrix().get(ModeGermany.RAIL_SHUTTLE).getValueAt(origin, destination) / 1000;
                        costEgress = distanceEgress * shuttleBusCostPerKm + shuttleBusCostBase;
                        costTotal = cost + costAccess + costEgress;
                    }
                }
            int vot = (int) coefficients.getValueAt(1, "vot_other");
            double impedance = costTotal / (vot) + timeTotal;
            impedances.put(vot, impedance);
            int votBusiness = (int) coefficients.getValueAt(1, "vot_business");
            double impedanceBusiness = costTotal / (votBusiness) + timeTotal;
            impedances.put(votBusiness, impedanceBusiness);


        } else {
            int vot =  (int) coefficients.getValueAt(1, "vot_other");;
            impedances.put(vot, Double.POSITIVE_INFINITY);
            int votBusiness = (int) coefficients.getValueAt(1, "vot_business");
            impedances.put(votBusiness, Double.POSITIVE_INFINITY);
        }
        return impedances;
    }
}
