package de.tum.bgu.msm.longDistance.io.reader;

import com.vividsolutions.jts.geom.Coordinate;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.LDModelGermany;
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
import java.util.LinkedHashMap;
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
    private String ddFilename;
    private String travellersFilename;
    private String workFolder;


    public SyntheticPopulationReaderGermany() {
    }

    public void setup(JSONObject prop, String inputFolder, String outputFolder) {

        this.prop = prop;
        workFolder = inputFolder;
        hhFilename = inputFolder +  JsonUtilMto.getStringProp(prop, "synthetic_population.households_file");
        ppFilename = inputFolder +  JsonUtilMto.getStringProp(prop, "synthetic_population.persons_file");
        boolean readJobDwelling = true;
        if (readJobDwelling){
            jjFilename = inputFolder +  JsonUtilMto.getStringProp(prop, "synthetic_population.jobs_file");
            ddFilename = inputFolder +  JsonUtilMto.getStringProp(prop, "synthetic_population.dwellings_file");
        }

        travellersFilename = outputFolder +  JsonUtilMto.getStringProp(prop, "output.travellers_file");
        Util.initializeRandomNumber(prop);
        logger.info("Synthetic population reader set up");

    }

    public void load(DataSet dataSet) {

        this.dataSet = dataSet;
        this.zoneLookup = dataSet.getZones();
        boolean runSubpopulations = JsonUtilMto.getBooleanProp(prop, "synthetic_population.runSubpopulations");
        if (runSubpopulations){
            hhFilename = workFolder + JsonUtilMto.getStringProp(prop,"synthetic_population.folder_subpopulation")
                    + dataSet.getPopulationSection() + JsonUtilMto.getStringProp(prop, "synthetic_population.households_file_subpopulation");
            ppFilename = workFolder + JsonUtilMto.getStringProp(prop,"synthetic_population.folder_subpopulation")
                    + dataSet.getPopulationSection() + JsonUtilMto.getStringProp(prop, "synthetic_population.persons_file_subpopulation");
        }
        boolean householdsWithoutCoordinates = false; //only set to true if the households do not have coordinates
        if (householdsWithoutCoordinates) {
            readSyntheticPopulationWithoutCoordinates();
            addEmploymentToZonesAndCoordinates();
            readSyntheticDwellingsAndAddCoordinates();
        } else {
            readSyntheticPopulation();
        }
        logger.info("Synthetic population loaded");
    }

    public void run(DataSet dataSet, int nThreads) {

    }


    public void readSyntheticPopulation() {
        logger.info("  Reading synthetic population");
        Map<Integer, Household> households = readSyntheticHouseholdsWithCoordinates();
        dataSet.setHouseholds(households);
        dataSet.setPersons(readSyntheticPersonsWithCoordinates(households));
    }


    public void readSyntheticPopulationWithoutCoordinates() {
        logger.info("  Reading synthetic population");
        Map<Integer, Household> households = readSyntheticHouseholds();
        dataSet.setHouseholds(households);
        dataSet.setPersons(readSyntheticPersons(households));
    }

    private Map<Integer, Household>  readSyntheticHouseholds() {

        Map<Integer, Household> householdMap = new LinkedHashMap<>();

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

                Household hh = new HouseholdGermany(id, taz, hhAutos, zone);
                ((HouseholdGermany) hh).setHhAutos(hhAutos);
                householdMap.put(id, hh);

            }
        } catch (IOException e) {
            logger.fatal("IO Exception caught reading synpop household file: " + hhFilename);
            logger.fatal("recCount = " + recCount + ", recString = <" + recString + ">");
        }
        logger.info("  Finished reading " + recCount + " households.");

        return householdMap;
    }


    private Map<Integer, Household>  readSyntheticHouseholdsWithCoordinates() {

        Map<Integer, Household> householdMap = new LinkedHashMap<>();

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
            int posTaz = Util.findPositionInArray("zone", header);
            int posCoordX = Util.findPositionInArray("coordX", header);
            int posCoordY = Util.findPositionInArray("coordY", header);

            // read line
            while ((recString = in.readLine()) != null) {
                recCount++;
                String[] lineElements = recString.split(",");
                int id = Integer.parseInt(lineElements[posId]);
                int hhAutos = Integer.parseInt(lineElements[posAutos]);
                int taz = Integer.parseInt(lineElements[posTaz]);

                ZoneGermany zone = (ZoneGermany) zoneLookup.get(taz);

                Household hh = new HouseholdGermany(id, taz, hhAutos, zone);
                ((HouseholdGermany) hh).setHhAutos(hhAutos);
                Coordinate homeLocation = new Coordinate(
                        Double.parseDouble(lineElements[posCoordX]), Double.parseDouble(lineElements[posCoordY]));
                ((HouseholdGermany) hh).setHomeLocation(homeLocation);
                householdMap.put(id, hh);

            }
        } catch (IOException e) {
            logger.fatal("IO Exception caught reading synpop household file: " + hhFilename);
            logger.fatal("recCount = " + recCount + ", recString = <" + recString + ">");
        }
        logger.info("  Finished reading " + recCount + " households.");

        return householdMap;
    }


    private void readSyntheticDwellingsAndAddCoordinates(){
        String recString = "";
        int recCount = 0;

        try (BufferedReader in = new BufferedReader(new FileReader(ddFilename))) {
            recString = in.readLine();

            // read header
            String[] header = recString.split(",");
            // Remove quotation marks if they are available in the header columns (after splitting by commas)
            for (int i = 0; i < header.length; i++) header[i] = header[i].replace("\"", "");

            int posId = Util.findPositionInArray("id", header);
            int posCoordX = Util.findPositionInArray("coordX", header);
            int posCoordY = Util.findPositionInArray("coordY", header);

            // read line
            while ((recString = in.readLine()) != null) {
                recCount++;
                String[] lineElements = recString.split(",");
                int id = Integer.parseInt(lineElements[posId]);
                if (dataSet.getHouseholds().containsKey(id)) {
                    Coordinate homeLocation = new Coordinate(
                            Double.parseDouble(lineElements[posCoordX]), Double.parseDouble(lineElements[posCoordY]));
                    ((HouseholdGermany) dataSet.getHouseholds().get(id)).setHomeLocation(homeLocation);
                }
            }
        } catch (IOException e) {
            logger.fatal("IO Exception caught reading synpop dwelling file: " + hhFilename);
            logger.fatal("recCount = " + recCount + ", recString = <" + recString + ">");
        }
        logger.info("  Finished reading " + recCount + " dwellings.");
    }


    private Map<Integer, Person>  readSyntheticPersons(Map<Integer, Household> householdMap) {

        Map<Integer, Person> personMap = new LinkedHashMap<>();

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
            // ALONA
            //int workplace = Util.findPositionInArray("workplace", header);
            //int jobType = Util.findPositionInArray("jobType", header);

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

                //if (age<19){
                //    income = translateIncomeNoIncome(age);
                //}
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


    private Map<Integer, Person>  readSyntheticPersonsWithCoordinates(Map<Integer, Household> householdMap) {

        Map<Integer, Person> personMap = new LinkedHashMap<>();

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
            int posCoordX = Util.findPositionInArray("jobCoordX", header);
            int posCoordY = Util.findPositionInArray("jobCoordY", header);
            //int posJobZone = Util.findPositionInArray("zone", header); // Job ZONE

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
                    ((PersonGermany)pp).setPpInc(income);
                    personMap.put(id, pp);
                    if (occupation.equals(OccupationStatus.WORKER)) {
                        Coordinate workLocation = new Coordinate(
                                Double.parseDouble(lineElements[posCoordX]), Double.parseDouble(lineElements[posCoordY]));
                        ((PersonGermany) pp).setWorkplaceLocation(workLocation);
                    } else {
                        ((PersonGermany) pp).setWorkplaceLocation(null);
                    }
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

    private void addEmploymentToZonesAndCoordinates() {

        String recString = "";
        int recCount = 0;
        try (BufferedReader in = new BufferedReader(new FileReader(jjFilename))) {
            recString = in.readLine();

            // read header
            String[] header = recString.split(",");
            // Remove quotation marks if they are available in the header columns (after splitting by commas)
            for (int i = 0; i < header.length; i++) header[i] = header[i].replace("\"", "");

            int posTaz = Util.findPositionInArray("zone", header); // Job ZONE
            int posWorker = Util.findPositionInArray("personId", header);
            int posCoordX = Util.findPositionInArray("coordX", header);
            int posCoordY = Util.findPositionInArray("coordY", header);

            // read line
            while ((recString = in.readLine()) != null) {
                recCount++;
                String[] lineElements = recString.split(",");
                int taz = Integer.parseInt(lineElements[posTaz]);
                ZoneGermany zone = (ZoneGermany) zoneLookup.get(taz);
                int jobs = zone.getEmployment() + 1;
                zone.setEmployment(jobs);
                int workerId = Integer.parseInt(lineElements[posWorker]);
                if (dataSet.getPersons().containsKey(workerId)) {
                    Coordinate workLocation = new Coordinate(
                            Double.parseDouble(lineElements[posCoordX]), Double.parseDouble(lineElements[posCoordY]));
                    ((PersonGermany) dataSet.getPersons().get(workerId)).setWorkplaceLocation(workLocation);
                }

            }
        } catch (IOException e) {
            logger.fatal("IO Exception caught reading synpop job file: " + hhFilename);
            logger.fatal("recCount = " + recCount + ", recString = <" + recString + ">");
        }
        logger.info("  Finished reading " + recCount + " jobs.");

    }

    public int translateIncomeNoIncome(int age){
        int valueCode = 0;
        double low = 0;
        double high = 1;
        int income = 0;
        float[] category;
        if (age < 6){
            category = new float[]{0.8891f,0.936f,0.986f,0.996f,0.9973f,0.9976f,0.9978f,0.9979f,0.9987f,1.0f};
        } else if (age < 11) {
            category = new float[]{0.880f,0.922f,0.981f,0.996f,0.9983f,0.9984f,0.9986f,0.999f,1.0f,1.0f};
        } else if (age < 16){
            category = new float[]{0.859f,0.914f,0.971f,0.994f,0.9978f,0.9985f,0.9986f,0.999f,1.0f,1.0f};
        } else {
            category = new float[]{0.595f,0.672f,0.765f,0.899f,0.971f,0.9887f,0.995f,0.9969f,0.9986f,1.0f};
        }
        int valueMicroData = 0;
        float threshold = LDModelGermany.rand.nextFloat();
        for (int i = 0; i < category.length; i++) {
            if (category[i] > threshold) {
                valueMicroData = i;
            }
        }
        switch (valueMicroData){
            case 0:
                income = 0;
                break;
            case 1: //income class
                income = 75;
                break;
            case 2: //income class
                income = 225;
                break;
            case 3: //income class
                income = 400;
                break;
            case 4: //income class
                income = 600;
                break;
            case 5: //income class
                income = 800;
                break;
            case 6: //income class
                income = 1000;
                break;
            case 7: //income class
                income = 1200;
                break;
            case 8: //income class
                income = 1400;
                break;
            case 9: //income class
                income = 1600;
                break;
        }
        return income;
    }


}
