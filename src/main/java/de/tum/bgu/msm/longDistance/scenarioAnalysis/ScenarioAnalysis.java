package de.tum.bgu.msm.longDistance.scenarioAnalysis;

import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.sp.*;
import de.tum.bgu.msm.longDistance.data.trips.*;
import de.tum.bgu.msm.longDistance.data.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneGermany;
import de.tum.bgu.msm.longDistance.io.reader.SyntheticPopulationReader;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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

public class ScenarioAnalysis implements SyntheticPopulationReader {

    private static Logger logger = Logger.getLogger(ScenarioAnalysis.class);
    private JSONObject prop;
    private DataSet dataSet;
    private TableDataSet scenarioVariables;
    private int[] distanceBins;
    private String outputFolder;


    public ScenarioAnalysis() {
    }

    public void setup(JSONObject prop, String inputFolder, String outputFolder) {

        this.prop = prop;
        this.outputFolder = outputFolder;
        scenarioVariables = Util.readCSVfile(inputFolder + JsonUtilMto.getStringProp(prop,"scenarioPolicy.scenarios"));
        distanceBins = JsonUtilMto.getArrayIntProp(prop, "scenarioPolicy.distanceBins");
        logger.info("Scenario analysis set up");

    }

    public void load(DataSet dataSet) {

        this.dataSet = dataSet;
        dataSet.setScenarioSettings(scenarioVariables);
        dataSet.setNumberOfScenarios(scenarioVariables.getRowCount());
        dataSet.setDistanceBins(distanceBins);
        Map<Integer, Map<Type, Map<Purpose, Map<Mode, Integer>>>> modalCountByModeByScenario = new LinkedHashMap<>();
        Map<Integer, Map<Type, Map<Purpose, Map<Mode, Float>>>> co2EmissionsByModeByScenario = new LinkedHashMap<>();

        for (int scenarioId = 1; scenarioId <= scenarioVariables.getRowCount(); scenarioId++){
            modalCountByModeByScenario.putIfAbsent(scenarioId, new HashMap<>());
            co2EmissionsByModeByScenario.putIfAbsent(scenarioId, new HashMap<>());
            for (Type t : TypeGermany.values()) {
                modalCountByModeByScenario.get(scenarioId).putIfAbsent(t, new HashMap<>());
                co2EmissionsByModeByScenario.get(scenarioId).putIfAbsent(t, new HashMap<>());
                for (Purpose p :PurposeGermany.values()){
                    modalCountByModeByScenario.get(scenarioId).get(t).putIfAbsent(p, new HashMap<>());
                    co2EmissionsByModeByScenario.get(scenarioId).get(t).putIfAbsent(p, new HashMap<>());
                    for (Mode m : ModeGermany.values()) {
                        modalCountByModeByScenario.get(scenarioId).get(t).get(p).put(m, 0);
                        co2EmissionsByModeByScenario.get(scenarioId).get(t).get(p).put(m, 0.f);
                    }
                }
            }
        }
        dataSet.setModalCountByModeByScenario(modalCountByModeByScenario);
        dataSet.setCo2EmissionsByModeByScenario(co2EmissionsByModeByScenario);
        logger.info("Scenario analysis loaded");
    }

    public void run(DataSet dataSet, int nThreads) {

        PrintWriter pw = Util.openFileForSequentialWriting(outputFolder + "/p" + dataSet.getPopulationSection()+  "_summaryModeChoiceEmissions.csv", false);
        Map<Integer, Map<Type, Map<Purpose, Map<Mode, Integer>>>> trips = dataSet.getModalCountByModeByScenario();
        Map<Integer, Map<Type, Map<Purpose, Map<Mode, Float>>>> co2Emissions = dataSet.getCo2EmissionsByModeByScenario();
        TableDataSet scenarioSettings = dataSet.getScenarioSettings();
        String header = "subpopulation,scenario";
        for (int col = 1; col <= scenarioSettings.getColumnCount(); col++){
            header = header + "," + scenarioSettings.getColumnLabel(col);
        }
        for (Type t : TypeGermany.values()) {
            if (!t.equals(TypeGermany.AWAY)) {
                for (Purpose p : PurposeGermany.values()) {
                    for (Mode m : ModeGermany.values()) {
                        header = header + "," + t.toString() + "." + p.toString() + "." + m.toString() + ".trips";
                    }
                }
            }
        }
        for (Type t : TypeGermany.values()) {
            if (!t.equals(TypeGermany.AWAY)) {
                for (Purpose p : PurposeGermany.values()) {
                    for (Mode m : ModeGermany.values()) {
                        header = header + "," + t.toString() + "." + p.toString() + "." + m.toString() + ".co2";
                    }
                }
            }
        }
        pw.println(header);
        for (int scenario = 1; scenario <= dataSet.getNumberOfScenarios(); scenario++) {
            String line = Integer.toString(dataSet.getPopulationSection());
            line = line + "," + Integer.toString(scenario);
            for (int col = 1; col <= scenarioSettings.getColumnCount(); col++){
                line = line + "," + scenarioSettings.getStringValueAt(scenario, col);
            }
            for (Type t : TypeGermany.values()) {
                if (!t.equals(TypeGermany.AWAY)) {
                    for (Purpose p : PurposeGermany.values()) {
                        for (Mode m : ModeGermany.values()) {
                            line = line + "," + trips.get(scenario).get(t).get(p).get(m);
                        }
                        for (Mode m : ModeGermany.values()) {
                            line = line + "," + co2Emissions.get(scenario).get(t).get(p).get(m);
                        }
                    }
                }
            }
            pw.println(line);
        }
        pw.close();


        logger.info("Scenario analysis finished");

    }



}
