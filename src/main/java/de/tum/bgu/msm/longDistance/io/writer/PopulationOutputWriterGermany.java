package de.tum.bgu.msm.longDistance.io.writer;

import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.sp.Household;
import de.tum.bgu.msm.longDistance.data.sp.HouseholdGermany;
import de.tum.bgu.msm.longDistance.data.sp.OccupationStatus;
import de.tum.bgu.msm.longDistance.data.sp.PersonGermany;
import de.tum.bgu.msm.longDistance.data.trips.*;
import de.tum.bgu.msm.longDistance.data.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneGermany;
import de.tum.bgu.msm.longDistance.io.reader.SyntheticPopulationReaderGermany;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

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

public class PopulationOutputWriterGermany implements OutputWriter {

    private static Logger logger = Logger.getLogger(PopulationOutputWriterGermany.class);
    private DataSet dataSet;
    private String outputFolder;
    private String outputFilehh;
    private String outputFilepp;
    private String outputFilejj;
    private int[] populationScaler;
    private Map<Integer, PersonGermany> workers;
    private int numberOfSubpopulations;
    private boolean populationScaling;
    private boolean populationSplitting;

    @Override
    public void setup(JSONObject prop, String inputFolder, String outputFolderInput) {
        outputFolder = outputFolderInput + "/";
        populationScaler = JsonUtilMto.getArrayIntProp(prop, "output.outputScalers");
        numberOfSubpopulations =  JsonUtilMto.getIntProp(prop, "synthetic_population.number_of_subpopulations");
        outputFilehh = JsonUtilMto.getStringProp(prop, "output.household_file");
        outputFilepp = JsonUtilMto.getStringProp(prop, "output.person_file");
        outputFilejj = JsonUtilMto.getStringProp(prop, "output.job_file");
        populationScaling = JsonUtilMto.getBooleanProp(prop,"output.scalePopulation");
        populationSplitting = JsonUtilMto.getBooleanProp(prop,"output.splitPopulation");
    }

    @Override
    public void load(DataSet dataSet) {
        this.dataSet = dataSet;
    }

    @Override
    public void run(DataSet dataSet, int nThreads) {
        if (populationScaling) {
            for (int scaleCount = 0; scaleCount < populationScaler.length; scaleCount++) {
                int scale = populationScaler[scaleCount];
                logger.info("Starting to write population at " + scale + " percent.");
                writeHouseholdsAndPersons(dataSet, scale);
                writeJobs(dataSet, scale);
            }
        }
        if (populationSplitting){
            writeMultipleFilesForHouseholdsAndPersons(dataSet);
            writeTotalsByZone(dataSet);
        }
    }


    private void writeHouseholdsAndPersons(DataSet dataSet, int scale){
        String filehh = outputFolder + scale + "_"+ outputFilehh;
        String filepp = outputFolder + scale + "_"+ outputFilepp;

        PrintWriter pwHousehold = Util.openFileForSequentialWriting(filehh, false);
        pwHousehold.println(HouseholdGermany.getHeader());
        PrintWriter pwPerson = Util.openFileForSequentialWriting(filepp, false);
        pwPerson.println(PersonGermany.getHeader());
        int hhCount = 1;
        int scalingFactor = (int) (100 / scale);
        int idWorker = 1;
        for (Household hh : dataSet.getHouseholds().values()) {
            if (hhCount % scalingFactor == 0) {
                pwHousehold.println(hh.toString());
                for (PersonGermany pp : ((HouseholdGermany) hh).getPersonsOfThisHousehold()){
                    pwPerson.println(pp.toString());
                    if (pp.getOccupation().equals(OccupationStatus.WORKER)){
                        workers.put(idWorker, pp);
                        idWorker++;
                    }
                }
            }
            hhCount++;
        }
        pwHousehold.close();
        pwPerson.close();

    }

