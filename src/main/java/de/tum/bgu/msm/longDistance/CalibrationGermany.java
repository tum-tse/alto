package de.tum.bgu.msm.longDistance;

import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.trips.*;
import de.tum.bgu.msm.longDistance.data.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneTypeGermany;
import de.tum.bgu.msm.longDistance.destinationChoice.*;
import de.tum.bgu.msm.longDistance.modeChoice.*;
import de.tum.bgu.msm.longDistance.tripGeneration.DomesticTripGenerationGermany;
import de.tum.bgu.msm.longDistance.tripGeneration.TripGenerationGermany;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by carlloga on 12-07-17.
 */
public class CalibrationGermany implements ModelComponent {

    private boolean calibrationTG;
    private boolean calibrationDC;
    private boolean calibrationMC;
    private boolean holiday;

    private JSONObject prop;
    String inputFolder;
    String outputFolder;

    private int maxIter;

    private TripGenerationGermany tgM;
    private DomesticTripGenerationGermany tgDomesticModel;

    private DestinationChoiceGermany dcM;
    private DomesticDestinationChoiceGermany dcDomesticModel;

    private ModeChoiceGermany mcM;
    private DomesticModeChoiceGermany mcDomesticModel;

    static Logger logger = Logger.getLogger(CalibrationGermany.class);

    private ArrayList<LongDistanceTripGermany> allTrips = new ArrayList<>();
    private Map<Integer, Zone> zonesMap;
    private Matrix distanceByAuto;

    public CalibrationGermany() {
    }

    enum TgModelName {
        residenceTg
    }

    enum DcModelName {
        domesticDc, internationalDc
    }

    enum McModelName {
        domesticMc
    }//internationalMc

    @Override
    public void setup(JSONObject prop, String inputFolder, String outputFolder) {

        this.prop = prop;
        this.inputFolder =  JsonUtilMto.getStringProp(prop, "work_folder");
        this.outputFolder = inputFolder + "output/" +  JsonUtilMto.getStringProp(prop, "scenario") + "/";
        calibrationTG = JsonUtilMto.getBooleanProp(prop, "trip_generation.calibration");
        calibrationDC = JsonUtilMto.getBooleanProp(prop, "destination_choice.calibration");
        calibrationMC = JsonUtilMto.getBooleanProp(prop, "mode_choice.calibration");
        holiday = JsonUtilMto.getBooleanProp(prop, "holiday");
        maxIter = 100;

    }

    @Override
    public void load(DataSet dataSet) {
        zonesMap = dataSet.getZones();
        distanceByAuto = dataSet.getDistanceMatrix().get(ModeGermany.AUTO);
    }

    @Override
    public void run(DataSet dataSet, int nThreads) {

        if (calibrationTG||calibrationDC || calibrationMC) {
            calibrateModel(calibrationTG, calibrationDC, calibrationMC, dataSet);
        }
    }


    public void calibrateModel(boolean tg, boolean dc, boolean mc, DataSet dataSet) {

        for (LongDistanceTrip t : dataSet.getTripsofPotentialTravellers()) {
            LongDistanceTripGermany trip = (LongDistanceTripGermany) t;
                allTrips.add(trip);
        }

        Map<TgModelName, Map<Purpose, Map<Type, Double>>> calibrationMatrixTg;
        Map<DcModelName, Map<Purpose, Map<Type, Double>>> calibrationMatrixDc;
        Map<McModelName, Map<Purpose, Map<Type, Map<Mode, Double>>>> calibrationMatrixMc;

        tgDomesticModel = new DomesticTripGenerationGermany(prop, inputFolder, outputFolder);
        tgDomesticModel.load(dataSet);

        dcDomesticModel = new DomesticDestinationChoiceGermany(prop, inputFolder);
        dcDomesticModel.load(dataSet);

        mcDomesticModel = new DomesticModeChoiceGermany(prop, inputFolder);
        mcDomesticModel.loadDomesticModeChoice(dataSet);

        for (int iteration = 0; iteration < maxIter; iteration++) {

            if (tg){
                logger.info("Calibration of trip generation: Iteration = " + iteration);
                int totalPopulation = dataSet.getPotentialTravellers().size();
                calibrationMatrixTg = calculateTGCalibrationFactors(allTrips, totalPopulation);
                tgDomesticModel.updateDomesticTgCalibration(calibrationMatrixTg.get(TgModelName.residenceTg));
                runTg();
            }

            if (dc){
                logger.info("Calibration of destination choice: Iteration = " + iteration);
                calibrationMatrixDc = calculateDCCalibrationFactors(allTrips);
                dcDomesticModel.updateDomesticDcCalibration(calibrationMatrixDc.get(DcModelName.domesticDc));
                runDc();
            }

            if (mc){
                logger.info("Calibration of mode choice: Iteration = " + iteration);
                calibrationMatrixMc = calculateMCCalibrationFactors(allTrips);
                Map<Purpose, Map<Type, Map<Mode, Double>>> updatedMatrix = calibrationMatrixMc.get(McModelName.domesticMc);
                mcDomesticModel.updateDomesticMcCalibration(updatedMatrix);
                runMc();
            }
        }
        printOutCalibrationResults(tgDomesticModel, dcDomesticModel, mcDomesticModel);
    }

