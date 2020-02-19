package de.tum.bgu.msm.longDistance;

import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.longDistance.data.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.destinationChoice.Distribution;
import de.tum.bgu.msm.longDistance.modeChoice.McModel;
import de.tum.bgu.msm.longDistance.timeOfDay.TimeOfDayChoice;
import de.tum.bgu.msm.longDistance.tripGeneration.Generation;
import de.tum.bgu.msm.longDistance.zoneSystem.ZonalData;
import de.tum.bgu.msm.longDistance.zoneSystem.ZoneDisaggregator;
import de.tum.bgu.msm.longDistance.data.sp.SyntheticPopulation;
import de.tum.bgu.msm.Util;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.io.PrintWriter;
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
    private ZonalData zonalData;
    private SyntheticPopulation syntheticPopulationReader;
    private Generation tripGenModel;
    private Distribution distribution;
    private McModel mcModel;
    private Calibration calib;
    private ZoneDisaggregator zd;
    private TimeOfDayChoice timeOfDayChoice;

    //developing options
    private boolean runTG;
    private boolean runDC;
    private String inputTripFile;

    //output options
    private boolean writeTrips;
    private String outputTripFile;


    public LDModel() {
        syntheticPopulationReader = new SyntheticPopulation();
        zonalData = new ZonalData();
        tripGenModel = new Generation();
        distribution = new Distribution();
        mcModel = new McModel();
        zd = new ZoneDisaggregator();
        calib = new Calibration();
        timeOfDayChoice = new TimeOfDayChoice();
    }

    public void setup(JSONObject prop, String inputFolder, String outputFolder) {

        Util.initializeRandomNumber(prop);

        //options
        runTG = JsonUtilMto.getBooleanProp(prop, "run.develop.trip_generation");
        runDC = JsonUtilMto.getBooleanProp(prop, "run.develop.destination_choice");
        inputTripFile = JsonUtilMto.getStringProp(prop, "run.develop.trip_input_file");
        outputTripFile = JsonUtilMto.getStringProp(prop, "output.trip_file");
        writeTrips = JsonUtilMto.getBooleanProp(prop, "output.write_trips");

        //setup modules
        zonalData.setup(prop, inputFolder, outputFolder);
        syntheticPopulationReader.setup(prop, inputFolder, outputFolder);
        tripGenModel.setup(prop, inputFolder, outputFolder);
        distribution.setup(prop, inputFolder, outputFolder);
        mcModel.setup(prop, inputFolder, outputFolder);
        calib.setup(prop, inputFolder, outputFolder);
        zd.setup(prop, inputFolder, outputFolder);
        timeOfDayChoice.setup(prop, inputFolder, outputFolder);

        logger.info("---------------------ALL MODULES SET UP---------------------");
    }

    public void load(DataSet dataSet) {
        dataSet.setModeChoiceModel(mcModel);
        dataSet.setDestinationChoiceModel(distribution);

        //LOAD the modules
        zonalData.load(dataSet);
        syntheticPopulationReader.load(dataSet);
        mcModel.load(dataSet);
        distribution.load(dataSet);

        tripGenModel.load(dataSet);
        calib.load(dataSet);
        zd.load(dataSet);

        logger.info("---------------------ALL MODULES LOADED---------------------");

    }

    public void run(DataSet dataSet, int nThreads) {

        //property change to avoid parallelization
        //System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "0");

        //run models

        tripGenModel.run(dataSet, -1);
        distribution.run(dataSet, -1);
        mcModel.run(dataSet, -1);

        //calib.getAverageModalShares(dataSet.getAllTrips());

        calib.run(dataSet, -1);
        zd.run(dataSet, -1);
        timeOfDayChoice.run(dataSet, -1);

        //print outputs
        writeLongDistanceOutputs(dataSet);


    }


    public void writeLongDistanceOutputs(DataSet dataSet) {
        if (writeTrips) {

            syntheticPopulationReader.writeSyntheticPopulation();
            writeTrips(dataSet.getAllTrips());
        }


//        if (analyzeAccess) {
//
//            AccessibilityAnalysis accAna = new AccessibilityAnalysis(rb, zonalData);
//            accAna.calculateAccessibilityForAnalysis();
//        }

    }

    public void writeTrips(ArrayList<LongDistanceTrip> trips) {
        logger.info("Writing out data of trips");

        PrintWriter pw = Util.openFileForSequentialWriting(outputTripFile, false);
        pw.println(LongDistanceTrip.getHeader());
        for (LongDistanceTrip tr : trips) {
            pw.println(tr.toString());
        }
        pw.close();
    }


}
