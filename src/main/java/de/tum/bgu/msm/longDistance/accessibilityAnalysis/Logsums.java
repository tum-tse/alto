package de.tum.bgu.msm.longDistance.accessibilityAnalysis;

//import com.pb.common.datafile.TableDataSet;

import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.common.datafile.TableDataSet;
import de.tum.bgu.msm.common.matrix.Matrix;
import de.tum.bgu.msm.common.matrix.RowVector;
import de.tum.bgu.msm.longDistance.ModelComponent;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.sp.Gender;
import de.tum.bgu.msm.longDistance.data.sp.HouseholdGermany;
import de.tum.bgu.msm.longDistance.data.sp.OccupationStatus;
import de.tum.bgu.msm.longDistance.data.sp.PersonGermany;
import de.tum.bgu.msm.longDistance.data.trips.*;
import de.tum.bgu.msm.longDistance.data.zoneSystem.AreaTypeGermany;
import de.tum.bgu.msm.longDistance.data.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneGermany;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneTypeGermany;
import de.tum.bgu.msm.longDistance.modeChoice.DomesticModeChoiceGermany;
import de.tum.bgu.msm.longDistance.modeChoice.EuropeModeChoiceGermany;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


public class Logsums implements ModelComponent {

    static Logger logger = Logger.getLogger(Logsums.class);
    private float NESTING_COEFFICIENT_AUTO_MODES;
    private float NESTING_COEFFICIENT_RAIL_MODES;
    private float WEIGHT_OF_ATTRACTION;
    private Map<Integer, Map<PurposeGermany, Map<Integer, Map<ModeGermany, Double>>>> impedanceTable;

    private DataSet dataSet;
    private TableDataSet costsPerKm;

    private DomesticModeChoiceGermany mcDomesticModel;
    private EuropeModeChoiceGermany mcEuropeModel;

    @Override
    public void setup(JSONObject prop, String inputFolder, String outputFolder) {
        //coefficients = Util.readCSVfile(inputFolder + JsonUtilMto.getStringProp(prop, "emissions.coef_file"));
        //coefficients.buildStringIndex(2);
        mcDomesticModel = new DomesticModeChoiceGermany(prop, inputFolder);
        mcEuropeModel = new EuropeModeChoiceGermany(prop, inputFolder);
        NESTING_COEFFICIENT_AUTO_MODES = 1 / JsonUtilMto.getFloatProp(prop, "scenarioPolicy.tollScenario.nested_incremental_logit_scale");
        NESTING_COEFFICIENT_RAIL_MODES = 1 / JsonUtilMto.getFloatProp(prop, "scenarioPolicy.shuttleBusToRail.nested_incremental_rail_scale");
        WEIGHT_OF_ATTRACTION = 1 / JsonUtilMto.getFloatProp(prop, "scenarioPolicy.logsums.attraction_weight");
        impedanceTable = new HashMap<>();
        costsPerKm = Util.readCSVfile(inputFolder + JsonUtilMto.getStringProp(prop, "mode_choice.costPerKm_file"));
        costsPerKm.buildStringIndex(2);

        logger.info("Cauculate individual-based logsum");
    }

