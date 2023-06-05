package de.tum.bgu.msm;

import de.tum.bgu.msm.common.datafile.TableDataFileReader;
import de.tum.bgu.msm.common.datafile.CSVFileWriter;
import de.tum.bgu.msm.common.datafile.TableDataSet;
import de.tum.bgu.msm.common.matrix.Matrix;
import de.tum.bgu.msm.common.util.ResourceUtil;
import de.tum.bgu.msm.longDistance.LDModelGermany;
import de.tum.bgu.msm.longDistance.LDModelOntario;
import omx.OmxMatrix;
import omx.hdf5.OmxHdf5Datatype;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;


import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.YearMonth;
import java.util.*;

import static java.lang.System.exit;

/**
 *
 * Utility methods for Ontario Provincial Model
 * Author: Rolf Moeckel, Technische Universität München (TUM), rolf.moeckel@tum.de
 * Date: 11 December 2015
 * Version 1
 *
 */
public class Util {
    static Logger logger = Logger.getLogger(Util.class);


    public static ResourceBundle mtoInitialization(String resourceBundleName) {

        File propFile = new File(resourceBundleName);

        return ResourceUtil.getPropertyBundle(propFile);
    }


    public static TableDataSet readCSVfile (String fileName) {
        // read csv file and return as TableDataSet
        File dataFile = new File(fileName);
        TableDataSet dataTable;
        boolean exists = dataFile.exists();
        if (!exists) {
            Path currentRelativePath = Paths.get("");
            String s = currentRelativePath.toAbsolutePath().toString();
            System.out.println("Current relative path is: " + s);
            logger.error("File not found: " + dataFile.getAbsolutePath());
            exit(1);
        }
        try {
            TableDataFileReader reader = TableDataFileReader.createReader(dataFile);
            dataTable = reader.readFile(dataFile);
            reader.close();
        } catch (Exception e) {
            logger.error("Error reading file " + dataFile);
            throw new RuntimeException(e);
        }
        return dataTable;
    }


    public static PrintWriter openFileForSequentialWriting(String fileName, boolean appendFile) {
        // open file and return PrintWriter object

        File outputFile = new File(fileName);
        try {
            FileWriter fw = new FileWriter(outputFile, appendFile);
            BufferedWriter bw = new BufferedWriter(fw);
            return new PrintWriter(bw);
        } catch (IOException e) {
            logger.error("Could not open file <" + fileName + ">.");
            return null;
        }
    }


    public static int getHighestVal(int[] array) {
        // return highest number in int array
        int high = Integer.MIN_VALUE;
        for (int num: array) high = Math.max(high, num);
        return high;
    }


    public static boolean containsElement (int[] array, int value) {
        // returns true if array contains value, otherwise false
        boolean found = false;
        for (int i: array) if (i == value) found = true;
        return found;
    }


    public static float rounder(float value, int digits) {
        // rounds value to digits behind the decimal point

        return Math.round(value * Math.pow(10, digits))/(float) Math.pow(10, digits);
    }

    public static int getDaysOfMonth(int year, int month) {
        YearMonth yearMonthObject = YearMonth.of(year, month);
        return yearMonthObject.lengthOfMonth();
    }


    public static Matrix convertOmxToMatrix (OmxMatrix omxMatrix) {
        // convert OMX matrix into java matrix

        OmxHdf5Datatype.OmxJavaType type = omxMatrix.getOmxJavaType();
        String name = omxMatrix.getName();
        int[] dimensions = omxMatrix.getShape();

        if (type.equals(OmxHdf5Datatype.OmxJavaType.FLOAT)) {
            float[][] fArray = (float[][]) omxMatrix.getData();
            Matrix mat = new Matrix(name, name, dimensions[0], dimensions[1]);
            for (int i = 0; i < dimensions[0]; i++)
                for (int j = 0; j < dimensions[1]; j++)
                    mat.setValueAt(i + 1, j + 1, fArray[i][j]);
            return mat;
        } else if (type.equals(OmxHdf5Datatype.OmxJavaType.DOUBLE)) {
            double[][] dArray = (double[][]) omxMatrix.getData();
            Matrix mat = new Matrix(name, name, dimensions[0], dimensions[1]);
            for (int i = 0; i < dimensions[0]; i++)
                for (int j = 0; j < dimensions[1]; j++)
                    mat.setValueAt(i + 1, j + 1, (float) dArray[i][j]);
            return mat;
        } else if (type.equals(OmxHdf5Datatype.OmxJavaType.INT)) {
            int[][] dArray = (int[][]) omxMatrix.getData();
            Matrix mat = new Matrix(name, name, dimensions[0], dimensions[1]);
            for (int i = 0; i < dimensions[0]; i++)
                for (int j = 0; j < dimensions[1]; j++)
                    mat.setValueAt(i + 1, j + 1, dArray[i][j]);
            return mat;
        } else {
            logger.info("OMX Matrix type " + type.toString() + " not yet implemented. Program exits.");
            exit(1);
            return null;
        }
    }

