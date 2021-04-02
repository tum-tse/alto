package de.tum.bgu.msm.longDistance;

import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.trips.*;
import de.tum.bgu.msm.longDistance.data.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneType;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneTypeGermany;
import de.tum.bgu.msm.longDistance.destinationChoice.*;
import de.tum.bgu.msm.longDistance.modeChoice.*;
import de.tum.bgu.msm.longDistance.tripGeneration.DomesticTripGenerationGermany;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import javax.xml.crypto.KeySelector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by carlloga on 12-07-17.
 */
public class CalibrationGermany implements ModelComponent {

    private boolean calibrationTG;
    private boolean calibrationDaytripDC;
    private boolean calibrationOvernightFirstLayerDC;
    private boolean calibrationOvernightDomesticDC;
    private boolean calibrationOvernightEuropeDC;
    private boolean calibrationOvernightOverseasDC;
    private boolean calibrationMC;
    private boolean holiday;

    private JSONObject prop;
    String inputFolder;
    String outputFolder;

    private int maxIter;

    //private TripGenerationGermany tgM;
    private DomesticTripGenerationGermany tgDomesticModel;

    //private DestinationChoiceGermany dcM;
    private DaytripDestinationChoiceGermany dcDaytripModel;
    private OvernightFirstLayerDestinationChoiceGermany dcOvernightFirstLayerModel;
    private OvernightDomesticDestinationChoiceGermany dcOvernightDomesticModel;
    private OvernightEuropeDestinationChoiceGermany dcOvernightEuropeModel;
    private OvernightOverseasDestinationChoiceGermany dcOvernightOverseasModel;

    //private ModeChoiceGermany mcM;
    private DomesticModeChoiceGermanyScenario mcDomesticModel;

    static Logger logger = Logger.getLogger(CalibrationGermany.class);

    private ArrayList<LongDistanceTripGermany> allTrips = new ArrayList<>();
    private Map<Integer, Zone> zonesMap;
    private Matrix distanceByAuto;
    private Matrix travelTimeByAuto;

    public CalibrationGermany() {
    }

    enum TgModelName {
        residenceTg
    }

    enum DcModelName {
        daytripDc, overnightDomesticDc, overnightEuropeDc, overnightOverseasDc
    }

    enum OvernightFirstLayerDcModelName {
        overnightFirstLayerDc
    }

    enum McModelName {
        domesticMc
    }

    @Override
    public void setup(JSONObject prop, String inputFolder, String outputFolder) {

        this.prop = prop;
        this.inputFolder =  JsonUtilMto.getStringProp(prop, "work_folder");
        this.outputFolder = inputFolder + "output/" +  JsonUtilMto.getStringProp(prop, "scenario") + "/";
        calibrationTG = JsonUtilMto.getBooleanProp(prop, "trip_generation.calibration");
        calibrationDaytripDC = JsonUtilMto.getBooleanProp(prop, "destination_choice.calibration_daytrip");
        calibrationOvernightFirstLayerDC = JsonUtilMto.getBooleanProp(prop, "destination_choice.calibration_overnight_firstlayer");
        calibrationOvernightDomesticDC = JsonUtilMto.getBooleanProp(prop, "destination_choice.calibration_overnight_domestic");
        calibrationOvernightEuropeDC = JsonUtilMto.getBooleanProp(prop, "destination_choice.calibration_overnight_europe");
        calibrationOvernightOverseasDC = JsonUtilMto.getBooleanProp(prop, "destination_choice.calibration_overnight_overseas");
        calibrationMC = JsonUtilMto.getBooleanProp(prop, "mode_choice.calibration");
        holiday = JsonUtilMto.getBooleanProp(prop, "holiday");
        maxIter = 2000;

    }

    @Override
    public void load(DataSet dataSet) {
        zonesMap = dataSet.getZones();
        distanceByAuto = dataSet.getDistanceMatrix().get(ModeGermany.AUTO);
        travelTimeByAuto = dataSet.getTravelTimeMatrix().get(ModeGermany.AUTO);
    }

    @Override
    public void run(DataSet dataSet, int nThreads) {

        for (LongDistanceTrip t : dataSet.getTripsofPotentialTravellers()) {
            LongDistanceTripGermany trip = (LongDistanceTripGermany) t;
            allTrips.add(trip);
        }

        if(calibrationTG){
            calibrateTgModel(calibrationTG, dataSet);
        }

        if(calibrationOvernightFirstLayerDC){
            calibrateOvernightFirstLayerDcModel(calibrationOvernightFirstLayerDC, dataSet);
        }

        if(calibrationDaytripDC||calibrationOvernightDomesticDC||calibrationOvernightEuropeDC||calibrationOvernightOverseasDC){
            calibrateDcModelByDistance(calibrationDaytripDC,calibrationOvernightDomesticDC,calibrationOvernightEuropeDC,calibrationOvernightOverseasDC, dataSet);
        }

        if(calibrationMC){
            calibrateMcModel(calibrationMC, dataSet);
        }

    }

    public void calibrateTgModel(boolean tg, DataSet dataSet) {
        Map<TgModelName, Map<Purpose, Map<Type, Double>>> calibrationMatrixTg;

        tgDomesticModel = new DomesticTripGenerationGermany(prop, inputFolder, outputFolder);
        tgDomesticModel.load(dataSet);

        for (int iteration = 0; iteration < maxIter; iteration++) {
            if (tg){
                logger.info("Calibration of trip generation: Iteration = " + iteration);
                int totalPopulation = dataSet.getPotentialTravellers().size();
                calibrationMatrixTg = calculateTGCalibrationFactors(allTrips, totalPopulation);
                tgDomesticModel.updateDomesticTgCalibration(calibrationMatrixTg.get(TgModelName.residenceTg));
                runTg();
            }
        }
        printOutCalibrationResults(tgDomesticModel);
    }

