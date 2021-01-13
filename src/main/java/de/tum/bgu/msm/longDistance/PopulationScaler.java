package de.tum.bgu.msm.longDistance;

import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.airportAnalysis.AirportAnalysis;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.io.reader.SkimsReader;
import de.tum.bgu.msm.longDistance.io.reader.SyntheticPopulationReader;
import de.tum.bgu.msm.longDistance.io.reader.ZoneReader;
import de.tum.bgu.msm.longDistance.io.writer.OutputWriter;
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

public class PopulationScaler implements ModelComponent {

    public static Random rand;
    static Logger logger = Logger.getLogger(PopulationScaler.class);

    //modules
    private ZoneReader zoneReader;
    private SyntheticPopulationReader syntheticPopulationReader;
    private OutputWriter outputWriter;

    public PopulationScaler(ZoneReader zoneReader, SyntheticPopulationReader syntheticPopulationReader, OutputWriter outputWriter) {
        this.zoneReader = zoneReader;
        this.syntheticPopulationReader = syntheticPopulationReader;
        this.outputWriter = outputWriter;
    }

    public void setup(JSONObject prop, String inputFolder, String outputFolder) {

        Util.initializeRandomNumber(prop);
        //options
        zoneReader.setup(prop, inputFolder, outputFolder);
        syntheticPopulationReader.setup(prop, inputFolder, outputFolder);
        outputWriter.setup(prop, inputFolder, outputFolder);
        logger.info("---------------------ALL MODULES SET UP---------------------");
    }

    public void load(DataSet dataSet) {

        zoneReader.load(dataSet);
        syntheticPopulationReader.load(dataSet);
        outputWriter.load(dataSet);
        logger.info("---------------------ALL MODULES LOADED---------------------");

    }

    public void run(DataSet dataSet, int nThreads) {

        syntheticPopulationReader.run(dataSet, -1);
        outputWriter.run(dataSet, -1);

    }





}
