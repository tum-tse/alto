package de.tum.bgu.msm.longDistance;

import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.RunModelGermany;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.accessibilityAnalysis.AccessibilityAnalysisGermany;
import de.tum.bgu.msm.longDistance.airportAnalysis.AirTripsGeneration;
import de.tum.bgu.msm.longDistance.airportAnalysis.AirportAnalysis;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.io.reader.SkimsAutoReaderGermany;
import de.tum.bgu.msm.longDistance.io.reader.SkimsReaderGermany;
import de.tum.bgu.msm.longDistance.io.reader.ZoneReaderGermany;
import de.tum.bgu.msm.longDistance.io.writer.OutputWriterAccessibility;
import de.tum.bgu.msm.longDistance.io.writer.OutputWriterGermany;
import de.tum.bgu.msm.longDistance.io.writer.OutputWriterOntario;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import static de.tum.bgu.msm.RunModelGermany.createDirectoryIfNotExistingYet;


/**
 * Ontario Provincial Model
 * Module to simulate long-distance travel
 * Author: Rolf Moeckel, Technische Universität München (TUM), rolf.moeckel@tum.de
 * Date: 11 December 2015
 * Version 1
 */

public class RunAccessibilityAnalysis {
    // main class
    private static Logger logger = Logger.getLogger(RunAccessibilityAnalysis.class);
    private JSONObject prop;

    private RunAccessibilityAnalysis(JSONObject prop) {
        // constructor
        this.prop = prop;
    }


    public static void main(String[] args) {
        // main model run method
        BasicConfigurator.configure(); // Alona

        logger.info("MITO Long distance model");
        long startTime = System.currentTimeMillis();
        JsonUtilMto jsonUtilMto = new JsonUtilMto(args[0]);
        JSONObject prop = jsonUtilMto.getJsonProperties();

        RunAccessibilityAnalysis model = new RunAccessibilityAnalysis(prop);
        model.runLongDistModel(args);
        float endTime = Util.rounder(((System.currentTimeMillis() - startTime) / 60000), 1);
        int hours = (int) (endTime / 60);
        int min = (int) (endTime - 60 * hours);
        int seconds = (int) ((endTime - 60 * hours - min) * 60);
        logger.info("Runtime: " + hours + " hours and " + min + " minutes and " + seconds + " seconds.");
    }


    private void runLongDistModel(String[] args) {
        // main method to run long-distance model
        BasicConfigurator.configure(); // Alona
        logger.info("Started runLongDistModel for the year " + JsonUtilMto.getIntProp(prop, "year"));
        DataSet dataSet = new DataSet();
        String inputFolder =  JsonUtilMto.getStringProp(prop, "work_folder");
        String outputFolder = inputFolder + "output/" +  JsonUtilMto.getStringProp(prop, "scenario") + "/";
        createDirectoryIfNotExistingYet(outputFolder);


        AccessibilityGermany ldModel = new AccessibilityGermany(new ZoneReaderGermany(), new SkimsReaderGermany(),
                new AirTripsGeneration(), new AccessibilityAnalysisGermany(), new OutputWriterAccessibility());
        ldModel.setup(prop, inputFolder, outputFolder);
        ldModel.load(dataSet);
        ldModel.run(dataSet, -1);
        logger.info("Module runLongDistModel completed.");

    }
}
