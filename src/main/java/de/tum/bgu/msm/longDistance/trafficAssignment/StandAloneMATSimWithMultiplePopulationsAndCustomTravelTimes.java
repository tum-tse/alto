package de.tum.bgu.msm.longDistance.trafficAssignment;

import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.LDModelGermany;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityModule;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.OnlyTravelTimeDependentScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.facilities.Facility;

import java.util.List;
import java.util.Random;

import static de.tum.bgu.msm.longDistance.trafficAssignment.TollTravelDisutility.avoidToll;

public class StandAloneMATSimWithMultiplePopulationsAndCustomTravelTimes {

    private static Logger logger = Logger.getLogger(StandAloneMATSim.class);
    private JSONObject prop;


    public static void main(String[] args) {

        logger.info("starting matsim");
        Config config = ConfigUtils.loadConfig(args[0]);

        config.controler().setOutputDirectory("F:/matsim_germany/output/base_210712");
//        config.controler().setLastIteration(1);
        //works only with MATSIm 13 or 14, better do it in the config
//        config.hermes().setFlowCapacityFactor(1.0);
//        config.hermes().setFlowCapacityFactor(1.0);
//        config.controler().setRoutingAlgorithmType(ControlerConfigGroup.RoutingAlgorithmType.SpeedyALT);

        String[] planFiles = new String[]{
                "plans/1_percent/plans_sd.xml.gz",
                "plans/1_percent/ld_trucks.xml.gz", //consider to change this by ld_trucks_corrected.xml.gz
                "plans/5_percent/ld_plans.xml.gz"};
        String[] planSuffixes = new String[]{"_sd", "_t", "_ld"};
        double[] reScalingFactors = new double[]{1. * 0.699825, 1. * 1.224390, 0.2 * 0.699825};

        Population population = StandAloneMATSimWithMultiplePopulations.combinePopulations(PopulationUtils.createPopulation(config),
                planFiles,
                planSuffixes,
                reScalingFactors);

        //population = assignTollPreferencesForTesting(population);
        population = assignTollPreferences(population);

        MutableScenario scenario = ScenarioUtils.createMutableScenario(config);
        scenario.setPopulation(population);
        //PopulationUtils.writePopulation(population, "plans/combinedPopulation_1_percent.xml.gz");
        ScenarioUtils.loadScenario(scenario);

        Controler controler = new Controler(scenario);

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addTravelDisutilityFactoryBinding(TransportMode.car).toInstance(new TollTravelDisutilityFactory());
                addTravelDisutilityFactoryBinding(TransportMode.truck).toInstance(new TollTravelDisutilityFactory());
            }
        });
        controler.run();

    }

    private static Population assignTollPreferences(Population population) {
        int counterAuto = 0;
        int counterAutoNoToll = 0;
        int counterOther = 0;
        for (Person person : population.getPersons().values()) {
            for (Plan plan : person.getPlans()) {
                for (PlanElement e : plan.getPlanElements()) {
                    if (e instanceof Leg) {
                        final Leg leg = (Leg) e;
                        if (leg.getMode().equals("auto")) {
                            person.getAttributes().putAttribute(avoidToll, false);
                            counterAuto++;
                        } else if (leg.getMode().equals("auto_noToll")) {
                            person.getAttributes().putAttribute(avoidToll, true);
                            leg.setMode("car");
                            counterAutoNoToll++;
                        } else if (leg.getMode().equals("car") || leg.getMode().equals("truck")) {
                            person.getAttributes().putAttribute(avoidToll, false);
                            counterOther++;
                        } else {
                            throw new RuntimeException("This mode is not recognized");
                        }
                    }
                }
            }
        }
        return population;
    }

    private static Population assignTollPreferencesForTesting(Population population) {
        Random rand = new Random(0);
        for (Person person : population.getPersons().values()) {
            if (rand.nextDouble() < 0.3) {
                person.getAttributes().putAttribute("TOLL", true);
            } else {
                person.getAttributes().putAttribute("TOLL", false);
            }
        }

        return population;
    }

}