    @Override
    public void load(DataSet dataSet) {
        mcDomesticModel.loadDomesticModeChoice(dataSet);
        mcEuropeModel.loadEuropeModeChoice(dataSet);
        this.dataSet = dataSet;


//        logger.info("Creating logsum matrix");
//        //This block overwrite some parameters of scenarios to allow automatic multiple runs; some settings might be duplicate
//        boolean runCityTollScenario = true;
//        double cityToll = dataSet.getScenarioSettings().getValueAt(dataSet.getScenario(), "cordonToll");
//        boolean runTollScenario = true;
//        boolean tollOnBundesstrasse = false;
//        double toll = dataSet.getScenarioSettings().getValueAt(dataSet.getScenario(), "freewayToll");
//        boolean runScenario1 = true;
//        double shuttleBusCostBase = dataSet.getScenarioSettings().getValueAt(dataSet.getScenario(), "railShuttleBase");
//        double shuttleBusCostPerKm = dataSet.getScenarioSettings().getValueAt(dataSet.getScenario(), "railShuttleKm");
//        boolean subsidyForRural = dataSet.getScenarioSettings().getBooleanValueAt(dataSet.getScenario(), "ruralFree");
//
//        //Todo: fake numbers, need to be improved later
//        double vot = 32.00;
//        double b_impedance = 1;
//        double alpha_impedance = 1;
//
//
//        Matrix autoTotalTime_hr = dataSet.getTravelTimeMatrix().get(ModeGermany.AUTO);
//        Matrix autoNoTollTotalTime_hr = dataSet.getTravelTimeMatrix().get(ModeGermany.AUTO_noTOLL).multiply(1 / 3600);
//        Matrix busTotalTime_hr = dataSet.getTravelTimeMatrix().get(ModeGermany.BUS).multiply(1 / 3600);
//        Matrix railTimeAccess_sec = dataSet.getRailAccessDistMatrix().get(ModeGermany.RAIL);
//        Matrix railTimeEgress_sec = dataSet.getRailEgressTimeMatrix().get(ModeGermany.RAIL);
//        Matrix railTime_sec = dataSet.getTravelTimeMatrix().get(ModeGermany.RAIL);
//        List<Matrix> matricesRail = new ArrayList<>();
//        matricesRail.add(railTimeAccess_sec);
//        matricesRail.add(railTimeEgress_sec);
//        matricesRail.add(railTime_sec);
//        Matrix railTotalTime_hr = sumMatrices(matricesRail).multiply(1 / 3600);
//        Matrix railShuttleTimeAccess_sec = dataSet.getRailAccessDistMatrix().get(ModeGermany.RAIL_SHUTTLE);
//        Matrix railShuttleTimeEgress_sec = dataSet.getRailEgressTimeMatrix().get(ModeGermany.RAIL_SHUTTLE);
//        Matrix railShuttleTime_sec = dataSet.getTravelTimeMatrix().get(ModeGermany.RAIL_SHUTTLE);
//        List<Matrix> matricesRailShuttle = new ArrayList<>();
//        matricesRailShuttle.add(railShuttleTimeAccess_sec);
//        matricesRailShuttle.add(railShuttleTimeEgress_sec);
//        matricesRailShuttle.add(railShuttleTime_sec);
//        Matrix railShuttleTotalTime_hr = sumMatrices(matricesRailShuttle).multiply(1 / 3600);
//
//        Matrix autoDistance_km = dataSet.getDistanceMatrix().get(ModeGermany.AUTO).multiply(1 / 1000);
//        Matrix autoNoTollDistance_km = dataSet.getDistanceMatrix().get(ModeGermany.AUTO_noTOLL).multiply(1 / 1000);
//        Matrix busDistance_km = dataSet.getDistanceMatrix().get(ModeGermany.BUS).multiply(1 / 1000);
//        Matrix railDistance_km = dataSet.getDistanceMatrix().get(ModeGermany.RAIL).multiply(1 / 1000);
//        Matrix railShuttleAccessDistance_km = dataSet.getRailAccessDistMatrix().get(ModeGermany.RAIL_SHUTTLE).multiply(1 / 1000);
//        Matrix railShuttleEgressDistance_km = dataSet.getRailEgressDistMatrix().get(ModeGermany.RAIL_SHUTTLE).multiply(1 / 1000);
//        Matrix railShuttleDistance_km = dataSet.getDistanceMatrix().get(ModeGermany.RAIL_SHUTTLE).multiply(1 / 1000);
//        Matrix autoTolledDistance_km = dataSet.getTollDistanceMatrix().get(ModeGermany.AUTO).multiply(1 / 1000);
//        Matrix autoNoTollTolledDistance_km = dataSet.getTollDistanceMatrix().get(ModeGermany.AUTO_noTOLL).multiply(1 / 1000);
//
//        Matrix utilityAuto = new Matrix(dataSet.getZones().size(), dataSet.getZones().size());
//        Matrix utilityAutoNoToll = new Matrix(dataSet.getZones().size(), dataSet.getZones().size());
//        Matrix utilityBus = new Matrix(dataSet.getZones().size(), dataSet.getZones().size());
//        Matrix utilityRail = new Matrix(dataSet.getZones().size(), dataSet.getZones().size());
//        Matrix utilityRailShuttle = new Matrix(dataSet.getZones().size(), dataSet.getZones().size());
//
//
//        AtomicInteger counterLogsumMatrix = new AtomicInteger(0);
//
//        for (Zone originZone : dataSet.getZones().values()) {
//            for (Zone destinationZone : dataSet.getZones().values()) {
//
//                ZoneGermany origin = (ZoneGermany) originZone;
//                ZoneGermany destination = (ZoneGermany) destinationZone;
//
//                double costAuto = costsPerKm.getStringIndexedValueAt("alpha", ModeGermany.AUTO.name()) * Math.pow(autoDistance_km.getValueAt(origin.getId(), destination.getId()), costsPerKm.getStringIndexedValueAt("beta", ModeGermany.AUTO.name())) * autoDistance_km.getValueAt(origin.getId(), destination.getId())
//                        + autoTolledDistance_km.getValueAt(origin.getId(), destination.getId()) * toll;
//                if (origin.getAreatype().equals(AreaTypeGermany.CORE_CITY)) {
//                    costAuto = costAuto + cityToll;
//                }
//                if (destination.getAreatype().equals(AreaTypeGermany.CORE_CITY)) {
//                    costAuto = costAuto + cityToll;
//                }
//                double impedanceAuto = costAuto / (vot) + autoTotalTime_hr.getValueAt(origin.getId(), destination.getId());
//                double utilityAuto_trip = b_impedance * Math.exp(alpha_impedance * impedanceAuto * 60);
//                utilityAuto.setValueAt(origin.getId(), destination.getId(), (float) utilityAuto_trip);
//
//                double costAutoNoToll = costsPerKm.getStringIndexedValueAt("alpha", ModeGermany.AUTO_noTOLL.name()) * Math.pow(autoNoTollDistance_km.getValueAt(origin.getId(), destination.getId()), costsPerKm.getStringIndexedValueAt("beta", ModeGermany.AUTO_noTOLL.name())) * autoNoTollDistance_km.getValueAt(origin.getId(), destination.getId())
//                        + autoNoTollTolledDistance_km.getValueAt(origin.getId(), destination.getId()) * toll;
//                if (origin.getAreatype().equals(AreaTypeGermany.CORE_CITY)) {
//                    costAutoNoToll = costAutoNoToll + cityToll;
//                }
//                if (destination.getAreatype().equals(AreaTypeGermany.CORE_CITY)) {
//                    costAutoNoToll = costAutoNoToll + cityToll;
//                }
//                double impedanceAutoNoToll = costAutoNoToll / (vot) + autoNoTollTotalTime_hr.getValueAt(origin.getId(), destination.getId());
//                double utilityAutoNoToll_trip = b_impedance * Math.exp(alpha_impedance * impedanceAutoNoToll * 60);
//                utilityAutoNoToll.setValueAt(origin.getId(), destination.getId(), (float) utilityAutoNoToll_trip);
//
//                double costBus = costsPerKm.getStringIndexedValueAt("alpha", ModeGermany.BUS.name()) * Math.pow(busDistance_km.getValueAt(origin.getId(), destination.getId()), costsPerKm.getStringIndexedValueAt("beta", ModeGermany.BUS.name())) * busDistance_km.getValueAt(origin.getId(), destination.getId());
//                double impedanceBus = costBus / (vot) + busTotalTime_hr.getValueAt(origin.getId(), destination.getId());
//                double utilityBus_trip = b_impedance * Math.exp(alpha_impedance * impedanceBus * 60);
//                utilityBus.setValueAt(origin.getId(), destination.getId(), (float) utilityBus_trip);
//
//                double costRail = costsPerKm.getStringIndexedValueAt("alpha", ModeGermany.RAIL.name()) * Math.pow(railDistance_km.getValueAt(origin.getId(), destination.getId()), costsPerKm.getStringIndexedValueAt("beta", ModeGermany.RAIL.name())) * railDistance_km.getValueAt(origin.getId(), destination.getId());
//                double impedanceRail = costRail / (vot) + railTotalTime_hr.getValueAt(origin.getId(), destination.getId());
//                double utilityRail_trip = b_impedance * Math.exp(alpha_impedance * impedanceRail * 60);
//                utilityRail.setValueAt(origin.getId(), destination.getId(), (float) utilityRail_trip);
//
//                if (shuttleBusCostBase > 10 && railShuttleDistance_km.getValueAt(origin.getId(), destination.getId()) == 0) {
//                    utilityRailShuttle.setValueAt(origin.getId(), destination.getId(), (float) Double.NEGATIVE_INFINITY);
//                } else {
//                    double costRailShuttle = costsPerKm.getStringIndexedValueAt("alpha", ModeGermany.RAIL_SHUTTLE.name()) * Math.pow(railShuttleDistance_km.getValueAt(origin.getId(), destination.getId()), costsPerKm.getStringIndexedValueAt("beta", ModeGermany.RAIL_SHUTTLE.name())) * railShuttleDistance_km.getValueAt(origin.getId(), destination.getId());
//                    double costRailShuttleAccess = railShuttleAccessDistance_km.getValueAt(origin.getId(), destination.getId()) * shuttleBusCostPerKm + shuttleBusCostBase;
//                    double costRailShuttleEgress = railShuttleEgressDistance_km.getValueAt(origin.getId(), destination.getId()) * shuttleBusCostPerKm + shuttleBusCostBase;
//                    if (subsidyForRural) {
//                        if (origin.getAreatype().equals(AreaTypeGermany.RURAL) || origin.getAreatype().equals(AreaTypeGermany.TOWN)) {
//                            costRailShuttleAccess = 0.00;
//                        }
//                        if (destination.getAreatype().equals(AreaTypeGermany.RURAL) || destination.getAreatype().equals(AreaTypeGermany.TOWN)) {
//                            costRailShuttleEgress = 0.00;
//                        }
//                    }
//                    costRailShuttle = costRailShuttle + costRailShuttleAccess + costRailShuttleEgress;
//                    double impedanceRailShuttle = costRailShuttle / (vot) + railShuttleTotalTime_hr.getValueAt(origin.getId(), destination.getId());
//                    double utilityRailShuttle_trip = b_impedance * Math.exp(alpha_impedance * impedanceRailShuttle * 60);
//                    utilityRailShuttle.setValueAt(origin.getId(), destination.getId(), (float) utilityRailShuttle_trip);
//                }
//
//                logger.info("Logsum matrix loaded: " + counterLogsumMatrix.get());
//                counterLogsumMatrix.getAndIncrement();
//            }
//        }
//
//        System.out.println("Stop and check");

    }