    public Map<TgModelName, Map<Purpose, Map<Type, Double>>> calculateTGCalibrationFactors(ArrayList<LongDistanceTripGermany> allTrips, int totalPopulation) {

        Map<TgModelName, Map<Purpose, Map<Type, Double>>> calibrationMatrix = new HashMap<>();
        Map<TgModelName, Map<Purpose, Map<Type, Double>>> simulatedTripStateShares = getAverageTripStateShares(allTrips, totalPopulation);
        Map<TgModelName, Map<Purpose, Map<Type, Double>>> surveyShares = new HashMap<>();

        double stepFactor = 10;

        //hard coded for calibration
        TgModelName domesticTg = TgModelName.residenceTg;
        surveyShares.putIfAbsent(domesticTg, new HashMap<>());
        surveyShares.get(domesticTg).putIfAbsent(PurposeGermany.PRIVATE, new HashMap<>());
        surveyShares.get(domesticTg).putIfAbsent(PurposeGermany.BUSINESS, new HashMap<>());
        surveyShares.get(domesticTg).putIfAbsent(PurposeGermany.LEISURE, new HashMap<>());

        if (!holiday){
            surveyShares.get(domesticTg).get(PurposeGermany.BUSINESS).putIfAbsent(TypeGermany.DAYTRIP, 0.0165);
            surveyShares.get(domesticTg).get(PurposeGermany.BUSINESS).putIfAbsent(TypeGermany.OVERNIGHT, 0.0035);
            surveyShares.get(domesticTg).get(PurposeGermany.BUSINESS).putIfAbsent(TypeGermany.AWAY, 0.0004);

            surveyShares.get(domesticTg).get(PurposeGermany.LEISURE).putIfAbsent(TypeGermany.DAYTRIP, 0.0089);
            surveyShares.get(domesticTg).get(PurposeGermany.LEISURE).putIfAbsent(TypeGermany.OVERNIGHT, 0.0088);
            surveyShares.get(domesticTg).get(PurposeGermany.LEISURE).putIfAbsent(TypeGermany.AWAY, 0.0011);

            surveyShares.get(domesticTg).get(PurposeGermany.PRIVATE).putIfAbsent(TypeGermany.DAYTRIP, 0.0111);
            surveyShares.get(domesticTg).get(PurposeGermany.PRIVATE).putIfAbsent(TypeGermany.OVERNIGHT, 0.0070);
            surveyShares.get(domesticTg).get(PurposeGermany.PRIVATE).putIfAbsent(TypeGermany.AWAY, 0.0004);
        }else{
            surveyShares.get(domesticTg).get(PurposeGermany.BUSINESS).putIfAbsent(TypeGermany.DAYTRIP, 0.0058);
            surveyShares.get(domesticTg).get(PurposeGermany.BUSINESS).putIfAbsent(TypeGermany.OVERNIGHT, 0.0022);
            surveyShares.get(domesticTg).get(PurposeGermany.BUSINESS).putIfAbsent(TypeGermany.AWAY, 0.0000);

            surveyShares.get(domesticTg).get(PurposeGermany.LEISURE).putIfAbsent(TypeGermany.DAYTRIP, 0.0407);
            surveyShares.get(domesticTg).get(PurposeGermany.LEISURE).putIfAbsent(TypeGermany.OVERNIGHT, 0.0223);
            surveyShares.get(domesticTg).get(PurposeGermany.LEISURE).putIfAbsent(TypeGermany.AWAY, 0.0056);

            surveyShares.get(domesticTg).get(PurposeGermany.PRIVATE).putIfAbsent(TypeGermany.DAYTRIP, 0.0116);
            surveyShares.get(domesticTg).get(PurposeGermany.PRIVATE).putIfAbsent(TypeGermany.OVERNIGHT, 0.0214);
            surveyShares.get(domesticTg).get(PurposeGermany.PRIVATE).putIfAbsent(TypeGermany.AWAY, 0.0012);
        }

        logger.info("Trip generation calibration ");
        //logger.info("model" + "\t" + "purpose" + " \t" + "tripState" + "\t" + "factor");

        for (TgModelName name : TgModelName.values()) {
            calibrationMatrix.put(name, new HashMap<>());
            for (Purpose purpose : PurposeGermany.values()) {
                calibrationMatrix.get(name).putIfAbsent(purpose, new HashMap<>());
                for (Type tripState : TypeGermany.values()) {
                    double observedShare = surveyShares.get(name).get(purpose).get(tripState);
                    double simulatedShare = simulatedTripStateShares.get(name).get(purpose).get(tripState);
                    double factor = stepFactor * (observedShare - simulatedShare); //obtain a negative value if simulation larger than observation
                    calibrationMatrix.get(name).get(purpose).putIfAbsent(tripState, factor);
                    logger.info(name + "\t" + purpose + " \t" + tripState + "\t" + "difference:" + "\t" + (simulatedShare-observedShare));
                }
            }
        }
        return calibrationMatrix;
    }

    public Map<TgModelName, Map<Purpose, Map<Type, Double>>> getAverageTripStateShares(ArrayList<LongDistanceTripGermany> allTrips, int totalPopulation) {

        Map<TgModelName, Map<Purpose, Map<Type, Double>>> countsByTripState = new HashMap<>();

        //Initialize countsByTripState map
        for (TgModelName name : TgModelName.values()) {
            countsByTripState.putIfAbsent(name, new HashMap<>());
            for (Purpose purpose : PurposeGermany.values()) {
                countsByTripState.get(name).put(purpose, new HashMap<>());
                for (Type tripState : TypeGermany.values()) {
                    countsByTripState.get(name).get(purpose).put(tripState, 0.);
                }
            }
        }

        //Count trip state share;
        for (LongDistanceTrip tripToCast : allTrips) {
            LongDistanceTripGermany t = (LongDistanceTripGermany) tripToCast;
            TgModelName name = TgModelName.residenceTg;
            addTripToTripStateShareCalculator(countsByTripState, t, name);
        }

        //logger.info("Simulated Trip State Shares");

        Map<TgModelName, Map<Purpose, Map<Type, Double>>> tripStateShares = new HashMap<>();
        //logger.info("model" + "\t" + "purpose" + " \t" + "tripState" + "\t" + "share");
        for (TgModelName name : TgModelName.values()) {
            tripStateShares.putIfAbsent(name, new HashMap<>());
            for (Purpose purpose : PurposeGermany.values()) {
                tripStateShares.get(name).put(purpose, new HashMap<>());
                for (Type tripState : TypeGermany.values()) {
                    double tripStateShare = countsByTripState.get(name).get(purpose).get(tripState) / totalPopulation;
                    tripStateShares.get(name).get(purpose).put(tripState, tripStateShare);
                    //logger.info(name + "\t" + purpose + " \t" + tripState + "\t" + tripStateShare);
                }
            }
        }
        return tripStateShares;
    }

    private void addTripToTripStateShareCalculator(Map<TgModelName, Map<Purpose, Map<Type, Double>>> countsByTripState, LongDistanceTripGermany t, TgModelName name) {
        double currentCount = countsByTripState.get(name).get(t.getTripPurpose()).get(t.getTripState());
        countsByTripState.get(name).get(t.getTripPurpose()).put(t.getTripState(), currentCount + 1);
    }

    private void  runTg() {
        logger.info("Running Trip Generation Model for " + allTrips.size() + " trips during model calibration");
        this.allTrips = tgDomesticModel.runCalibration();
    }

    public void calibrateOvernightFirstLayerDcModel(boolean dcOvFL, DataSet dataSet) {

        dcOvernightFirstLayerModel = new OvernightFirstLayerDestinationChoiceGermany(prop, inputFolder);
        dcOvernightFirstLayerModel.load(dataSet);
        Map<Purpose, Map<ZoneType, Double>> calibrationMatrixDcByZoneType;

        for (int iteration = 0; iteration < maxIter; iteration++) {

            Map<Purpose, Map<ZoneType, Double>> overnightDistribution = getDestinationDistribution(allTrips);

            if(dcOvFL){
                logger.info("Calibration of daytrip destination choice: Iteration = " + iteration);
                calibrationMatrixDcByZoneType = calculateDcCalibrationFactorsByZoneType(allTrips, overnightDistribution);
                dcOvernightFirstLayerModel.updateOvernightFirstLayerDcCalibration(calibrationMatrixDcByZoneType);
            }

            runDc();
        }
        printOutCalibrationResults(dcOvernightFirstLayerModel);
    }

