package de.tum.bgu.msm.longDistance;

import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.longDistance.data.trips.*;
import de.tum.bgu.msm.longDistance.destinationChoice.*;
import de.tum.bgu.msm.longDistance.modeChoice.DomesticModeChoice;
import de.tum.bgu.msm.longDistance.modeChoice.IntModeChoice;
import de.tum.bgu.msm.longDistance.modeChoice.McModel;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneType;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by carlloga on 12-07-17.
 */
public class Calibration implements ModelComponent {

    private boolean calibrationDC;
    private boolean calibrationMC;

    private int maxIter;


    private DomesticDestinationChoice dcModel;
    private IntOutboundDestinationChoice dcOutboundModel;
    private IntInboundDestinationChoice dcInBoundModel;
    private Distribution dcM;
    private DomesticModeChoice mcDomesticModel;
    private IntModeChoice intModeChoice;
    private McModel mcM;

    static Logger logger = Logger.getLogger(Calibration.class);

    private ArrayList<LongDistanceTrip> allTrips = new ArrayList<>();

    public Calibration() {
    }

    enum DcModelName {

        domesticDc, internationalOutboundDc, internationalInboundDc;

    }

    enum McModelName {
        domesticResidentsMc, domesticVisitorsMc, internationalOutboundMc, internationalInboundMc;
    }

    @Override
    public void setup(JSONObject prop, String inputFolder, String outputFolder) {

        calibrationDC = JsonUtilMto.getBooleanProp(prop, "destination_choice.calibration");
        calibrationMC = JsonUtilMto.getBooleanProp(prop, "mode_choice.calibration");

        maxIter = 50;

    }

    @Override
    public void load(DataSet dataSet) {


    }

    @Override
    public void run(DataSet dataSet, int nThreads) {

        //mcM = dataSet.getModeChoiceModel();


        if (calibrationDC || calibrationMC) {
            calibrateModel(calibrationDC, calibrationMC, dataSet);
            getAverageModalShares(dataSet.getAllTrips());
        }
    }


    public void calibrateModel(boolean dc, boolean mc, DataSet dataSet) {

        for (LongDistanceTrip t : dataSet.getAllTrips()) {
            if (!t.getTripState().equals(TypeOntario.AWAY)) {
                allTrips.add(t);
            }
        }


        Map<DcModelName, Map<Purpose, Double>> calibrationMatrixDc = new HashMap<>();
        Map<McModelName, Map<Purpose, Map<Mode, Double>>> calibrationMatrixMc = new HashMap<>();


        for (int iteration = 0; iteration < maxIter; iteration++) {

            logger.info("Calibration of destination choice: Iteration = " + iteration);
            calibrationMatrixDc = calculateCalibrationMatrix(allTrips);
            dcModel.updatedomDcCalibrationV(calibrationMatrixDc.get(DcModelName.domesticDc));
            dcOutboundModel.updateIntOutboundCalibrationV(calibrationMatrixDc.get(DcModelName.internationalOutboundDc));
            dcInBoundModel.updateIntInboundCalibrationV(calibrationMatrixDc.get(DcModelName.internationalInboundDc));

            runDc();
            runMc();

            logger.info("Calibration of mode choice: Iteration = " + iteration);
            calibrationMatrixMc = calculateMCCalibrationFactors(allTrips);
            mcDomesticModel.updateCalibrationDomestic(calibrationMatrixMc.get(McModelName.domesticResidentsMc));
            mcDomesticModel.updateCalibrationDomesticVisitors(calibrationMatrixMc.get(McModelName.domesticVisitorsMc));
            intModeChoice.updateCalibrationOutbound(calibrationMatrixMc.get(McModelName.internationalOutboundMc));
            intModeChoice.updateCalibrationInbound(calibrationMatrixMc.get(McModelName.internationalInboundMc));

            //runDestinationChoice(allTrips);
            runDc();
            runMc();


            //getAverageModalShares(allTrips);


        }


        /*dcM.run(dataSet, -1);
        mcM.run(dataSet, -1);*/

        printOutCalibrationResults(dcModel, dcOutboundModel, dcInBoundModel, mcDomesticModel, intModeChoice);

        //dataSet.setAllTrips(allTrips);

        /*printOutCalibrationResults(dataSet.getDcDomestic(),
                 dataSet.getDcIntOutbound(), dataSet.getDcIntInbound(), dataSet.getMcDomestic(), dataSet.getMcInt());*/

    }


