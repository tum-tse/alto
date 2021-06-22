package de.tum.bgu.msm.longDistance.modeChoice;

import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.trips.*;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneTypeGermany;
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
    private EuropeModeChoiceGermany mcEuropeModel;

    @Override
    public void setup(JSONObject prop, String inputFolder, String outputFolder) {
        mcDomesticModel = new DomesticModeChoiceGermany(prop, inputFolder);
        mcEuropeModel = new EuropeModeChoiceGermany(prop, inputFolder);
    }

    @Override
    public void load(DataSet dataSet) {
        //load submodels
        mcDomesticModel.loadDomesticModeChoice(dataSet);
        mcEuropeModel.loadEuropeModeChoice(dataSet);


    }

    @Override
    public void run(DataSet dataSet, int nThreads) {
        runModeChoice(dataSet.getTripsofPotentialTravellers());
    }

    public void runModeChoice(ArrayList<LongDistanceTrip> trips) {
        logger.info("Running Mode Choice Model for " + trips.size() + " trips");
        trips.parallelStream().forEach(t -> {
            if (!((LongDistanceTripGermany)t).isInternational() ) {
                if (!((LongDistanceTripGermany)t).getTripState().equals(TypeGermany.AWAY)) {
                //domestic mode choice for synthetic persons in Germany
                    Mode mode = mcDomesticModel.selectModeDomestic(t);
                    ((LongDistanceTripGermany)t).setMode(mode);
                    ((LongDistanceTripGermany)t).setTravelTimeByMode(mcDomesticModel.getDomesticModalTravelTime(t));
                    ((LongDistanceTripGermany)t).setDistanceByMode(mcDomesticModel.getDomesticModalDistance(t));
                } else {
                    //for trips away we do not assign any mode because they are not travelling that they.
                    //to avoid issues on the pie chart generation, we assign now auto mode to all
                    Mode mode = ModeGermany.AUTO;
                    ((LongDistanceTripGermany)t).setMode(mode);
                    ((LongDistanceTripGermany)t).setTravelTimeByMode(mcDomesticModel.getDomesticModalTravelTime(t));
                    ((LongDistanceTripGermany)t).setDistanceByMode(mcDomesticModel.getDomesticModalDistance(t));
                }

            }else{
                if (((LongDistanceTripGermany)t).getDestZoneType().equals(ZoneTypeGermany.EXTEU)||((LongDistanceTripGermany)t).getDestZoneType().equals(ZoneTypeGermany.EXTOVERSEAS)){
                    if (!((LongDistanceTripGermany)t).getTripState().equals(TypeGermany.AWAY)) {
                        //domestic mode choice for synthetic persons in Germany
                        Mode mode = mcEuropeModel.selectModeEurope(t);
                        ((LongDistanceTripGermany)t).setMode(mode);
                        ((LongDistanceTripGermany)t).setTravelTimeByMode(mcEuropeModel.getEuropeModalTravelTime(t));
                        ((LongDistanceTripGermany)t).setDistanceByMode(mcEuropeModel.getEuropeModalDistance(t));
                    } else {
                        //for trips away we do not assign any mode because they are not travelling that they.
                        //to avoid issues on the pie chart generation, we assign now auto mode to all
                        Mode mode = ModeGermany.AUTO;
                        ((LongDistanceTripGermany)t).setMode(mode);
                        ((LongDistanceTripGermany)t).setTravelTimeByMode(mcEuropeModel.getEuropeModalTravelTime(t));
                        ((LongDistanceTripGermany)t).setDistanceByMode(mcEuropeModel.getEuropeModalDistance(t));
                    }

                }else{
                    ////for trips to overseas we do not assign air mode
                    //Mode mode = ModeGermany.AIR;
                    //((LongDistanceTripGermany)t).setMode(mode);
                    //((LongDistanceTripGermany)t).setTravelTimeByMode(mcDomesticModel.getDomesticModalTravelTime(t));
                    //((LongDistanceTripGermany)t).setDistanceByMode(mcDomesticModel.getDomesticModalDistance(t));
                }

            }

        });
    }


}
