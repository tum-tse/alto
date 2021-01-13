package de.tum.bgu.msm.longDistance;

import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.airportAnalysis.AirportAnalysis;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.io.writer.OutputWriter;
import de.tum.bgu.msm.longDistance.io.reader.SkimsReader;
import de.tum.bgu.msm.longDistance.io.reader.ZoneReader;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.Random;

/**
 * Ontario Provincial Model
 * Class to run long-distance travel demand model
 * Author: Rolf Moeckel, Technical University of Munich (TUM), rolf.moeckel@tum.de
 * Date: 18 April 2016
 * Version 1
 */

public class AirportAnalysisModel implements ModelComponent {

    public static Random rand;
    static Logger logger = Logger.getLogger(AirportAnalysisModel.class);

    //modules
    private ZoneReader zoneReader;
    private SkimsReader skimsReader;
    private AirportAnalysis airportAnalysis;
    private OutputWriter outputWriter;

    public AirportAnalysisModel(ZoneReader zoneReader, SkimsReader skimsReader,
                                AirportAnalysis airportAnalysis, OutputWriter outputWriter) {
        this.zoneReader = zoneReader;
        this.skimsReader = skimsReader;
        this.airportAnalysis = airportAnalysis;
        this.outputWriter = outputWriter;
    }

    public void setup(JSONObject prop, String inputFolder, String outputFolder) {

        Util.initializeRandomNumber(prop);
        //options
        zoneReader.setup(prop, inputFolder, outputFolder);
        skimsReader.setup(prop, inputFolder, outputFolder);
        airportAnalysis.setup(prop, inputFolder, outputFolder);
        outputWriter.setup(prop, inputFolder, outputFolder);
        logger.info("---------------------ALL MODULES SET UP---------------------");
    }

    public void load(DataSet dataSet) {

        zoneReader.load(dataSet);
        skimsReader.load(dataSet);
        airportAnalysis.load(dataSet);
        outputWriter.load(dataSet);
        logger.info("---------------------ALL MODULES LOADED---------------------");

    }

    public void run(DataSet dataSet, int nThreads) {

        airportAnalysis.run(dataSet, -1);
        outputWriter.run(dataSet, -1);

    }





}