    public Map<DcModelName, Map<Purpose, Double>> getAverageTripDistances(ArrayList<LongDistanceTrip> allTrips) {

        Map<DcModelName, Map<Purpose, Double>> averageDistances = new HashMap<>();
        Map<DcModelName, Map<Purpose, Double>> counts = new HashMap<>();

        for (DcModelName name : DcModelName.values()) {
            averageDistances.putIfAbsent(name, new HashMap<>());
            counts.putIfAbsent(name, new HashMap<>());
            for (Purpose purpose : PurposeOntario.values()) {
                averageDistances.get(name).putIfAbsent(purpose, 0.);
                counts.get(name).putIfAbsent(purpose, 0.);
            }

        }

        for (LongDistanceTrip t : allTrips) {
            if (t.getOrigZone().getZoneType().equals(ZoneType.ONTARIO) || t.getDestZoneType().equals(ZoneType.ONTARIO)) {
                //trip that starts in Ontario or end in ontario
                if (!t.isInternational()) {
                    //domestic from Ontario - row 0
                    if (t.getTravelDistanceLevel2() < 2000) {
                        DcModelName name = DcModelName.domesticDc;
                        addTripToAverageCalculator(averageDistances, counts, t, name);
                    }

                } else if (t.getDestZoneType().equals(ZoneType.EXTUS)) {
                    //international from ontario to us - row 1
                    if (t.getTravelDistanceLevel2() < 4000) {
                        DcModelName name = DcModelName.internationalOutboundDc;
                        addTripToAverageCalculator(averageDistances, counts, t, name);
                    }
                } else if (t.getOrigZone().getZoneType().equals(ZoneType.EXTUS) /*&& t.getDestZoneType().equals(ZoneType.ONTARIO)*/) {
                    //international from US to ontario + row 2
                    if (t.getTravelDistanceLevel2() < 4000) {
                        DcModelName name = DcModelName.internationalInboundDc;
                        addTripToAverageCalculator(averageDistances, counts, t, name);
                    }
                }
            }
        }

        logger.info("Destination choice average distances");
        logger.info("model" + "\t" + "purpose" + "\t" + "avgDistance");
        for (DcModelName name : DcModelName.values()) {
            for (Purpose purpose : PurposeOntario.values()) {
                double sum = averageDistances.get(name).get(purpose);
                double count = counts.get(name).get(purpose);

                averageDistances.get(name).put(purpose, sum / count);
                logger.info(name.toString() + "\t" + purpose + "\t" + sum / count);

            }

        }

        return averageDistances;

    }

    private void addTripToAverageCalculator(Map<DcModelName, Map<Purpose, Double>> averageDistances, Map<DcModelName, Map<Purpose, Double>> counts, LongDistanceTrip t, DcModelName name) {
        double previousDistance = averageDistances.get(name).get(t.getTripPurpose());
        double previousCount = counts.get(name).get(t.getTripPurpose());

        averageDistances.get(name).put(t.getTripPurpose(), previousDistance + t.getTravelDistanceLevel2() * getTripWeight(t));
        counts.get(name).put(t.getTripPurpose(), previousCount + getTripWeight(t));
    }