    @Override
    public void run(DataSet dataSet, int nThreads) {

        ArrayList<LongDistanceTrip> trips = dataSet.getTripsofPotentialTravellers();
        logger.info("Running logsum-based accessibility calculation for " + trips.size() + " trips/travellers");
        AtomicInteger counterTrip = new AtomicInteger(0);

//        for (int testTrip = 1; testTrip <= 60; testTrip ++){
//            LongDistanceTripGermany trip = (LongDistanceTripGermany) trips.get(testTrip);
//            calculateLogsums(dataSet, trip);
//            logger.info("Trips logsum calculated: " + counterTrip.get());
//            counterTrip.getAndIncrement();
//        }

        trips.parallelStream().forEach(tripFromArray -> {
            LongDistanceTripGermany trip = (LongDistanceTripGermany) tripFromArray;
            calculateLogsums(dataSet, trip);
            logger.info("Trips logsum calculated: " + counterTrip.get());
            counterTrip.getAndIncrement();
        });
        logger.info("Finished individual-based logsum calculator");
    }

    private void calculateLogsums(DataSet dataSet, LongDistanceTripGermany t) {

        int[] zones = dataSet.getZones().keySet().stream().mapToInt(u -> u).toArray(); //select all the zones
        double[] logsumsByZones = Arrays.stream(zones)
                //calculate logsums for each destination
                .mapToDouble(a -> Math.exp(calculateLogsumsByZone(t, a))).toArray();
        double logsumsAllZones = Arrays.stream(logsumsByZones).sum();

        Map<String, Float> attributes = t.getAdditionalAttributes();
        attributes.put("logsums_accessibility", (float) logsumsAllZones);
    }

