package de.tum.bgu.msm.longDistance.io.reader;

import de.tum.bgu.msm.common.datafile.TableDataSet;
import de.tum.bgu.msm.common.matrix.Matrix;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.trips.*;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneGermany;
import omx.OmxFile;
import omx.OmxLookup;
import omx.OmxMatrix;
import omx.hdf5.OmxConstants;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.*;

//import static de.tum.bgu.msm.longDistance.io.reader.SkimsAutoReaderGermany.omxToMatrix;

/**
 * Germany Model
 * Class to read skims
 * Author: Ana Moreno, Technical University of Munich (TUM), ana.moreno@tum.de
 * Date: 8 December 2020
 * Version 1
 * Adapted from Ontario
 * Version 1
 */

public class SkimsReaderGermany implements SkimsReader {

    private static Logger logger = Logger.getLogger(SkimsReaderGermany.class);

    private DataSet dataSet;
    private String inputFolder;
    private String outputFolder;
    private JSONObject prop;

    private Map<ModeGermany, String> travelTimeFileNames = new HashMap<>();
    private Map<ModeGermany, String> travelTimeMatrixNames = new HashMap<>();
    private Map<ModeGermany, String> distanceFileNames = new HashMap<>();
    private Map<ModeGermany, String> distanceMatrixNames = new HashMap<>();
    private Map<ModeGermany, String> tollDistanceMatrixNames = new HashMap<>();

    private Map<ModeGermany, String> inPtTimeFileNames = new HashMap<>();
    private Map<ModeGermany, String> accessTimeFileNames = new HashMap<>();
    private Map<ModeGermany, String> egressTimeFileNames = new HashMap<>();
    private Map<ModeGermany, String> accessDistanceFileNames = new HashMap<>(); // Alona
    private Map<ModeGermany, String> egressDistanceFileNames = new HashMap<>(); // Alona


    private Map<ModeGermany, String> lookUps = new HashMap<>();

    private boolean readSkimsByStage;
    private Map<ModeGermany, String> accessToTrainFileName = new HashMap<>(); // Wei: original- private String accessToTrainFileName;
    private String airAccessAirportFileName;
    private String airEgressAirportFileName;

    private boolean runRailShuttleBus;
    private boolean runBusSpeedImprovement;
    private boolean runDeutschlandTakt;
    private boolean runDeutschlandTaktLow = false;
    private boolean runDeutschlandTaktMedium = false;
    private boolean runDeutschlandTaktExtreme = false;
    private boolean runTollScenario;
    private boolean runRailShuttleBusAndDeutschlandTakt;
    private boolean tollOnBundesstrasse = false;
    private boolean congestedTraffic;

