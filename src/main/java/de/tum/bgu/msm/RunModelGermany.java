package de.tum.bgu.msm;

import de.tum.bgu.msm.longDistance.LDModelGermany;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.destinationChoice.DestinationChoiceGermany;
import de.tum.bgu.msm.longDistance.emissions.Emissions;
import de.tum.bgu.msm.longDistance.io.OutputWriterGermany;
import de.tum.bgu.msm.longDistance.io.reader.*;
import de.tum.bgu.msm.longDistance.modeChoice.ModeChoiceGermany;
import de.tum.bgu.msm.longDistance.timeOfDay.TimeOfDayChoiceGermany;
import de.tum.bgu.msm.longDistance.tripGeneration.TripGenerationGermany;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;


/**
 * Germany Model
 * Module to simulate long-distance travel
 * Author: Ana Moreno, Technische Universität München (TUM), ana.moreno@tum.de
 * Date: 11 December 2020
 * Version 1
 * Adapted from Ontario Provincial Model
 */

public class RunModelGermany {
    // main class
    private static Logger logger = Logger.getLogger(RunModelGermany.class);
    private JSONObject prop;

    private RunModelGermany(JSONObject prop) {
        // constructor
        this.prop = prop;
    }


    public static void main(String[] args) {
        // main model run method

        logger.info("MITO Long distance model");
        long startTime = System.currentTimeMillis();
        JsonUtilMto jsonUtilMto = new JsonUtilMto(args[0]);
        JSONObject prop = jsonUtilMto.getJsonProperties();

        RunModelGermany model = new RunModelGermany(prop);
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



        LDModelGermany ldModelGermany = new LDModelGermany(new ZoneReaderGermany(), new SkimsReaderGermany(),
                new SyntheticPopulationReaderGermany(),new EconomicStatusReader(),
                new TripGenerationGermany(), new DestinationChoiceGermany(), new ModeChoiceGermany(),
                new TimeOfDayChoiceGermany(), new Emissions(), new OutputWriterGermany());
        ldModelGermany.setup(prop, inputFolder, outputFolder);
        ldModelGermany.load(dataSet);
        ldModelGermany.run(dataSet, -1);
        logger.info("Module runLongDistModel completed.");

    }
}
