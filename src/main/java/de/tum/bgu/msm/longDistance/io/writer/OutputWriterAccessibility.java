package de.tum.bgu.msm.longDistance.io.writer;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.trips.*;
import de.tum.bgu.msm.longDistance.data.zoneSystem.Zone;
import org.json.simple.JSONObject;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * Germany Model
 * Class to write outputs
 * Author: Ana Moreno, Technical University of Munich (TUM), ana.moreno@tum.de
 * Date: 8 December 2020
 * Version 1
 * Adapted from Ontario
 * Version 1
 *
 */

public class OutputWriterAccessibility implements OutputWriter {


    private DataSet dataSet;
    private String outputFile;
    private String outputFolder;
    private String outputFileName;

    @Override
    public void setup(JSONObject prop, String inputFolder, String outputFolderInput) {
        outputFolder = outputFolderInput;
        outputFileName = JsonUtilMto.getStringProp(prop, "accessibility.accessibility_file") ;
    }

    @Override
    public void load(DataSet dataSet) {
        this.dataSet = dataSet;
        this.outputFile = outputFolder + dataSet.getPopulationSection() + "_" + outputFileName;
    }

    @Override
    public void run(DataSet dataSet, int nThreads) {

        Map<Mode, Map<Zone, Map<String, Map<Integer, Double>>>> accessibilityByModeZone = dataSet.getAccessibilityByModeZone();
        PrintWriter pw = Util.openFileForSequentialWriting(outputFile, false);
        String header = "zone";
        for (Mode m : accessibilityByModeZone.keySet()){
            for (String vot : accessibilityByModeZone.get(m).get(1).keySet()) {
                for (int combination : accessibilityByModeZone.get(m).get(1).get("vot_other").keySet()) {
                    header = header + "," + m.toString() + "_" + vot + "_" + combination;
                }
            }
        }
        pw.println(header);
        for (Zone zone : accessibilityByModeZone.get(ModeGermany.AUTO).keySet()) {
            String values = String.valueOf(zone.getId());
            for (Mode m : accessibilityByModeZone.keySet()) {
                for (String vot : accessibilityByModeZone.get(m).get(zone.getId()).keySet()) {
                    for (int combination : accessibilityByModeZone.get(m).get(zone.getId()).get(vot).keySet()) {
                        values = values + "," + accessibilityByModeZone.get(m).get(zone.getId()).get(vot).get(combination);
                    }
                }
            }
            pw.println(values);
        }
        pw.close();

    }


}
