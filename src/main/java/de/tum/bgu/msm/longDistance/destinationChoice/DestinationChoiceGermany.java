package de.tum.bgu.msm.longDistance.destinationChoice;

//import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.common.matrix.Matrix;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.grids.GridGermany;
import de.tum.bgu.msm.longDistance.data.sp.Household;
import de.tum.bgu.msm.longDistance.data.trips.*;
import de.tum.bgu.msm.longDistance.data.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneGermany;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneTypeGermany;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by carlloga on 8/2/2017.
 */
public class DestinationChoiceGermany implements DestinationChoice {

    static Logger logger = Logger.getLogger(DestinationChoiceGermany.class);

    private DaytripDestinationChoiceGermany daytripDcModel;
    private OvernightFirstLayerDestinationChoiceGermany overnightFirstLayerDcModel;
    private OvernightDomesticDestinationChoiceGermany overnightDomesticDcModel;
    private OvernightEuropeDestinationChoiceGermany overnightEuropeDcModel;
    private OvernightOverseasDestinationChoiceGermany overnightOverseasDcModel;
    private Map<Integer, Zone> zonesMap;
    private Matrix distanceByAuto;
    private Matrix travelTimeByAuto;
    private AtomicInteger atomicInteger = new AtomicInteger(0);
    private Map<Integer, Household> hhMap;
    int count = 0;

    public DestinationChoiceGermany() {
    }


    @Override
    public void setup(JSONObject prop, String inputFolder, String outputFolder) {
        daytripDcModel = new DaytripDestinationChoiceGermany(prop, inputFolder);
        overnightFirstLayerDcModel = new OvernightFirstLayerDestinationChoiceGermany(prop, inputFolder);
        overnightDomesticDcModel = new OvernightDomesticDestinationChoiceGermany(prop, inputFolder);
        overnightEuropeDcModel = new OvernightEuropeDestinationChoiceGermany(prop, inputFolder);
        overnightOverseasDcModel = new OvernightOverseasDestinationChoiceGermany(prop, inputFolder);

    }

    @Override
    public void load(DataSet dataSet) {

        //load submodels
        daytripDcModel.load(dataSet);
        overnightFirstLayerDcModel.load(dataSet);
        overnightDomesticDcModel.load(dataSet);
        overnightEuropeDcModel.load(dataSet);
        overnightOverseasDcModel.load(dataSet);

        zonesMap = dataSet.getZones();
        distanceByAuto = dataSet.getDistanceMatrix().get(ModeGermany.AUTO);
        travelTimeByAuto = dataSet.getTravelTimeMatrix().get(ModeGermany.AUTO);

    }

    @Override
    public void run(DataSet dataSet, int nThreads) {
        runDestinationChoice(dataSet);
        sampleDestinationMicrolocation(dataSet.getAllTrips(), dataSet);
    }

    public void sampleDestinationMicrolocation(ArrayList<LongDistanceTrip> trips, DataSet dataSet){

        logger.info("Sampling Microlocation of Destination for " + trips.size() + " trips");

        trips.parallelStream().forEach( t ->{
            selectFromGrids(dataSet, (LongDistanceTripGermany) t);
        });
        logger.info("Finished Sampling Microlocation of Destination for " + trips.size() + " trips");
    }

    public void selectFromGrids(DataSet dataSet, LongDistanceTripGermany t){
        Map<Integer, List> gridMap = dataSet.getGrids();

        ZoneGermany destZone = (ZoneGermany) t.getDestZone();

        t.setDestX(destZone.getZoneX());
        t.setDestY(destZone.getZoneY());

        int destZoneId = destZone.getId();
        List subGrid = gridMap.get(destZoneId);

        if (gridMap.get(destZoneId).size()>0){
            double selPosition = Math.random();
            double prob = 0.0;

            for (int i=0; i<subGrid.size(); i++){
                GridGermany gg = (GridGermany) subGrid.get(i);

                if (t.getTripPurpose().toString().equals(PurposeGermany.PRIVATE.toString()) | t.getTripPurpose().toString().equals(PurposeGermany.LEISURE.toString())){

                    prob += gg.getPopDensity();
                    double destX = gg.getCoordX();
                    double destY = gg.getCoordY();

                    if (prob >= selPosition){
                        t.setDestX(destX);
                        t.setDestY(destY);
                        break;
                    }else if(i==gridMap.size()){
                        t.setDestX(destX);
                        t.setDestY(destY);
                    }

                }else{

                    prob += gg.getJobDensity();
                    double destX = gg.getCoordX();
                    double destY = gg.getCoordY();

                    if (prob >= selPosition){
                        t.setDestX(destX);
                        t.setDestY(destY);
                        break;
                    }else if(i==gridMap.size()){
                        t.setDestX(destX);
                        t.setDestY(destY);
                    }
                }
            }
        }


    }