    public Map<DcModelName, Map<Purpose, Double>> calculateCalibrationMatrix(ArrayList<LongDistanceTrip> allTrips) {

        Map<DcModelName, Map<Purpose, Double>> averageDistances = getAverageTripDistances(allTrips);
        Map<DcModelName, Map<Purpose, Double>> calibrationMatrix = new HashMap<>();

        double expansionFactor = 1;

        //hard coded for calibration
        calibrationMatrix.putIfAbsent(DcModelName.domesticDc, new HashMap<>());
        calibrationMatrix.get(DcModelName.domesticDc).put(PurposeOntario.VISIT, (averageDistances.get(DcModelName.domesticDc).get(PurposeOntario.VISIT) / 133 - 1) * expansionFactor + 1); //domestic visit
        calibrationMatrix.get(DcModelName.domesticDc).put(PurposeOntario.BUSINESS, (averageDistances.get(DcModelName.domesticDc).get(PurposeOntario.BUSINESS) / 175 - 1) * expansionFactor + 1);//domestic business
        calibrationMatrix.get(DcModelName.domesticDc).put(PurposeOntario.LEISURE, (averageDistances.get(DcModelName.domesticDc).get(PurposeOntario.LEISURE) / 134 - 1) * expansionFactor + 1);//domestic leisure

        calibrationMatrix.putIfAbsent(DcModelName.internationalOutboundDc, new HashMap<>());
        calibrationMatrix.get(DcModelName.internationalOutboundDc).put(PurposeOntario.VISIT, (averageDistances.get(DcModelName.internationalOutboundDc).get(PurposeOntario.VISIT) / 642 - 1) * expansionFactor + 1); //to us visit
        calibrationMatrix.get(DcModelName.internationalOutboundDc).put(PurposeOntario.BUSINESS, (averageDistances.get(DcModelName.internationalOutboundDc).get(PurposeOntario.BUSINESS) / 579 - 1) * expansionFactor + 1);//to us business
        calibrationMatrix.get(DcModelName.internationalOutboundDc).put(PurposeOntario.LEISURE, (averageDistances.get(DcModelName.internationalOutboundDc).get(PurposeOntario.LEISURE) / 515 - 1) * expansionFactor + 1);//to us leisure

        calibrationMatrix.putIfAbsent(DcModelName.internationalInboundDc, new HashMap<>());
        calibrationMatrix.get(DcModelName.internationalInboundDc).put(PurposeOntario.VISIT, (averageDistances.get(DcModelName.internationalInboundDc).get(PurposeOntario.VISIT) / 697 - 1) * expansionFactor + 1); //from us visit
        calibrationMatrix.get(DcModelName.internationalInboundDc).put(PurposeOntario.BUSINESS, (averageDistances.get(DcModelName.internationalInboundDc).get(PurposeOntario.BUSINESS) / 899 - 1) * expansionFactor + 1);//from us business
        calibrationMatrix.get(DcModelName.internationalInboundDc).put(PurposeOntario.LEISURE, (averageDistances.get(DcModelName.internationalInboundDc).get(PurposeOntario.LEISURE) / 516 - 1) * expansionFactor + 1);//from us leisure


        logger.info("Destination choice average calibration coefficients");
        logger.info("model" + "\t" + "purpose" + "\t" + "coefficient");
        for (DcModelName name : DcModelName.values()) {
            for (Purpose purpose : PurposeOntario.values()) {
                logger.info(name.toString() + "\t" + purpose + "\t" + calibrationMatrix.get(name).get(purpose));
            }

        }

        return calibrationMatrix;

    }

    public Map<McModelName, Map<Purpose, Map<Mode, Double>>> getAverageModalShares(ArrayList<LongDistanceTrip> allTrips) {

        Map<McModelName, Map<Purpose, Map<Mode, Double>>> countsByMode = new HashMap<>();

        for (McModelName name : McModelName.values()) {
            countsByMode.putIfAbsent(name, new HashMap<>());
            for (Purpose purpose : PurposeOntario.values()) {
                countsByMode.get(name).put(purpose, new HashMap<>());
                for (Mode mode : ModeOntario.values()) {
                    countsByMode.get(name).get(purpose).put(mode, 0.);
                }
            }
        }


        //int tripCounter = 0;

        for (LongDistanceTrip t : allTrips) {
            if (t.getOrigZone().getZoneType().equals(ZoneType.ONTARIO) || t.getDestZoneType().equals(ZoneType.ONTARIO)) {
                if (!t.isInternational()) {
                    //domestic to or from ontario
                    if (t.getOrigZone().getZoneType().equals(ZoneType.ONTARIO)) {
                        McModelName name = McModelName.domesticResidentsMc;
                        //domestic from Ontario - row 0
                        addTripToModalShareCalculator(countsByMode, t, name);

                    } else {
                        //domestic to Ontario - row 3
                        McModelName name = McModelName.domesticVisitorsMc;
                        addTripToModalShareCalculator(countsByMode, t, name);
                    }


                } else if (t.getDestZoneType().equals(ZoneType.EXTUS)) {
                    McModelName name = McModelName.internationalOutboundMc;
                    //international from ontario to US - row 1
                    addTripToModalShareCalculator(countsByMode, t, name);

                } else if (t.getOrigZone().getZoneType().equals(ZoneType.EXTUS)) {
                    //international from US to ontario - row 2
                    McModelName name = McModelName.internationalInboundMc;
                    addTripToModalShareCalculator(countsByMode, t, name);
                }
            }
        }

        logger.info("Simulated Modal Shares");

        Map<McModelName, Map<Purpose, Map<Mode, Double>>> modalShares = new HashMap<>();
        logger.info("model" + "\t" + "purpose" + " \t" + "mode" + "\t" + "share");
        for (McModelName name : McModelName.values()) {
            modalShares.putIfAbsent(name, new HashMap<>());
            for (Purpose purpose : PurposeOntario.values()) {
                modalShares.get(name).put(purpose, new HashMap<>());
                double total = 0;
                for (Mode mode : ModeOntario.values()) {
                    total += countsByMode.get(name).get(purpose).get(mode);
                }
                for (Mode mode : ModeOntario.values()) {
                    double modalShare = countsByMode.get(name).get(purpose).get(mode) / total;
                    modalShares.get(name).get(purpose).put(mode, modalShare);
                    logger.info(name + "\t" + purpose + " \t" + mode + "\t" + modalShare);

                }
            }
        }

        return modalShares;
    }

