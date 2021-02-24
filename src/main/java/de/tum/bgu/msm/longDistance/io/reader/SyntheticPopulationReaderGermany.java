package de.tum.bgu.msm.longDistance.io.reader;

import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.LDModelGermany;
import de.tum.bgu.msm.longDistance.LDModelOntario;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.sp.*;
import de.tum.bgu.msm.longDistance.data.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneGermany;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Germany wide travel demand model
 * Class to read synthetic population
 * Author: Ana Moreno, Technical University of Munich (TUM), ana.moreno@tum.de
 * Date: 8 December 2020
 * Version 1
 * Adapted from SyntheticPopulationReaderOntario
 * <p>
 * Added read CD data to the method read zonal data (Carlos Llorca 20.07.16
 */

public class SyntheticPopulationReaderGermany implements SyntheticPopulationReader {

    private static Logger logger = Logger.getLogger(SyntheticPopulationReaderGermany.class);
    private JSONObject prop;
    private Map<Integer, Zone> zoneLookup;
    private DataSet dataSet;
    private String hhFilename;
    private String ppFilename;
    private String jjFilename;
    private String travellersFilename;
    private double scaleFactor;




    public SyntheticPopulationReaderGermany() {
    }

    public void setup(JSONObject prop, String inputFolder, String outputFolder) {

        this.prop = prop;
        hhFilename = inputFolder +  JsonUtilMto.getStringProp(prop, "synthetic_population.households_file");
        ppFilename = inputFolder +  JsonUtilMto.getStringProp(prop, "synthetic_population.persons_file");
        jjFilename = inputFolder +  JsonUtilMto.getStringProp(prop, "synthetic_population.jobs_file");
        travellersFilename = outputFolder +  JsonUtilMto.getStringProp(prop, "output.travellers_file");
        scaleFactor =  JsonUtilMto.getFloatProp(prop, "synthetic_population.scale_factor");

        logger.info("Synthetic population reader set up");

    }

    public void load(DataSet dataSet) {

        this.dataSet = dataSet;
        this.zoneLookup = dataSet.getZones();
        readSyntheticPopulation();
        populateZones();
        addEmploymentToZones();
        logger.info("Synthetic population loaded");
    }

    public void run(DataSet dataSet, int nThreads) {

    }

    public void populateZones() {
        for (Household hh : dataSet.getHouseholds().values()) {
            ZoneGermany zone = ((HouseholdGermany) hh).getZone();
            zone.addHouseholds(1);
            zone.addPopulation(((HouseholdGermany) hh).getHhSize());
        }
    }


    public void readSyntheticPopulation() {
        logger.info("  Reading synthetic population");
        Map<Integer, Household> households = readSyntheticHouseholds();
        dataSet.setHouseholds(households);
        dataSet.setPersons(readSyntheticPersons(households));
    }

    private Map<Integer, Household>  readSyntheticHouseholds() {

        Map<Integer, Household> householdMap = new HashMap<>();

        String recString = "";
        int recCount = 0;
        try (BufferedReader in = new BufferedReader(new FileReader(hhFilename))) {
            recString = in.readLine();

            // read header
            String[] header = recString.split(",");
            // Remove quotation marks if they are available in the header columns (after splitting by commas)
            for (int i = 0; i < header.length; i++) header[i] = header[i].replace("\"", "");

            int posId = Util.findPositionInArray("id", header);
            int posAutos = Util.findPositionInArray("autos", header);
            int posTaz = Util.findPositionInArray("zone", header); /*is the new sp*/

            // read line
            while ((recString = in.readLine()) != null) {
                recCount++;
                String[] lineElements = recString.split(",");
                int id = Integer.parseInt(lineElements[posId]);
                int hhAutos = Integer.parseInt(lineElements[posAutos]);
                int taz = Integer.parseInt(lineElements[posTaz]);

                ZoneGermany zone = (ZoneGermany) zoneLookup.get(taz);

                if (LDModelGermany.rand.nextDouble() < scaleFactor) {
                    Household hh = new HouseholdGermany(id, taz, hhAutos, zone);
                    ((HouseholdGermany) hh).setHhAutos(hhAutos);
                    householdMap.put(id, hh);
                }


            }
        } catch (IOException e) {
            logger.fatal("IO Exception caught reading synpop household file: " + hhFilename);
            logger.fatal("recCount = " + recCount + ", recString = <" + recString + ">");
        }
        logger.info("  Finished reading " + recCount + " households.");

        return householdMap;
    }


    private Map<Integer, Person>  readSyntheticPersons(Map<Integer, Household> householdMap) {

        Map<Integer, Person> personMap = new HashMap<>();

        boolean logUnmatchedHh = true;

        String recString = "";
        int recCount = 0;
        try (BufferedReader in = new BufferedReader(new FileReader(ppFilename))) {
            recString = in.readLine();

            // read header
            String[] header = recString.split(",");
            for (int i = 0; i < header.length; i++) header[i] = header[i].replace("\"", "");

            int posHhId = Util.findPositionInArray("hhid", header);
            int posId = Util.findPositionInArray("id", header);
            int posAge = Util.findPositionInArray("age", header);
            int posGender = Util.findPositionInArray("gender", header);
            int posOccupation = Util.findPositionInArray("occupation", header);
            int posIncome = Util.findPositionInArray("income", header);
            int posLicense = Util.findPositionInArray("driversLicense", header);

            // read line
            while ((recString = in.readLine()) != null) {
                recCount++;
                String[] lineElements = recString.split(",");
                int id = Integer.parseInt(lineElements[posId]);
                int hhId = Integer.parseInt(lineElements[posHhId]);
                int age = Integer.parseInt(lineElements[posAge]);
                Gender gender = Gender.valueOf(Integer.parseInt(lineElements[posGender]));
                OccupationStatus occupation = OccupationStatus.valueOf(Integer.parseInt(lineElements[posOccupation]));
                HouseholdGermany hh = (HouseholdGermany) householdMap.get(hhId);
                int income = Integer.parseInt(lineElements[posIncome]);
                hh.addIncome(income);
                boolean license = Boolean.parseBoolean(lineElements[posLicense]);
                if (hh != null) {
                    Person pp = new PersonGermany(id, hhId, age, gender, occupation, license, hh);  // this automatically puts it in id->household map in Household class
                    personMap.put(id, pp);
                } else {
                    if (logUnmatchedHh) {
                        logger.warn("The household " + hhId + " is not found. Maybe you are scaling down the population. This message will not appear anymore");
                        logUnmatchedHh = false;
                    }
                }
            }
        } catch (IOException e) {
            logger.fatal("IO Exception caught reading synpop person file: " + ppFilename);
            logger.fatal("recCount = " + recCount + ", recString = <" + recString + ">");
        }
        logger.info("  Finished reading " + recCount + " persons.");
        logger.info("  The population has " + personMap.size() + " persons in " + householdMap.size() + " households");

        return personMap;
    }

    public void writeSyntheticPopulation() {
        logger.info("Writing out data for trip generation (travellers)");


        PrintWriter pw2 = Util.openFileForSequentialWriting(travellersFilename, false);

        pw2.println("personId, away, daytrip, inOutTrip");
        for (Person person : dataSet.getPersons().values()) {
            PersonGermany trav = (PersonGermany) person;


            //takes only persons travelling
            if (trav.isAway() | trav.isDaytrip() | trav.isInOutTrip()) {
                pw2.println(trav.getPersonId() + "," + trav.isAway() + "," + trav.isDaytrip() + "," + trav.isInOutTrip());
            }
        }
        pw2.close();
    }

    private void addEmploymentToZones() {



        String recString = "";
        int recCount = 0;
        try (BufferedReader in = new BufferedReader(new FileReader(jjFilename))) {
            recString = in.readLine();

            // read header
            String[] header = recString.split(",");
            // Remove quotation marks if they are available in the header columns (after splitting by commas)
            for (int i = 0; i < header.length; i++) header[i] = header[i].replace("\"", "");

            int posTaz = Util.findPositionInArray("zone", header); /*is the new sp*/

            // read line
            while ((recString = in.readLine()) != null) {
                recCount++;
                String[] lineElements = recString.split(",");
                int taz = Integer.parseInt(lineElements[posTaz]);
                ZoneGermany zone = (ZoneGermany) zoneLookup.get(taz);
                int jobs = zone.getEmployment() + 1;
                zone.setEmployment(jobs);

            }
        } catch (IOException e) {
            logger.fatal("IO Exception caught reading synpop household file: " + hhFilename);
            logger.fatal("recCount = " + recCount + ", recString = <" + recString + ">");
        }
        logger.info("  Finished reading " + recCount + " jobs.");

    }

}