    public Map<Purpose, Map<ZoneType, Double>> getDestinationDistribution(ArrayList<LongDistanceTripGermany> allTrips){

        Map<Purpose, Map<ZoneType, Double>> destinationDistribution = new HashMap<>();
        Map<Purpose, Double> overnightCount = new HashMap<>();

        //initialize map
        for (Purpose purpose : PurposeGermany.values()){
            destinationDistribution.putIfAbsent(purpose, new HashMap<>());
            for (ZoneType zoneType : ZoneTypeGermany.values()){
                destinationDistribution.get(purpose).putIfAbsent(zoneType, .0);
            }
        }

        for (Purpose purpose : PurposeGermany.values()){
            overnightCount.putIfAbsent(purpose, 0.0);
        }

        for (LongDistanceTrip tripToCast : allTrips) {
            LongDistanceTripGermany t = (LongDistanceTripGermany) tripToCast;
            if (t.getTripState().equals(TypeGermany.OVERNIGHT)){
                double previousCount = destinationDistribution.get(t.getTripPurpose()).get(t.getDestZoneType());
                destinationDistribution.get(t.getTripPurpose()).put(t.getDestZoneType(), previousCount+1);
                double previousCountByPurpose = overnightCount.get(t.getTripPurpose());
                overnightCount.put(t.getTripPurpose(), previousCountByPurpose+1);
            }
        }

        for (Purpose purpose : PurposeGermany.values()){
            for (ZoneType zoneType : ZoneTypeGermany.values()){
                destinationDistribution.get(purpose).putIfAbsent(zoneType, destinationDistribution.get(purpose).get(zoneType)/overnightCount.get(purpose));
            }
        }
        return destinationDistribution;
    }

    public Map<Purpose, Map<ZoneType, Double>> calculateDcCalibrationFactorsByZoneType(ArrayList<LongDistanceTripGermany> allTrips, Map<Purpose, Map<ZoneType, Double>> simulatedOvernightDistribution){

        Map<Purpose, Map<ZoneType, Double>> calibrationMatrix = new HashMap<>();
        Map<Purpose, Map<ZoneType, Double>> observedOvernightDistribution = new HashMap<>();

        double stepFactor = 10;

        //hard coded for calibration
        for (Purpose purpose : PurposeGermany.values()){
            calibrationMatrix.putIfAbsent(purpose, new HashMap<>());
            observedOvernightDistribution.putIfAbsent(purpose, new HashMap<>());
            for (ZoneType zoneType : ZoneTypeGermany.values()){
                calibrationMatrix.get(purpose).putIfAbsent(zoneType, .0);
                observedOvernightDistribution.get(purpose).putIfAbsent(zoneType, .0);
            }
        }

        observedOvernightDistribution.get(PurposeGermany.LEISURE).put(ZoneTypeGermany.GERMANY, .0);
        observedOvernightDistribution.get(PurposeGermany.LEISURE).put(ZoneTypeGermany.EXTEU, .0);
        observedOvernightDistribution.get(PurposeGermany.LEISURE).put(ZoneTypeGermany.EXTOVERSEAS, .0);

        observedOvernightDistribution.get(PurposeGermany.BUSINESS).put(ZoneTypeGermany.GERMANY, .0);
        observedOvernightDistribution.get(PurposeGermany.BUSINESS).put(ZoneTypeGermany.EXTEU, .0);
        observedOvernightDistribution.get(PurposeGermany.BUSINESS).put(ZoneTypeGermany.EXTOVERSEAS, .0);

        observedOvernightDistribution.get(PurposeGermany.PRIVATE).put(ZoneTypeGermany.GERMANY, .0);
        observedOvernightDistribution.get(PurposeGermany.PRIVATE).put(ZoneTypeGermany.EXTEU, .0);
        observedOvernightDistribution.get(PurposeGermany.PRIVATE).put(ZoneTypeGermany.EXTOVERSEAS, .0);

        logger.info("Overnight First Layer Destination Choice Calibration ");
        //logger.info("model" + "\t" + "purpose" + " \t" + "tripState" + "\t" + "factor");

        for (Purpose purpose : PurposeGermany.values()){
            for (ZoneType zoneType : ZoneTypeGermany.values()){
                double observedShare = observedOvernightDistribution.get(purpose).get(zoneType);
                double simulatedShare = simulatedOvernightDistribution.get(purpose).get(zoneType);
                double factor = stepFactor * (observedShare - simulatedShare); //obtain a negative value if simulation larger than observation
                calibrationMatrix.get(purpose).put(zoneType, factor);
                logger.info(purpose + " \t" + zoneType + "\t" + "differences:" + "\t" + (simulatedShare-observedShare));
            }
        }
        return calibrationMatrix;
    }

    public void calibrateDcModelByDistance(boolean dcDay, boolean dcOvDomestic, boolean dcOvEurope, boolean dcOvOverseas, DataSet dataSet) {

        dcDaytripModel = new DaytripDestinationChoiceGermany(prop, inputFolder);
        dcDaytripModel.load(dataSet);

        dcOvernightFirstLayerModel = new OvernightFirstLayerDestinationChoiceGermany(prop, inputFolder);
        dcOvernightFirstLayerModel.load(dataSet);

        dcOvernightDomesticModel = new OvernightDomesticDestinationChoiceGermany(prop, inputFolder);
        dcOvernightDomesticModel.load(dataSet);

        dcOvernightEuropeModel = new OvernightEuropeDestinationChoiceGermany(prop, inputFolder);
        dcOvernightEuropeModel.load(dataSet);

        dcOvernightOverseasModel = new OvernightOverseasDestinationChoiceGermany(prop, inputFolder);
        dcOvernightOverseasModel.load(dataSet);

        Map<Type, Map<ZoneType, Map<Purpose, Double>>> calibrationMatrixDcByDistance;

        for (int iteration = 0; iteration < maxIter; iteration++) {

            Map<Type, Map<ZoneType, Map<Purpose, Double>>> averageDistances = getAverageTripDistances(allTrips);

            if (dcDay||dcOvDomestic||dcOvEurope||dcOvOverseas){
                logger.info("Calibration of daytrip destination choice: Iteration = " + iteration);
                calibrationMatrixDcByDistance = calculateDcCalibrationFactorsByDistance(allTrips, averageDistances);
                dcDaytripModel.updateDaytripDcCalibration(calibrationMatrixDcByDistance);
                dcOvernightDomesticModel.updateOvernightDomesticDcCalibration(calibrationMatrixDcByDistance);
                dcOvernightEuropeModel.updateOvernightEuropeDcCalibration(calibrationMatrixDcByDistance);
                dcOvernightOverseasModel.updateOvernightOverseasDcCalibration(calibrationMatrixDcByDistance);
            }
            runDc();
        }
        printOutCalibrationResults(dcDaytripModel, dcOvernightDomesticModel, dcOvernightEuropeModel, dcOvernightOverseasModel);
    }

