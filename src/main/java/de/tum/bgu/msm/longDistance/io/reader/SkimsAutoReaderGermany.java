package de.tum.bgu.msm.longDistance.io.reader;

import de.tum.bgu.msm.common.datafile.TableDataSet;
import de.tum.bgu.msm.common.matrix.Matrix;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.trips.Mode;
import de.tum.bgu.msm.longDistance.data.trips.ModeGermany;
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
 *
 * Germany Model
 * Class to read skims
 * Author: Ana Moreno, Technical University of Munich (TUM), ana.moreno@tum.de
 * Date: 8 December 2020
 * Version 1
 * Adapted from Ontario
 * Version 1
 *
 */

public class SkimsAutoReaderGermany implements SkimsReader {

    private static Logger logger = Logger.getLogger(SkimsAutoReaderGermany.class);

    private DataSet dataSet;
    private String inputFolder;
    private String outputFolder;
    private JSONObject prop;

    private String[] autoFileMatrixLookup;
    private String[] distanceFileMatrixLookup;

    private Map<ModeGermany, String> travelTimeFileNames = new HashMap<>();
    private Map<ModeGermany, String> travelTimeMatrixNames = new HashMap<>();
    private Map<ModeGermany, String> distanceFileNames = new HashMap<>();
    private Map<ModeGermany, String> distanceMatrixNames = new HashMap<>();

    private Map<ModeGermany, String> lookUps = new HashMap<>();

    @Override
    public void setup(JSONObject prop, String inputFolder, String outputFolder) {
        this.inputFolder = inputFolder;
        this.outputFolder = outputFolder;
        this.prop = prop;

        autoFileMatrixLookup = new String[]{inputFolder + JsonUtilMto.getStringProp(prop, "mode_choice.skim.all_car"),
                JsonUtilMto.getStringProp(prop, "mode_choice.skim.matrixTime_auto"),
                JsonUtilMto.getStringProp(prop, "mode_choice.skim.auto_matrix_lookup")};
        distanceFileMatrixLookup = new String[]{inputFolder +  JsonUtilMto.getStringProp(prop, "mode_choice.skim.all_car"),
                JsonUtilMto.getStringProp(prop, "mode_choice.skim.matrixDistance_auto"),
                JsonUtilMto.getStringProp(prop, "mode_choice.skim.auto_matrix_lookup")};

        //AUTO:
        travelTimeFileNames.put(ModeGermany.AUTO, inputFolder + JsonUtilMto.getStringProp(prop, "mode_choice.skim.all_car"));
        distanceFileNames.put(ModeGermany.AUTO, inputFolder + JsonUtilMto.getStringProp(prop, "mode_choice.skim.all_car"));
        travelTimeMatrixNames.put(ModeGermany.AUTO, JsonUtilMto.getStringProp(prop, "mode_choice.skim.matrixTime_auto"));
        distanceMatrixNames.put(ModeGermany.AUTO, JsonUtilMto.getStringProp(prop, "mode_choice.skim.matrixDistance_auto"));
        lookUps.put(ModeGermany.AUTO, JsonUtilMto.getStringProp(prop, "mode_choice.skim.auto_matrix_lookup"));


    }

    @Override
    public void load(DataSet dataSet) {
        this.dataSet = dataSet;
        readSkimByMode(dataSet);
    }


    @Override
    public void run(DataSet dataSet, int nThreads) {

    }

    private void readSkimByMode(DataSet dataSet) {

        Map<String, Matrix> modeMatrixMap = new HashMap<>();
        Map<Mode, Matrix> modeTimeMatrixMap = new HashMap<>();
        Map<Mode, Matrix> modeDistanceMatrixMap = new HashMap<>();

        // read skim file
        ModeGermany m;

        long time = System.currentTimeMillis();

        m = ModeGermany.AUTO;
        Matrix autoTravelTime = omxToMatrix(travelTimeFileNames.get(m), travelTimeMatrixNames.get(m), lookUps.get(m));

        time = logReading(time, "car time");
        Matrix autoDistance = omxToMatrix(distanceFileNames.get(m), distanceMatrixNames.get(m), lookUps.get(m));
        time = logReading(time, "car distance");

        modeMatrixMap = assignIntrazonalTravelTimes(autoTravelTime, autoDistance, m,5,10*60,0.33F);
        time = logReading(time, "car intrazonals");

        modeTimeMatrixMap.put(m, modeMatrixMap.get("travelTime"));
        modeDistanceMatrixMap.put(m, modeMatrixMap.get("distance"));

        dataSet.setAutoTravelTime(modeMatrixMap.get("travelTime"));
        dataSet.setAutoTravelDistance(modeMatrixMap.get("distance")); //for safety and compatibility

        dataSet.setTravelTimeMatrix(modeTimeMatrixMap);
        dataSet.setDistanceMatrix(modeDistanceMatrixMap);




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

    private static long logReading(long time, String object) {
        long duration;
        duration = System.currentTimeMillis() - time;
        logger.info("Read " + object + " in (s): " + duration/1000  );
        return System.currentTimeMillis();
    }

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
                minDistValues[k] = maximumSeconds / 60 * speed; //maximum distance results from maximum time at 50 km/h
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
        logger.info("Calculated intrazonal times and distances using the " + numberOfNeighbours + " nearest neighbours and maximum minutes of " + maximumSeconds/60 +".");
        logger.info("The calculation of intrazonals has also assigned values for cells with travel time equal to 0, that are not intrazonal: (" +
                nonIntrazonalCounter + " cases).");

        Map<String, Matrix> modeMatrixMap = new HashMap<>();
        modeMatrixMap.put("travelTime", travelTimeMatrix);
        modeMatrixMap.put("distance", distanceMatrix);

        return modeMatrixMap;
    }


}
