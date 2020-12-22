package de.tum.bgu.msm.longDistance.io.reader;

import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.trips.Mode;
import de.tum.bgu.msm.longDistance.data.trips.ModeGermany;
import de.tum.bgu.msm.longDistance.data.trips.ModeOntario;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneGermany;
import omx.OmxFile;
import omx.OmxLookup;
import omx.OmxMatrix;
import omx.hdf5.OmxConstants;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Map;

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

public class SkimsReaderGermany implements SkimsReader {

    private static Logger logger = Logger.getLogger(SkimsReaderGermany.class);

    private DataSet dataSet;
    private String inputFolder;
    private String outputFolder;
    private JSONObject prop;

    private String[] autoFileMatrixLookup;
    private String[] distanceFileMatrixLookup;
    private Map<Mode, String> travelTimeFileNames = new HashMap<>();
    private Map<Mode, String> distanceFileNames = new HashMap<>();
    private String lookUpName;
    private String matrixName;

    @Override
    public void setup(JSONObject prop, String inputFolder, String outputFolder) {
        this.inputFolder = inputFolder;
        this.outputFolder = outputFolder;
        this.prop = prop;

        autoFileMatrixLookup = new String[]{inputFolder + JsonUtilMto.getStringProp(prop, "zone_system.skim.time.file"),
                JsonUtilMto.getStringProp(prop, "zone_system.skim.time.matrix"),
                JsonUtilMto.getStringProp(prop, "zone_system.skim.time.lookup")};
        distanceFileMatrixLookup = new String[]{inputFolder +  JsonUtilMto.getStringProp(prop, "zone_system.skim.distance.file"),
                JsonUtilMto.getStringProp(prop, "zone_system.skim.distance.matrix"),
                JsonUtilMto.getStringProp(prop, "zone_system.skim.distance.lookup")};

        for (Mode m : ModeGermany.values()) {
            String travelTimeFileName = inputFolder + JsonUtilMto.getStringProp(prop, "mode_choice.skim.time_file_" + m);
            String distanceFileName = inputFolder + JsonUtilMto.getStringProp(prop, "mode_choice.skim.distance_file_" + m);
            travelTimeFileNames.put(m, travelTimeFileName);
            distanceFileNames.put(m, distanceFileName);
        }
        lookUpName = JsonUtilMto.getStringProp(prop, "mode_choice.skim.lookup");
        matrixName = JsonUtilMto.getStringProp(prop, "mode_choice.skim.matrixName");

    }

    @Override
    public void load(DataSet dataSet) {
        this.dataSet = dataSet;
        readSkims();
        readSkimByMode(dataSet, prop, inputFolder);
    }

    @Override
    public void run(DataSet dataSet, int nThreads) {

    }

    public void readSkims() {
        Matrix autoTravelTime = convertSkimToMatrix(autoFileMatrixLookup);
        dataSet.setAutoTravelTime(autoTravelTime);

        Matrix autoTravelDistance = convertSkimToMatrix(distanceFileMatrixLookup);
        dataSet.setAutoTravelDistance(autoTravelDistance);
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
                int minDistance =  ((ZoneGermany) dataSet.getZones().get(zoneId)).getArea();
                matrix.setValueAt(zoneId, zoneId, (float) Math.sqrt(minDistance / 3.14));
            }
        }
        logger.info("Calculated intrazonal values - nearest neighbour");
        return matrix;
    }


    private void readSkimByMode(DataSet dataSet, JSONObject prop, String inputFolder) {

        Map<Mode, Matrix> travelTimeMatrix = new HashMap<>();
        Map<Mode, Matrix> distanceMatrix = new HashMap<>();


        // read skim file
        for (Mode m : ModeGermany.values()) {

            String travelTimeFileName = travelTimeFileNames.get(m);
            String distanceFileName = distanceFileNames.get(m);

            OmxFile skim = new OmxFile(travelTimeFileName);
            skim.openReadOnly();
            OmxMatrix omxMatrix = skim.getMatrix(matrixName);
            Matrix travelTime = Util.convertOmxToMatrix(omxMatrix);
            OmxLookup omxLookUp = skim.getLookup(lookUpName);
            int[] externalNumbers = (int[]) omxLookUp.getLookup();
            travelTime.setExternalNumbersZeroBased(externalNumbers);
            travelTimeMatrix.put(m, travelTime);

            OmxFile skimDistance = new OmxFile(distanceFileName);
            skimDistance.openReadOnly();
            OmxMatrix omxMatrixDistance = skimDistance.getMatrix(matrixName);
            Matrix distance = Util.convertOmxToMatrix(omxMatrixDistance);
            OmxLookup omxLookUpDistance = skimDistance.getLookup(lookUpName);
            int[] externalNumbersDistance = (int[]) omxLookUpDistance.getLookup();
            distance.setExternalNumbersZeroBased(externalNumbersDistance);
            distanceMatrix.put(m, distance);

            logger.info("Finished reading " + m + " skims.");
        }

        dataSet.setTravelTimeMatrix(travelTimeMatrix);
        dataSet.setDistanceMatrix(distanceMatrix);

    }

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
                int minDistance =  ((ZoneGermany) dataSet.getZones().get(zoneId)).getArea();
                matrix.setValueAt(zoneId, zoneId, (float) Math.sqrt(minDistance / 3.14) / speed);
            }
        }
        logger.info("Calculated intrazonal values - nearest neighbour");
        return matrix;
    }


}
