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

        //modify scenario name!!!
        String runId = args[1];
        config.controler().setRunId(runId);

        //modify the output folder!!!
        config.controler().setOutputDirectory("F:/matsim_germany/" + runId + "/");

        String[] planFiles = new String[]{
                "plans/5_percent/2030/plans_sd.xml.gz", //modify plan file path!!!
                "plans/1_percent/ld_trucks_corrected.xml.gz",
                args[2]}; //modify plan file path!!!
        String[] planSuffixes = new String[]{"_sd", "_t", "_ld"};
        double[] reScalingFactors = new double[]{0.2 * 0.52, 1., 0.2 * 1.};

        Population population = StandAloneMATSimWithMultiplePopulations.combinePopulations(PopulationUtils.createPopulation(config),
                planFiles,
                planSuffixes,
                reScalingFactors,
                0);


        population = assignTollPreferences(population);

        MutableScenario scenario = ScenarioUtils.createMutableScenario(config);
        scenario.setPopulation(population);
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