    public void runDestinationChoice(DataSet dataSet) {
        ArrayList<LongDistanceTrip> trips = dataSet.getAllTrips();
        logger.info("Running Destination Choice Model for " + trips.size() + " trips");
        AtomicInteger counter = new AtomicInteger(0);

        trips.parallelStream().forEach(t -> {

            if(t.getTripState().equals(TypeGermany.DAYTRIP)){
                int destZoneId = daytripDcModel.selectDestination(t);  // trips with an origin and a destination in Canada
                ((LongDistanceTripGermany)t).setDestZoneType((ZoneTypeGermany) zonesMap.get(destZoneId).getZoneType());
                ((LongDistanceTripGermany)t).setDestZone(zonesMap.get(destZoneId));
                float distance = distanceByAuto.getValueAt(((LongDistanceTripGermany) t).getOrigZone().getId(), destZoneId);
                float time = travelTimeByAuto.getValueAt(((LongDistanceTripGermany) t).getOrigZone().getId(), destZoneId);
                ((LongDistanceTripGermany)t).setAutoTravelDistance(distance);
                ((LongDistanceTripGermany)t).setAutoTravelTime(time);

                if (zonesMap.get(destZoneId).getZoneType().equals(ZoneTypeGermany.GERMANY)){
                    ((LongDistanceTripGermany) t).setInternational(false);
                }else{
                    ((LongDistanceTripGermany) t).setInternational(true);
                }

            }else if(t.getTripState().equals(TypeGermany.OVERNIGHT) || t.getTripState().equals(TypeGermany.AWAY)){

                ZoneTypeGermany zoneType = overnightFirstLayerDcModel.selectFirstLayerDestination(t);

                if(zoneType.equals(ZoneTypeGermany.GERMANY)){

                    int destZoneId = overnightDomesticDcModel.selectDestination(t);
                    ((LongDistanceTripGermany)t).setDestZoneType((ZoneTypeGermany) zonesMap.get(destZoneId).getZoneType());
                    ((LongDistanceTripGermany)t).setDestZone(zonesMap.get(destZoneId));
                    float distance = distanceByAuto.getValueAt(((LongDistanceTripGermany) t).getOrigZone().getId(), destZoneId);
                    float time = travelTimeByAuto.getValueAt(((LongDistanceTripGermany) t).getOrigZone().getId(), destZoneId);
                    ((LongDistanceTripGermany)t).setAutoTravelDistance(distance);
                    ((LongDistanceTripGermany)t).setAutoTravelTime(time);
                    ((LongDistanceTripGermany) t).setInternational(false);

                }else if(zoneType.equals(ZoneTypeGermany.EXTEU)){

                    int destZoneId = overnightEuropeDcModel.selectDestination(t);
                    ((LongDistanceTripGermany)t).setDestZoneType((ZoneTypeGermany) zonesMap.get(destZoneId).getZoneType());
                    ((LongDistanceTripGermany)t).setDestZone(zonesMap.get(destZoneId));
                    float distance = distanceByAuto.getValueAt(((LongDistanceTripGermany) t).getOrigZone().getId(), destZoneId);
                    float time = travelTimeByAuto.getValueAt(((LongDistanceTripGermany) t).getOrigZone().getId(), destZoneId);
                    ((LongDistanceTripGermany)t).setAutoTravelDistance(distance);
                    ((LongDistanceTripGermany)t).setAutoTravelTime(time);
                    ((LongDistanceTripGermany) t).setInternational(true);

                }else{

                    int destZoneId = overnightOverseasDcModel.selectDestination(t);
                    ((LongDistanceTripGermany)t).setDestZoneType((ZoneTypeGermany) zonesMap.get(destZoneId).getZoneType());
                    ((LongDistanceTripGermany)t).setDestZone(zonesMap.get(destZoneId));
                    float distance = distanceByAuto.getValueAt(((LongDistanceTripGermany) t).getOrigZone().getId(), destZoneId);
                    float time = travelTimeByAuto.getValueAt(((LongDistanceTripGermany) t).getOrigZone().getId(), destZoneId);
                    ((LongDistanceTripGermany)t).setAutoTravelDistance(distance);
                    ((LongDistanceTripGermany)t).setAutoTravelTime(time);
                    ((LongDistanceTripGermany) t).setInternational(true);

                }

            }else{
                //TODO. Add code for away trips; for now it is assume to be the same as overnight trips
            }

            if (Util.isPowerOfFour(counter.getAndIncrement())){
                logger.info("Trips destination assigned: " + counter.get());
            }

        });
        logger.info("Finished Destination Choice Model");
    }
}