    public static Matrix convertOmxToMatrix (OmxMatrix omxMatrix, float scaler) {
        // convert OMX matrix into java matrix

        OmxHdf5Datatype.OmxJavaType type = omxMatrix.getOmxJavaType();
        String name = omxMatrix.getName();
        int[] dimensions = omxMatrix.getShape();

        if (type.equals(OmxHdf5Datatype.OmxJavaType.FLOAT)) {
            float[][] fArray = (float[][]) omxMatrix.getData();
            Matrix mat = new Matrix(name, name, dimensions[0], dimensions[1]);
            for (int i = 0; i < dimensions[0]; i++)
                for (int j = 0; j < dimensions[1]; j++)
                    mat.setValueAt(i + 1, j + 1, fArray[i][j] * scaler);
            return mat;
        } else if (type.equals(OmxHdf5Datatype.OmxJavaType.DOUBLE)) {
            double[][] dArray = (double[][]) omxMatrix.getData();
            Matrix mat = new Matrix(name, name, dimensions[0], dimensions[1]);
            for (int i = 0; i < dimensions[0]; i++)
                for (int j = 0; j < dimensions[1]; j++)
                    mat.setValueAt(i + 1, j + 1, (float) dArray[i][j] * scaler);
            return mat;
        } else {
            logger.info("OMX Matrix type " + type.toString() + " not yet implemented. Program exits.");
            exit(1);
            return null;
        }
    }


    public static int findPositionInArray (String element, String[] arr){
        // return index position of element in array arr
        int ind = -1;
        for (int a = 0; a < arr.length; a++) if (arr[a].equalsIgnoreCase(element)) ind = a;
        if (ind == -1) logger.error ("Could not find element " + element +
                " in array (see method <findPositionInArray> in class <SiloUtil>");
        return ind;
    }

    public static double[] scaleArray (double[] array, double maxVal) {
        // scale float array so that largest value equals maxVal

        double highestVal = Double.MIN_VALUE;
        for (double val: array) highestVal = Math.max(val, highestVal);
        for (int i = 0; i < array.length; i++) array[i] = ((array[i] * maxVal * 1.) / (highestVal * 1.));
        return array;
    }

    public static void initializeRandomNumber(JSONObject prop) {
        // initialize random number generator


        int seed = JsonUtilMto.getIntProp(prop, "run.random_seed");
        if (seed == -1) {
            LDModelOntario.rand = new Random();
            LDModelGermany.rand = new Random();

        } else {
            LDModelOntario.rand = new Random(seed);
            LDModelGermany.rand = new Random(seed);
        }

    }

    //select method to avoid randomization of enumIntegerDistr object
    public static Object select(double[] probabilities, Object[] id) {
        // select item based on probabilities (for zero-based float array)
        double selPos = Arrays.stream(probabilities).sum() * LDModelOntario.rand.nextFloat();
        double sum = 0;
        for (int i = 0; i < probabilities.length; i++) {
            sum += probabilities[i];
            if (sum > selPos) {
                //return i;
                return id[i];
            }
        }
        return id[probabilities.length - 1];
    }

    public static int select(double[] probabilities, int[] id) {
        // select item based on probabilities (for zero-based float array)
        double selPos = Arrays.stream(probabilities).sum() * LDModelOntario.rand.nextFloat();
        double sum = 0;
        for (int i = 0; i < probabilities.length; i++) {
            sum += probabilities[i];
            if (sum > selPos) {
                //return i;
                return id[i];
            }
        }
        return id[probabilities.length - 1];
    }

    //select method to avoid randomization of enumIntegerDistr object
    public static Object selectGermany(double[] probabilities, Object[] id) {
        // select item based on probabilities (for zero-based float array)
        double selPos = Arrays.stream(probabilities).sum() * LDModelGermany.rand.nextFloat();
        double sum = 0;
        for (int i = 0; i < probabilities.length; i++) {
            sum += probabilities[i];
            if (sum > selPos) {
                //return i;
                return id[i];
            }
        }
        return id[probabilities.length - 1];
    }

    public static Object selectGermany(double[] probabilities, Object[] id, double selPos) {
        // select item based on probabilities (for zero-based float array)
        //double selPos = Arrays.stream(probabilities).sum() * LDModelGermany.rand.nextFloat();
        double sum = 0;
        for (int i = 0; i < probabilities.length; i++) {
            sum += probabilities[i];
            if (sum > selPos) {
                //return i;
                return id[i];
            }
        }
        return id[probabilities.length - 1];
    }

    public static int selectGermany(double[] probabilities, int[] id) {
        // select item based on probabilities (for zero-based float array)
        double selPos = Arrays.stream(probabilities).sum() * LDModelGermany.rand.nextFloat();
        double sum = 0;
        for (int i = 0; i < probabilities.length; i++) {
            sum += probabilities[i];
            if (sum > selPos) {
                //return i;
                return id[i];
            }
        }
        return id[probabilities.length - 1];
    }



    public static boolean isPowerOfFour(int number){
        double pow = Math.pow(number, 0.25);
        if (pow - Math.floor(pow) == 0){
            return  true;
        } else {
            return false;
        }

    }


    public static void writeTableDataSet (TableDataSet data, String fileName) {
        try {
            CSVFileWriter cfwWriter = new CSVFileWriter();
            cfwWriter.writeFile(data, new File(fileName));
            cfwWriter.close();
        }
        catch (IOException e) {
            e.printStackTrace();
            logger.error("Could not write TableDataSet to file " + fileName + ".");
        }
    }




}
