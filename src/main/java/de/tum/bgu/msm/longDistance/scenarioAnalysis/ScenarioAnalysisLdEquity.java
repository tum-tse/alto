package de.tum.bgu.msm.longDistance.scenarioAnalysis;

//import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.common.datafile.TableDataSet;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.trips.*;
import de.tum.bgu.msm.longDistance.io.reader.SyntheticPopulationReader;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Germany wide travel demand model
 * Class to read alternative scenarios
 * Author: Ana Moreno, Technical University of Munich (TUM), ana.moreno@tum.de
 * Date: 16 February 2021
 * Version 1
 */

public class ScenarioAnalysisLdEquity implements SyntheticPopulationReader {

    private static Logger logger = Logger.getLogger(ScenarioAnalysisLdEquity.class);
    private JSONObject prop;
    private DataSet dataSet;
    private TableDataSet scenarioVariables;
    //private int[] distanceBins;
    private String outputFolder;
    //private boolean runSubpopulations;


    public ScenarioAnalysisLdEquity() {
    }

    public void setup(JSONObject prop, String inputFolder, String outputFolder) {

        this.prop = prop;
        this.outputFolder = outputFolder;
        scenarioVariables = Util.readCSVfile(inputFolder + JsonUtilMto.getStringProp(prop,"scenarioPolicy.scenarios"));
        //distanceBins = JsonUtilMto.getArrayIntProp(prop, "scenarioPolicy.distanceBins");
        //runSubpopulations = JsonUtilMto.getBooleanProp(prop, "synthetic_population.runSubpopulations");
        logger.info("Scenario analysis set up");

    }

    public void load(DataSet dataSet) {

        this.dataSet = dataSet;
        dataSet.setScenarioSettings(scenarioVariables);
        dataSet.setNumberOfScenarios(scenarioVariables.getRowCount());
        //dataSet.setDistanceBins(distanceBins);
        //Map<Integer, Map<Type, Map<Purpose, Map<Mode, Integer>>>> modalCountByModeByScenario = new LinkedHashMap<>();
        //Map<Integer, Map<Type, Map<Purpose, Map<Mode, Float>>>> co2EmissionsByModeByScenario = new LinkedHashMap<>();
        //Map<Integer, Map<Type, Map<Purpose, Map<Mode, Map<Integer, Integer>>>>> modalCountByDistance = new LinkedHashMap<>();
        //Map<Integer, Map<Type, Map<Purpose, Map<Mode, Map<Integer, Float>>>>> co2EmissionsByDistance = new LinkedHashMap<>();

//        for (int scenarioId = 1; scenarioId <= scenarioVariables.getRowCount(); scenarioId++){
//            modalCountByModeByScenario.putIfAbsent(scenarioId, new HashMap<>());
//            co2EmissionsByModeByScenario.putIfAbsent(scenarioId, new HashMap<>());
//            modalCountByDistance.putIfAbsent(scenarioId, new HashMap<>());
//            co2EmissionsByDistance.putIfAbsent(scenarioId, new HashMap<>());
//            for (Type t : TypeGermany.values()) {
//                modalCountByModeByScenario.get(scenarioId).putIfAbsent(t, new HashMap<>());
//                co2EmissionsByModeByScenario.get(scenarioId).putIfAbsent(t, new HashMap<>());
//                modalCountByDistance.get(scenarioId).putIfAbsent(t, new HashMap<>());
//                co2EmissionsByDistance.get(scenarioId).putIfAbsent(t, new HashMap<>());
//                for (Purpose p :PurposeGermany.values()){
//                    modalCountByModeByScenario.get(scenarioId).get(t).putIfAbsent(p, new HashMap<>());
//                    co2EmissionsByModeByScenario.get(scenarioId).get(t).putIfAbsent(p, new HashMap<>());
//                    modalCountByDistance.get(scenarioId).get(t).putIfAbsent(p, new HashMap<>());
//                    co2EmissionsByDistance.get(scenarioId).get(t).putIfAbsent(p, new HashMap<>());
//                    for (Mode m : ModeGermany.values()) {
//                        modalCountByModeByScenario.get(scenarioId).get(t).get(p).put(m, 0);
//                        co2EmissionsByModeByScenario.get(scenarioId).get(t).get(p).put(m, 0.f);
//                        modalCountByDistance.get(scenarioId).get(t).get(p).putIfAbsent(m, new HashMap<>());
//                        co2EmissionsByDistance.get(scenarioId).get(t).get(p).putIfAbsent(m, new HashMap<>());
//                        for (int d : distanceBins){
//                            modalCountByDistance.get(scenarioId).get(t).get(p).get(m).put(d, 0);
//                            co2EmissionsByDistance.get(scenarioId).get(t).get(p).get(m).put(d, 0.f);
//                        }
//                    }
//                }
//            }
//        }
//        dataSet.setModalCountByModeByScenario(modalCountByModeByScenario);
//        dataSet.setCo2EmissionsByModeByScenario(co2EmissionsByModeByScenario);
//        dataSet.setModalCountByModeByScenarioByDistance(modalCountByDistance);
//        dataSet.setCo2EmissionsByModeByScenarioByDistance(co2EmissionsByDistance);
        logger.info("Scenario analysis loaded");
    }