    private void addTripToModalShareCalculator(Map<McModelName, Map<Purpose, Map<Mode, Double>>> countsByMode, LongDistanceTrip t, McModelName name) {
        double currentCount = countsByMode.get(name).get(t.getTripPurpose()).get(t.getMode());
        countsByMode.get(name).get(t.getTripPurpose()).put(t.getMode(), currentCount + getTripWeight(t));
    }

    public Map<McModelName, Map<Purpose, Map<Mode, Double>>> calculateMCCalibrationFactors(ArrayList<LongDistanceTrip> allTrips) {

        Map<McModelName, Map<Purpose, Map<Mode, Double>>> calibrationMatrix = new HashMap<>();

        Map<McModelName, Map<Purpose, Map<Mode, Double>>> simulatedModalShares = getAverageModalShares(allTrips);

        Map<McModelName, Map<Purpose, Map<Mode, Double>>> surveyShares = new HashMap<>();

        double expansionFactor = 1;

        //hard coded for calibration
        //domestic
        McModelName type = McModelName.domesticResidentsMc;
        surveyShares.putIfAbsent(type, new HashMap<>());
        surveyShares.get(type).putIfAbsent(PurposeOntario.VISIT, new HashMap<>());
        surveyShares.get(type).get(PurposeOntario.VISIT).putIfAbsent(ModeOntario.AUTO, 0.93);
        surveyShares.get(type).get(PurposeOntario.VISIT).putIfAbsent(ModeOntario.AIR, 0.01);
        surveyShares.get(type).get(PurposeOntario.VISIT).putIfAbsent(ModeOntario.RAIL, 0.03);
        surveyShares.get(type).get(PurposeOntario.VISIT).putIfAbsent(ModeOntario.BUS, 0.03);

        surveyShares.get(type).putIfAbsent(PurposeOntario.BUSINESS, new HashMap<>());
        surveyShares.get(type).get(PurposeOntario.BUSINESS).putIfAbsent(ModeOntario.AUTO, 0.86);
        surveyShares.get(type).get(PurposeOntario.BUSINESS).putIfAbsent(ModeOntario.AIR, 0.06);
        surveyShares.get(type).get(PurposeOntario.BUSINESS).putIfAbsent(ModeOntario.RAIL, 0.03);
        surveyShares.get(type).get(PurposeOntario.BUSINESS).putIfAbsent(ModeOntario.BUS, 0.05);

        surveyShares.get(type).putIfAbsent(PurposeOntario.LEISURE, new HashMap<>());
        surveyShares.get(type).get(PurposeOntario.LEISURE).putIfAbsent(ModeOntario.AUTO, 0.96);
        surveyShares.get(type).get(PurposeOntario.LEISURE).putIfAbsent(ModeOntario.AIR, 0.00);
        surveyShares.get(type).get(PurposeOntario.LEISURE).putIfAbsent(ModeOntario.RAIL, 0.03);
        surveyShares.get(type).get(PurposeOntario.LEISURE).putIfAbsent(ModeOntario.BUS, 0.01);

        //int outbound
        type = McModelName.internationalOutboundMc;
        surveyShares.putIfAbsent(type, new HashMap<>());
        surveyShares.get(type).putIfAbsent(PurposeOntario.VISIT, new HashMap<>());
        surveyShares.get(type).get(PurposeOntario.VISIT).putIfAbsent(ModeOntario.AUTO, 0.76);
        surveyShares.get(type).get(PurposeOntario.VISIT).putIfAbsent(ModeOntario.AIR, 0.23);
        surveyShares.get(type).get(PurposeOntario.VISIT).putIfAbsent(ModeOntario.RAIL, 0.00);
        surveyShares.get(type).get(PurposeOntario.VISIT).putIfAbsent(ModeOntario.BUS, 0.01);

        surveyShares.get(type).putIfAbsent(PurposeOntario.BUSINESS, new HashMap<>());
        surveyShares.get(type).get(PurposeOntario.BUSINESS).putIfAbsent(ModeOntario.AUTO, 0.74);
        surveyShares.get(type).get(PurposeOntario.BUSINESS).putIfAbsent(ModeOntario.AIR, 0.25);
        surveyShares.get(type).get(PurposeOntario.BUSINESS).putIfAbsent(ModeOntario.RAIL, 0.00);
        surveyShares.get(type).get(PurposeOntario.BUSINESS).putIfAbsent(ModeOntario.BUS, 0.01);

        surveyShares.get(type).putIfAbsent(PurposeOntario.LEISURE, new HashMap<>());
        //todo do not sum up 1?
        surveyShares.get(type).get(PurposeOntario.LEISURE).putIfAbsent(ModeOntario.AUTO, 0.87);
        surveyShares.get(type).get(PurposeOntario.LEISURE).putIfAbsent(ModeOntario.AIR, 0.10);
        surveyShares.get(type).get(PurposeOntario.LEISURE).putIfAbsent(ModeOntario.RAIL, 0.00);
        surveyShares.get(type).get(PurposeOntario.LEISURE).putIfAbsent(ModeOntario.BUS, 0.01);

        //int inbound
        type = McModelName.internationalInboundMc;
        surveyShares.putIfAbsent(type, new HashMap<>());
        surveyShares.get(type).putIfAbsent(PurposeOntario.VISIT, new HashMap<>());
        surveyShares.get(type).get(PurposeOntario.VISIT).putIfAbsent(ModeOntario.AUTO, 0.75);
        surveyShares.get(type).get(PurposeOntario.VISIT).putIfAbsent(ModeOntario.AIR, 0.24);
        surveyShares.get(type).get(PurposeOntario.VISIT).putIfAbsent(ModeOntario.RAIL, 0.00);
        surveyShares.get(type).get(PurposeOntario.VISIT).putIfAbsent(ModeOntario.BUS, 0.01);

        surveyShares.get(type).putIfAbsent(PurposeOntario.BUSINESS, new HashMap<>());
        surveyShares.get(type).get(PurposeOntario.BUSINESS).putIfAbsent(ModeOntario.AUTO, 0.39);
        surveyShares.get(type).get(PurposeOntario.BUSINESS).putIfAbsent(ModeOntario.AIR, 0.60);
        surveyShares.get(type).get(PurposeOntario.BUSINESS).putIfAbsent(ModeOntario.RAIL, 0.00);
        surveyShares.get(type).get(PurposeOntario.BUSINESS).putIfAbsent(ModeOntario.BUS, 0.01);

        surveyShares.get(type).putIfAbsent(PurposeOntario.LEISURE, new HashMap<>());
        surveyShares.get(type).get(PurposeOntario.LEISURE).putIfAbsent(ModeOntario.AUTO, 0.85);
        surveyShares.get(type).get(PurposeOntario.LEISURE).putIfAbsent(ModeOntario.AIR, 0.06);
        surveyShares.get(type).get(PurposeOntario.LEISURE).putIfAbsent(ModeOntario.RAIL, 0.00);
        surveyShares.get(type).get(PurposeOntario.LEISURE).putIfAbsent(ModeOntario.BUS, 0.09);


        //domestio visitors to ontario
        type = McModelName.domesticVisitorsMc;
        surveyShares.get(type).putIfAbsent(PurposeOntario.VISIT, new HashMap<>());
        surveyShares.get(type).get(PurposeOntario.VISIT).putIfAbsent(ModeOntario.AUTO, 0.67);
        surveyShares.get(type).get(PurposeOntario.VISIT).putIfAbsent(ModeOntario.AIR, 0.24);
        surveyShares.get(type).get(PurposeOntario.VISIT).putIfAbsent(ModeOntario.RAIL, 0.05);
        surveyShares.get(type).get(PurposeOntario.VISIT).putIfAbsent(ModeOntario.BUS, 0.04);

        surveyShares.get(type).putIfAbsent(PurposeOntario.BUSINESS, new HashMap<>());
        surveyShares.get(type).get(PurposeOntario.BUSINESS).putIfAbsent(ModeOntario.AUTO, 0.35);
        surveyShares.get(type).get(PurposeOntario.BUSINESS).putIfAbsent(ModeOntario.AIR, 0.59);
        surveyShares.get(type).get(PurposeOntario.BUSINESS).putIfAbsent(ModeOntario.RAIL, 0.02);
        surveyShares.get(type).get(PurposeOntario.BUSINESS).putIfAbsent(ModeOntario.BUS, 0.04);

        surveyShares.get(type).putIfAbsent(PurposeOntario.LEISURE, new HashMap<>());
        surveyShares.get(type).get(PurposeOntario.LEISURE).putIfAbsent(ModeOntario.AUTO, 0.84);
        surveyShares.get(type).get(PurposeOntario.LEISURE).putIfAbsent(ModeOntario.AIR, 0.08);
        surveyShares.get(type).get(PurposeOntario.LEISURE).putIfAbsent(ModeOntario.RAIL, 0.06);
        surveyShares.get(type).get(PurposeOntario.LEISURE).putIfAbsent(ModeOntario.BUS, 0.02);

        logger.info("Mode choice calibration factors");
        logger.info("model" + "\t" + "purpose" + " \t" + "mode" + "\t" + "factor");

        for (McModelName name : McModelName.values()) {
            calibrationMatrix.put(name, new HashMap<>());
            for (Purpose purpose : PurposeOntario.values()) {
                calibrationMatrix.get(name).putIfAbsent(purpose, new HashMap<>());
                for (Mode mode : ModeOntario.values()) {
                    double observedShare = surveyShares.get(name).get(purpose).get(mode);
                    double simulatedShare = simulatedModalShares.get(name).get(purpose).get(mode);
                    double factor = expansionFactor * (simulatedShare - observedShare);
                    calibrationMatrix.get(name).get(purpose).putIfAbsent(mode, factor);

                    logger.info(name + "\t" + purpose + " \t" + mode + "\t" + factor);

                }
            }
        }


        return calibrationMatrix;

        /*type = 0;
        System.out.println("domestic");
        System.out.println("km: visit: - auto " + calibrationMatrix[type][0][0] + " - air: " + calibrationMatrix[type][0][1] + " - rail: " + calibrationMatrix[type][0][2] + " - bus: " + calibrationMatrix[type][0][3]);
        System.out.println("km: business: - auto " + calibrationMatrix[type][1][0] + " - air: " + calibrationMatrix[type][1][1] + " - rail: " + calibrationMatrix[type][1][2] + " - bus: " + calibrationMatrix[type][1][3]);
        System.out.println("km: leisure - auto: " + calibrationMatrix[type][2][0] + " - air: " + calibrationMatrix[type][2][1] + " - rail: " + calibrationMatrix[type][2][2] + " - bus: " + calibrationMatrix[type][2][3]);
        type = 1;
        System.out.println("international_outbound");
        System.out.println("km: visit: - auto " + calibrationMatrix[type][0][0] + " - air: " + calibrationMatrix[type][0][1] + " - rail: " + calibrationMatrix[type][0][2] + " - bus: " + calibrationMatrix[type][0][3]);
        System.out.println("km: business: - auto " + calibrationMatrix[type][1][0] + " - air: " + calibrationMatrix[type][1][1] + " - rail: " + calibrationMatrix[type][1][2] + " - bus: " + calibrationMatrix[type][1][3]);
        System.out.println("km: leisure - auto: " + calibrationMatrix[type][2][0] + " - air: " + calibrationMatrix[type][2][1] + " - rail: " + calibrationMatrix[type][2][2] + " - bus: " + calibrationMatrix[type][2][3]);
        type = 2;
        System.out.println("international_inbound");
        System.out.println("km: visit: - auto " + calibrationMatrix[type][0][0] + " - air: " + calibrationMatrix[type][0][1] + " - rail: " + calibrationMatrix[type][0][2] + " - bus: " + calibrationMatrix[type][0][3]);
        System.out.println("km: business: - auto " + calibrationMatrix[type][1][0] + " - air: " + calibrationMatrix[type][1][1] + " - rail: " + calibrationMatrix[type][1][2] + " - bus: " + calibrationMatrix[type][1][3]);
        System.out.println("km: leisure - auto: " + calibrationMatrix[type][2][0] + " - air: " + calibrationMatrix[type][2][1] + " - rail: " + calibrationMatrix[type][2][2] + " - bus: " + calibrationMatrix[type][2][3]);
        type = 3;
        System.out.println("domestic visitors ");
        System.out.println("km: visit: - auto " + calibrationMatrix[type][0][0] + " - air: " + calibrationMatrix[type][0][1] + " - rail: " + calibrationMatrix[type][0][2] + " - bus: " + calibrationMatrix[type][0][3]);
        System.out.println("km: business: - auto " + calibrationMatrix[type][1][0] + " - air: " + calibrationMatrix[type][1][1] + " - rail: " + calibrationMatrix[type][1][2] + " - bus: " + calibrationMatrix[type][1][3]);
        System.out.println("km: leisure - auto: " + calibrationMatrix[type][2][0] + " - air: " + calibrationMatrix[type][2][1] + " - rail: " + calibrationMatrix[type][2][2] + " - bus: " + calibrationMatrix[type][2][3]);
        */

    }


