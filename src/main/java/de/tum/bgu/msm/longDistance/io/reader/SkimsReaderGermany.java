package de.tum.bgu.msm.longDistance.io.reader;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.trips.*;
import de.tum.bgu.msm.longDistance.data.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneGermany;
import omx.OmxFile;
import omx.OmxLookup;
import omx.OmxMatrix;
import omx.hdf5.OmxConstants;
import omx.hdf5.OmxHdf5Datatype;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.*;

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

    private Map<ModeGermany, String> inPtTimeFileNames = new HashMap<>();
    private Map<ModeGermany, String> accessTimeFileNames = new HashMap<>();
    private Map<ModeGermany, String> egressTimeFileNames = new HashMap<>();

    private Map<ModeGermany, String> lookUps = new HashMap<>();

    private String accessToTrainFileName;
    private boolean readSkimsByStage;
    private String airAccessAirportFileName;
    private String airEgressAirportFileName;

    @Override
    public void setup(JSONObject prop, String inputFolder, String outputFolder) {
        this.inputFolder = inputFolder;
        this.outputFolder = outputFolder;
        this.prop = prop;

        for (ModeGermany m : ModeGermany.values()) {
            switch (m) {
                case AUTO:
                    travelTimeFileNames.put(m, JsonUtilMto.getStringProp(prop, "mode_choice.skim.time_file_auto"));
                    travelTimeMatrixNames.put(m, JsonUtilMto.getStringProp(prop, "mode_choice.skim.matrixName_auto"));
                    distanceFileNames.put(m, JsonUtilMto.getStringProp(prop, "mode_choice.skim.distance_file_auto"));
                    distanceMatrixNames.put(m, JsonUtilMto.getStringProp(prop, "mode_choice.skim.matrixName_auto"));
                    lookUps.put(m, JsonUtilMto.getStringProp(prop, "mode_choice.skim.auto_matrix_lookup"));
                    break;
                case RAIL:
                    inPtTimeFileNames.put(m, JsonUtilMto.getStringProp(prop, "mode_choice.skim.all_rail"));
                    accessTimeFileNames.put(m, JsonUtilMto.getStringProp(prop, "mode_choice.skim.all_rail"));
                    egressTimeFileNames.put(m, JsonUtilMto.getStringProp(prop, "mode_choice.skim.all_rail"));
                    distanceFileNames.put(m, JsonUtilMto.getStringProp(prop, "mode_choice.skim.all_rail"));
                    distanceMatrixNames.put(m, JsonUtilMto.getStringProp(prop, "mode_choice.skim.matrixName_distance"));
                    lookUps.put(m, JsonUtilMto.getStringProp(prop, "mode_choice.skim.pt_matrix_lookup"));
                    accessToTrainFileName = JsonUtilMto.getStringProp(prop, "zone_system.accessToRail_time_matrix");
                    break;
                case BUS:
                    inPtTimeFileNames.put(m, JsonUtilMto.getStringProp(prop, "mode_choice.skim.all_bus"));
                    accessTimeFileNames.put(m, JsonUtilMto.getStringProp(prop, "mode_choice.skim.all_bus"));
                    egressTimeFileNames.put(m, JsonUtilMto.getStringProp(prop, "mode_choice.skim.all_bus"));
                    distanceFileNames.put(m, JsonUtilMto.getStringProp(prop, "mode_choice.skim.all_bus"));
                    distanceMatrixNames.put(m, JsonUtilMto.getStringProp(prop, "mode_choice.skim.matrixName_distance"));
                    lookUps.put(m, JsonUtilMto.getStringProp(prop, "mode_choice.skim.pt_matrix_lookup"));
                    break;
                case AIR:
                    airAccessAirportFileName = JsonUtilMto.getStringProp(prop, "airport.access_airport_file");
                    airEgressAirportFileName = JsonUtilMto.getStringProp(prop, "airport.egress_airport_file");
                    break;
                default:
                    throw new RuntimeException("This mode is not implemented: " + m);
            }
        }


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

            lookup1Data = matrix.getExternalRowNumbersZeroBased();

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

        Map<Mode, Matrix> modeTimeMatrixMap = new HashMap<>();
        Map<Mode, Matrix> modeDistanceMatrixMap = new HashMap<>();

        // read skim file
        for (ModeGermany m : ModeGermany.values()) {

            switch (m) {
                case AUTO:
                    Matrix autoTravelTime = omxToMatrix(travelTimeFileNames.get(m), travelTimeMatrixNames.get(m), lookUps.get(m));
                    modeTimeMatrixMap.put(m, autoTravelTime);
                    dataSet.setAutoTravelTime(autoTravelTime);
                    Matrix autoDistance = omxToMatrix(distanceFileNames.get(m), distanceMatrixNames.get(m), lookUps.get(m));
                    modeDistanceMatrixMap.put(m, autoDistance);
                    dataSet.setAutoTravelDistance(autoDistance); //for safety and compatibility
                    break;
                case AIR:
                    //initialize empty matrices
                    Set<Integer> zones = dataSet.getZones().keySet();
                    Matrix airTravelTime = new Matrix(zones.size(), zones.size());
                    Matrix airTravelDistance = new Matrix(zones.size(), zones.size());
                    int[] externalNumbers = new int[zones.size()];
                    int index = 0;
                    for (int i : zones){
                        externalNumbers[index] = i;
                        index++;
                    }
                    airTravelTime.setExternalNumbersZeroBased(externalNumbers);
                    airTravelDistance.setExternalNumbersZeroBased(externalNumbers);
                    modeTimeMatrixMap.put(m, airTravelTime);
                    modeDistanceMatrixMap.put(m, airTravelDistance);
                    break;
                case BUS:
                    List<Matrix> matricesBus = new ArrayList<>();
                    matricesBus.add(omxToMatrix(inPtTimeFileNames.get(m), "travel_time_s", lookUps.get(m)));
                    logger.info("Finished reading " + m + " time skim.");
                    matricesBus.add(omxToMatrix(accessTimeFileNames.get(m), "access_time_s", lookUps.get(m)));
                    logger.info("Finished reading " + m + " access skim.");
                    matricesBus.add(omxToMatrix(egressTimeFileNames.get(m), "egress_time_s", lookUps.get(m)));
                    logger.info("Finished reading " + m + " egress skim.");
                    Matrix totalTravelTimeBus = sumMatrices(matricesBus);
                    modeTimeMatrixMap.put(m, totalTravelTimeBus);
                    Matrix distanceBus = omxToMatrix(distanceFileNames.get(m), distanceMatrixNames.get(m), lookUps.get(m));
                    modeDistanceMatrixMap.put(m, distanceBus);
                    break;
                case RAIL:
                    List<Matrix> matricesRail = new ArrayList<>();
                    matricesRail.add(omxToMatrix(inPtTimeFileNames.get(m), "travel_time_s", lookUps.get(m)));
                    logger.info("Finished reading " + m + " time skim.");
                    matricesRail.add(omxToMatrix(accessTimeFileNames.get(m), "access_time_s", lookUps.get(m)));
                    logger.info("Finished reading " + m + " access skim.");
                    matricesRail.add(omxToMatrix(egressTimeFileNames.get(m), "egress_time_s", lookUps.get(m)));
                    logger.info("Finished reading " + m + " egress skim.");
                    Matrix totalTravelTimeRail = sumMatrices(matricesRail);
                    modeTimeMatrixMap.put(m, totalTravelTimeRail);
                    Matrix distanceRail = omxToMatrix(distanceFileNames.get(m), distanceMatrixNames.get(m), lookUps.get(m));
                    modeDistanceMatrixMap.put(m, distanceRail);
                    //todo is this not the same as the one above called access_time_s?
                    readTimeToRail(dataSet,accessToTrainFileName, "mat1", "lookup1");
                    break;
                default:
                    throw new RuntimeException();
            }

            logger.info("Finished reading " + m + " skims.");
        }

        dataSet.setTravelTimeMatrix(modeTimeMatrixMap);
        dataSet.setDistanceMatrix(modeDistanceMatrixMap);

    }

    private static Matrix sumMatrices(List<Matrix> matrices) {

        Matrix sumMatrix = new Matrix(matrices.get(0).getRowCount(), matrices.get(0).getColumnCount());
        sumMatrix.setExternalNumbers(matrices.get(0).getExternalNumbers());
        for (Matrix matrix : matrices){
            for (int orig : sumMatrix.getExternalRowNumbers()){
                for (int dest : sumMatrix.getExternalColumnNumbers()){
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

    private Matrix assignIntrazonalTravelTimes(Matrix matrix, Mode mode) {

        if (!ModeGermany.AIR.equals(mode)) {
            float speed = 30; //km/h
            if (ModeGermany.AUTO.equals(mode)) {
                speed = 30;
            } else if (ModeGermany.RAIL.equals(mode)) {
                speed = 25;
            } else if (ModeGermany.BUS.equals(mode)) {
                speed = 15;
            }
            for (int zoneId : dataSet.getZones().keySet()) {
                int minDistance = ((ZoneGermany) dataSet.getZones().get(zoneId)).getArea();
                matrix.setValueAt(zoneId, zoneId, (float) Math.sqrt(minDistance / 3.14) / speed);
            }
        }
        logger.info("Calculated intrazonal values - nearest neighbour");
        return matrix;
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


    private static void readTimeToRail(DataSet dataSet, String fileName, String matrixName, String lookUpName) {

        OmxFile skim = new OmxFile(fileName);
        skim.openReadOnly();
        OmxMatrix omxMatrix = skim.getMatrix(matrixName);
        OmxLookup omxLookUp = skim.getLookup(lookUpName);
        int[] externalNumbers = (int[]) omxLookUp.getLookup();

        OmxHdf5Datatype.OmxJavaType type = omxMatrix.getOmxJavaType();
        int[] dimensions = omxMatrix.getShape();
        if (type.equals(OmxHdf5Datatype.OmxJavaType.FLOAT)) {
            float[][] fArray = (float[][]) omxMatrix.getData();
            float minDistance;
            for (int i = 0; i < dimensions[0]; i++) {
                minDistance = 100000f;
                for (int j = 0; j < dimensions[1]; j++) {
                    if (fArray[i][j] > 0 && fArray[i][j] < minDistance) {
                        minDistance = fArray[i][j];
                    }
                }
                ((ZoneGermany) dataSet.getZones().get(externalNumbers[i])).setTimeToLongDistanceRail(minDistance);
            }
        } else if (type.equals(OmxHdf5Datatype.OmxJavaType.DOUBLE)) {
            double[][] dArray = (double[][]) omxMatrix.getData();
            double minDistance;
            for (int i = 0; i < dataSet.getZones().keySet().size() - 1; i++) {
                minDistance = 100000f;
                for (int j = 0; j < dimensions[1]; j++) {
                    if (dArray[i][j] > 0 && dArray[i][j] < minDistance) {
                        minDistance = dArray[i][j];
                    }
                }
                ((ZoneGermany) dataSet.getZones().get(externalNumbers[i])).setTimeToLongDistanceRail((float) minDistance);
            }
        }

    }

    private static TableDataSet addFloatColumnToTableDataSet(TableDataSet table, String label) {
        float[] anArray = new float[table.getRowCount()];
        Arrays.fill(anArray, 0f);
        table.appendColumn(anArray, label);
        return table;
    }
}
