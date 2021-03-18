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

import java.io.*;
import java.util.ArrayList;
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
    private String outputFiledd;
    private int[] populationScaler;
    private int numberOfSubpopulations;
    private boolean populationScaling;
    private boolean populationSplitting;
    private String hhFilename;
    private String jjFilename;
    private String ddFilename;

    @Override
    public void setup(JSONObject prop, String inputFolder, String outputFolderInput) {
        outputFolder = outputFolderInput + "/";
        populationScaler = JsonUtilMto.getArrayIntProp(prop, "output.outputScalers");
        numberOfSubpopulations =  JsonUtilMto.getIntProp(prop, "synthetic_population.number_of_subpopulations");
        outputFilehh = JsonUtilMto.getStringProp(prop, "output.household_file");
        outputFilepp = JsonUtilMto.getStringProp(prop, "output.person_file");
        outputFilejj = JsonUtilMto.getStringProp(prop, "output.job_file");
        outputFiledd = JsonUtilMto.getStringProp(prop, "output.dwelling_file");
        populationScaling = JsonUtilMto.getBooleanProp(prop,"output.scalePopulation");
        populationSplitting = JsonUtilMto.getBooleanProp(prop,"output.splitPopulation");
        hhFilename = inputFolder +  JsonUtilMto.getStringProp(prop, "synthetic_population.households_file");
        jjFilename = inputFolder +  JsonUtilMto.getStringProp(prop, "synthetic_population.jobs_file");
        ddFilename = inputFolder +  JsonUtilMto.getStringProp(prop, "synthetic_population.dwellings_file");
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
                Map<String, List<Integer>> writtenPopulation = writeHouseholdsAndPersons(dataSet, scale);
                //writeJobs(dataSet, scale, writtenPopulation); //removed this method because it takes very long to find the worker in the worker map. Consider to store in a map all the String and then write out only the strings whose worker is in the subpopulation
                //writeDwellings(dataSet, scale, writtenPopulation);
            }
        }
        if (populationSplitting){
            writeMultipleFilesForHouseholdsAndPersons(dataSet);
            writeTotalsByZone(dataSet);
        }
    }


    private Map<String, List<Integer>> writeHouseholdsAndPersons(DataSet dataSet, int scale){

        String filehh = outputFolder + scale + "perc_"+ outputFilehh;
        String filepp = outputFolder + scale + "perc_"+ outputFilepp;

        Map<String, List<Integer>> writtenPopulation = new HashMap<>();
        writtenPopulation.put("household", new ArrayList<>());
        writtenPopulation.put("worker", new ArrayList<>());
        PrintWriter pwHousehold = Util.openFileForSequentialWriting(filehh, false);
        pwHousehold.println(HouseholdGermany.getHeader());
        PrintWriter pwPerson = Util.openFileForSequentialWriting(filepp, false);
        pwPerson.println(PersonGermany.getHeader());
        int hhCount = 1;
        int scalingFactor = (int) (100 / scale);
        for (Household hh : dataSet.getHouseholds().values()) {
            if (hhCount % scalingFactor == 0) {
                pwHousehold.println(hh.toString());
                writtenPopulation.get("household").add(hh.getId());
                for (PersonGermany pp : ((HouseholdGermany) hh).getPersonsOfThisHousehold()){
                    pwPerson.println(pp.toString());
                    if (pp.getOccupation().equals(OccupationStatus.WORKER)){
                        writtenPopulation.get("worker").add(pp.getPersonId());
                    }
                }
            }
            hhCount++;
        }
        pwHousehold.close();
        pwPerson.close();
        return writtenPopulation;
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

    private void writeJobs(DataSet dataSet, int scale, Map<String, List<Integer>> writtenPopulation) {

        String filejj = outputFolder + scale + "perc_" + outputFilejj;
        PrintWriter pwJob = Util.openFileForSequentialWriting(filejj, false);

        String recString = "";
        int recCount = 0;

        try (BufferedReader in = new BufferedReader(new FileReader(jjFilename))) {
            recString = in.readLine();
            String[] header = recString.split(",");
            // Remove quotation marks if they are available in the header columns (after splitting by commas)
            for (int i = 0; i < header.length; i++) header[i] = header[i].replace("\"", "");

            int posId = Util.findPositionInArray("personId", header);

            pwJob.println(recString);
            // read line
            while ((recString = in.readLine()) != null) {
                recCount++;
                String[] lineElements = recString.split(",");
                int workerId = Integer.parseInt(lineElements[posId]);
                if (writtenPopulation.get("worker").contains(workerId)){
                    pwJob.println(recString);
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        pwJob.close();
    }


    private void writeDwellings(DataSet dataSet, int scale, Map<String, List<Integer>> writtenPopulation) {

        String filedd = outputFolder + scale + "perc_" + outputFiledd;
        PrintWriter pwDwelling = Util.openFileForSequentialWriting(filedd, false);

        String recString = "";
        int recCount = 0;

        try (BufferedReader in = new BufferedReader(new FileReader(ddFilename))) {
            recString = in.readLine();
            String[] header = recString.split(",");
            // Remove quotation marks if they are available in the header columns (after splitting by commas)
            for (int i = 0; i < header.length; i++) header[i] = header[i].replace("\"", "");

            int posId = Util.findPositionInArray("hhId", header);

            pwDwelling.println(recString);
            // read line
            while ((recString = in.readLine()) != null) {
                recCount++;
                String[] lineElements = recString.split(",");
                int hhId = Integer.parseInt(lineElements[posId]);
                if (writtenPopulation.get("household").contains(hhId)){
                    pwDwelling.println(recString);
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        pwDwelling.close();
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
