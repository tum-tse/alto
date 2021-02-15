package de.tum.bgu.msm.longDistance;

import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.destinationChoice.DestinationChoice;
import de.tum.bgu.msm.longDistance.io.writer.OutputWriter;
import de.tum.bgu.msm.longDistance.io.reader.*;
import de.tum.bgu.msm.longDistance.modeChoice.ModeChoice;
import de.tum.bgu.msm.longDistance.timeOfDay.TimeOfDayChoice;
import de.tum.bgu.msm.longDistance.destinationChoice.ZoneDisaggregator;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.tripGeneration.TripGeneration;
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

public class LDModelOntario implements ModelComponent, LDModel {

    public static Random rand;
    static Logger logger = Logger.getLogger(LDModelOntario.class);

    //modules
    private ZoneReader zoneReader;
    private SkimsReader skimsReader;
    private SyntheticPopulationReader syntheticPopulationReader;
    private TripGeneration tripGenModel;
    private DestinationChoice destinationChoice;
    private ModeChoice mcModel;
    private ZoneDisaggregator zd;
    private TimeOfDayChoice timeOfDayChoice;
    private OutputWriter outputWriter;

    public LDModelOntario(ZoneReader zoneReader, SkimsReader skimsReader,
                          SyntheticPopulationReader syntheticPopulationReader,
                          TripGeneration tripGenModel,
                          DestinationChoice destinationChoice,
                          ModeChoice mcModel,
                          ZoneDisaggregator zd, TimeOfDayChoice timeOfDayChoice, OutputWriter outputWriter) {
        this.zoneReader = zoneReader;
        this.skimsReader = skimsReader;
        this.syntheticPopulationReader = syntheticPopulationReader;
        this.tripGenModel = tripGenModel;
        this.destinationChoice = destinationChoice;
        this.mcModel = mcModel;
        this.zd = zd;
        this.timeOfDayChoice = timeOfDayChoice;
        this.outputWriter = outputWriter;
    }

    public void setup(JSONObject prop, String inputFolder, String outputFolder) {

        Util.initializeRandomNumber(prop);
        //options
        zoneReader.setup(prop, inputFolder, outputFolder);
        skimsReader.setup(prop, inputFolder, outputFolder);
        syntheticPopulationReader.setup(prop, inputFolder, outputFolder);
        tripGenModel.setup(prop, inputFolder, outputFolder);
        destinationChoice.setup(prop, inputFolder, outputFolder);
        mcModel.setup(prop, inputFolder, outputFolder);
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
        destinationChoice.load(dataSet);
        tripGenModel.load(dataSet);
        zd.load(dataSet);
        outputWriter.load(dataSet);
        logger.info("---------------------ALL MODULES LOADED---------------------");

    }

    public void run(DataSet dataSet, int nThreads) {

        tripGenModel.run(dataSet, -1);
        destinationChoice.run(dataSet, -1);
        mcModel.run(dataSet, -1);
        zd.run(dataSet, -1);
        timeOfDayChoice.run(dataSet, -1);
        outputWriter.run(dataSet, -1);
        //print outputs



    }





}
