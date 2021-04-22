package de.tum.bgu.msm.longDistance.trafficAssignment;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;


public class StandAloneMATSimWithBackgroundTraffic {

    private static Logger logger = Logger.getLogger(StandAloneMATSimWithBackgroundTraffic.class);

    public static void main(String[] args) {

        logger.info("starting matsim");
        Config config = ConfigUtils.loadConfig("configStandAloneTrucks.xml");

        config.network().setTimeVariantNetwork(true);
        config.network().setChangeEventsInputFile("externalDemand/networkChangeEvents.xml.gz");


        Scenario scenario = ScenarioUtils.loadScenario(config);
        Controler controler = new Controler(scenario);
        controler.run();

    }
}