    private double calculateLogsumsByZone(LongDistanceTripGermany t, int zones) {
        ZoneGermany destinations = (ZoneGermany) dataSet.getZones().get(zones);

        LongDistanceTripGermany tempTrip = t;
        tempTrip.setDestZone(destinations);
        tempTrip.setDestZoneType(destinations.getZoneType());

        int pop = destinations.getPopulation();
        //double distance = dataSet.getDistanceMatrix().get(ModeGermany.AUTO).getValueAt(tempTrip.getOrigZone().getId(), destinations.getId()) / 3600;

        double[] utilities = new double[ModeGermany.values().length];
        double[] expUtilities = new double[ModeGermany.values().length];

        double logsumsByZones;

        if (!destinations.getZoneType().equals(ZoneTypeGermany.EXTOVERSEAS) && !tempTrip.getTripState().equals(TypeGermany.AWAY)) {
            if (destinations.getZoneType().equals(ZoneTypeGermany.GERMANY)) {
                utilities = Arrays.stream(ModeGermany.values()).mapToDouble(m -> mcDomesticModel.calculateUtilityFromGermany(tempTrip, m)).toArray();

            } else if (destinations.getZoneType().equals(ZoneTypeGermany.EXTEU)) {
                utilities = Arrays.stream(ModeGermany.values()).mapToDouble(m -> mcEuropeModel.calculateUtilityForEurope(tempTrip, m)).toArray();
            }

            expUtilities[0] = Math.exp(utilities[0]);
            expUtilities[1] = Math.exp(utilities[1]);
            expUtilities[2] = Math.exp(Double.NEGATIVE_INFINITY);
            expUtilities[3] = Math.exp(utilities[3]);
            expUtilities[4] = Math.exp(utilities[4]);
            expUtilities[5] = Math.exp(utilities[5]);

            double utilityNestAuto;
            utilityNestAuto =
                    Math.log(Math.exp(utilities[0] * NESTING_COEFFICIENT_AUTO_MODES) + Math.exp(utilities[5] * NESTING_COEFFICIENT_AUTO_MODES)) / NESTING_COEFFICIENT_AUTO_MODES;
            expUtilities[0] = Math.exp(utilityNestAuto);
            expUtilities[5] = Math.exp(utilityNestAuto);

            double utilityNestRail;

            utilityNestRail =
                    Math.log(Math.exp(utilities[2] * NESTING_COEFFICIENT_RAIL_MODES) + Math.exp(utilities[4] * NESTING_COEFFICIENT_RAIL_MODES)) / NESTING_COEFFICIENT_RAIL_MODES;
            expUtilities[2] = Math.exp(utilityNestRail);
            expUtilities[4] = Math.exp(utilityNestRail);

            logsumsByZones = Math.log(expUtilities[0] + expUtilities[1] + expUtilities[2] + expUtilities[3]);
            logsumsByZones = logsumsByZones * Math.pow(pop, WEIGHT_OF_ATTRACTION);
        } else {
            logsumsByZones = 0;
        }
        return logsumsByZones;
    }

    private static Matrix sumMatrices(List<Matrix> matrices) {

        Matrix sumMatrix = new Matrix(matrices.get(0).getRowCount(), matrices.get(0).getColumnCount());
        sumMatrix.setExternalNumbers(matrices.get(0).getExternalNumbers());
        for (Matrix matrix : matrices) {
            for (int orig : sumMatrix.getExternalRowNumbers()) {
                for (int dest : sumMatrix.getExternalColumnNumbers()) {
                    float current = sumMatrix.getValueAt(orig, dest);
                    current += matrix.getValueAt(orig, dest);
                    sumMatrix.setValueAt(orig, dest, current);
                }
            }

        }
        return sumMatrix;
    }

}
