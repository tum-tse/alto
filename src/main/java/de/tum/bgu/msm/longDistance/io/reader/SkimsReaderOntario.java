package de.tum.bgu.msm.longDistance.io.reader;

import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.trips.Mode;
import de.tum.bgu.msm.longDistance.data.trips.ModeOntario;
import omx.OmxFile;
import omx.OmxLookup;
import omx.OmxMatrix;
import omx.hdf5.OmxConstants;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SkimsReaderOntario implements SkimsReader {

    private static Logger logger = Logger.getLogger(SkimsReaderOntario.class);

    private DataSet dataSet;
    private String inputFolder;
    private String outputFolder;
    private JSONObject prop;

    private String[] autoFileMatrixLookup;
    private String[] distanceFileMatrixLookup;

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
        //dataSet.setAutoTravelTime(autoTravelTime);
        dataSet.setAutoTravelTime(assignIntrazonalValues(autoTravelTime));

        //convertMatrixToSkim(autoFileMatrixLookup, autoTravelTime);

        Matrix autoTravelDistance = convertSkimToMatrix(distanceFileMatrixLookup);
        //dataSet.setAutoTravelDistance(autoTravelDistance);
        dataSet.setAutoTravelDistance(assignIntrazonalValues(autoTravelDistance));

        //convertMatrixToSkim(distanceFileMatrixLookup, autoTravelDistance);

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

    private Matrix assignIntrazonalValues(Matrix matrix) {
        for (int i : matrix.getExternalRowNumbers()) {
            float minDistance = 999;
            for (int j : matrix.getExternalRowNumbers()) {
                if (i != j && minDistance > matrix.getValueAt(i, j) && matrix.getValueAt(i, j) != 0) {
                    minDistance = matrix.getValueAt(i, j);
                }
            }
            matrix.setValueAt(i, i, minDistance / 2);
        }
        logger.info("Calculated intrazonal values - nearest neighbour");
        return matrix;
    }


    private void readSkimByMode(DataSet dataSet, JSONObject prop, String inputFolder) {

        Map<Mode, Matrix> travelTimeMatrix = new HashMap<>();
        Map<Mode, Matrix> priceMatrix = new HashMap<>();
        Map<Mode, Matrix> transferMatrix = new HashMap<>();
        Map<Mode, Matrix> frequencyMatrix = new HashMap<>();


        String travelTimeFileName = inputFolder + JsonUtilMto.getStringProp(prop, "mode_choice.skim.time_file");
        String priceFileName =  inputFolder +  JsonUtilMto.getStringProp(prop, "mode_choice.skim.price_file");
        String transfersFileName =  inputFolder +  JsonUtilMto.getStringProp(prop, "mode_choice.skim.transfer_file");
        String freqFileName =  inputFolder +  JsonUtilMto.getStringProp(prop, "mode_choice.skim.frequency_file");
        String lookUpName = JsonUtilMto.getStringProp(prop, "mode_choice.skim.lookup");

        // read skim file
        for (Mode m : ModeOntario.values()) {

            String matrixName = m.toString().toLowerCase();

            OmxFile skim = new OmxFile(travelTimeFileName);
            skim.openReadOnly();
            OmxMatrix omxMatrix = skim.getMatrix(matrixName);
            Matrix travelTime = Util.convertOmxToMatrix(omxMatrix);
            OmxLookup omxLookUp = skim.getLookup(lookUpName);
            int[] externalNumbers = (int[]) omxLookUp.getLookup();
            travelTime.setExternalNumbersZeroBased(externalNumbers);
            travelTimeMatrix.put(m, travelTime);

            skim = new OmxFile(priceFileName);
            skim.openReadOnly();
            omxMatrix = skim.getMatrix(matrixName);
            Matrix price = Util.convertOmxToMatrix(omxMatrix);
            price.setExternalNumbersZeroBased(externalNumbers);
            priceMatrix.put(m, price);

            skim = new OmxFile(transfersFileName);
            skim.openReadOnly();
            omxMatrix = skim.getMatrix(matrixName);
            Matrix transfers = Util.convertOmxToMatrix(omxMatrix);
            transfers.setExternalNumbersZeroBased(externalNumbers);
            transferMatrix.put(m, transfers);

            skim = new OmxFile(freqFileName);
            skim.openReadOnly();
            omxMatrix = skim.getMatrix(matrixName);
            Matrix freq = Util.convertOmxToMatrix(omxMatrix);
            freq.setExternalNumbersZeroBased(externalNumbers);
            frequencyMatrix.put(m, freq);

        }

        dataSet.setTravelTimeMatrix(travelTimeMatrix);
        dataSet.setPriceMatrix(priceMatrix);
        dataSet.setTransferMatrix(transferMatrix);
        dataSet.setFrequencyMatrix(frequencyMatrix);

    }


}
