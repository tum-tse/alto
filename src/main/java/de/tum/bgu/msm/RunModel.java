package de.tum.bgu.msm;

import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.LDModelOntario;

import de.tum.bgu.msm.longDistance.destinationChoice.DestinationChoiceOntario;
import de.tum.bgu.msm.longDistance.destinationChoice.ZoneDisaggregatorOntario;
import de.tum.bgu.msm.longDistance.io.writer.OutputWriterOntario;
import de.tum.bgu.msm.longDistance.io.reader.SkimsReaderOntario;
import de.tum.bgu.msm.longDistance.io.reader.SyntheticPopulationReaderOntario;
import de.tum.bgu.msm.longDistance.io.reader.ZoneReaderOntario;
import de.tum.bgu.msm.longDistance.modeChoice.ModeChoiceOntario;
import de.tum.bgu.msm.longDistance.timeOfDay.TimeOfDayChoiceOntario;
import de.tum.bgu.msm.longDistance.tripGeneration.TripGenerationOntario;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;


/**
 * Ontario Provincial Model
 * Module to simulate long-distance travel
 * Author: Rolf Moeckel, Technische Universität München (TUM), rolf.moeckel@tum.de
 * Date: 11 December 2015
 * Version 1
 */

public class RunModel {
    // main class
    private static Logger logger = Logger.getLogger(RunModel.class);
    private JSONObject prop;

    private RunModel(JSONObject prop) {
        // constructor
        this.prop = prop;
    }


    public static void main(String[] args) {
        // main model run method

        logger.info("MITO Long distance model");
        long startTime = System.currentTimeMillis();
        JsonUtilMto jsonUtilMto = new JsonUtilMto(args[0]);
        JSONObject prop = jsonUtilMto.getJsonProperties();

        RunModel model = new RunModel(prop);
        model.runLongDistModel();
        float endTime = Util.rounder(((System.currentTimeMillis() - startTime) / 60000), 1);
        int hours = (int) (endTime / 60);
        int min = (int) (endTime - 60 * hours);
        int seconds = (int) ((endTime - 60 * hours - min) * 60);
        logger.info("Runtime: " + hours + " hours and " + min + " minutes and " + seconds + " seconds.");
    }


    private void runLongDistModel() {
        // main method to run long-distance model
        logger.info("Started runLongDistModel for the year " + JsonUtilMto.getIntProp(prop, "year"));
        DataSet dataSet = new DataSet();
        String inputFolder =  JsonUtilMto.getStringProp(prop, "work_folder");
        String outputFolder = JsonUtilMto.getStringProp(prop, "work_folder");

        LDModelOntario ldModelOntario = new LDModelOntario(new ZoneReaderOntario(), new SkimsReaderOntario(), new SyntheticPopulationReaderOntario(),
                new TripGenerationOntario(), new DestinationChoiceOntario(), new ModeChoiceOntario(), new ZoneDisaggregatorOntario(),
                new TimeOfDayChoiceOntario(), new OutputWriterOntario());
        ldModelOntario.setup(prop, inputFolder, outputFolder);
        ldModelOntario.load(dataSet);
        ldModelOntario.run(dataSet, -1);
        logger.info("Module runLongDistModel completed.");

    }
}