    public Map<Type, Map<ZoneType, Map<Purpose, Double>>> getAverageTripDistances(ArrayList<LongDistanceTripGermany> allTrips) {

        Map<Type, Map<ZoneType, Map<Purpose, Double>>> averageDistances = new HashMap<>();
        Map<Type, Map<ZoneType, Map<Purpose, Double>>> counts = new HashMap<>();

        for (Type tripState : TypeGermany.values()){
            averageDistances.putIfAbsent(tripState, new HashMap<>());
            counts.putIfAbsent(tripState, new HashMap<>());
            if (tripState.equals(TypeGermany.DAYTRIP)||tripState.equals(TypeGermany.AWAY)){
                averageDistances.get(tripState).putIfAbsent(ZoneTypeGermany.GERMANY, new HashMap<>());
                counts.get(tripState).putIfAbsent(ZoneTypeGermany.GERMANY, new HashMap<>());
                for (Purpose purpose : PurposeGermany.values()){
                    averageDistances.get(tripState).get(ZoneTypeGermany.GERMANY).putIfAbsent(purpose, .0);
                    counts.get(tripState).get(ZoneTypeGermany.GERMANY).putIfAbsent(purpose, .0);
                }
            }else{
                for (ZoneType zoneType : ZoneTypeGermany.values()){
                    averageDistances.get(tripState).putIfAbsent(zoneType, new HashMap<>());
                    counts.get(tripState).putIfAbsent(zoneType, new HashMap<>());
                    for (Purpose purpose : PurposeGermany.values()){
                        averageDistances.get(tripState).get(zoneType).putIfAbsent(purpose, .0);
                        counts.get(tripState).get(zoneType).putIfAbsent(purpose, .0);
                    }
                }
            }
        }

        for (LongDistanceTrip tripToCast : allTrips) {
            LongDistanceTripGermany t = (LongDistanceTripGermany) tripToCast;
            addTripToAverageCalculator(averageDistances, counts, t);
        }

        logger.info("Destination choice average distances");
        logger.info("tripState" + "\t" + "zoneType" + "\t" + "purpose");

        for (Type tripState : TypeGermany.values()){
            for (ZoneType zoneType : ZoneTypeGermany.values()){
                for (Purpose purpose : PurposeGermany.values()) {
                    double sum;
                    double count;

                    if (tripState.equals(TypeGermany.DAYTRIP)||tripState.equals(TypeGermany.AWAY)){
                        sum = averageDistances.get(tripState).get(ZoneTypeGermany.GERMANY).get(purpose);
                        count = counts.get(tripState).get(ZoneTypeGermany.GERMANY).get(purpose);
                        averageDistances.get(tripState).get(ZoneTypeGermany.GERMANY).put(purpose, sum / count);
                    }else{
                        sum = averageDistances.get(tripState).get(zoneType).get(purpose);
                        count = counts.get(tripState).get(zoneType).get(purpose);
                        averageDistances.get(tripState).get(zoneType).put(purpose, sum / count);
                    }
                }
            }
        }

        return averageDistances;
    }

    private void addTripToAverageCalculator(Map<Type, Map<ZoneType, Map<Purpose, Double>>> averageDistances, Map<Type, Map<ZoneType, Map<Purpose, Double>>> counts, LongDistanceTripGermany tripToCast) {
        LongDistanceTripGermany t = tripToCast;

        double previousDistance;
        double previousCount;

        if (t.getTripState().equals(TypeGermany.DAYTRIP)||t.getTripState().equals(TypeGermany.AWAY)){
            previousDistance = averageDistances.get(t.getTripState()).get(ZoneTypeGermany.GERMANY).get(t.getTripPurpose());
            previousCount = counts.get(t.getTripState()).get(ZoneTypeGermany.GERMANY).get(t.getTripPurpose());
            averageDistances.get(t.getTripState()).get(ZoneTypeGermany.GERMANY).put(t.getTripPurpose(), previousDistance + t.getAutoTravelDistance()/1000);
            counts.get(t.getTripState()).get(ZoneTypeGermany.GERMANY).put(t.getTripPurpose(), previousDistance + previousCount + 1);

        }else{
            previousDistance = averageDistances.get(t.getTripState()).get(t.getDestZoneType()).get(t.getTripPurpose());
            previousCount = counts.get(t.getTripState()).get(t.getDestZoneType()).get(t.getTripPurpose());
            averageDistances.get(t.getTripState()).get(t.getDestZoneType()).put(t.getTripPurpose(), previousDistance + t.getAutoTravelDistance()/1000);
            counts.get(t.getTripState()).get(t.getDestZoneType()).put(t.getTripPurpose(), previousDistance + previousCount + 1);
        }
    }