    public double getTripWeight(LongDistanceTrip t) {
        double weight = 0;
        switch ((TypeOntario) t.getTripState()) {
            case AWAY:
                weight = 0;
                break;
            case DAYTRIP:
                weight = 1;
                break;
            case INOUT:
                weight = 0.5;
                break;
        }

        //tripStates = Arrays.asList("away", "daytrip", "inout");
        return weight;
    }

    public void printOutCalibrationResults(DomesticDestinationChoice domDc, IntOutboundDestinationChoice intOutDc, IntInboundDestinationChoice intInDc,
                                           DomesticModeChoice domMc, IntModeChoice intMc) {

        logger.info("Destination choice calibration");
        logger.info("model" + "\t" + "purpose" + "\t" + "factor");
        Map<Purpose, Double> map;
        for (Purpose purpose : PurposeOntario.values()) {
            map = domDc.getDomDcCalibrationV();
            logger.info(DcModelName.domesticDc + "\t" + purpose + "\t" + map.get(purpose));
        }
        map = intOutDc.getCalibrationV();
        for (Purpose purpose : PurposeOntario.values()) {
            logger.info(DcModelName.internationalOutboundDc + "\t" + purpose + "\t" + map.get(purpose));
        }
        map = intInDc.getCalibrationV();
        for (Purpose purpose : PurposeOntario.values()) {
            logger.info(DcModelName.internationalInboundDc + "\t" + purpose + "\t" + map.get(purpose));
        }

        logger.info("Mode choice calibration");
        logger.info("model" + "\t" + "purpose" + "\t" + "mode" + "\t" + "factor");
        Map<Purpose, Map<Mode, Double>> map2;
        map2 = domMc.getCalibrationMatrix();
        for (Purpose purpose : PurposeOntario.values()) {
            for (Mode mode : ModeOntario.values()){
                logger.info(McModelName.domesticResidentsMc + "\t" + purpose + "\t" + mode + "\t" + map2.get(purpose).get(mode));
            }

        }
        map2 = domMc.getCalibrationMatrixVisitors();
        for (Purpose purpose : PurposeOntario.values()) {
            for (Mode mode : ModeOntario.values()){
                logger.info(McModelName.domesticVisitorsMc + "\t" + purpose + "\t" + mode + "\t" + map2.get(purpose).get(mode));
            }

        }
        map2 = intMc.getCalibrationMatrixOutbound();
        for (Purpose purpose : PurposeOntario.values()) {
            for (Mode mode : ModeOntario.values()){
                logger.info(McModelName.internationalOutboundMc + "\t" + purpose + "\t" + mode + "\t" + map2.get(purpose).get(mode));
            }

        }
        map2 = intMc.getCalibrationMatrixInbound();
        for (Purpose purpose : PurposeOntario.values()) {
            for (Mode mode : ModeOntario.values()){
                logger.info(McModelName.internationalInboundMc + "\t" + purpose + "\t" + mode + "\t" + map2.get(purpose).get(mode));
            }

        }





    }

    public void runDc() {
        logger.info("Running Destination Choice Model for " + allTrips.size() + " trips during model calibration");
        allTrips.parallelStream().forEach(t -> { //Easy parallel makes for fun times!!!
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

        });

    }

    private void runMc() {
        logger.info("Running Mode Choice Model for " + allTrips.size() + " trips during model calibration");
        allTrips.parallelStream().forEach(t -> {
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
