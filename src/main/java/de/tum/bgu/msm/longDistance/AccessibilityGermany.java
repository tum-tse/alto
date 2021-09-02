package de.tum.bgu.msm.longDistance;

import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.accessibilityAnalysis.AccessibilityAnalysisGermany;
import de.tum.bgu.msm.longDistance.airportAnalysis.AirTripsGeneration;
import de.tum.bgu.msm.longDistance.airportAnalysis.AirportAnalysis;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.io.reader.SkimsReader;
import de.tum.bgu.msm.longDistance.io.reader.ZoneReader;
import de.tum.bgu.msm.longDistance.io.writer.OutputWriter;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.Random;

/**
 * Germany Long Distance Model
 * Class to run long-distance travel demand model and calculates accessibilities
 * Author: Ana Moreno, Technical University of Munich (TUM), ana.moreno@tum.de
 * Date: 17 August 2021
 * Version 1
 */

public class AccessibilityGermany implements ModelComponent {

    public static Random rand;
    static Logger logger = Logger.getLogger(AccessibilityGermany.class);

    //modules
    private ZoneReader zoneReader;
    private SkimsReader skimsReader;
    private AccessibilityAnalysisGermany accessibilityAnalysis;
    private OutputWriter outputWriter;
    private AirTripsGeneration airTripsGeneration;

    public AccessibilityGermany(ZoneReader zoneReader, SkimsReader skimsReader,
                                AirTripsGeneration airTripsGeneration,
                                AccessibilityAnalysisGermany accessibilityAnalysis, OutputWriter outputWriter) {
        this.zoneReader = zoneReader;
        this.skimsReader = skimsReader;
        this.accessibilityAnalysis = accessibilityAnalysis;
        this.outputWriter = outputWriter;
        this.airTripsGeneration = airTripsGeneration;
    }

    public void setup(JSONObject prop, String inputFolder, String outputFolder) {

        Util.initializeRandomNumber(prop);
        //options
        zoneReader.setup(prop, inputFolder, outputFolder);
        skimsReader.setup(prop, inputFolder, outputFolder);
        airTripsGeneration.setup(prop, inputFolder, outputFolder);
        accessibilityAnalysis.setup(prop, inputFolder, outputFolder);
        outputWriter.setup(prop, inputFolder, outputFolder);
        logger.info("---------------------ALL MODULES SET UP---------------------");
    }

    public void load(DataSet dataSet) {

        zoneReader.load(dataSet);
        skimsReader.load(dataSet);
        airTripsGeneration.load(dataSet);
        accessibilityAnalysis.load(dataSet);
        outputWriter.load(dataSet);
        logger.info("---------------------ALL MODULES LOADED---------------------");

    }

    public void run(DataSet dataSet, int nThreads) {

        airTripsGeneration.run(dataSet, -1);
        accessibilityAnalysis.run(dataSet, -1);
        //outputWriter.run(dataSet, -1);

    }





}
