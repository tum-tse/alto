package de.tum.bgu.msm.longDistance.io.writer;

import de.tum.bgu.msm.common.datafile.TableDataSet;
import de.tum.bgu.msm.common.matrix.Matrix;
import omx.OmxFile;
import omx.OmxLookup;
import omx.OmxMatrix;
import omx.hdf5.OmxConstants;

public class OmxMatrixWriter {

    public static void createOmxFile(String omxFilePath, int numberOfZones) {

        try (OmxFile omxFile = new OmxFile(omxFilePath)) {
            int dim0 = numberOfZones;
            int dim1 = dim0;
            int[] shape = {dim0, dim1};
            omxFile.openNew(shape);
            omxFile.save();

        }
    }


    public static void createOmxSkimMatrix(Matrix matrix, String omxFilePath, String omxMatrixName) {
        try (OmxFile omxFile = new OmxFile(omxFilePath)) {
            omxFile.openReadWrite();
            float mat1NA = -1;

            float[][] array = matrix.getValues();
            int[] indices = matrix.getExternalRowNumbers();

            OmxLookup lookup = new OmxLookup.OmxIntLookup("lookup1", indices, -1);
            OmxMatrix.OmxFloatMatrix mat1 = new OmxMatrix.OmxFloatMatrix(omxMatrixName, array, mat1NA);
            mat1.setAttribute(OmxConstants.OmxNames.OMX_DATASET_TITLE_KEY.getKey(), "skim_matrix");
            omxFile.addMatrix(mat1);
            omxFile.addLookup(lookup);
            omxFile.save();
            System.out.println(omxFile.summary());
            omxFile.close();
            System.out.println(omxMatrixName + "matrix written");
        }
    }

}
