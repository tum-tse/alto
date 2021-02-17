package de.tum.bgu.msm.longDistance.destinationChoice;

import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.trips.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.data.trips.LongDistanceTripGermany;
import de.tum.bgu.msm.longDistance.data.trips.ModeGermany;
import de.tum.bgu.msm.longDistance.data.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneTypeGermany;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by carlloga on 8/2/2017.
 */
public class DestinationChoiceGermany implements DestinationChoice {

    static Logger logger = Logger.getLogger(DestinationChoiceGermany.class);

    private DomesticDestinationChoiceGermany dcModel;
    private Map<Integer, Zone> zonesMap;
    private Matrix distanceByAuto;
    private AtomicInteger atomicInteger = new AtomicInteger(0);

    public DestinationChoiceGermany() {
    }


    @Override
    public void setup(JSONObject prop, String inputFolder, String outputFolder) {
        dcModel = new DomesticDestinationChoiceGermany(prop, inputFolder);

    }

    @Override
    public void load(DataSet dataSet) {

        //load submodels
        dcModel.load(dataSet);
        zonesMap = dataSet.getZones();
        distanceByAuto = dataSet.getDistanceMatrix().get(ModeGermany.AUTO);
        //TODO. code the international destination choice model
        // replace the commented the lines for inbound and outbound models by the new model


    }

    @Override
    public void run(DataSet dataSet, int nThreads) {
        runDestinationChoice(dataSet.getTripsofPotentialTravellers());
    }


    public void runDestinationChoice(ArrayList<LongDistanceTrip> trips) {
        logger.info("Running Destination Choice Model for " + trips.size() + " trips");
        AtomicInteger counter = new AtomicInteger(0);

        trips.parallelStream().forEach(t -> {
            if (! ((LongDistanceTripGermany)t).isInternational()) {
                int destZoneId = dcModel.selectDestination(t);  // trips with an origin and a destination in Canada
                ((LongDistanceTripGermany)t).setDestZoneType(ZoneTypeGermany.GERMANY);
                ((LongDistanceTripGermany)t).setDestZone(zonesMap.get(destZoneId));
                float distance = distanceByAuto.getValueAt(((LongDistanceTripGermany) t).getOrigZone().getId(), destZoneId);
                ((LongDistanceTripGermany)t).setAutoTravelDistance(distance);
                if (  Util.isPowerOfFour(counter.getAndIncrement())){
                    logger.info("Domestic trips: " + counter.get());
                }
            } else {
                //TODO. Replace by the international destination choice model
/*                if (t.getOrigZone().getZoneType() == ZoneTypeOntario.ONTARIO || t.getOrigZone().getZoneType() == ZoneTypeOntario.EXTCANADA) {
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
                }*/
            }

/*            if (Util.isPowerOfFour(counter.getAndIncrement())){
                logger.info("dc done for: " + counter.get());
            }*/

        });
        logger.info("Finished Destination Choice Model");
    }
}
