package de.tum.bgu.msm.longDistance.io;

import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.trips.LongDistanceTripOntario;
import org.json.simple.JSONObject;

import java.io.PrintWriter;

public class OutputWriterGermany implements OutputWriter {


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
        pw.println(LongDistanceTripOntario.getHeader());
        for (LongDistanceTripOntario tr : dataSet.getAllTrips()) {
            pw.println(tr.toString());
        }
        pw.close();
    }
}