    private void writeMultipleFilesForHouseholdsAndPersons(DataSet dataSet){


        Map<Integer, PrintWriter> householdWriter = new HashMap<>();
        Map<Integer, PrintWriter> personWriter = new HashMap<>();

        for (int part = 0; part <= numberOfSubpopulations; part++) {
            String filehh = outputFolder + "subPop_" + part + "_"+ outputFilehh;
            String filepp = outputFolder + "subPop_" + part + "_"+ outputFilepp;
            PrintWriter pwHousehold0 = Util.openFileForSequentialWriting(filehh, false);
            pwHousehold0.println(HouseholdGermany.getHeader());
            PrintWriter pwPerson = Util.openFileForSequentialWriting(filepp, false);
            pwPerson.println(PersonGermany.getHeader());
            householdWriter.put(part, pwHousehold0);
            personWriter.put(part, pwPerson);
        }

        int hhCount = 1;
        int partCount = 0;
        int numberOfHhSubpopulation = (int) (dataSet.getHouseholds().size() / numberOfSubpopulations);
        for (Household hh : dataSet.getHouseholds().values()) {
            if (hhCount < numberOfHhSubpopulation) {
                householdWriter.get(partCount).println(hh.toString());
                for (PersonGermany pp : ((HouseholdGermany) hh).getPersonsOfThisHousehold()){
                    personWriter.get(partCount).println(pp.toString());
                }
            } else {
                hhCount = 1;
                partCount++;
            }
            hhCount++;
        }
        for (int part = 0; part <= numberOfSubpopulations; part++) {
            householdWriter.get(part).close();
            personWriter.get(part).close();
        }

    }

    private void writeJobs(DataSet dataSet, int scale){
        String filejj= outputFolder + scale + "_"+ outputFilejj;
        PrintWriter pwJob = Util.openFileForSequentialWriting(filejj, false);
        pwJob.println("id,zone");
        int jjCount = 1;
        int scalingFactor = (int) (100 / scale);
        for (Zone zz: dataSet.getZones().values()){
            int numberOfJobs = ((ZoneGermany)zz).getEmployment();
            for (int job = 1; job <= numberOfJobs; job++){
                if (jjCount % scalingFactor == 0) {
                    pwJob.print(jjCount);
                    pwJob.print(",");
                    pwJob.println(zz.getId());
                }
                jjCount++;
            }
        }
        pwJob.close();
    }

    private void writeTotalsByZone(DataSet dataSet){
        String file = outputFolder + "subPop_totals_"+ outputFilehh;
        PrintWriter pwTotals = Util.openFileForSequentialWriting(file, false);
        pwTotals.println("zone,households,population,jobs");
        Map<Integer, Integer> populationByZone = new HashMap<>();
        Map<Integer, Integer> householdsByZone = new HashMap<>();

        dataSet.getHouseholds().values().stream()
                .collect(Collectors.groupingBy(Household::getZoneId, Collectors.counting()))
                .forEach((zone, count) -> {
                    householdsByZone.put(zone, (int) (double) count);
                    }
                );
        dataSet.getHouseholds().values().stream()
                .collect(Collectors.groupingBy(Household::getZoneId, Collectors.summarizingDouble(Household::getHouseholdSize)))
                .forEach((zone, value) -> {
                    populationByZone.put(zone, (int) value.getSum());
                        }
                );
        for (Zone zone : dataSet.getZones().values()) {
            logger.info(zone.getId());
            String line = Integer.toString(zone.getId());
            if (householdsByZone.containsKey(zone.getId())) {
                line = line + "," + householdsByZone.get(zone.getId());
            } else {
                line = line + "," + 0;
            }
            if (populationByZone.containsKey(zone.getId())) {
                line = line + "," + populationByZone.get(zone.getId());
            } else {
                line = line + "," + 0;
            }
            line = line + "," + ((ZoneGermany)zone).getEmployment();
            pwTotals.println(line);
        }
        pwTotals.close();
    }
}
