package de.tum.bgu.msm.longDistance.destinationChoice;

import de.tum.bgu.msm.longDistance.DataSet;
import de.tum.bgu.msm.longDistance.data.trips.LongDistanceTrip;

import de.tum.bgu.msm.longDistance.data.trips.LongDistanceTripOntario;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneTypeOntario;
import de.tum.bgu.msm.longDistance.modeChoice.DomesticModeChoice;
import de.tum.bgu.msm.longDistance.modeChoice.IntModeChoice;
import org.json.simple.JSONObject;
import org.apache.log4j.Logger;
import java.util.ArrayList;

/**
 * Created by carlloga on 8/2/2017.
 */
public class DestinationChoiceOntario implements DestinationChoice {

    static Logger logger = Logger.getLogger(DestinationChoiceOntario.class);

    private DomesticDestinationChoice dcModel;
    private IntOutboundDestinationChoice dcOutboundModel;
    private IntInboundDestinationChoice dcInBoundModel;


    private IntModeChoice intModeChoice;
    private DomesticModeChoice domesticModeChoice;

    public DestinationChoiceOntario() {
    }


    @Override
    public void setup(JSONObject prop, String inputFolder, String outputFolder) {
        dcModel = new DomesticDestinationChoice(prop);
        dcOutboundModel = new IntOutboundDestinationChoice(prop);
        dcInBoundModel = new IntInboundDestinationChoice(prop);
    }

    @Override
    public void load(DataSet dataSet) {

        //load submodels
        dcModel.load(dataSet);
        dcInBoundModel.load(dataSet);
        dcOutboundModel.load(dataSet);

    }

    @Override
    public void run(DataSet dataSet, int nThreads) {
        runDestinationChoice(dataSet.getAllTrips());
    }


    public void runDestinationChoice(ArrayList<LongDistanceTripOntario> trips) {
        logger.info("Running Destination Choice Model for " + trips.size() + " trips");
        //AtomicInteger counter = new AtomicInteger(0);

        trips.parallelStream().forEach(t -> {
            if (!t.isInternational()) {
                int destZoneId = dcModel.selectDestination(t);  // trips with an origin and a destination in Canada
                t.setCombinedDestZoneId(destZoneId);
                t.setDestZoneType(dcModel.getDestinationZoneType(destZoneId));
                t.setTravelDistanceLevel2(dcModel.getAutoDist().getValueAt(t.getOrigZone().getCombinedZoneId(), destZoneId));
            } else {
                if (t.getOrigZone().getZoneType() == ZoneTypeOntario.ONTARIO || t.getOrigZone().getZoneType() == ZoneTypeOntario.EXTCANADA) {
                    // residents to international
                    int destZoneId = dcOutboundModel.selectDestination(t);
                    t.setCombinedDestZoneId(destZoneId);
                    t.setDestZoneType(dcOutboundModel.getDestinationZoneType(destZoneId));
                    if (t.getDestZoneType().equals(ZoneTypeOntario.EXTUS))
                        t.setTravelDistanceLevel2(dcModel.getAutoDist().getValueAt(t.getOrigZone().getCombinedZoneId(), destZoneId));

                } else if (t.getOrigZone().getZoneType() == ZoneTypeOntario.EXTUS) {
                    // us visitors with destination in CANADA
                    int destZoneId = dcInBoundModel.selectDestination(t);
                    t.setCombinedDestZoneId(destZoneId);
                    t.setDestZoneType(dcModel.getDestinationZoneType(destZoneId));
                    t.setTravelDistanceLevel2(dcModel.getAutoDist().getValueAt(t.getOrigZone().getCombinedZoneId(), destZoneId));
                } else {
                    //os visitors to Canada
                    int destZoneId = dcInBoundModel.selectDestination(t);
                    t.setCombinedDestZoneId(destZoneId);
                    t.setDestZoneType(dcModel.getDestinationZoneType(destZoneId));
                }
            }

//            if (Util.isPowerOfFour(counter.getAndIncrement())){
//                logger.info("dc done for: " + counter.get());
//            }

        });
    }
}
