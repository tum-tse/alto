package de.tum.bgu.msm.longDistance.trafficAssignment;

import de.tum.bgu.msm.JsonUtilMto;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class StandAloneMATSimWithMultiplePopulations {

    private static Logger logger = Logger.getLogger(StandAloneMATSim.class);
    private JSONObject prop;


    public static void main(String[] args) {
        logger.info("starting matsim");
        Config config = ConfigUtils.loadConfig(args[0]);

        //JsonUtilMto jsonUtilMto = new JsonUtilMto(args[1]);
        //JSONObject prop = jsonUtilMto.getJsonProperties();


        ///String[] planFiles = JsonUtilMto.getStringArrayProp(prop, "assignment.population_files");
        //String[] planSuffixes = JsonUtilMto.getStringArrayProp(prop, "assignment.population_suffixes");

        String[] planFiles = new String[]{/*"externalDemand/trucks_1_percent/ld_trucks.xml.gz",*/
                "externalDemand/sd_1_percent_20210504/sd_trips.xml.gz",
                "externalDemand/ld_wei_1_percent/ld_trips.xml.gz"};
        String[] planSuffixes = new String[]{/*"",*/ "sd_", "ld_"};

        Population population = combinePopulations(PopulationUtils.createPopulation(config), planFiles, planSuffixes);
        MutableScenario scenario = ScenarioUtils.createMutableScenario(config);
        scenario.setPopulation(population);
        PopulationUtils.writePopulation(population, "externalDemand/combinedPopulation_1_percent.xml.gz");
        ScenarioUtils.loadScenario(scenario);

        Controler controler = new Controler(scenario);
        controler.run();

    }

    private static Population combinePopulations(Population population, String[] planFiles, String[] planSuffixes) {
        if (planFiles.length != planSuffixes.length){
            throw new RuntimeException("Inconsistent inputs");
        }

        for (int i =0; i < planFiles.length; i++){

            Population thisPopulation = PopulationUtils.readPopulation(planFiles[i]);
            String thisSuffix = planSuffixes[i];

            for (Person person : thisPopulation.getPersons().values()){
                Person newPerson = population.getFactory().createPerson(Id.createPersonId(person.getId().toString() + "_" + thisSuffix));
                population.addPerson(newPerson);
                for (Plan plan : person.getPlans()){
                    newPerson.addPlan(plan);
                }
            }
        }
        return population;
    }
}
