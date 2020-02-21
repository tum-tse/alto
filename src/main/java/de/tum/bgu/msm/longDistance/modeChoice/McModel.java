package de.tum.bgu.msm.longDistance.modeChoice;

import de.tum.bgu.msm.longDistance.DataSet;
import de.tum.bgu.msm.longDistance.data.trips.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.ModelComponent;
import de.tum.bgu.msm.longDistance.data.trips.Mode;
import de.tum.bgu.msm.longDistance.data.trips.ModeOntario;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneType;

import org.json.simple.JSONObject;
import org.apache.log4j.Logger;
import java.util.ArrayList;

/**
 * Created by carlloga on 8/2/2017.
 */
public class McModel implements ModelComponent {

    static Logger logger = Logger.getLogger(McModel.class);

    private DomesticModeChoice mcDomesticModel;
    private IntModeChoice intModeChoice;

    @Override
    public void setup(JSONObject prop, String inputFolder, String outputFolder) {
        mcDomesticModel = new DomesticModeChoice(prop);
        intModeChoice = new IntModeChoice(prop);
    }

    @Override
    public void load(DataSet dataSet) {
        //load submodels
        mcDomesticModel.loadDomesticModeChoice(dataSet);
        intModeChoice.loadIntModeChoice(dataSet);


    }

    @Override
    public void run(DataSet dataSet, int nThreads) {
        runModeChoice(dataSet.getAllTrips());
    }

    public void runModeChoice(ArrayList<LongDistanceTrip> trips) {
        logger.info("Running Mode Choice Model for " + trips.size() + " trips");
        trips.parallelStream().forEach(t -> {
            if (!t.isInternational()) {
                //domestic mode choice for synthetic persons in Ontario
                Mode mode = mcDomesticModel.selectModeDomestic(t);
                t.setMode(mode);
                t.setTravelTimeLevel2(mcDomesticModel.getDomesticModalTravelTime(t));
                // international mode choice
            } else if (t.getOrigZone().getZoneType().equals(ZoneType.ONTARIO) || t.getOrigZone().getZoneType().equals(ZoneType.EXTCANADA)) {
                //residents
                if (t.getDestZoneType().equals(ZoneType.EXTUS)) {
                    //international from Canada to US
                    Mode mode = intModeChoice.selectMode(t);
                    t.setMode(mode);
                } else {
                    //international from Canada to OS
                    t.setMode(ModeOntario.AIR); //always by air
                }
                t.setTravelTimeLevel2(intModeChoice.getInternationalModalTravelTime(t));
                //visitors
            } else if (t.getOrigZone().getZoneType().equals(ZoneType.EXTUS)) {
                //international visitors from US
                Mode mode = intModeChoice.selectMode(t);
                t.setMode(mode);
                t.setTravelTimeLevel2(intModeChoice.getInternationalModalTravelTime(t));

            } else if (t.getOrigZone().getZoneType().equals(ZoneType.EXTOVERSEAS)) {
                //international visitors from US
                t.setMode(ModeOntario.AIR); //always by air
                t.setTravelTimeLevel2(intModeChoice.getInternationalModalTravelTime(t));
            }

        });
    }


}
