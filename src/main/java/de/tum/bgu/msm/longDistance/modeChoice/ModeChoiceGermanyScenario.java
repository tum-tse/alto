package de.tum.bgu.msm.longDistance.modeChoice;

import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.trips.*;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.ArrayList;

/**
 * Created by carlloga on 8/2/2017.
 */
public class ModeChoiceGermanyScenario implements ModeChoice {

    static Logger logger = Logger.getLogger(ModeChoiceGermanyScenario.class);
    int scenario;
    private DomesticModeChoiceGermanyScenario mcDomesticModel;
    //private IntModeChoice intModeChoice;


    @Override
    public void setup(JSONObject prop, String inputFolder, String outputFolder) {
        mcDomesticModel = new DomesticModeChoiceGermanyScenario(prop, inputFolder);
        //intModeChoice = new IntModeChoice(prop);
    }

    @Override
    public void load(DataSet dataSet) {
        //load submodels
        mcDomesticModel.loadDomesticModeChoice(dataSet);
        //intModeChoice.loadIntModeChoice(dataSet);


    }

    @Override
    public void run(DataSet dataSet, int nThreads) {

        runModeChoice(dataSet.getAllTrips());
    }

    public void setScenario(int scenario){
        this.scenario = scenario;
    }

    public void runModeChoice(ArrayList<LongDistanceTrip> trips) {
        logger.info("Running Mode Choice Model for " + trips.size() + " trips");
        trips.parallelStream().forEach(t -> {
            if (!((LongDistanceTripGermany)t).isInternational() ) {
                if (!((LongDistanceTripGermany)t).getTripState().equals(TypeGermany.AWAY)) {
                //domestic mode choice for synthetic persons in Germany
                    Mode mode = mcDomesticModel.selectModeDomestic(t);
                    ((LongDistanceTripGermany)t).setMode(mode);
                    ((LongDistanceTripGermany)t).setTravelTime(mcDomesticModel.getDomesticModalTravelTime(t));
                    ((LongDistanceTripGermany)t).setDistanceByMode(mcDomesticModel.getDomesticModalDistance(t));
                } else {
                    //for trips away we do not assign any mode because they are not travelling that they.
                    //to avoid issues on the pie chart generation, we assign now auto mode to all
                    Mode mode = ModeGermany.AUTO;
                    ((LongDistanceTripGermany)t).setMode(mode);
                    ((LongDistanceTripGermany)t).setTravelTime(mcDomesticModel.getDomesticModalTravelTime(t));
                    ((LongDistanceTripGermany)t).setDistanceByMode(mcDomesticModel.getDomesticModalDistance(t));
                }
                // international mode choice
            } /*else if (t.getOrigZone().getZoneType().equals(ZoneTypeOntario.ONTARIO) || t.getOrigZone().getZoneType().equals(ZoneTypeOntario.EXTCANADA)) {
                //residents
                if (t.getDestZoneType().equals(ZoneTypeOntario.EXTUS)) {
                    //international from Canada to US
                    Mode mode = intModeChoice.selectMode(t);
                    t.setMode(mode);
                } else {
                    //international from Canada to OS
                    t.setMode(ModeOntario.AIR); //always by air
                }
                t.setTravelTimeLevel2(intModeChoice.getInternationalModalTravelTime(t));
                //visitors
            } else if (t.getOrigZone().getZoneType().equals(ZoneTypeOntario.EXTUS)) {
                //international visitors from US
                Mode mode = intModeChoice.selectMode(t);
                t.setMode(mode);
                t.setTravelTimeLevel2(intModeChoice.getInternationalModalTravelTime(t));

            } else if (t.getOrigZone().getZoneType().equals(ZoneTypeOntario.EXTOVERSEAS)) {
                //international visitors from US
                t.setMode(ModeOntario.AIR); //always by air
                t.setTravelTimeLevel2(intModeChoice.getInternationalModalTravelTime(t));
            }*/

        });
    }


}