    public void run(DataSet dataSet, int nThreads) {

//        String fileName = outputFolder + "/summaryModeChoiceEmissions.csv";
//        String fileNameDistance = outputFolder + "/summaryModeChoiceEmissionsByDistance.csv";
//        if (runSubpopulations){
//            fileName = outputFolder + "/p" + dataSet.getPopulationSection()+  "_summaryModeChoiceEmissions.csv";
//            fileNameDistance = outputFolder + "/p" + dataSet.getPopulationSection()+  "_summaryModeChoiceEmissionsByDistance.csv";
//        }
//        PrintWriter pw = Util.openFileForSequentialWriting(fileName, false);
//        PrintWriter pwDistance = Util.openFileForSequentialWriting(fileNameDistance, false);
//        Map<Integer, Map<Type, Map<Purpose, Map<Mode, Integer>>>> trips = dataSet.getModalCountByModeByScenario();
//        Map<Integer, Map<Type, Map<Purpose, Map<Mode, Float>>>> co2Emissions = dataSet.getCo2EmissionsByModeByScenario();
//        Map<Integer, Map<Type, Map<Purpose, Map<Mode, Map<Integer, Integer>>>>> modalCountByDistance = dataSet.getModalCountByModeByScenarioByDistance();
//        Map<Integer, Map<Type, Map<Purpose, Map<Mode, Map<Integer, Float>>>>> co2EmissionsByDistance = dataSet.getCo2EmissionsByModeByScenarioByDistance();
//        TableDataSet scenarioSettings = dataSet.getScenarioSettings();
//        String header = "subpopulation,scenario";
//        String headerDistance = "subpopulation,scenario";
//        for (int col = 1; col <= scenarioSettings.getColumnCount(); col++){
//            header = header + "," + scenarioSettings.getColumnLabel(col);
//            headerDistance = headerDistance + "," + scenarioSettings.getColumnLabel(col);
//        }
//        for (Type t : TypeGermany.values()) {
//            if (!t.equals(TypeGermany.AWAY)) {
//                for (Purpose p : PurposeGermany.values()) {
//                    for (Mode m : ModeGermany.values()) {
//                        header = header + "," + t.toString() + "." + p.toString() + "." + m.toString() + ".trips";
//                    }
//                    for (Mode m : ModeGermany.values()) {
//                        header = header + "," + t.toString() + "." + p.toString() + "." + m.toString() + ".co2";
//                    }
//                    for (Mode m : ModeGermany.values()) {
//                        for (int d : distanceBins) {
//                            headerDistance = headerDistance + "," + t.toString() + "." + p.toString() + "." + m.toString() + "." + d + ".trips";
//                        }
//                    }
//                    for (Mode m : ModeGermany.values()) {
//                        for (int d : distanceBins) {
//                            headerDistance = headerDistance + "," + t.toString() + "." + p.toString() + "." + m.toString() + "." + d + ".co2";
//                        }
//                    }
//                }
//            }
//        }
//        pw.println(header);
//        pwDistance.println(headerDistance);
//        for (int scenario = 1; scenario <= dataSet.getNumberOfScenarios(); scenario++) {
//            String line = "0";
//            String lineDistance = "0";
//            if (runSubpopulations){
//                line = Integer.toString(dataSet.getPopulationSection());
//                lineDistance = Integer.toString(dataSet.getPopulationSection());
//            }
//            line = line + "," + Integer.toString(scenario);
//            lineDistance = lineDistance + "," + Integer.toString(scenario);
//            for (int col = 1; col <= scenarioSettings.getColumnCount(); col++){
//                line = line + "," + scenarioSettings.getStringValueAt(scenario, col);
//                lineDistance = lineDistance + "," + scenarioSettings.getStringValueAt(scenario, col);
//            }
//            for (Type t : TypeGermany.values()) {
//                if (!t.equals(TypeGermany.AWAY)) {
//                    for (Purpose p : PurposeGermany.values()) {
//                        for (Mode m : ModeGermany.values()) {
//                            line = line + "," + trips.get(scenario).get(t).get(p).get(m);
//                        }
//                        for (Mode m : ModeGermany.values()) {
//                            line = line + "," + co2Emissions.get(scenario).get(t).get(p).get(m);
//                        }
//                        for (Mode m : ModeGermany.values()) {
//                            for (int d : distanceBins) {
//                                lineDistance = lineDistance + "," + modalCountByDistance.get(scenario).get(t).get(p).get(m).get(d);
//                            }
//                        }
//                        for (Mode m : ModeGermany.values()) {
//                            for (int d : distanceBins) {
//                                lineDistance = lineDistance + "," + co2EmissionsByDistance.get(scenario).get(t).get(p).get(m).get(d);
//                            }
//                        }
//                    }
//                }
//            }
//            pw.println(line);
//            pwDistance.println(lineDistance);
//        }
//        pw.close();
//        pwDistance.close();

//        logger.info("Scenario analysis finished");

    }



}
