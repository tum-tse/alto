package de.tum.bgu.msm.longDistance.scaling;

import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.sp.*;
import de.tum.bgu.msm.longDistance.data.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.io.reader.SyntheticPopulationReader;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

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

public class PotentialTravelersSelectionGermany implements SyntheticPopulationReader {

    private static Logger logger = Logger.getLogger(PotentialTravelersSelectionGermany.class);
    private JSONObject prop;
    private Map<Integer, Zone> zoneLookup;
    private DataSet dataSet;
    private double numberOfSubpopulations;
    private Map<Integer, Household> householdsAll;
    private Map<Integer, Person> personsAll;
    private int maxPotentialTravelers;



    public PotentialTravelersSelectionGermany() {
    }

    public void setup(JSONObject prop, String inputFolder, String outputFolder) {

        this.prop = prop;
        numberOfSubpopulations =  JsonUtilMto.getIntProp(prop, "synthetic_population.number_of_subpopulations");
        logger.info("Synthetic population reader set up");

    }

    public void load(DataSet dataSet) {

        this.dataSet = dataSet;
        this.zoneLookup = dataSet.getZones();
        maxPotentialTravelers = (int) Math.round(dataSet.getHouseholds().entrySet().size() / numberOfSubpopulations);
        dataSet.setNumberOfSubpopulations(numberOfSubpopulations);
        logger.info("Synthetic population loaded");
    }

    public void run(DataSet dataSet, int nThreads) {
        int populationSection = dataSet.getPopulationSection();
        Map<Integer, Household> households = selectHouseholds(populationSection);
        Map<Integer, Person> persons = selectPersons(populationSection, households);
        dataSet.setHouseholdsPotentialTravelers(households);
        dataSet.setPotentialTravelers(persons);
    }


    public Map<Integer, Household> selectHouseholds(int populationSection){
        Map<Integer, Household> householdMap = new LinkedHashMap<>();
        Integer[] keysArray = dataSet.getHouseholds().keySet().toArray(new Integer[dataSet.getHouseholds().keySet().size()]);
        if (populationSection == -1) {
            householdMap = dataSet.getHouseholds();
        } else {
            int firstHouseholdScenario = maxPotentialTravelers * (populationSection - 1);
            int lastHouseholdScenario = maxPotentialTravelers * populationSection - 1;
            if (populationSection == numberOfSubpopulations) {
                lastHouseholdScenario = dataSet.getHouseholds().entrySet().size() - 1;
            }
            for (int hhId = keysArray[firstHouseholdScenario]; hhId <= keysArray[lastHouseholdScenario]; hhId++) {
                Household hh = dataSet.getHouseholds().get(hhId);
                householdMap.put(hhId, hh);
            }
        }
        logger.info("   Selected " + householdMap.size() + " households as potential travelers.");
        return householdMap;
    }


    public Map<Integer, Person> selectPersons(int populationScenario, Map<Integer, Household> households){
        Map<Integer, Person> personMap = new LinkedHashMap<>();
        if (populationScenario == -1){
            personMap = dataSet.getPersons();
        } else {
            for (Household hh : households.values()){
                for (Person pp : ((HouseholdGermany) hh).getPersonsOfThisHousehold()){
                    personMap.put(pp.getPersonId(), pp);
                }
            }
        }
        logger.info("   Selected " + personMap.size() + " potential travelers.");
        return personMap;
    }


}
