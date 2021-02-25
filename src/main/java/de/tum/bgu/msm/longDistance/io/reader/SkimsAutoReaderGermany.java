package de.tum.bgu.msm.longDistance.io.reader;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
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

import java.util.Arrays;
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

public class SkimsAutoReaderGermany implements SkimsReader {

    private static Logger logger = Logger.getLogger(SkimsAutoReaderGermany.class);

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


}