    public Map<TgModelName, Map<Purpose, Map<Type, Double>>> calculateTGCalibrationFactors(ArrayList<LongDistanceTripGermany> allTrips, int totalPopulation) {

        Map<TgModelName, Map<Purpose, Map<Type, Double>>> calibrationMatrix = new HashMap<>();
        Map<TgModelName, Map<Purpose, Map<Type, Double>>> simulatedTripStateShares = getAverageTripStateShares(allTrips, totalPopulation);
        Map<TgModelName, Map<Purpose, Map<Type, Double>>> surveyShares = new HashMap<>();

        double stepFactor = 1;

        //hard coded for calibration
        TgModelName type = TgModelName.residenceTg;
        surveyShares.putIfAbsent(type, new HashMap<>());
        surveyShares.get(type).putIfAbsent(PurposeGermany.PRIVATE, new HashMap<>());
        surveyShares.get(type).putIfAbsent(PurposeGermany.BUSINESS, new HashMap<>());
        surveyShares.get(type).putIfAbsent(PurposeGermany.LEISURE, new HashMap<>());

        if (!holiday){
            surveyShares.get(type).get(PurposeGermany.BUSINESS).putIfAbsent(TypeGermany.DAYTRIP, 0.0165);
            surveyShares.get(type).get(PurposeGermany.BUSINESS).putIfAbsent(TypeGermany.OVERNIGHT, 0.0035);
            surveyShares.get(type).get(PurposeGermany.BUSINESS).putIfAbsent(TypeGermany.AWAY, 0.0004);

            surveyShares.get(type).get(PurposeGermany.LEISURE).putIfAbsent(TypeGermany.DAYTRIP, 0.0089);
            surveyShares.get(type).get(PurposeGermany.LEISURE).putIfAbsent(TypeGermany.OVERNIGHT, 0.0088);
            surveyShares.get(type).get(PurposeGermany.LEISURE).putIfAbsent(TypeGermany.AWAY, 0.0011);

            surveyShares.get(type).get(PurposeGermany.PRIVATE).putIfAbsent(TypeGermany.DAYTRIP, 0.0111);
            surveyShares.get(type).get(PurposeGermany.PRIVATE).putIfAbsent(TypeGermany.OVERNIGHT, 0.0070);
            surveyShares.get(type).get(PurposeGermany.PRIVATE).putIfAbsent(TypeGermany.AWAY, 0.0004);
        }else{
            surveyShares.get(type).get(PurposeGermany.BUSINESS).putIfAbsent(TypeGermany.DAYTRIP, 0.0058);
            surveyShares.get(type).get(PurposeGermany.BUSINESS).putIfAbsent(TypeGermany.OVERNIGHT, 0.0022);
            surveyShares.get(type).get(PurposeGermany.BUSINESS).putIfAbsent(TypeGermany.AWAY, 0.0000);

            surveyShares.get(type).get(PurposeGermany.LEISURE).putIfAbsent(TypeGermany.DAYTRIP, 0.0407);
            surveyShares.get(type).get(PurposeGermany.LEISURE).putIfAbsent(TypeGermany.OVERNIGHT, 0.0223);
            surveyShares.get(type).get(PurposeGermany.LEISURE).putIfAbsent(TypeGermany.AWAY, 0.0056);

            surveyShares.get(type).get(PurposeGermany.PRIVATE).putIfAbsent(TypeGermany.DAYTRIP, 0.0116);
            surveyShares.get(type).get(PurposeGermany.PRIVATE).putIfAbsent(TypeGermany.OVERNIGHT, 0.0214);
            surveyShares.get(type).get(PurposeGermany.PRIVATE).putIfAbsent(TypeGermany.AWAY, 0.0012);
        }

        logger.info("Trip generation calibration factors");
        logger.info("model" + "\t" + "purpose" + " \t" + "tripState" + "\t" + "factor");

        for (TgModelName name : TgModelName.values()) {
            calibrationMatrix.put(name, new HashMap<>());
            for (Purpose purpose : PurposeGermany.values()) {
                calibrationMatrix.get(name).putIfAbsent(purpose, new HashMap<>());
                for (Type tripState : TypeGermany.values()) {
                    double observedShare = surveyShares.get(name).get(purpose).get(tripState);
                    double simulatedShare = simulatedTripStateShares.get(name).get(purpose).get(tripState);
                    double factor = stepFactor * (observedShare - simulatedShare); //obtain a negative value if simulation larger than observation
                    calibrationMatrix.get(name).get(purpose).putIfAbsent(tripState, factor);
                    logger.info(name + "\t" + purpose + " \t" + tripState + "\t" + factor);
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

        logger.info("Simulated Trip State Shares");

        Map<TgModelName, Map<Purpose, Map<Type, Double>>> tripStateShares = new HashMap<>();
        logger.info("model" + "\t" + "purpose" + " \t" + "tripState" + "\t" + "share");
        for (TgModelName name : TgModelName.values()) {
            tripStateShares.putIfAbsent(name, new HashMap<>());
            for (Purpose purpose : PurposeGermany.values()) {
                tripStateShares.get(name).put(purpose, new HashMap<>());
                for (Type tripState : TypeGermany.values()) {
                    double tripStateShare = countsByTripState.get(name).get(purpose).get(tripState) / totalPopulation;
                    tripStateShares.get(name).get(purpose).put(tripState, tripStateShare);
                    logger.info(name + "\t" + purpose + " \t" + tripState + "\t" + tripStateShare);
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

    public Map<DcModelName, Map<Purpose, Map<Type, Double>>> calculateDCCalibrationFactors(ArrayList<LongDistanceTripGermany> allTrips) {

        Map<DcModelName, Map<Purpose, Map<Type, Double>>> averageDistances = getAverageTripDistances(allTrips);
        Map<DcModelName, Map<Purpose, Map<Type, Double>>> calibrationMatrix = new HashMap<>();

        double stepFactor = 1;

        //hard coded for calibration
        for (DcModelName name : DcModelName.values()) {
            calibrationMatrix.putIfAbsent(name, new HashMap<>());
            for (Purpose purpose : PurposeGermany.values()) {
                calibrationMatrix.get(name).put(purpose, new HashMap<>());
                for (Type tripState : TypeGermany.values()) {
                    calibrationMatrix.get(name).get(purpose).put(tripState, 0.);
                }
            }
        }

        calibrationMatrix.get(DcModelName.domesticDc).get(PurposeGermany.PRIVATE).put(TypeGermany.AWAY, (averageDistances.get(DcModelName.domesticDc).get(PurposeGermany.PRIVATE).get(TypeGermany.AWAY) / 204.94 - 1) * stepFactor + 1); //domestic visit
        calibrationMatrix.get(DcModelName.domesticDc).get(PurposeGermany.PRIVATE).put(TypeGermany.DAYTRIP, (averageDistances.get(DcModelName.domesticDc).get(PurposeGermany.PRIVATE).get(TypeGermany.DAYTRIP) / 204.94 - 1) * stepFactor + 1); //domestic visit
        calibrationMatrix.get(DcModelName.domesticDc).get(PurposeGermany.PRIVATE).put(TypeGermany.OVERNIGHT, (averageDistances.get(DcModelName.domesticDc).get(PurposeGermany.PRIVATE).get(TypeGermany.OVERNIGHT) / 226.69 - 1) * stepFactor + 1); //domestic visit

        calibrationMatrix.get(DcModelName.domesticDc).get(PurposeGermany.BUSINESS).put(TypeGermany.AWAY, (averageDistances.get(DcModelName.domesticDc).get(PurposeGermany.BUSINESS).get(TypeGermany.AWAY) / 179.78 - 1) * stepFactor + 1); //domestic visit
        calibrationMatrix.get(DcModelName.domesticDc).get(PurposeGermany.BUSINESS).put(TypeGermany.DAYTRIP, (averageDistances.get(DcModelName.domesticDc).get(PurposeGermany.BUSINESS).get(TypeGermany.DAYTRIP) / 179.78 - 1) * stepFactor + 1); //domestic visit
        calibrationMatrix.get(DcModelName.domesticDc).get(PurposeGermany.BUSINESS).put(TypeGermany.OVERNIGHT, (averageDistances.get(DcModelName.domesticDc).get(PurposeGermany.BUSINESS).get(TypeGermany.OVERNIGHT) / 258.21 - 1) * stepFactor + 1); //domestic visit

        calibrationMatrix.get(DcModelName.domesticDc).get(PurposeGermany.LEISURE).put(TypeGermany.AWAY, (averageDistances.get(DcModelName.domesticDc).get(PurposeGermany.LEISURE).get(TypeGermany.AWAY) / 176.09 - 1) * stepFactor + 1); //domestic visit
        calibrationMatrix.get(DcModelName.domesticDc).get(PurposeGermany.LEISURE).put(TypeGermany.DAYTRIP, (averageDistances.get(DcModelName.domesticDc).get(PurposeGermany.LEISURE).get(TypeGermany.DAYTRIP) / 176.09 - 1) * stepFactor + 1); //domestic visit
        calibrationMatrix.get(DcModelName.domesticDc).get(PurposeGermany.LEISURE).put(TypeGermany.OVERNIGHT, (averageDistances.get(DcModelName.domesticDc).get(PurposeGermany.LEISURE).get(TypeGermany.OVERNIGHT) / 229.18 - 1) * stepFactor + 1); //domestic visit

        logger.info("Destination choice average calibration coefficients");
        logger.info("model" + "\t" + "purpose" + "\t" + "coefficient");
        for (DcModelName name : DcModelName.values()) {
            for (Purpose purpose : PurposeGermany.values()) {
                for (Type tripState : TypeGermany.values()){
                    logger.info(name.toString() + "\t" + purpose + "\t" + tripState + "\t" + calibrationMatrix.get(name).get(purpose).get(tripState));
                }
            }
        }
        return calibrationMatrix;
    }

    public Map<DcModelName, Map<Purpose, Map<Type, Double>>> getAverageTripDistances(ArrayList<LongDistanceTripGermany> allTrips) {

        Map<DcModelName, Map<Purpose, Map<Type, Double>>> averageDistances = new HashMap<>();
        Map<DcModelName, Map<Purpose, Map<Type, Double>>> counts = new HashMap<>();

        for (DcModelName name : DcModelName.values()) {
            averageDistances.putIfAbsent(name, new HashMap<>(new HashMap<>()));
            counts.putIfAbsent(name, new HashMap<>(new HashMap<>()));

            for (Purpose purpose : PurposeGermany.values()){
                averageDistances.get(name).putIfAbsent(purpose, new HashMap<>());
                counts.get(name).putIfAbsent(purpose, new HashMap<>());

                for (Type tripState : TypeGermany.values()) {
                    averageDistances.get(name).get(purpose).putIfAbsent(tripState, .0);
                    counts.get(name).get(purpose).putIfAbsent(tripState, .0);
                }
            }
        }

        for (LongDistanceTrip tripToCast : allTrips) {
            LongDistanceTripGermany t = (LongDistanceTripGermany) tripToCast;
            if (!t.isInternational()) {
                DcModelName name = DcModelName.domesticDc;
                addTripToAverageCalculator(averageDistances, counts, t, name);
            } /*else if(t.isInternational()){
                DcModelName name = DcModelName.internationalDc;
                addTripToAverageCalculator(averageDistances, counts, t, name);
            }*/
        }

        logger.info("Destination choice average distances");
        logger.info("model" + "\t" + "purpose" + "\t" + "avgDistance");
        for (DcModelName name : DcModelName.values()) {
            for (Purpose purpose : PurposeGermany.values()) {
                for (Type tripState : TypeGermany.values()){
                    double sum = averageDistances.get(name).get(purpose).get(tripState);
                    double count = counts.get(name).get(purpose).get(tripState);
                    averageDistances.get(name).get(purpose).put(tripState, sum / count);
                    logger.info(name.toString() + "\t" + purpose + "\t" + tripState + "\t" + sum / count);
                }
            }

        }
        return averageDistances;
    }

    private void addTripToAverageCalculator(Map<DcModelName, Map<Purpose, Map<Type, Double>>> averageDistances, Map<DcModelName, Map<Purpose, Map<Type, Double>>> counts, LongDistanceTripGermany tripToCast, DcModelName name) {
        LongDistanceTripGermany t = tripToCast;
        double previousDistance = averageDistances.get(name).get(t.getTripPurpose()).get(t.getTripState());
        double previousCount = counts.get(name).get(t.getTripPurpose()).get(t.getTripState());
        averageDistances.get(name).get(t.getTripPurpose()).put(t.getTripState(), previousDistance + t.getAutoTravelDistance()/1000);
        counts.get(name).get(t.getTripPurpose()).put(t.getTripState(), previousCount + 1);
    }

    public void runDc() {
        logger.info("Running Destination Choice Model for " + allTrips.size() + " trips during model calibration");
        allTrips.parallelStream().forEach(tripToCast -> { //Easy parallel makes for fun times!!!
            LongDistanceTripGermany t = (LongDistanceTripGermany) tripToCast;
            if (!t.isInternational()) {
                int destZoneId = dcDomesticModel.selectDestination(t);
                t.setDestZone(zonesMap.get(destZoneId));
                t.setDestZoneType(ZoneTypeGermany.GERMANY);
                float distance = distanceByAuto.getValueAt(t.getOrigZone().getId(), destZoneId);
                t.setAutoTravelDistance(distance);
            } /*else {
                int destZoneId = dcInternational.selectDestination(t);
                t.setDestZone(zonesMap.get(destZoneId));
                if (dcDomesticModel.getDestinationZoneType(destZoneId)==ZoneTypeGermany.EXTEU) {
                    t.setDestZoneType(ZoneTypeGermany.EXTEU);
                }else if(dcDomesticModel.getDestinationZoneType(destZoneId)==ZoneTypeGermany.EXTOVERSEAS){
                    t.setDestZoneType(ZoneTypeGermany.EXTOVERSEAS);
                }
                float distance = distanceByAuto.getValueAt(t.getOrigZone().getId(), destZoneId);
                t.setTravelDistance(distance);
            }*/
        });
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
        surveyShares.get(type).get(PurposeGermany.BUSINESS).get(TypeGermany.DAYTRIP).putIfAbsent(ModeGermany.AUTO, 0.8991);
        surveyShares.get(type).get(PurposeGermany.BUSINESS).get(TypeGermany.DAYTRIP).putIfAbsent(ModeGermany.AIR, 0.0004);
        surveyShares.get(type).get(PurposeGermany.BUSINESS).get(TypeGermany.DAYTRIP).putIfAbsent(ModeGermany.RAIL, 0.0234);
        surveyShares.get(type).get(PurposeGermany.BUSINESS).get(TypeGermany.DAYTRIP).putIfAbsent(ModeGermany.BUS, 0.0771);
        surveyShares.get(type).get(PurposeGermany.BUSINESS).putIfAbsent(TypeGermany.OVERNIGHT, new HashMap<>());
        surveyShares.get(type).get(PurposeGermany.BUSINESS).get(TypeGermany.OVERNIGHT).putIfAbsent(ModeGermany.AUTO, 0.8289);
        surveyShares.get(type).get(PurposeGermany.BUSINESS).get(TypeGermany.OVERNIGHT).putIfAbsent(ModeGermany.AIR, 0.0146);
        surveyShares.get(type).get(PurposeGermany.BUSINESS).get(TypeGermany.OVERNIGHT).putIfAbsent(ModeGermany.RAIL, 0.0114);
        surveyShares.get(type).get(PurposeGermany.BUSINESS).get(TypeGermany.OVERNIGHT).putIfAbsent(ModeGermany.BUS, 0.1451);
        surveyShares.get(type).get(PurposeGermany.BUSINESS).putIfAbsent(TypeGermany.AWAY, new HashMap<>());
        surveyShares.get(type).get(PurposeGermany.BUSINESS).get(TypeGermany.AWAY).putIfAbsent(ModeGermany.AUTO, 1.);
        surveyShares.get(type).get(PurposeGermany.BUSINESS).get(TypeGermany.AWAY).putIfAbsent(ModeGermany.AIR, 0.);
        surveyShares.get(type).get(PurposeGermany.BUSINESS).get(TypeGermany.AWAY).putIfAbsent(ModeGermany.RAIL, 0.);
        surveyShares.get(type).get(PurposeGermany.BUSINESS).get(TypeGermany.AWAY).putIfAbsent(ModeGermany.BUS, 0.);

        surveyShares.get(type).putIfAbsent(PurposeGermany.LEISURE, new HashMap<>());
        surveyShares.get(type).get(PurposeGermany.LEISURE).putIfAbsent(TypeGermany.DAYTRIP, new HashMap<>());
        surveyShares.get(type).get(PurposeGermany.LEISURE).get(TypeGermany.DAYTRIP).putIfAbsent(ModeGermany.AUTO, 0.9135);
        surveyShares.get(type).get(PurposeGermany.LEISURE).get(TypeGermany.DAYTRIP).putIfAbsent(ModeGermany.AIR, 0.0001);
        surveyShares.get(type).get(PurposeGermany.LEISURE).get(TypeGermany.DAYTRIP).putIfAbsent(ModeGermany.RAIL, 0.0417);
        surveyShares.get(type).get(PurposeGermany.LEISURE).get(TypeGermany.DAYTRIP).putIfAbsent(ModeGermany.BUS, 0.0447);
        surveyShares.get(type).get(PurposeGermany.LEISURE).putIfAbsent(TypeGermany.OVERNIGHT, new HashMap<>());
        surveyShares.get(type).get(PurposeGermany.LEISURE).get(TypeGermany.OVERNIGHT).putIfAbsent(ModeGermany.AUTO, 0.9080);
        surveyShares.get(type).get(PurposeGermany.LEISURE).get(TypeGermany.OVERNIGHT).putIfAbsent(ModeGermany.AIR, 0.0043);
        surveyShares.get(type).get(PurposeGermany.LEISURE).get(TypeGermany.OVERNIGHT).putIfAbsent(ModeGermany.RAIL, 0.0254);
        surveyShares.get(type).get(PurposeGermany.LEISURE).get(TypeGermany.OVERNIGHT).putIfAbsent(ModeGermany.BUS, 0.0623);
        surveyShares.get(type).get(PurposeGermany.LEISURE).putIfAbsent(TypeGermany.AWAY, new HashMap<>());
        surveyShares.get(type).get(PurposeGermany.LEISURE).get(TypeGermany.AWAY).putIfAbsent(ModeGermany.AUTO, 1.);
        surveyShares.get(type).get(PurposeGermany.LEISURE).get(TypeGermany.AWAY).putIfAbsent(ModeGermany.AIR, 0.);
        surveyShares.get(type).get(PurposeGermany.LEISURE).get(TypeGermany.AWAY).putIfAbsent(ModeGermany.RAIL, 0.);
        surveyShares.get(type).get(PurposeGermany.LEISURE).get(TypeGermany.AWAY).putIfAbsent(ModeGermany.BUS, 0.);

        surveyShares.get(type).putIfAbsent(PurposeGermany.PRIVATE, new HashMap<>());
        surveyShares.get(type).get(PurposeGermany.PRIVATE).putIfAbsent(TypeGermany.DAYTRIP, new HashMap<>());
        surveyShares.get(type).get(PurposeGermany.PRIVATE).get(TypeGermany.DAYTRIP).putIfAbsent(ModeGermany.AUTO, 0.9348);
        surveyShares.get(type).get(PurposeGermany.PRIVATE).get(TypeGermany.DAYTRIP).putIfAbsent(ModeGermany.AIR, 0.0004);
        surveyShares.get(type).get(PurposeGermany.PRIVATE).get(TypeGermany.DAYTRIP).putIfAbsent(ModeGermany.RAIL, 0.0440);
        surveyShares.get(type).get(PurposeGermany.PRIVATE).get(TypeGermany.DAYTRIP).putIfAbsent(ModeGermany.BUS, 0.0208);
        surveyShares.get(type).get(PurposeGermany.PRIVATE).putIfAbsent(TypeGermany.OVERNIGHT, new HashMap<>());
        surveyShares.get(type).get(PurposeGermany.PRIVATE).get(TypeGermany.OVERNIGHT).putIfAbsent(ModeGermany.AUTO, 0.8897);
        surveyShares.get(type).get(PurposeGermany.PRIVATE).get(TypeGermany.OVERNIGHT).putIfAbsent(ModeGermany.AIR, 0.0033);
        surveyShares.get(type).get(PurposeGermany.PRIVATE).get(TypeGermany.OVERNIGHT).putIfAbsent(ModeGermany.RAIL, 0.0144);
        surveyShares.get(type).get(PurposeGermany.PRIVATE).get(TypeGermany.OVERNIGHT).putIfAbsent(ModeGermany.BUS, 0.0926);
        surveyShares.get(type).get(PurposeGermany.PRIVATE).putIfAbsent(TypeGermany.AWAY, new HashMap<>());
        surveyShares.get(type).get(PurposeGermany.PRIVATE).get(TypeGermany.AWAY).putIfAbsent(ModeGermany.AUTO, 1.);
        surveyShares.get(type).get(PurposeGermany.PRIVATE).get(TypeGermany.AWAY).putIfAbsent(ModeGermany.AIR, 0.);
        surveyShares.get(type).get(PurposeGermany.PRIVATE).get(TypeGermany.AWAY).putIfAbsent(ModeGermany.RAIL, 0.);
        surveyShares.get(type).get(PurposeGermany.PRIVATE).get(TypeGermany.AWAY).putIfAbsent(ModeGermany.BUS, 0.);

        logger.info("Mode choice calibration factors");
        logger.info("model" + "\t" + "purpose" + " \t" + "mode" + "\t" + "factor");

        for (McModelName name : McModelName.values()) {
            calibrationMatrix.put(name, new HashMap<>());
            for (Purpose purpose : PurposeGermany.values()) {
                calibrationMatrix.get(name).putIfAbsent(purpose, new HashMap<>());
                for (Type tripState : TypeGermany.values()){
                    calibrationMatrix.get(name).get(purpose).putIfAbsent(tripState, new HashMap<>());
                    for (Mode mode : ModeGermany.values()) {
                        double observedShare = surveyShares.get(name).get(purpose).get(tripState).get(mode);
                        double simulatedShare = simulatedModalShares.get(name).get(purpose).get(tripState).get(mode);
                        double factor = stepFactor * (observedShare - simulatedShare);
                        calibrationMatrix.get(name).get(purpose).get(tripState).putIfAbsent(mode, factor);
                        logger.info(name + "\t" + purpose + " \t" + mode + "\t" + factor);
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

        logger.info("Simulated Modal Shares");

        Map<McModelName, Map<Purpose, Map<Type, Map<Mode, Double>>>> modalShares = new HashMap<>();
        logger.info("model" + "\t" + "purpose" + " \t" + "mode" + "\t" + "share");
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
                        logger.info(name + "\t" + purpose + " \t" + tripState+ " \t" + mode + "\t" + modalShare);
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
                t.setTravelTime(mcDomesticModel.getDomesticModalTravelTime(t));
            }
        });
    }

//    public double getTripWeight(LongDistanceTripGermany t) {
//        double weight = 0;
//        switch ((TypeGermany) t.getTripState()) {
//            case AWAY:
//                weight = 0;
//                break;
//            case DAYTRIP:
//                weight = 1;
//                break;
//            case OVERNIGHT:
//                weight = 0.5;
//                break;
//        }
//
//        //tripStates = Arrays.asList("away", "daytrip", "inout");
//        return weight;
//    }

    public void printOutCalibrationResults(DomesticTripGenerationGermany domTg, DomesticDestinationChoiceGermany domDc, DomesticModeChoiceGermany domMc) {

        logger.info("Trip generation calibration");
        logger.info("model" + "\t" + "purpose" + "\t" + "tripState" + "\t" + "factor");
        Map<Purpose, Map<Type, Double>> mapTG;
        mapTG = domTg.getDomesticTgCalibrationMatrix();
        for (Purpose purpose : PurposeGermany.values()) {
            for (Type tripState : TypeGermany.values()) {
                logger.info(TgModelName.residenceTg + "\t" + purpose + "\t" + tripState + "\t" + mapTG.get(purpose).get(tripState));
            }
        }

        logger.info("Destination choice calibration");
        logger.info("model" + "\t" + "purpose" + "\t" + "factor");
        Map<Purpose, Map<Type, Double>> mapDC;
        for (Purpose purpose : PurposeGermany.values()) {
            for (Type tripState : TypeGermany.values()){
                mapDC = domDc.getDomesticDcCalibration();
                logger.info(DcModelName.domesticDc + "\t" + purpose + "\t" + mapDC.get(purpose).get(tripState));
            }
        }

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
