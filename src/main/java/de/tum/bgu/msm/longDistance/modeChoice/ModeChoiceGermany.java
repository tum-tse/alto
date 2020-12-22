package de.tum.bgu.msm.longDistance.modeChoice;

import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.trips.*;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneTypeOntario;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.ArrayList;

/**
 * Created by carlloga on 8/2/2017.
 */
public class ModeChoiceGermany implements ModeChoice {

    static Logger logger = Logger.getLogger(ModeChoiceGermany.class);

    private DomesticModeChoiceGermany mcDomesticModel;
    //private IntModeChoice intModeChoice;

    @Override
    public void setup(JSONObject prop, String inputFolder, String outputFolder) {
        mcDomesticModel = new DomesticModeChoiceGermany(prop);
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

    public void runModeChoice(ArrayList<LongDistanceTrip> trips) {
        logger.info("Running Mode Choice Model for " + trips.size() + " trips");
        trips.parallelStream().forEach(t -> {
            if (!((LongDistanceTripGermany)t).isInternational()) {
                //domestic mode choice for synthetic persons in Ontario
                Mode mode = mcDomesticModel.selectModeDomestic(t);
                ((LongDistanceTripGermany)t).setMode(mode);
                ((LongDistanceTripGermany)t).setTravelTime(mcDomesticModel.getDomesticModalTravelTime(t));
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