    public Map<Type, Map<ZoneType, Map<Purpose, Double>>> calculateDcCalibrationFactorsByDistance(ArrayList<LongDistanceTripGermany> allTrips, Map<Type, Map<ZoneType, Map<Purpose, Double>>> averageDistances) {

        Map<Type, Map<ZoneType, Map<Purpose, Double>>> calibrationMatrix = new HashMap<>();

        double stepFactor = 1;

        //hard coded for calibration
        for (Type tripState : TypeGermany.values()){
            calibrationMatrix.putIfAbsent(tripState, new HashMap<>());
            if (tripState.equals(TypeGermany.DAYTRIP)||tripState.equals(TypeGermany.AWAY)){
                calibrationMatrix.get(tripState).putIfAbsent(ZoneTypeGermany.GERMANY, new HashMap<>());
                for (Purpose purpose : PurposeGermany.values()){
                    calibrationMatrix.get(tripState).get(ZoneTypeGermany.GERMANY).putIfAbsent(purpose, .0);
                }
            }else{
                for (ZoneType zoneType : ZoneTypeGermany.values()){
                    calibrationMatrix.get(tripState).putIfAbsent(zoneType, new HashMap<>());
                    for (Purpose purpose : PurposeGermany.values()){
                        calibrationMatrix.get(tripState).get(zoneType).putIfAbsent(purpose, .0);
                    }
                }
            }
        }

        // Daytrip: domestic + international
        calibrationMatrix.get(TypeGermany.DAYTRIP).get(ZoneTypeGermany.GERMANY).put(PurposeGermany.PRIVATE, (averageDistances.get(TypeGermany.DAYTRIP).get(ZoneTypeGermany.GERMANY).get(PurposeGermany.PRIVATE) / 204.94 - 1) * stepFactor + 1);
        calibrationMatrix.get(TypeGermany.DAYTRIP).get(ZoneTypeGermany.GERMANY).put(PurposeGermany.BUSINESS, (averageDistances.get(TypeGermany.DAYTRIP).get(ZoneTypeGermany.GERMANY).get(PurposeGermany.BUSINESS) / 179.78 - 1) * stepFactor + 1);
        calibrationMatrix.get(TypeGermany.DAYTRIP).get(ZoneTypeGermany.GERMANY).put(PurposeGermany.LEISURE, (averageDistances.get(TypeGermany.DAYTRIP).get(ZoneTypeGermany.GERMANY).get(PurposeGermany.LEISURE) / 176.09 - 1) * stepFactor + 1);

        // Overnight: domestic
        calibrationMatrix.get(TypeGermany.OVERNIGHT).get(ZoneTypeGermany.GERMANY).put(PurposeGermany.PRIVATE, (averageDistances.get(TypeGermany.OVERNIGHT).get(ZoneTypeGermany.GERMANY).get(PurposeGermany.PRIVATE) / 226.69 - 1) * stepFactor + 1);
        calibrationMatrix.get(TypeGermany.OVERNIGHT).get(ZoneTypeGermany.GERMANY).put(PurposeGermany.BUSINESS, (averageDistances.get(TypeGermany.OVERNIGHT).get(ZoneTypeGermany.GERMANY).get(PurposeGermany.BUSINESS) / 258.21 - 1) * stepFactor + 1);
        calibrationMatrix.get(TypeGermany.OVERNIGHT).get(ZoneTypeGermany.GERMANY).put(PurposeGermany.LEISURE, (averageDistances.get(TypeGermany.OVERNIGHT).get(ZoneTypeGermany.GERMANY).get(PurposeGermany.LEISURE) / 229.18 - 1) * stepFactor + 1);

        //Overnight: europe
        calibrationMatrix.get(TypeGermany.OVERNIGHT).get(ZoneTypeGermany.EXTEU).put(PurposeGermany.PRIVATE, (averageDistances.get(TypeGermany.OVERNIGHT).get(ZoneTypeGermany.EXTEU).get(PurposeGermany.PRIVATE) /  - 1) * stepFactor + 1); //ToDo add average distance from B1 reise
        calibrationMatrix.get(TypeGermany.OVERNIGHT).get(ZoneTypeGermany.EXTEU).put(PurposeGermany.BUSINESS, (averageDistances.get(TypeGermany.OVERNIGHT).get(ZoneTypeGermany.EXTEU).get(PurposeGermany.BUSINESS) /  - 1) * stepFactor + 1); //ToDo add average distance from B1 reise
        calibrationMatrix.get(TypeGermany.OVERNIGHT).get(ZoneTypeGermany.EXTEU).put(PurposeGermany.LEISURE, (averageDistances.get(TypeGermany.OVERNIGHT).get(ZoneTypeGermany.EXTEU).get(PurposeGermany.LEISURE) /  - 1) * stepFactor + 1); //ToDo add average distance from B1 reiseb


        //Overnight: overseas
        calibrationMatrix.get(TypeGermany.OVERNIGHT).get(ZoneTypeGermany.EXTOVERSEAS).put(PurposeGermany.PRIVATE, (averageDistances.get(TypeGermany.OVERNIGHT).get(ZoneTypeGermany.EXTOVERSEAS).get(PurposeGermany.PRIVATE) /  - 1) * stepFactor + 1); //ToDo add average distance from B1 reise
        calibrationMatrix.get(TypeGermany.OVERNIGHT).get(ZoneTypeGermany.EXTOVERSEAS).put(PurposeGermany.BUSINESS, (averageDistances.get(TypeGermany.OVERNIGHT).get(ZoneTypeGermany.EXTOVERSEAS).get(PurposeGermany.BUSINESS) /  - 1) * stepFactor + 1); //ToDo add average distance from B1 reise
        calibrationMatrix.get(TypeGermany.OVERNIGHT).get(ZoneTypeGermany.EXTOVERSEAS).put(PurposeGermany.LEISURE, (averageDistances.get(TypeGermany.OVERNIGHT).get(ZoneTypeGermany.EXTOVERSEAS).get(PurposeGermany.LEISURE) /  - 1) * stepFactor + 1); //ToDo add average distance from B1 reise

        //Away: consider all
        calibrationMatrix.get(TypeGermany.AWAY).get(ZoneTypeGermany.GERMANY).put(PurposeGermany.PRIVATE, (averageDistances.get(TypeGermany.AWAY).get(ZoneTypeGermany.GERMANY).get(PurposeGermany.PRIVATE) / 204.94 - 1) * stepFactor + 1); //ToDo add average distance from B3 Wege
        calibrationMatrix.get(TypeGermany.AWAY).get(ZoneTypeGermany.GERMANY).put(PurposeGermany.BUSINESS, (averageDistances.get(TypeGermany.AWAY).get(ZoneTypeGermany.GERMANY).get(PurposeGermany.BUSINESS) / 179.78 - 1) * stepFactor + 1); //ToDo add average distance from B3 Wege
        calibrationMatrix.get(TypeGermany.AWAY).get(ZoneTypeGermany.GERMANY).put(PurposeGermany.LEISURE, (averageDistances.get(TypeGermany.AWAY).get(ZoneTypeGermany.GERMANY).get(PurposeGermany.LEISURE) / 176.09 - 1) * stepFactor + 1); //ToDo add average distance from B3 Wege

        logger.info("Calibration coefficients of destination choice models by distance");
        logger.info("tripType" + "\t" + "zoneType" + "\t" +"purpose" + "\t" + "coefficient");
        for (Type tripState : TypeGermany.values()) {
            if (tripState.equals(TypeGermany.DAYTRIP)||tripState.equals(TypeGermany.AWAY)){
                for (Purpose purpose : PurposeGermany.values()) {
                    logger.info(tripState.toString() + "\t" + ZoneTypeGermany.GERMANY + "\t" +purpose + "\t" + calibrationMatrix.get(tripState).get(ZoneTypeGermany.GERMANY).get(purpose));
                }
            }else{
                for (ZoneType zoneType : ZoneTypeGermany.values()){
                    for (Purpose purpose : PurposeGermany.values()) {
                        logger.info(tripState.toString() + "\t" + zoneType.toString() + "\t" +purpose + "\t" + calibrationMatrix.get(tripState).get(zoneType).get(purpose));
                    }
                }
            }
        }
        return calibrationMatrix;
    }

