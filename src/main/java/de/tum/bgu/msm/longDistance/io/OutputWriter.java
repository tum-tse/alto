package de.tum.bgu.msm.longDistance.io;

import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.DataSet;
import de.tum.bgu.msm.longDistance.ModelComponent;
import de.tum.bgu.msm.longDistance.data.trips.LongDistanceTrip;
import org.json.simple.JSONObject;

import java.io.PrintWriter;
import java.util.ArrayList;

public class OutputWriter implements ModelComponent {


    private DataSet dataSet;
    private String outputFile;



    @Override
    public void setup(JSONObject prop, String inputFolder, String outputFolder) {
        outputFile = JsonUtilMto.getStringProp(prop, "output.trip_file");
    }

    @Override
    public void load(DataSet dataSet) {
        this.dataSet = dataSet;
    }

    @Override
    public void run(DataSet dataSet, int nThreads) {
        PrintWriter pw = Util.openFileForSequentialWriting(outputFile, false);
        pw.println(LongDistanceTrip.getHeader());
        for (LongDistanceTrip tr : dataSet.getAllTrips()) {
            pw.println(tr.toString());
        }
        pw.close();
    }
}
