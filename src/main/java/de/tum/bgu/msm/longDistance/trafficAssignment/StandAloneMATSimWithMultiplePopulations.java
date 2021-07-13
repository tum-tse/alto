package de.tum.bgu.msm.longDistance.trafficAssignment;

import de.tum.bgu.msm.JsonUtilMto;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

import java.util.Random;

public class StandAloneMATSimWithMultiplePopulations {

    private static Logger logger = Logger.getLogger(StandAloneMATSim.class);
    private JSONObject prop;


    public static void main(String[] args) {
        logger.info("starting matsim");
        Config config = ConfigUtils.loadConfig(args[0]);

        String[] planFiles = new String[]{
                "plans/1_percent/plans_sd.xml.gz",
                "plans/1_percent/ld_trucks_corrected.xml.gz",
                "plans/5_percent/ld_plans_congested_2.xml.gz"};
        String[] planSuffixes = new String[]{"_sd", "_t", "_ld"};
        double[] reScalingFactors = new double[]{1.,1.,0.2};

        Population population = combinePopulations(PopulationUtils.createPopulation(config), planFiles, planSuffixes, reScalingFactors);
        MutableScenario scenario = ScenarioUtils.createMutableScenario(config);
        scenario.setPopulation(population);
        //PopulationUtils.writePopulation(population, "plans/combinedPopulation_1_percent.xml.gz");
        ScenarioUtils.loadScenario(scenario);

        Controler controler = new Controler(scenario);
        controler.run();

    }

    public static Population combinePopulations(Population population, String[] planFiles, String[] planSuffixes, double[] reScalingFactors) {
        if (planFiles.length != planSuffixes.length) {
            throw new RuntimeException("Inconsistent inputs");
        }

        Random random = new Random();
        for (int i = 0; i < planFiles.length; i++) {

            Population thisPopulation = PopulationUtils.readPopulation(planFiles[i]);
            String thisSuffix = planSuffixes[i];

            for (Person person : thisPopulation.getPersons().values()) {
                boolean add = true;
                boolean hasActivity = false;
                double scaleFactor = reScalingFactors[i];
                if (random.nextDouble() < scaleFactor) {
                    Person newPerson = population.getFactory().createPerson(Id.createPersonId(person.getId().toString() + "_" + thisSuffix));

                    for (Plan plan : person.getPlans()) {
                        newPerson.addPlan(plan);

                        for (PlanElement planElement : plan.getPlanElements()) {
                            if (planElement instanceof Activity){
                                hasActivity = true;
                                final Activity activity = (Activity) planElement;
                                if (activity.getType().equals("Airport") ||
                                        activity.getType().equals("Other")){
                                    add = false;
                                }
                                if (activity.getEndTime().orElse(1) < 0){
                                    add = false;
                                }
                            }
                            if (planElement instanceof Leg){
                                final Leg leg = (Leg) planElement;
                                final String mode = leg.getMode();
                                if (mode.equalsIgnoreCase("air")){
                                    add = false;
                                }
                                if (mode.equalsIgnoreCase("auto")){
                                    leg.setMode("car");
                                }
                            }
                        }
                    }
                    if (add && hasActivity){
                        population.addPerson(newPerson);
                    }

                }

            }
        }
        return population;
    }
}