    public void runDc() {
        logger.info("Running Destination Choice Model for " + allTrips.size() + " trips during model calibration");

        allTrips.parallelStream().forEach(t -> {

            if(t.getTripState().equals(TypeGermany.DAYTRIP)){
                int destZoneId = dcDaytripModel.selectDestination(t);  // trips with an origin and a destination in Canada
                ((LongDistanceTripGermany)t).setDestZoneType((ZoneTypeGermany) zonesMap.get(destZoneId).getZoneType());
                ((LongDistanceTripGermany)t).setDestZone(zonesMap.get(destZoneId));
                float distance = distanceByAuto.getValueAt(((LongDistanceTripGermany) t).getOrigZone().getId(), destZoneId);
                float time = travelTimeByAuto.getValueAt(((LongDistanceTripGermany) t).getOrigZone().getId(), destZoneId);
                ((LongDistanceTripGermany)t).setAutoTravelDistance(distance);
                ((LongDistanceTripGermany)t).setAutoTravelTime(time);

                if (zonesMap.get(destZoneId).getZoneType().equals(ZoneTypeGermany.GERMANY)){
                    ((LongDistanceTripGermany)t).setInternational(false);
                }else{
                    ((LongDistanceTripGermany)t).setInternational(true);
                }

            }else if(t.getTripState().equals(TypeGermany.OVERNIGHT) || t.getTripState().equals(TypeGermany.AWAY)){

                ZoneTypeGermany zoneType = dcOvernightFirstLayerModel.selectFirstLayerDestination(t);

                if(zoneType.equals(ZoneTypeGermany.GERMANY)){

                    int destZoneId = dcOvernightDomesticModel.selectDestination(t);
                    ((LongDistanceTripGermany)t).setDestZoneType((ZoneTypeGermany) zonesMap.get(destZoneId).getZoneType());
                    ((LongDistanceTripGermany)t).setDestZone(zonesMap.get(destZoneId));
                    float distance = distanceByAuto.getValueAt(((LongDistanceTripGermany) t).getOrigZone().getId(), destZoneId);
                    float time = travelTimeByAuto.getValueAt(((LongDistanceTripGermany) t).getOrigZone().getId(), destZoneId);
                    ((LongDistanceTripGermany)t).setAutoTravelDistance(distance);
                    ((LongDistanceTripGermany)t).setAutoTravelTime(time);
                    ((LongDistanceTripGermany)t).setInternational(false);

                }else if(zoneType.equals(ZoneTypeGermany.EXTEU)){

                    int destZoneId = dcOvernightEuropeModel.selectDestination(t);
                    ((LongDistanceTripGermany)t).setDestZoneType((ZoneTypeGermany) zonesMap.get(destZoneId).getZoneType());
                    ((LongDistanceTripGermany)t).setDestZone(zonesMap.get(destZoneId));
                    float distance = distanceByAuto.getValueAt(((LongDistanceTripGermany) t).getOrigZone().getId(), destZoneId);
                    float time = travelTimeByAuto.getValueAt(((LongDistanceTripGermany) t).getOrigZone().getId(), destZoneId);
                    ((LongDistanceTripGermany)t).setAutoTravelDistance(distance);
                    ((LongDistanceTripGermany)t).setAutoTravelTime(time);
                    ((LongDistanceTripGermany)t).setInternational(true);

                }else{

                    int destZoneId = dcOvernightOverseasModel.selectDestination(t);
                    ((LongDistanceTripGermany)t).setDestZoneType((ZoneTypeGermany) zonesMap.get(destZoneId).getZoneType());
                    ((LongDistanceTripGermany)t).setDestZone(zonesMap.get(destZoneId));
                    float distance = distanceByAuto.getValueAt(((LongDistanceTripGermany) t).getOrigZone().getId(), destZoneId);
                    float time = travelTimeByAuto.getValueAt(((LongDistanceTripGermany) t).getOrigZone().getId(), destZoneId);
                    ((LongDistanceTripGermany)t).setAutoTravelDistance(distance);
                    ((LongDistanceTripGermany)t).setAutoTravelTime(time);
                    ((LongDistanceTripGermany)t).setInternational(true);

                }
            }else{
                //TODO. Add code for away trips; for now it is assume to be the same as overnight trips
            }

        });
    }

    public void calibrateMcModel(boolean mc, DataSet dataSet) {
        Map<McModelName, Map<Purpose, Map<Type, Map<Mode, Double>>>> calibrationMatrixMc;

        mcDomesticModel = new DomesticModeChoiceGermanyScenario(prop, inputFolder);
        mcDomesticModel.loadDomesticModeChoice(dataSet);

        for (int iteration = 0; iteration < maxIter; iteration++) {
            if (mc){
                logger.info("Calibration of mode choice: Iteration = " + iteration);
                calibrationMatrixMc = calculateMCCalibrationFactors(allTrips);
                Map<Purpose, Map<Type, Map<Mode, Double>>> updatedMatrix = calibrationMatrixMc.get(McModelName.domesticMc);
                mcDomesticModel.updateDomesticMcCalibration(updatedMatrix);
                runMc();
            }
        }
        printOutCalibrationResults(mcDomesticModel);
    }

