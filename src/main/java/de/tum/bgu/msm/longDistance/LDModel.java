package de.tum.bgu.msm.longDistance;

import de.tum.bgu.msm.longDistance.destinationChoice.Distribution;
import de.tum.bgu.msm.longDistance.io.OutputWriter;
import de.tum.bgu.msm.longDistance.io.reader.SkimsReader;
import de.tum.bgu.msm.longDistance.modeChoice.McModel;
import de.tum.bgu.msm.longDistance.timeOfDay.TimeOfDayChoice;
import de.tum.bgu.msm.longDistance.tripGeneration.Generation;
import de.tum.bgu.msm.longDistance.io.reader.ZoneReader;
import de.tum.bgu.msm.longDistance.destinationChoice.ZoneDisaggregator;
import de.tum.bgu.msm.longDistance.io.reader.SyntheticPopulationReader;
import de.tum.bgu.msm.Util;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.*;

/**
 * Ontario Provincial Model
 * Class to run long-distance travel demand model
 * Author: Rolf Moeckel, Technical University of Munich (TUM), rolf.moeckel@tum.de
 * Date: 18 April 2016
 * Version 1
 */

public class LDModel implements ModelComponent {

    public static Random rand;
    static Logger logger = Logger.getLogger(LDModel.class);

    //modules
    private ZoneReader zoneReader;
    private SkimsReader skimsReader;
    private SyntheticPopulationReader syntheticPopulationReader;
    private Generation tripGenModel;
    private Distribution distribution;
    private McModel mcModel;
    private Calibration calib;
    private ZoneDisaggregator zd;
    private TimeOfDayChoice timeOfDayChoice;
    private OutputWriter outputWriter;

    //developing options
    private boolean runTG;
    private boolean runDC;
    private String inputTripFile;

    //output options
    private boolean writeTrips;
    private String outputTripFile;


    public LDModel() {
        syntheticPopulationReader = new SyntheticPopulationReader();
        zoneReader = new ZoneReader();
        skimsReader = new SkimsReader();
        tripGenModel = new Generation();
        distribution = new Distribution();
        mcModel = new McModel();
        zd = new ZoneDisaggregator();
        calib = new Calibration();
        timeOfDayChoice = new TimeOfDayChoice();
        outputWriter = new OutputWriter();
    }

    public void setup(JSONObject prop, String inputFolder, String outputFolder) {

        Util.initializeRandomNumber(prop);

        //options




        zoneReader.setup(prop, inputFolder, outputFolder);
        skimsReader.setup(prop, inputFolder, outputFolder);
        syntheticPopulationReader.setup(prop, inputFolder, outputFolder);
        tripGenModel.setup(prop, inputFolder, outputFolder);
        distribution.setup(prop, inputFolder, outputFolder);
        mcModel.setup(prop, inputFolder, outputFolder);
        calib.setup(prop, inputFolder, outputFolder);
        zd.setup(prop, inputFolder, outputFolder);
        timeOfDayChoice.setup(prop, inputFolder, outputFolder);
        outputWriter.setup(prop, inputFolder, outputFolder);

        logger.info("---------------------ALL MODULES SET UP---------------------");
    }

    public void load(DataSet dataSet) {

        zoneReader.load(dataSet);
        skimsReader.load(dataSet);
        syntheticPopulationReader.load(dataSet);
        mcModel.load(dataSet);
        distribution.load(dataSet);
        tripGenModel.load(dataSet);
        calib.load(dataSet);
        zd.load(dataSet);
        outputWriter.load(dataSet);

        logger.info("---------------------ALL MODULES LOADED---------------------");

    }

    public void run(DataSet dataSet, int nThreads) {

        tripGenModel.run(dataSet, -1);
        distribution.run(dataSet, -1);
        mcModel.run(dataSet, -1);
        calib.run(dataSet, -1);
        zd.run(dataSet, -1);
        timeOfDayChoice.run(dataSet, -1);

        //print outputs



    }





}
