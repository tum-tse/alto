package de.tum.bgu.msm.longDistance.destinationChoice;

import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.DataSet;
import de.tum.bgu.msm.longDistance.data.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.ModelComponent;
import de.tum.bgu.msm.longDistance.zoneSystem.ZoneType;

import org.json.simple.JSONObject;
import org.apache.log4j.Logger;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by carlloga on 8/2/2017.
 */
public class Distribution implements ModelComponent {

    static Logger logger = Logger.getLogger(Distribution.class);

    private DomesticDestinationChoice dcModel;
    private IntOutboundDestinationChoice dcOutboundModel;
    private IntInboundDestinationChoice dcInBoundModel;


    @Override
    public void setup(JSONObject prop, String inputFolder, String outputFolder) {

        dcModel = new DomesticDestinationChoice(prop);
        dcOutboundModel = new IntOutboundDestinationChoice(prop);
        dcInBoundModel = new IntInboundDestinationChoice(prop);
    }

    @Override
    public void load(DataSet dataSet) {
        //store the models in the dataset
        dataSet.setDcDomestic(dcModel);
        dataSet.setDcIntOutbound(dcOutboundModel);
        dataSet.setDcIntInbound(dcInBoundModel);

        //load submodels
        dcModel.load(dataSet);
        dcInBoundModel.load(dataSet);
        dcOutboundModel.load(dataSet);

    }

    @Override
    public void run(DataSet dataSet, int nThreads) {
        runDestinationChoice(dataSet.getAllTrips());
    }


    public void runDestinationChoice(ArrayList<LongDistanceTrip> trips) {
        logger.info("Running Destination Choice Model for " + trips.size() + " trips");
        //AtomicInteger counter = new AtomicInteger(0);

        trips.parallelStream().forEach(t -> {
            if (!t.isInternational()) {
                int destZoneId = dcModel.selectDestination(t);  // trips with an origin and a destination in Canada
                t.setCombinedDestZoneId(destZoneId);
                t.setDestZoneType(dcModel.getDestinationZoneType(destZoneId));
                t.setTravelDistanceLevel2(dcModel.getAutoDist().getValueAt(t.getOrigZone().getCombinedZoneId(), destZoneId));
            } else {
                if (t.getOrigZone().getZoneType() == ZoneType.ONTARIO || t.getOrigZone().getZoneType() == ZoneType.EXTCANADA) {
                    // residents to international
                    int destZoneId = dcOutboundModel.selectDestination(t);
                    t.setCombinedDestZoneId(destZoneId);
                    t.setDestZoneType(dcOutboundModel.getDestinationZoneType(destZoneId));
                    if (t.getDestZoneType().equals(ZoneType.EXTUS))
                        t.setTravelDistanceLevel2(dcModel.getAutoDist().getValueAt(t.getOrigZone().getCombinedZoneId(), destZoneId));

                } else if (t.getOrigZone().getZoneType() == ZoneType.EXTUS) {
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
