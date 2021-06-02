package de.tum.bgu.msm.longDistance.trafficAssignment;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;


public class StandAloneMATSim {

    private static Logger logger = Logger.getLogger(StandAloneMATSim.class);

    public static void main(String[] args) {

        logger.info("starting matsim");
        Config config = ConfigUtils.loadConfig("configStandAlone.xml");

        config.qsim().setFlowCapFactor(0.02);
        config.qsim().setStorageCapFactor(0.02);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        Controler controler = new Controler(scenario);
        controler.run();

    }
}