    @Override
    public void setup(JSONObject prop, String inputFolder, String outputFolder) {
        this.inputFolder = inputFolder;
        this.outputFolder = outputFolder;
        this.prop = prop;

        // Scenario
        runRailShuttleBus = JsonUtilMto.getBooleanProp(prop, "scenarioPolicy.shuttleBusToRail.run");
        runBusSpeedImprovement = JsonUtilMto.getBooleanProp(prop, "scenarioPolicy.BusSpeedImprovement.run");
        runDeutschlandTakt = JsonUtilMto.getBooleanProp(prop, "scenarioPolicy.DeutschlandTakt_InVehTransferTimesReduction.run");
        if (runDeutschlandTakt){
            runDeutschlandTaktLow = JsonUtilMto.getBooleanProp(prop, "scenarioPolicy.DeutschlandTakt_InVehTransferTimesReduction.magnitude.low");
            runDeutschlandTaktMedium = JsonUtilMto.getBooleanProp(prop, "scenarioPolicy.DeutschlandTakt_InVehTransferTimesReduction.magnitude.medium");
            runDeutschlandTaktExtreme = JsonUtilMto.getBooleanProp(prop, "scenarioPolicy.DeutschlandTakt_InVehTransferTimesReduction.magnitude.extreme");
        }
        runTollScenario = JsonUtilMto.getBooleanProp(prop, "scenarioPolicy.tollScenario.run");
        if (runTollScenario) {
            tollOnBundesstrasse = JsonUtilMto.getBooleanProp(prop, "scenarioPolicy.tollScenario.appliedInBundesstrasse");
        }
        runRailShuttleBusAndDeutschlandTakt = false; //JsonUtilMto.getBooleanProp(prop, "scenarioPolicy.DeutschlandTakt_shuttleBusToRail.run");
        congestedTraffic = JsonUtilMto.getBooleanProp(prop, "scenarioPolicy.congestedTraffic");

        //AUTO:
        //Auto_Toll->
        if (!runTollScenario) {
            if (!congestedTraffic) {
                travelTimeFileNames.put(ModeGermany.AUTO, inputFolder + JsonUtilMto.getStringProp(prop, "mode_choice.skim.all_car"));
                distanceFileNames.put(ModeGermany.AUTO, inputFolder + JsonUtilMto.getStringProp(prop, "mode_choice.skim.all_car"));
            } else {
                travelTimeFileNames.put(ModeGermany.AUTO, inputFolder + JsonUtilMto.getStringProp(prop, "mode_choice.skim.all_car_congested"));
                distanceFileNames.put(ModeGermany.AUTO, inputFolder + JsonUtilMto.getStringProp(prop, "mode_choice.skim.all_car_congested"));
            }
        } else {
            if (!congestedTraffic) {
                if (tollOnBundesstrasse) {
                    travelTimeFileNames.put(ModeGermany.AUTO, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.tollScenario.all_car_toll_ab"));
                    distanceFileNames.put(ModeGermany.AUTO, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.tollScenario.all_car_toll_ab"));
                } else {
                    travelTimeFileNames.put(ModeGermany.AUTO, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.tollScenario.all_car_toll"));
                    distanceFileNames.put(ModeGermany.AUTO, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.tollScenario.all_car_toll"));
                }
            } else {
                if (tollOnBundesstrasse) {
                    travelTimeFileNames.put(ModeGermany.AUTO, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.tollScenario.all_car_toll_ab_congested"));
                    distanceFileNames.put(ModeGermany.AUTO, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.tollScenario.all_car_toll_ab_congested"));
                } else {
                    travelTimeFileNames.put(ModeGermany.AUTO, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.tollScenario.all_car_toll_congested"));
                    distanceFileNames.put(ModeGermany.AUTO, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.tollScenario.all_car_toll_congested"));
                }
            }
        }

        travelTimeMatrixNames.put(ModeGermany.AUTO, JsonUtilMto.getStringProp(prop, "mode_choice.skim.matrixTime_auto"));
        distanceMatrixNames.put(ModeGermany.AUTO, JsonUtilMto.getStringProp(prop, "mode_choice.skim.matrixDistance_auto"));
        tollDistanceMatrixNames.put(ModeGermany.AUTO, JsonUtilMto.getStringProp(prop, "mode_choice.skim.tolledDistance_auto"));
        lookUps.put(ModeGermany.AUTO, JsonUtilMto.getStringProp(prop, "mode_choice.skim.auto_matrix_lookup"));

        //Auto_noToll ->
        //if (runTollScenario) {
        if (!congestedTraffic) {
            if (tollOnBundesstrasse) {
                travelTimeFileNames.put(ModeGermany.AUTO_noTOLL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.tollScenario.all_car_noToll_ab"));
                distanceFileNames.put(ModeGermany.AUTO_noTOLL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.tollScenario.all_car_noToll_ab"));
            } else {
                travelTimeFileNames.put(ModeGermany.AUTO_noTOLL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.tollScenario.all_car_noToll"));
                distanceFileNames.put(ModeGermany.AUTO_noTOLL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.tollScenario.all_car_noToll"));
            }
        } else {
            if (tollOnBundesstrasse) {
                travelTimeFileNames.put(ModeGermany.AUTO_noTOLL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.tollScenario.all_car_noToll_ab_congested"));
                distanceFileNames.put(ModeGermany.AUTO_noTOLL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.tollScenario.all_car_noToll_ab_congested"));
            } else {
                travelTimeFileNames.put(ModeGermany.AUTO_noTOLL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.tollScenario.all_car_noToll_congested"));
                distanceFileNames.put(ModeGermany.AUTO_noTOLL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.tollScenario.all_car_noToll_congested"));
            }
        }
        travelTimeMatrixNames.put(ModeGermany.AUTO_noTOLL, JsonUtilMto.getStringProp(prop, "mode_choice.skim.matrixTime_auto"));
        distanceMatrixNames.put(ModeGermany.AUTO_noTOLL, JsonUtilMto.getStringProp(prop, "mode_choice.skim.matrixDistance_auto"));
        tollDistanceMatrixNames.put(ModeGermany.AUTO_noTOLL, JsonUtilMto.getStringProp(prop, "mode_choice.skim.tolledDistance_auto"));
        lookUps.put(ModeGermany.AUTO_noTOLL, JsonUtilMto.getStringProp(prop, "mode_choice.skim.auto_matrix_lookup"));
        //}

        //RAIL and Rail_shuttle:

        inPtTimeFileNames.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "mode_choice.skim.all_rail"));
        accessTimeFileNames.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "mode_choice.skim.all_rail"));
        egressTimeFileNames.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "mode_choice.skim.all_rail"));
        accessDistanceFileNames.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "mode_choice.skim.all_rail")); // A
        egressDistanceFileNames.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "mode_choice.skim.all_rail")); // A
        egressTimeFileNames.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "mode_choice.skim.all_rail"));
        distanceFileNames.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "mode_choice.skim.all_rail"));
        distanceMatrixNames.put(ModeGermany.RAIL, JsonUtilMto.getStringProp(prop, "mode_choice.skim.matrixName_distance"));
        lookUps.put(ModeGermany.RAIL, JsonUtilMto.getStringProp(prop, "mode_choice.skim.pt_matrix_lookup"));
        accessToTrainFileName.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "zone_system.accessToRail_time_matrix"));

        inPtTimeFileNames.put(ModeGermany.RAIL_SHUTTLE, inputFolder);
        accessTimeFileNames.put(ModeGermany.RAIL_SHUTTLE, inputFolder);
        egressTimeFileNames.put(ModeGermany.RAIL_SHUTTLE, inputFolder);
        accessDistanceFileNames.put(ModeGermany.RAIL_SHUTTLE, inputFolder); // A
        egressDistanceFileNames.put(ModeGermany.RAIL_SHUTTLE, inputFolder); // A
        egressTimeFileNames.put(ModeGermany.RAIL_SHUTTLE, inputFolder);
        distanceFileNames.put(ModeGermany.RAIL_SHUTTLE, inputFolder);
        distanceMatrixNames.put(ModeGermany.RAIL_SHUTTLE, JsonUtilMto.getStringProp(prop, "mode_choice.skim.matrixName_distance"));
        lookUps.put(ModeGermany.RAIL_SHUTTLE, JsonUtilMto.getStringProp(prop, "mode_choice.skim.pt_matrix_lookup"));
        accessToTrainFileName.put(ModeGermany.RAIL_SHUTTLE, inputFolder + JsonUtilMto.getStringProp(prop, "zone_system.accessToRail_time_matrix"));

        if (runDeutschlandTakt) {

            if(runDeutschlandTaktLow){
                inPtTimeFileNames.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.DeutschlandTakt_InVehTransferTimesReduction.all_rail_low"));
                accessTimeFileNames.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.DeutschlandTakt_InVehTransferTimesReduction.all_rail_low"));
                egressTimeFileNames.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.DeutschlandTakt_InVehTransferTimesReduction.all_rail_low"));
                accessDistanceFileNames.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.DeutschlandTakt_InVehTransferTimesReduction.all_rail_low")); // A
                egressDistanceFileNames.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.DeutschlandTakt_InVehTransferTimesReduction.all_rail_low")); // A
                egressTimeFileNames.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.DeutschlandTakt_InVehTransferTimesReduction.all_rail_low"));
                distanceFileNames.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.DeutschlandTakt_InVehTransferTimesReduction.all_rail_low"));
                distanceMatrixNames.put(ModeGermany.RAIL, JsonUtilMto.getStringProp(prop, "mode_choice.skim.matrixName_distance"));
                lookUps.put(ModeGermany.RAIL, JsonUtilMto.getStringProp(prop, "mode_choice.skim.pt_matrix_lookup"));
                accessToTrainFileName.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "zone_system.accessToRail_time_matrix"));
            }else if(runDeutschlandTaktMedium){
                inPtTimeFileNames.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.DeutschlandTakt_InVehTransferTimesReduction.all_rail_medium"));
                accessTimeFileNames.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.DeutschlandTakt_InVehTransferTimesReduction.all_rail_medium"));
                egressTimeFileNames.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.DeutschlandTakt_InVehTransferTimesReduction.all_rail_medium"));
                accessDistanceFileNames.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.DeutschlandTakt_InVehTransferTimesReduction.all_rail_medium")); // A
                egressDistanceFileNames.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.DeutschlandTakt_InVehTransferTimesReduction.all_rail_medium")); // A
                egressTimeFileNames.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.DeutschlandTakt_InVehTransferTimesReduction.all_rail_medium"));
                distanceFileNames.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.DeutschlandTakt_InVehTransferTimesReduction.all_rail_medium"));
                distanceMatrixNames.put(ModeGermany.RAIL, JsonUtilMto.getStringProp(prop, "mode_choice.skim.matrixName_distance"));
                lookUps.put(ModeGermany.RAIL, JsonUtilMto.getStringProp(prop, "mode_choice.skim.pt_matrix_lookup"));
                accessToTrainFileName.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "zone_system.accessToRail_time_matrix"));
            }else{
                inPtTimeFileNames.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.DeutschlandTakt_InVehTransferTimesReduction.all_rail_extreme"));
                accessTimeFileNames.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.DeutschlandTakt_InVehTransferTimesReduction.all_rail_extreme"));
                egressTimeFileNames.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.DeutschlandTakt_InVehTransferTimesReduction.all_rail_extreme"));
                accessDistanceFileNames.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.DeutschlandTakt_InVehTransferTimesReduction.all_rail_extreme")); // A
                egressDistanceFileNames.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.DeutschlandTakt_InVehTransferTimesReduction.all_rail_extreme")); // A
                egressTimeFileNames.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.DeutschlandTakt_InVehTransferTimesReduction.all_rail_extreme"));
                distanceFileNames.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.DeutschlandTakt_InVehTransferTimesReduction.all_rail_extreme"));
                distanceMatrixNames.put(ModeGermany.RAIL, JsonUtilMto.getStringProp(prop, "mode_choice.skim.matrixName_distance"));
                lookUps.put(ModeGermany.RAIL, JsonUtilMto.getStringProp(prop, "mode_choice.skim.pt_matrix_lookup"));
                accessToTrainFileName.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "zone_system.accessToRail_time_matrix"));
            }

            if(!runDeutschlandTaktLow && !runDeutschlandTaktMedium && !runDeutschlandTaktExtreme) {
                logger.error("check your setting for Deutschlandtakt scenario, especially the magnitude section. At least one magnitude level should be chosen.");
                throw new RuntimeException("See above");
            }


        }

        if (runRailShuttleBusAndDeutschlandTakt) {
            inPtTimeFileNames.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.DeutschlandTakt_shuttleBusToRail.all_rail"));
            accessTimeFileNames.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.DeutschlandTakt_shuttleBusToRail.all_rail"));
            egressTimeFileNames.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.DeutschlandTakt_shuttleBusToRail.all_rail"));
            accessDistanceFileNames.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.DeutschlandTakt_shuttleBusToRail.all_rail")); // A
            egressDistanceFileNames.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.DeutschlandTakt_shuttleBusToRail.all_rail")); // A
            egressTimeFileNames.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.DeutschlandTakt_shuttleBusToRail.all_rail"));
            distanceFileNames.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.DeutschlandTakt_shuttleBusToRail.all_rail"));
            distanceMatrixNames.put(ModeGermany.RAIL, JsonUtilMto.getStringProp(prop, "mode_choice.skim.matrixName_distance"));
            lookUps.put(ModeGermany.RAIL, JsonUtilMto.getStringProp(prop, "mode_choice.skim.pt_matrix_lookup"));
            accessToTrainFileName.put(ModeGermany.RAIL, inputFolder + JsonUtilMto.getStringProp(prop, "zone_system.accessToRail_time_matrix"));

            inPtTimeFileNames.put(ModeGermany.RAIL_SHUTTLE, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.DeutschlandTakt_shuttleBusToRail.all_rail_shuttle"));
            accessTimeFileNames.put(ModeGermany.RAIL_SHUTTLE, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.DeutschlandTakt_shuttleBusToRail.all_rail_shuttle"));
            egressTimeFileNames.put(ModeGermany.RAIL_SHUTTLE, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.DeutschlandTakt_shuttleBusToRail.all_rail_shuttle"));
            accessDistanceFileNames.put(ModeGermany.RAIL_SHUTTLE, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.DeutschlandTakt_shuttleBusToRail.all_rail_shuttle")); // A
            egressDistanceFileNames.put(ModeGermany.RAIL_SHUTTLE, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.DeutschlandTakt_shuttleBusToRail.all_rail_shuttle")); // A
            egressTimeFileNames.put(ModeGermany.RAIL_SHUTTLE, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.DeutschlandTakt_shuttleBusToRail.all_rail_shuttle"));
            distanceFileNames.put(ModeGermany.RAIL_SHUTTLE, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.DeutschlandTakt_shuttleBusToRail.all_rail_shuttle"));
            distanceMatrixNames.put(ModeGermany.RAIL_SHUTTLE, JsonUtilMto.getStringProp(prop, "mode_choice.skim.matrixName_distance"));
            lookUps.put(ModeGermany.RAIL_SHUTTLE, JsonUtilMto.getStringProp(prop, "mode_choice.skim.pt_matrix_lookup"));
            accessToTrainFileName.put(ModeGermany.RAIL_SHUTTLE, inputFolder + JsonUtilMto.getStringProp(prop, "zone_system.accessToRail_time_matrix"));
        }

        //BUS:
        inPtTimeFileNames.put(ModeGermany.BUS, inputFolder + JsonUtilMto.getStringProp(prop, "mode_choice.skim.all_bus"));
        accessTimeFileNames.put(ModeGermany.BUS, inputFolder + JsonUtilMto.getStringProp(prop, "mode_choice.skim.all_bus"));
        egressTimeFileNames.put(ModeGermany.BUS, inputFolder + JsonUtilMto.getStringProp(prop, "mode_choice.skim.all_bus"));
        distanceFileNames.put(ModeGermany.BUS, inputFolder + JsonUtilMto.getStringProp(prop, "mode_choice.skim.all_bus"));
        distanceMatrixNames.put(ModeGermany.BUS, JsonUtilMto.getStringProp(prop, "mode_choice.skim.matrixName_distance"));
        lookUps.put(ModeGermany.BUS, JsonUtilMto.getStringProp(prop, "mode_choice.skim.pt_matrix_lookup"));

        if (runBusSpeedImprovement) {
            inPtTimeFileNames.put(ModeGermany.BUS, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.BusSpeedImprovement.all_bus"));
            accessTimeFileNames.put(ModeGermany.BUS, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.BusSpeedImprovement.all_bus"));
            egressTimeFileNames.put(ModeGermany.BUS, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.BusSpeedImprovement.all_bus"));
            accessDistanceFileNames.put(ModeGermany.BUS, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.BusSpeedImprovement.all_bus")); // A
            egressDistanceFileNames.put(ModeGermany.BUS, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.BusSpeedImprovement.all_bus")); // A
            distanceFileNames.put(ModeGermany.BUS, inputFolder + JsonUtilMto.getStringProp(prop, "scenarioPolicy.BusSpeedImprovement.all_bus"));
            distanceMatrixNames.put(ModeGermany.BUS, JsonUtilMto.getStringProp(prop, "mode_choice.skim.matrixName_distance"));
            lookUps.put(ModeGermany.BUS, JsonUtilMto.getStringProp(prop, "mode_choice.skim.pt_matrix_lookup"));
        }

        //AIR:
        airAccessAirportFileName = JsonUtilMto.getStringProp(prop, "airport.access_airport_file");
        airEgressAirportFileName = JsonUtilMto.getStringProp(prop, "airport.egress_airport_file");

    }

    @Override
    public void load(DataSet dataSet) {
        this.dataSet = dataSet;
        //readSkims();
        readSkimByMode(dataSet);
    }


    @Override
    public void run(DataSet dataSet, int nThreads) {

    }


    private Matrix convertSkimToMatrix(String[] fileMatrixLookupName) {

        OmxFile skim = new OmxFile(fileMatrixLookupName[0]);
        skim.openReadOnly();
        OmxMatrix skimMatrix = skim.getMatrix(fileMatrixLookupName[1]);
        Matrix matrix = Util.convertOmxToMatrix(skimMatrix);
        OmxLookup omxLookUp = skim.getLookup(fileMatrixLookupName[2]);
        int[] externalNumbers = (int[]) omxLookUp.getLookup();
        matrix.setExternalNumbersZeroBased(externalNumbers);
        logger.info("  Skim matrix was read: " + fileMatrixLookupName[0]);
        return matrix;
    }


    private void convertMatrixToSkim(String[] fileMatrixLookupName, Matrix matrix) {

        String fileName = "output/" + fileMatrixLookupName[0];

        try (OmxFile omxFile = new OmxFile(fileName)) {

            int dim0 = matrix.getRowCount();

            int dim1 = dim0;

            int[] shape = {dim0, dim1};
            float mat1NA = -1;

            OmxMatrix.OmxFloatMatrix mat1 = new OmxMatrix.OmxFloatMatrix(fileMatrixLookupName[1], matrix.getValues(), mat1NA);
            mat1.setAttribute(OmxConstants.OmxNames.OMX_DATASET_TITLE_KEY.getKey(), "values");

            int lookup1NA = -1;
            int[] lookup1Data;

            lookup1Data = matrix.getExternalRowNumbers();

            OmxLookup.OmxIntLookup lookup1 = new OmxLookup.OmxIntLookup(fileMatrixLookupName[2], lookup1Data, lookup1NA);

            omxFile.openNew(shape);
            omxFile.addMatrix(mat1);
            omxFile.addLookup(lookup1);
            omxFile.save();
            System.out.println(omxFile.summary());

            omxFile.close();
            System.out.println(fileMatrixLookupName[0] + "matrix written");

        }

    }

    private Matrix assignIntrazonalDistances(Matrix matrix, Mode mode) {
        //For air, the intrazonal should be not possible. Keep the big number from the skim
        if (!ModeGermany.AIR.equals(mode)) {
            for (int zoneId : dataSet.getZones().keySet()) {
                int minDistance = ((ZoneGermany) dataSet.getZones().get(zoneId)).getArea();
                matrix.setValueAt(zoneId, zoneId, (float) Math.sqrt(minDistance / 3.14));
            }
        }
        logger.info("Calculated intrazonal values - nearest neighbour");
        return matrix;
    }


    private void readSkimByMode(DataSet dataSet) {

        Map<String, Matrix> modeMatrixMap = new HashMap<>();
        Map<Mode, Matrix> modeTimeMatrixMap = new HashMap<>();
        Map<Mode, Matrix> modeDistanceMatrixMap = new HashMap<>();
        Map<Mode, Matrix> railAccessDistanceMatrixMap = new HashMap<>();
        Map<Mode, Matrix> railEgressDistanceMatrixMap = new HashMap<>();
        Map<Mode, Matrix> railAccessTimeMatrixMap = new HashMap<>();
        Map<Mode, Matrix> railEgressTimeMatrixMap = new HashMap<>();
        Map<Mode, Matrix> modeTollDistanceMatrixMap = new HashMap<>();

        // read skim file
        ModeGermany m;

        long time = System.currentTimeMillis();

        m = ModeGermany.AUTO;
        Matrix autoTravelTime = omxToMatrix(travelTimeFileNames.get(m), travelTimeMatrixNames.get(m), lookUps.get(m));
        time = logReading(time, "car time");

        Matrix autoDistance = omxToMatrix(distanceFileNames.get(m), distanceMatrixNames.get(m), lookUps.get(m));
        time = logReading(time, "car distance");

        Matrix autoTollDistance = omxToMatrix(distanceFileNames.get(m), tollDistanceMatrixNames.get(m), lookUps.get(m));

        modeMatrixMap = assignIntrazonalTravelTimes(autoTravelTime, autoDistance, m, 5, 10 * 60, 0.33F);
        time = logReading(time, "car intrazonals");

        modeTimeMatrixMap.put(m, modeMatrixMap.get("travelTime"));
        modeDistanceMatrixMap.put(m, modeMatrixMap.get("distance"));
        modeTollDistanceMatrixMap.put(m, autoTollDistance);

        dataSet.setAutoTravelTime(modeMatrixMap.get("travelTime"));
        dataSet.setAutoTravelDistance(modeMatrixMap.get("distance")); //for safety and compatibility

        //if (runTollScenario) {

        m = ModeGermany.AUTO_noTOLL;
        Matrix autoTravelTimeToll = omxToMatrix(travelTimeFileNames.get(m), travelTimeMatrixNames.get(m), lookUps.get(m));
        time = logReading(time, "car toll time");

        Matrix autoDistanceToll = omxToMatrix(distanceFileNames.get(m), distanceMatrixNames.get(m), lookUps.get(m));
        time = logReading(time, "car toll distance");

        Matrix autoTollDistanceToll = omxToMatrix(distanceFileNames.get(m), tollDistanceMatrixNames.get(m), lookUps.get(m));

        Map<String, Matrix> modeMatrixMapToll = assignIntrazonalTravelTimes(autoTravelTimeToll, autoDistanceToll, m, 5, 10 * 60, 0.33F);
        time = logReading(time, "car toll intrazonals");

        modeTimeMatrixMap.put(m, modeMatrixMapToll.get("travelTime"));
        modeDistanceMatrixMap.put(m, modeMatrixMapToll.get("distance"));
        modeTollDistanceMatrixMap.put(m, autoTollDistanceToll);

        //}

        m = ModeGermany.AIR;
        //initialize empty matrices
        Set<Integer> zones = dataSet.getZones().keySet();
        Matrix airTravelTime = new Matrix(zones.size(), zones.size());
        Matrix airTravelDistance = new Matrix(zones.size(), zones.size());
        int[] externalNumbers = new int[zones.size()];
        int index = 0;
        for (int i : zones) {
            externalNumbers[index] = i;
            index++;
        }
        airTravelTime.setExternalNumbersZeroBased(externalNumbers);
        airTravelDistance.setExternalNumbersZeroBased(externalNumbers);
        modeTimeMatrixMap.put(m, airTravelTime);
        modeDistanceMatrixMap.put(m, airTravelDistance);
        time = logReading(time, "air empty matrices");

        m = ModeGermany.BUS;
        List<Matrix> matricesBus = new ArrayList<>();
        matricesBus.add(omxToMatrix(inPtTimeFileNames.get(m), "travel_time_s", lookUps.get(m)));
        time = logReading(time, "bus time");
        //matricesBus.add(omxToMatrix(accessTimeFileNames.get(m), "access_time_s", lookUps.get(m)));
        time = logReading(time, "bus access");
        //matricesBus.add(omxToMatrix(egressTimeFileNames.get(m), "egress_time_s", lookUps.get(m)));
        time = logReading(time, "bus egress");
        Matrix totalTravelTimeBus = sumMatrices(matricesBus);
        Matrix distanceBus = omxToMatrix(distanceFileNames.get(m), distanceMatrixNames.get(m), lookUps.get(m));
        time = logReading(time, "bus distance");

        modeMatrixMap = assignIntrazonalTravelTimes(totalTravelTimeBus, distanceBus, m, 5, 10 * 60, 0.33F);
        time = logReading(time, "bus intrazonals");

        modeTimeMatrixMap.put(m, modeMatrixMap.get("travelTime"));
        modeDistanceMatrixMap.put(m, modeMatrixMap.get("distance"));


        m = ModeGermany.RAIL;
        List<Matrix> matricesRail = new ArrayList<>();
        matricesRail.add(omxToMatrix(inPtTimeFileNames.get(m), "in_vehicle_time_s", lookUps.get(m))); // "travel_time_s" // includes inVeh, access, egress
        time = logReading(time, "rail time");
        //matricesRail.add(omxToMatrix(accessTimeFileNames.get(m), "access_time_s", lookUps.get(m)));
        time = logReading(time, "rail access");
        //matricesRail.add(omxToMatrix(egressTimeFileNames.get(m), "egress_time_s", lookUps.get(m)));
        time = logReading(time, "rail egress");
        Matrix totalTravelTimeRail = sumMatrices(matricesRail);
        Matrix distanceRail = omxToMatrix(distanceFileNames.get(m), distanceMatrixNames.get(m), lookUps.get(m));
        //time = logReading(time, "rail distance");

        Matrix railAccessTime = omxToMatrix(accessTimeFileNames.get(m), "access_time_s", lookUps.get(m));
        time = logReading(time, "access_time_s");
        Matrix railEgressTime = omxToMatrix(egressTimeFileNames.get(m), "egress_time_s", lookUps.get(m));
        time = logReading(time, "egress_time_s");

        modeMatrixMap = assignIntrazonalTravelTimes(totalTravelTimeRail, distanceRail, m, 5, 10 * 60, 0.33F);
        time = logReading(time, "rail intrazonals");

        modeTimeMatrixMap.put(m, modeMatrixMap.get("travelTime"));
        modeDistanceMatrixMap.put(m, modeMatrixMap.get("distance"));

        railAccessTimeMatrixMap.put(m, railAccessTime);
        railEgressTimeMatrixMap.put(m, railEgressTime);

        // added the access time of each zone to ld rail station
        readTimeToRail(omxToMatrix(accessTimeFileNames.get(m), "access_time_s", lookUps.get(m)), dataSet, 5, 10 * 60, 1);
        time = logReading(time, "access to train");

        //if (runRailShuttleBus) {
        m = ModeGermany.RAIL_SHUTTLE;
        List<Matrix> matricesRailShuttle = new ArrayList<>();
        matricesRailShuttle.add(omxToMatrix(inPtTimeFileNames.get(m), "in_vehicle_time_s", lookUps.get(m))); // "travel_time_s" // includes inVeh, access, egress
        time = logReading(time, "rail time");
        //matricesRail.add(omxToMatrix(accessTimeFileNames.get(m), "access_time_s", lookUps.get(m)));
        time = logReading(time, "rail access");
        //matricesRail.add(omxToMatrix(egressTimeFileNames.get(m), "egress_time_s", lookUps.get(m)));
        time = logReading(time, "rail egress");
        Matrix totalTravelTimeRailShuttle = sumMatrices(matricesRailShuttle);
        Matrix distanceRailShuttle = omxToMatrix(distanceFileNames.get(m), distanceMatrixNames.get(m), lookUps.get(m));
        //time = logReading(time, "rail distance");

        Matrix railAccessTimeShuttle = omxToMatrix(accessTimeFileNames.get(m), "access_time_s", lookUps.get(m));
        time = logReading(time, "access_time_s");
        Matrix railEgressTimeShuttle = omxToMatrix(egressTimeFileNames.get(m), "egress_time_s", lookUps.get(m));
        time = logReading(time, "egress_time_s");

        modeMatrixMap = assignIntrazonalTravelTimes(totalTravelTimeRailShuttle, distanceRailShuttle, m, 5, 10 * 60, 0.33F);
        time = logReading(time, "rail intrazonals");

        modeTimeMatrixMap.put(m, modeMatrixMap.get("travelTime"));
        modeDistanceMatrixMap.put(m, modeMatrixMap.get("distance"));

        railAccessTimeMatrixMap.put(m, railAccessTimeShuttle);
        railEgressTimeMatrixMap.put(m, railEgressTimeShuttle);
        //}


        // added the access time of each zone to ld rail station
        //readTimeToRail(omxToMatrix(accessTimeFileNames.get(m), "access_time_s", lookUps.get(m)), dataSet, 5, 10*60, 1);
        //time = logReading(time, "access to train");

        Matrix railAccessDistanceShuttle = omxToMatrix(accessDistanceFileNames.get(m), "access_distance_m", lookUps.get(m));
        time = logReading(time, "access_distance_m");
        Matrix railEgressDistanceShuttle = omxToMatrix(egressDistanceFileNames.get(m), "egress_distance_m", lookUps.get(m));
        time = logReading(time, "egress_distance_m");
        railAccessDistanceMatrixMap.put(m, railAccessDistanceShuttle);
        railEgressDistanceMatrixMap.put(m, railEgressDistanceShuttle);

        dataSet.setTravelTimeMatrix(modeTimeMatrixMap);
        dataSet.setDistanceMatrix(modeDistanceMatrixMap);
        dataSet.setRailAccessDistMatrix(railAccessDistanceMatrixMap);
        dataSet.setRailEgressDistMatrix(railEgressDistanceMatrixMap);
        dataSet.setRailAccessTimeMatrix(railAccessTimeMatrixMap);
        dataSet.setRailEgressTimeMatrix(railEgressTimeMatrixMap);
        dataSet.setTollDistanceMatrix(modeTollDistanceMatrixMap);

    }

    private static long logReading(long time, String object) {
        long duration;
        duration = System.currentTimeMillis() - time;
        logger.info("Read " + object + " in (s): " + duration / 1000);
        return System.currentTimeMillis();
    }

    private static Matrix sumMatrices(List<Matrix> matrices) {

        Matrix sumMatrix = new Matrix(matrices.get(0).getRowCount(), matrices.get(0).getColumnCount());
        sumMatrix.setExternalNumbers(matrices.get(0).getExternalNumbers());
        for (Matrix matrix : matrices) {
            for (int orig : sumMatrix.getExternalRowNumbers()) {
                for (int dest : sumMatrix.getExternalColumnNumbers()) {
                    float current = sumMatrix.getValueAt(orig, dest);
                    current += matrix.getValueAt(orig, dest);
                    sumMatrix.setValueAt(orig, dest, current);
                }
            }

        }
        return sumMatrix;
    }

    private static Matrix omxToMatrix(String travelTimeFileName, String matrixName, String lookUpName) {
        OmxFile skim = new OmxFile(travelTimeFileName);
        skim.openReadOnly();
        OmxMatrix omxMatrix = skim.getMatrix(matrixName);
        Matrix travelTime = Util.convertOmxToMatrix(omxMatrix);
        OmxLookup omxLookUp = skim.getLookup(lookUpName);
        int[] externalNumbers = (int[]) omxLookUp.getLookup();
        travelTime.setExternalNumbersZeroBased(externalNumbers);
        return travelTime;
    }

//    private Matrix addAccessAndEgress(Matrix travelTime, Mode m) {
//        if (ModeGermany.BUS.equals(m)) {
//            OmxFile skimA = new OmxFile(accessTimeFileNames.get(m));
//            skimA.openReadOnly();
//            OmxMatrix omxMatrixA = skimA.getMatrix(matrixName);
//            Matrix access = Util.convertOmxToMatrix(omxMatrixA);
//            OmxFile skimE = new OmxFile(egressTimeFileNames.get(m));
//            skimE.openReadOnly();
//            OmxMatrix omxMatrixE = skimE.getMatrix(matrixName);
//            Matrix egress = Util.convertOmxToMatrix(omxMatrixE);
//            OmxFile skimInVehProp = new OmxFile(inPtTimeFileNames.get(m));
//            skimInVehProp.openReadOnly();
//            OmxMatrix omxMatrixInVehProp = skimInVehProp.getMatrix(matrixName);
//            Matrix inVeh = Util.convertOmxToMatrix(omxMatrixInVehProp);
//            for (int zoneId : dataSet.getZones().keySet()) {
//                for (int zoneDestination : dataSet.getZones().keySet()) {
//                    if (zoneId == 11) {
//                        travelTime.setValueAt(zoneId, zoneDestination, 1000000000);
//                    } else {
//                        float inVehicleTravelTime = travelTime.getValueAt(zoneId, zoneDestination);
//                        if (inVeh.getValueAt(zoneId, zoneDestination) == 0) {
//                            travelTime.setValueAt(zoneId, zoneDestination, 1000000000);
//                        } else {
//                            float travelTimeAll = inVehicleTravelTime +
//                                    access.getValueAt(zoneId, zoneDestination) +
//                                    egress.getValueAt(zoneId, zoneDestination);
//                            travelTime.setValueAt(zoneId, zoneDestination, travelTimeAll);
//                        }
//                    }
//                }
//            }
//
//        }
//        if (ModeGermany.RAIL.equals(m)) {
//            OmxFile skimA = new OmxFile(accessTimeFileNames.get(m));
//            skimA.openReadOnly();
//            OmxMatrix omxMatrixA = skimA.getMatrix(matrixName);
//            Matrix access = Util.convertOmxToMatrix(omxMatrixA);
//            OmxFile skimE = new OmxFile(egressTimeFileNames.get(m));
//            skimE.openReadOnly();
//            OmxMatrix omxMatrixE = skimE.getMatrix(matrixName);
//            Matrix egress = Util.convertOmxToMatrix(omxMatrixE);
//
//            for (int zoneId : dataSet.getZones().keySet()) {
//                for (int zoneDestination : dataSet.getZones().keySet()) {
//                    float inVehicleTravelTime = travelTime.getValueAt(zoneId, zoneDestination);
//                    float travelTimeAll = inVehicleTravelTime +
//                            access.getValueAt(zoneId, zoneDestination) +
//                            egress.getValueAt(zoneId, zoneDestination);
//                    travelTime.setValueAt(zoneId, zoneDestination, travelTimeAll);
//
//                }
//            }
//        }
//        return travelTime;
//    }

    private Map<String, Matrix> assignIntrazonalTravelTimes(Matrix travelTimeMatrix, Matrix distanceMatrix, Mode mode, int numberOfNeighbours, float maximumSeconds, float proportionOfTime) {

        float speed = 30; //km/h
        if (!ModeGermany.AIR.equals(mode)) {
            if (ModeGermany.AUTO.equals(mode)) {
                speed = 30;
            } else if (ModeGermany.RAIL.equals(mode)) {
                speed = 25;
            } else if (ModeGermany.BUS.equals(mode)) {
                speed = 15;
            }
        }

        int nonIntrazonalCounter = 0;
        for (int i = 0; i < travelTimeMatrix.getColumnCount(); i++) {
            int i_id = travelTimeMatrix.getInternalColumnNumber(i);
            double[] minTimeValues = new double[numberOfNeighbours];
            double[] minDistValues = new double[numberOfNeighbours];
            for (int k = 0; k < numberOfNeighbours; k++) {
                minTimeValues[k] = maximumSeconds;
                minDistValues[k] = maximumSeconds * speed / 3.6; //maximum distance results from maximum time at 50 km/h
            }
            //find the  n closest neighbors - the lower travel time values in the matrix column
            for (int j = 0; j < travelTimeMatrix.getRowCount(); j++) {
                int j_id = travelTimeMatrix.getInternalRowNumber(j);
                int minimumPosition = 0;
                while (minimumPosition < numberOfNeighbours) {
                    if (minTimeValues[minimumPosition] > travelTimeMatrix.getValueAt(i_id, j_id) && travelTimeMatrix.getValueAt(i_id, j_id) != 0) {
                        for (int k = numberOfNeighbours - 1; k > minimumPosition; k--) {
                            minTimeValues[k] = minTimeValues[k - 1];
                            minDistValues[k] = minDistValues[k - 1];

                        }
                        minTimeValues[minimumPosition] = travelTimeMatrix.getValueAt(i_id, j_id);
                        minDistValues[minimumPosition] = distanceMatrix.getValueAt(i_id, j_id);

                        break;
                    }
                    minimumPosition++;
                }
            }
            float globalMinTime = 0;
            float globalMinDist = 0;
            for (int k = 0; k < numberOfNeighbours; k++) {
                globalMinTime += minTimeValues[k];
                globalMinDist += minDistValues[k];
            }
            globalMinTime = globalMinTime / numberOfNeighbours * proportionOfTime;
            globalMinDist = globalMinDist / numberOfNeighbours * proportionOfTime;

            //fill with the calculated value the cells with zero
            for (int j = 0; j < travelTimeMatrix.getRowCount(); j++) {
                int j_id = travelTimeMatrix.getInternalColumnNumber(j);
                if (travelTimeMatrix.getValueAt(i_id, j_id) == 0) {
                    travelTimeMatrix.setValueAt(i_id, j_id, globalMinTime);
                    distanceMatrix.setValueAt(i, j, globalMinDist);
                    if (i != j) {
                        nonIntrazonalCounter++;
                    }
                }
            }
        }
        logger.info("Calculated intrazonal times and distances using the " + numberOfNeighbours + " nearest neighbours and maximum minutes of " + maximumSeconds / 60 + ".");
        logger.info("The calculation of intrazonals has also assigned values for cells with travel time equal to 0, that are not intrazonal: (" +
                nonIntrazonalCounter + " cases).");

        Map<String, Matrix> modeMatrixMap = new HashMap<>();
        modeMatrixMap.put("travelTime", travelTimeMatrix);
        modeMatrixMap.put("distance", distanceMatrix);

        return modeMatrixMap;
    }


    private Matrix addBigTravelTimeToSameAirportCatchmentAreaTrips(Matrix matrix, Mode mode) {

        if (ModeGermany.AIR.equals(mode)) {
            for (int zoneId : dataSet.getZones().keySet()) {
                for (int zoneDestination : dataSet.getZones().keySet()) {
                    float travelTime = matrix.getValueAt(zoneId, zoneDestination);
                    if (travelTime == 0) { //they are in the same airport catchment area - penalize travel time to 1000 hours
                        travelTime = 1000 * 3600;
                    }
                    matrix.setValueAt(zoneId, zoneDestination, travelTime);
                }
            }
        }
        return matrix;
    }


    private static void readTimeToRail(Matrix accessTimeMatrix, DataSet dataSet, int numberOfNeighbours, float maximumSeconds, float proportionOfTime) {


        int nonIntrazonalCounter = 0;
        for (int i = 0; i < accessTimeMatrix.getColumnCount(); i++) {
            int i_id = accessTimeMatrix.getExternalColumnNumber(i); //deleted

            double[] minTimeValues = new double[numberOfNeighbours];
            for (int k = 0; k < numberOfNeighbours; k++) {
                minTimeValues[k] = maximumSeconds;
            }
            //find the  n closest neighbors - the lower travel time values in the matrix column
            for (int j = 0; j < accessTimeMatrix.getRowCount(); j++) {
                int j_id = accessTimeMatrix.getExternalRowNumber(j);

                int minimumPosition = 0;
                while (minimumPosition < numberOfNeighbours) {
                    if (minTimeValues[minimumPosition] > accessTimeMatrix.getValueAt(i_id, j_id) && accessTimeMatrix.getValueAt(i_id, j_id) != 0) {
                        for (int k = numberOfNeighbours - 1; k > minimumPosition; k--) {
                            minTimeValues[k] = minTimeValues[k - 1];
                        }
                        minTimeValues[minimumPosition] = accessTimeMatrix.getValueAt(i_id, j_id);
                        break;
                    }
                    minimumPosition++;
                }
            }
            float globalMinTime = 0;
            for (int k = 0; k < numberOfNeighbours; k++) {
                globalMinTime += minTimeValues[k];
            }
            globalMinTime = globalMinTime / numberOfNeighbours * proportionOfTime;

            //fill with the calculated value the cells with zero
            ((ZoneGermany) dataSet.getZones().get(i_id)).setTimeToLongDistanceRail(globalMinTime);
        }
        logger.info("Added rail access time using the " + numberOfNeighbours + " nearest neighbours and maximum minutes of " + maximumSeconds / 60 + ".");

    }

    private static TableDataSet addFloatColumnToTableDataSet(TableDataSet table, String label) {
        float[] anArray = new float[table.getRowCount()];
        Arrays.fill(anArray, 0f);
        table.appendColumn(anArray, label);
        return table;
    }
}