    public Map<McModelName, Map<Purpose, Map<Type, Map<Mode, Double>>>> calculateMCCalibrationFactors(ArrayList<LongDistanceTripGermany> allTrips) {

        Map<McModelName, Map<Purpose, Map<Type, Map<Mode, Double>>>> calibrationMatrix = new HashMap<>();

        Map<McModelName, Map<Purpose, Map<Type, Map<Mode, Double>>>> simulatedModalShares = getAverageModalShares(allTrips);

        Map<McModelName, Map<Purpose, Map<Type, Map<Mode, Double>>>> surveyShares = new HashMap<>();

        double stepFactor = 1;

        //hard coded for calibration
        //domestic
        McModelName type = McModelName.domesticMc;
        surveyShares.putIfAbsent(type, new HashMap<>());

        surveyShares.get(type).putIfAbsent(PurposeGermany.BUSINESS, new HashMap<>());
        surveyShares.get(type).get(PurposeGermany.BUSINESS).putIfAbsent(TypeGermany.DAYTRIP, new HashMap<>());
        surveyShares.get(type).get(PurposeGermany.BUSINESS).get(TypeGermany.DAYTRIP).putIfAbsent(ModeGermany.AUTO, 0.8735);
        surveyShares.get(type).get(PurposeGermany.BUSINESS).get(TypeGermany.DAYTRIP).putIfAbsent(ModeGermany.AIR, 0.0151);
        surveyShares.get(type).get(PurposeGermany.BUSINESS).get(TypeGermany.DAYTRIP).putIfAbsent(ModeGermany.RAIL, 0.0994);
        surveyShares.get(type).get(PurposeGermany.BUSINESS).get(TypeGermany.DAYTRIP).putIfAbsent(ModeGermany.BUS, 0.0120);
        surveyShares.get(type).get(PurposeGermany.BUSINESS).putIfAbsent(TypeGermany.OVERNIGHT, new HashMap<>());
        surveyShares.get(type).get(PurposeGermany.BUSINESS).get(TypeGermany.OVERNIGHT).putIfAbsent(ModeGermany.AUTO, 0.6901);
        surveyShares.get(type).get(PurposeGermany.BUSINESS).get(TypeGermany.OVERNIGHT).putIfAbsent(ModeGermany.AIR, 0.1088);
        surveyShares.get(type).get(PurposeGermany.BUSINESS).get(TypeGermany.OVERNIGHT).putIfAbsent(ModeGermany.RAIL, 0.1738);
        surveyShares.get(type).get(PurposeGermany.BUSINESS).get(TypeGermany.OVERNIGHT).putIfAbsent(ModeGermany.BUS, 0.0273);
        surveyShares.get(type).get(PurposeGermany.BUSINESS).putIfAbsent(TypeGermany.AWAY, new HashMap<>());
        surveyShares.get(type).get(PurposeGermany.BUSINESS).get(TypeGermany.AWAY).putIfAbsent(ModeGermany.AUTO, 1.);
        surveyShares.get(type).get(PurposeGermany.BUSINESS).get(TypeGermany.AWAY).putIfAbsent(ModeGermany.AIR, 0.);
        surveyShares.get(type).get(PurposeGermany.BUSINESS).get(TypeGermany.AWAY).putIfAbsent(ModeGermany.RAIL, 0.);
        surveyShares.get(type).get(PurposeGermany.BUSINESS).get(TypeGermany.AWAY).putIfAbsent(ModeGermany.BUS, 0.);

        surveyShares.get(type).putIfAbsent(PurposeGermany.LEISURE, new HashMap<>());
        surveyShares.get(type).get(PurposeGermany.LEISURE).putIfAbsent(TypeGermany.DAYTRIP, new HashMap<>());
        surveyShares.get(type).get(PurposeGermany.LEISURE).get(TypeGermany.DAYTRIP).putIfAbsent(ModeGermany.AUTO, 0.8413);
        surveyShares.get(type).get(PurposeGermany.LEISURE).get(TypeGermany.DAYTRIP).putIfAbsent(ModeGermany.AIR, 0.0081);
        surveyShares.get(type).get(PurposeGermany.LEISURE).get(TypeGermany.DAYTRIP).putIfAbsent(ModeGermany.RAIL, 0.1022);
        surveyShares.get(type).get(PurposeGermany.LEISURE).get(TypeGermany.DAYTRIP).putIfAbsent(ModeGermany.BUS, 0.0484);
        surveyShares.get(type).get(PurposeGermany.LEISURE).putIfAbsent(TypeGermany.OVERNIGHT, new HashMap<>());
        surveyShares.get(type).get(PurposeGermany.LEISURE).get(TypeGermany.OVERNIGHT).putIfAbsent(ModeGermany.AUTO, 0.7475);
        surveyShares.get(type).get(PurposeGermany.LEISURE).get(TypeGermany.OVERNIGHT).putIfAbsent(ModeGermany.AIR, 0.0589);
        surveyShares.get(type).get(PurposeGermany.LEISURE).get(TypeGermany.OVERNIGHT).putIfAbsent(ModeGermany.RAIL, 0.1252);
        surveyShares.get(type).get(PurposeGermany.LEISURE).get(TypeGermany.OVERNIGHT).putIfAbsent(ModeGermany.BUS, 0.0684);
        surveyShares.get(type).get(PurposeGermany.LEISURE).putIfAbsent(TypeGermany.AWAY, new HashMap<>());
        surveyShares.get(type).get(PurposeGermany.LEISURE).get(TypeGermany.AWAY).putIfAbsent(ModeGermany.AUTO, 1.);
        surveyShares.get(type).get(PurposeGermany.LEISURE).get(TypeGermany.AWAY).putIfAbsent(ModeGermany.AIR, 0.);
        surveyShares.get(type).get(PurposeGermany.LEISURE).get(TypeGermany.AWAY).putIfAbsent(ModeGermany.RAIL, 0.);
        surveyShares.get(type).get(PurposeGermany.LEISURE).get(TypeGermany.AWAY).putIfAbsent(ModeGermany.BUS, 0.);

        surveyShares.get(type).putIfAbsent(PurposeGermany.PRIVATE, new HashMap<>());
        surveyShares.get(type).get(PurposeGermany.PRIVATE).putIfAbsent(TypeGermany.DAYTRIP, new HashMap<>());
        surveyShares.get(type).get(PurposeGermany.PRIVATE).get(TypeGermany.DAYTRIP).putIfAbsent(ModeGermany.AUTO, 0.9112);
        surveyShares.get(type).get(PurposeGermany.PRIVATE).get(TypeGermany.DAYTRIP).putIfAbsent(ModeGermany.AIR, 0.0008);
        surveyShares.get(type).get(PurposeGermany.PRIVATE).get(TypeGermany.DAYTRIP).putIfAbsent(ModeGermany.RAIL, 0.0781);
        surveyShares.get(type).get(PurposeGermany.PRIVATE).get(TypeGermany.DAYTRIP).putIfAbsent(ModeGermany.BUS, 0.0099);
        surveyShares.get(type).get(PurposeGermany.PRIVATE).putIfAbsent(TypeGermany.OVERNIGHT, new HashMap<>());
        surveyShares.get(type).get(PurposeGermany.PRIVATE).get(TypeGermany.OVERNIGHT).putIfAbsent(ModeGermany.AUTO, 0.8032);
        surveyShares.get(type).get(PurposeGermany.PRIVATE).get(TypeGermany.OVERNIGHT).putIfAbsent(ModeGermany.AIR, 0.0036);
        surveyShares.get(type).get(PurposeGermany.PRIVATE).get(TypeGermany.OVERNIGHT).putIfAbsent(ModeGermany.RAIL, 0.1498);
        surveyShares.get(type).get(PurposeGermany.PRIVATE).get(TypeGermany.OVERNIGHT).putIfAbsent(ModeGermany.BUS, 0.0434);
        surveyShares.get(type).get(PurposeGermany.PRIVATE).putIfAbsent(TypeGermany.AWAY, new HashMap<>());
        surveyShares.get(type).get(PurposeGermany.PRIVATE).get(TypeGermany.AWAY).putIfAbsent(ModeGermany.AUTO, 1.);
        surveyShares.get(type).get(PurposeGermany.PRIVATE).get(TypeGermany.AWAY).putIfAbsent(ModeGermany.AIR, 0.);
        surveyShares.get(type).get(PurposeGermany.PRIVATE).get(TypeGermany.AWAY).putIfAbsent(ModeGermany.RAIL, 0.);
        surveyShares.get(type).get(PurposeGermany.PRIVATE).get(TypeGermany.AWAY).putIfAbsent(ModeGermany.BUS, 0.);

        logger.info("Mode choice calibration factors");
        logger.info("model" + "\t" + "purpose" + " \t" + "mode" + "\t" + "diff(obs-sim)");

        for (McModelName name : McModelName.values()) {
            calibrationMatrix.put(name, new HashMap<>());
            for (Purpose purpose : PurposeGermany.values()) {
                calibrationMatrix.get(name).putIfAbsent(purpose, new HashMap<>());
                for (Type tripState : TypeGermany.values()){
                    calibrationMatrix.get(name).get(purpose).putIfAbsent(tripState, new HashMap<>());
                    for (Mode mode : ModeGermany.values()) {
                        double observedShare = surveyShares.get(name).get(purpose).get(tripState).get(mode);
                        double simulatedShare = simulatedModalShares.get(name).get(purpose).get(tripState).get(mode);
                        double factor;

                        if (mode.equals(ModeGermany.AUTO)){
                            factor = 0;
                        }else{
                            factor = stepFactor * (observedShare - simulatedShare);
                        }

                        calibrationMatrix.get(name).get(purpose).get(tripState).putIfAbsent(mode, factor);

                        //logger.info(name + "\t" + purpose + " \t" + tripState + " \t" + mode + "\t" + factor);
                        logger.info(name + "\t" + purpose + " \t" + tripState + " \t" + mode + "\t" + (observedShare - simulatedShare));
                    }
                }
            }
        }
        return calibrationMatrix;
    }

