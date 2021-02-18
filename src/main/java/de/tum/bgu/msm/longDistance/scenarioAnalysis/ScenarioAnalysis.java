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
        Map<Integer, Map<Type, Map<Mode, Integer>>> modalCountByModeByScenario = new LinkedHashMap<>();
        Map<Integer, Map<Type, Map<Mode, Float>>> co2EmissionsByModeByScenario = new LinkedHashMap<>();

        for (int scenarioId = 1; scenarioId <= scenarioVariables.getRowCount(); scenarioId++){
            modalCountByModeByScenario.putIfAbsent(scenarioId, new HashMap<>());
            co2EmissionsByModeByScenario.putIfAbsent(scenarioId, new HashMap<>());
            for (Type p : TypeGermany.values()) {
                modalCountByModeByScenario.get(scenarioId).putIfAbsent(p, new HashMap<>());
                co2EmissionsByModeByScenario.get(scenarioId).putIfAbsent(p, new HashMap<>());
                for (Mode m : ModeGermany.values()) {
                    modalCountByModeByScenario.get(scenarioId).get(p).put(m, 0);
                    co2EmissionsByModeByScenario.get(scenarioId).get(p).put(m, 0.f);
                }
            }
        }
        dataSet.setModalCountByModeByScenario(modalCountByModeByScenario);
        dataSet.setCo2EmissionsByModeByScenario(co2EmissionsByModeByScenario);
        logger.info("Scenario analysis loaded");
    }

    public void run(DataSet dataSet, int nThreads) {

        PrintWriter pw = Util.openFileForSequentialWriting(outputFolder + "/summaryModeChoiceEmissions.csv", false);
        Map<Integer, Map<Type, Map<Mode, Integer>>> trips = dataSet.getModalCountByModeByScenario();
        Map<Integer, Map<Type, Map<Mode, Float>>> co2Emissions = dataSet.getCo2EmissionsByModeByScenario();
        float scaleFactor = JsonUtilMto.getFloatProp(prop, "synthetic_population.scale_factor");
        TableDataSet scenarioSettings = dataSet.getScenarioSettings();
        String header = "scenario";
        for (int col = 1; col <= scenarioSettings.getColumnCount(); col++){
            header = header + "," + scenarioSettings.getColumnLabel(col);
        }
        header = header + "," + "scale_factor";
        for (Type p : TypeGermany.values()) {
            if (!p.equals(TypeGermany.AWAY)) {
                for (Mode m : ModeGermany.values()) {
                    header = header + "," + p.toString() + "." + m.toString() + ".trips";
                }
                for (Mode m : ModeGermany.values()) {
                    header = header + "," + p.toString() + "." + m.toString() + ".co2";
                }
            }
        }
        pw.println(header);
        for (int scenario = 1; scenario <= dataSet.getNumberOfScenarios(); scenario++) {
            String line = Integer.toString(scenario);
            for (int col = 1; col <= scenarioSettings.getColumnCount(); col++){
                line = line + "," + scenarioSettings.getStringValueAt(scenario, col);
            }
            line = line + "," + scaleFactor;
            for (Type p : TypeGermany.values()) {
                if (!p.equals(TypeGermany.AWAY)) {
                    for (Mode m : ModeGermany.values()) {
                        line = line + "," + trips.get(scenario).get(p).get(m) * scaleFactor;
                    }
                    for (Mode m : ModeGermany.values()) {
                        line = line + "," + co2Emissions.get(scenario).get(p).get(m) * scaleFactor;
                    }
                }
            }
            pw.println(line);
        }
        pw.close();


        logger.info("Scenario analysis finished");

    }



}