    public Map<McModelName, Map<Purpose, Map<Type, Map<Mode, Double>>>> getAverageModalShares(ArrayList<LongDistanceTripGermany> allTrips) {

        Map<McModelName, Map<Purpose, Map<Type, Map<Mode, Double>>>> countsByMode = new HashMap<>();

        for (McModelName name : McModelName.values()) {
            countsByMode.putIfAbsent(name, new HashMap<>());
            for (Purpose purpose : PurposeGermany.values()) {
                countsByMode.get(name).put(purpose, new HashMap<>());
                for (Type tripState : TypeGermany.values()){
                    countsByMode.get(name).get(purpose).put(tripState, new HashMap<>());
                    for (Mode mode : ModeGermany.values()) {
                        countsByMode.get(name).get(purpose).get(tripState).put(mode, 0.);
                    }
                }
            }
        }

        for (LongDistanceTrip tripToCast : allTrips) {
            LongDistanceTripGermany t = (LongDistanceTripGermany) tripToCast;
            if (!t.isInternational()) {
                if (t.getOrigZone().getZoneType().equals(ZoneTypeGermany.GERMANY)) {
                    McModelName name = McModelName.domesticMc;
                    addTripToModalShareCalculator(countsByMode, t, name);
                }
            }
        }

        //logger.info("Simulated Modal Shares");

        Map<McModelName, Map<Purpose, Map<Type, Map<Mode, Double>>>> modalShares = new HashMap<>();
        //logger.info("model" + "\t" + "purpose" + " \t" + "mode" + "\t" + "share");
        for (McModelName name : McModelName.values()) {
            modalShares.putIfAbsent(name, new HashMap<>());
            for (Purpose purpose : PurposeGermany.values()) {
                modalShares.get(name).put(purpose, new HashMap<>());
                for (Type tripState : TypeGermany.values()){
                    modalShares.get(name).get(purpose).put(tripState, new HashMap<>());

                    double total = 0;
                    for (Mode mode : ModeGermany.values()) {
                        total += countsByMode.get(name).get(purpose).get(tripState).get(mode);
                    }
                    for (Mode mode : ModeGermany.values()) {
                        double modalShare = countsByMode.get(name).get(purpose).get(tripState).get(mode) / total;
                        modalShares.get(name).get(purpose).get(tripState).put(mode, modalShare);
                        //logger.info(name + "\t" + purpose + " \t" + tripState+ " \t" + mode + "\t" + modalShare);
                    }
                }
            }
        }

        return modalShares;
    }

    private void addTripToModalShareCalculator(Map<McModelName, Map<Purpose, Map<Type, Map<Mode, Double>>>> countsByMode, LongDistanceTripGermany t, McModelName name) {
        double currentCount = countsByMode.get(name).get(t.getTripPurpose()).get(t.getTripState()).get(t.getMode());
        countsByMode.get(name).get(t.getTripPurpose()).get(t.getTripState()).put(t.getMode(), currentCount + 1);
    }

    private void runMc() {
        logger.info("Running Mode Choice Model for " + allTrips.size() + " trips during model calibration");
        allTrips.parallelStream().forEach(tripToCast -> {
            LongDistanceTripGermany t = (LongDistanceTripGermany) tripToCast;
            if (!t.isInternational()) {
                Mode mode = mcDomesticModel.selectModeDomestic(t);
                t.setMode(mode);
                t.setTravelTimeByMode(mcDomesticModel.getDomesticModalTravelTime(t));
            }
        });
    }

    public void printOutCalibrationResults(DomesticTripGenerationGermany domTg) {

        logger.info("Trip generation calibration");
        logger.info("model" + "\t" + "purpose" + "\t" + "tripState" + "\t" + "factor");
        Map<Purpose, Map<Type, Double>> mapTG;
        mapTG = domTg.getDomesticTgCalibrationMatrix();
        for (Purpose purpose : PurposeGermany.values()) {
            for (Type tripState : TypeGermany.values()) {
                logger.info(TgModelName.residenceTg + "\t" + purpose + "\t" + tripState + "\t" + "k-factor:" + "\t" + mapTG.get(purpose).get(tripState));
            }
        }
    }

    public void printOutCalibrationResults(OvernightFirstLayerDestinationChoiceGermany overnightFirstLayerDc) {

        logger.info("Destination choice calibration");
        logger.info("model" + "\t" + "purpose" + "\t" + "factor");

        Map<Purpose, Map<ZoneType, Double>> mapOvernightFirstLayerDC = overnightFirstLayerDc.getDomesticDcCalibration();

        for (Purpose purpose : PurposeGermany.values()) {
            for (ZoneType zoneType : ZoneTypeGermany.values()) {
                logger.info(purpose + "\t" + zoneType + "\t" + mapOvernightFirstLayerDC.get(purpose).get(zoneType));
            }
        }
    }

    public void printOutCalibrationResults(DaytripDestinationChoiceGermany daytripDc, OvernightDomesticDestinationChoiceGermany overnightDomesticDc, OvernightEuropeDestinationChoiceGermany overnightEuropeDc, OvernightOverseasDestinationChoiceGermany overnightOverseasDc) {

        logger.info("Destination choice calibration");
        logger.info("model" + "\t" + "purpose" + "\t" + "factor");

        Map<Type, Map<ZoneType, Map<Purpose, Double>>> mapDaytripDC = daytripDc.getDomesticDcCalibration();
        Map<Type, Map<ZoneType, Map<Purpose, Double>>> mapOvernightDomesticDC = overnightDomesticDc.getOvernightDomesticDcCalibration();
        Map<Type, Map<ZoneType, Map<Purpose, Double>>> mapOvernightEuropeDC = overnightEuropeDc.getOvernightEuropeDcCalibration();
        Map<Type, Map<ZoneType, Map<Purpose, Double>>> mapOvernightOverseasDC = overnightOverseasDc.getOvernightOverseasDcCalibration();

        for (Purpose purpose : PurposeGermany.values()) {
            logger.info(TypeGermany.DAYTRIP + "\t" + ZoneTypeGermany.GERMANY + "\t" + purpose + "\t" + mapDaytripDC.get(TypeGermany.DAYTRIP).get(ZoneTypeGermany.GERMANY).get(purpose));
            logger.info(TypeGermany.OVERNIGHT + "\t" + ZoneTypeGermany.GERMANY + "\t" + purpose + "\t" + mapOvernightDomesticDC.get(TypeGermany.DAYTRIP).get(ZoneTypeGermany.GERMANY).get(purpose));
            logger.info(TypeGermany.OVERNIGHT + "\t" + ZoneTypeGermany.EXTEU + "\t" + purpose + "\t" + mapOvernightEuropeDC.get(TypeGermany.DAYTRIP).get(ZoneTypeGermany.EXTEU).get(purpose));
            logger.info(TypeGermany.OVERNIGHT + "\t" + ZoneTypeGermany.EXTOVERSEAS + "\t" + purpose + "\t" + mapOvernightOverseasDC.get(TypeGermany.DAYTRIP).get(ZoneTypeGermany.EXTOVERSEAS).get(purpose));
        }
    }

    public void printOutCalibrationResults(DomesticModeChoiceGermanyScenario domMc) {

        logger.info("Mode choice calibration");
        logger.info("model" + "\t" + "purpose" + "\t" + "mode" + "\t" + "factor");
        Map<Purpose, Map<Type, Map<Mode, Double>>> mapMC;
        mapMC = domMc.getCalibrationMatrix();
        for (Purpose purpose : PurposeGermany.values()) {
            for (Type tripState : TypeGermany.values()){
                for (Mode mode : ModeGermany.values()) {
                    logger.info(McModelName.domesticMc + "\t" + purpose + "\t" + tripState + "\t" + mode + "\t" + mapMC.get(purpose).get(tripState).get(mode));
                }
            }
        }
    }
}
