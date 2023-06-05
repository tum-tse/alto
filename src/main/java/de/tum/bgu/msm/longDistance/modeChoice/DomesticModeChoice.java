package de.tum.bgu.msm.longDistance.modeChoice;

import de.tum.bgu.msm.common.datafile.TableDataSet;
import de.tum.bgu.msm.common.matrix.Matrix;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.sp.PersonOntario;
import de.tum.bgu.msm.longDistance.data.trips.*;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneTypeOntario;
import de.tum.bgu.msm.longDistance.destinationChoice.DomesticDestinationChoice;

import org.json.simple.JSONObject;
import org.apache.log4j.Logger;
import java.util.*;

import java.util.ResourceBundle;

/**
 * Created by carlloga on 15.03.2017.
 */
public class DomesticModeChoice {
    private static Logger logger = Logger.getLogger(DomesticDestinationChoice.class);

    ResourceBundle rb;

    private DataSet dataSet;

    private TableDataSet mcOntarioCoefficients;
    private TableDataSet mcExtCanadaCoefficients;
    private TableDataSet combinedZones;

    private boolean calibration;
    private Map<Purpose, Map<Mode, Double>> calibrationMatrix;
    private Map<Purpose, Map<Mode, Double>> calibrationMatrixVisitors;


    public DomesticModeChoice(JSONObject prop) {
        this.rb = rb;

        //mcOntarioCoefficients = Util.readCSVfile(rb.getString("mc.domestic.coefs"));
        mcOntarioCoefficients = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"mode_choice.domestic.ontarian.coef_file"));

        mcOntarioCoefficients.buildStringIndex(1);

        //mcExtCanadaCoefficients = Util.readCSVfile(rb.getString("mc.extcanada.coefs"));
        mcExtCanadaCoefficients = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"mode_choice.domestic.other_canadian.coef_file"));
        mcExtCanadaCoefficients.buildStringIndex(1);

        //taken from destination choice
        //combinedZones = Util.readCSVfile(rb.getString("dc.combined.zones"));
        combinedZones = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"destination_choice.domestic.alternatives_file"));
        combinedZones.buildIndex(1);

        //matrix names
//        travelTimeFileName = rb.getString("travel.time.combined");
//        priceFileName = rb.getString("price.combined");
//        transfersFileName = rb.getString("transfer.combined");
//        freqFileName = rb.getString("freq.combined");
//        lookUpName = rb.getString("skim.mode.choice.lookup");




        calibration = JsonUtilMto.getBooleanProp(prop,"mode_choice.calibration");


        logger.info("Domestic MC set up");

    }


    public void loadDomesticModeChoice(DataSet dataSet){
        this.dataSet = dataSet;
        calibrationMatrix = new HashMap<>();
        calibrationMatrixVisitors = new HashMap<>();
        //fill map
        for(Purpose purpose : PurposeOntario.values()){
            calibrationMatrixVisitors.put(purpose, new HashMap<>());
            calibrationMatrix.put(purpose, new HashMap<>());
            for (Mode mode : ModeOntario.values()){
                calibrationMatrixVisitors.get(purpose).put(mode, 0.);
                calibrationMatrix.put(purpose, new HashMap<>());
            }
        }
        logger.info("Domestic MC loaded");
    }



    public Mode selectModeDomestic(LongDistanceTripOntario trip) {

        double[] expUtilities;
        if (trip.getOrigZone().getZoneType().equals(ZoneTypeOntario.ONTARIO)) {
            //calculate exp(Ui) for each destination
            expUtilities = Arrays.stream(ModeOntario.values()).mapToDouble(m -> Math.exp(calculateUtilityFromOntario(trip, m, trip.getDestCombinedZoneId()))).toArray();
        } else {
            //calculate exp(Ui) for each destination
            expUtilities = Arrays.stream(ModeOntario.values()).mapToDouble(m -> Math.exp(calculateUtilityFromExtCanada(trip, m, trip.getDestCombinedZoneId()))).toArray();
        }
        double probability_denominator = Arrays.stream(expUtilities).sum();

        //if there is no access by any mode for the selected OD pair, just go by car
        if (probability_denominator == 0) {
            expUtilities[0] = 1;
        }

        //choose one destination, weighted at random by the probabilities
        return (Mode) Util.select(expUtilities, ModeOntario.values());
        //return new EnumeratedIntegerDistribution(modes, expUtilities).sample();

    }


    public double calculateUtilityFromExtCanada(LongDistanceTripOntario trip, Mode m, int destination) {

        double utility;
        String tripPurpose = trip.getTripPurpose().toString().toLowerCase();
        String column = m.toString().toLowerCase() + "." + tripPurpose;
        String tripState = trip.getTripState().toString().toLowerCase();

        //trip-related variables
        int party = trip.getAdultsHhTravelPartySize() + trip.getKidsHhTravelPartySize() + trip.getNonHhTravelPartySize();

        int overnight = 1;
        if (tripState.equals("daytrip")) {
            overnight = 0;
        }

        int origin = trip.getOrigZone().getCombinedZoneId();

        double interMetro = combinedZones.getIndexedValueAt(origin, "alt_is_metro")
                * combinedZones.getIndexedValueAt(destination, "alt_is_metro");
        double ruralRural = 0;
        if (combinedZones.getIndexedValueAt(origin, "alt_is_metro") == 0 && combinedZones.getIndexedValueAt(destination, "alt_is_metro") == 0) {
            ruralRural = 1;
        }

        double time = dataSet.getTravelTimeMatrix().get(m).getValueAt(origin, destination);
        double price = dataSet.getPriceMatrix().get(m).getValueAt(origin, destination);
        double frequency = dataSet.getFrequencyMatrix().get(m).getValueAt(origin, destination);

        double vot = mcExtCanadaCoefficients.getStringIndexedValueAt("vot", column);

        double impedance = 0;
        if (vot != 0) {
            impedance = price / (vot / 60) + time;
        }


        //todo solve intrazonal times
        if (origin == destination) {
            if (m.equals(ModeOntario.AUTO)) {
                time = 60;
                price = 20;
            }
        }

        double k_calibration = mcExtCanadaCoefficients.getStringIndexedValueAt("k_calibration", column);
        double b_intercept = mcExtCanadaCoefficients.getStringIndexedValueAt("intercept", column);
        double b_frequency = mcExtCanadaCoefficients.getStringIndexedValueAt("frequency", column);
        double b_price = mcExtCanadaCoefficients.getStringIndexedValueAt("price", column);
        double b_time = mcExtCanadaCoefficients.getStringIndexedValueAt("time", column);

        double b_interMetro = mcExtCanadaCoefficients.getStringIndexedValueAt("inter_metro", column);
        double b_ruralRural = mcExtCanadaCoefficients.getStringIndexedValueAt("rural_rural", column);

        double b_overnight = mcExtCanadaCoefficients.getStringIndexedValueAt("overnight", column);
        double b_party = mcExtCanadaCoefficients.getStringIndexedValueAt("party", column);
        double b_impedance = mcExtCanadaCoefficients.getStringIndexedValueAt("impedance", column);
        double alpha_impedance = mcExtCanadaCoefficients.getStringIndexedValueAt("alpha", column);


        if (calibration) k_calibration = calibrationMatrixVisitors.get(trip.getTripPurpose()).get(trip.getMode());

        utility = b_intercept + b_frequency * frequency +
                k_calibration +
                b_price * price +
                b_time * time +
//                b_incomeLow * incomeLow +
//                b_incomeHigh * incomeHigh +
//                b_young * young +
                b_interMetro * interMetro +
                b_ruralRural * ruralRural +
//                b_male * male +
//                b_educationUniv * educationUniv +
                b_overnight * overnight +
                b_party * party +
                b_impedance * Math.exp(alpha_impedance * impedance);


        if (time < 0) utility = Double.NEGATIVE_INFINITY;


        return utility;

    }


    public double calculateUtilityFromOntario(LongDistanceTripOntario trip, Mode m, int destination) {


        double utility;
        String tripPurpose = trip.getTripPurpose().toString().toLowerCase();
        String column = m.toString() + "." + tripPurpose;
        String tripState = trip.getTripState().toString().toLowerCase();

        //trip-related variables
        int party = trip.getAdultsHhTravelPartySize() + trip.getKidsHhTravelPartySize() + trip.getNonHhTravelPartySize();

        int overnight = 1;
        if (tripState.equals("daytrip")) {
            overnight = 0;
        }

        int origin = trip.getOrigZone().getCombinedZoneId();
        //int destination = trip.getDestCombinedZoneId();

        //zone-related variables

        double interMetro = combinedZones.getIndexedValueAt(origin, "alt_is_metro")
                * combinedZones.getIndexedValueAt(destination, "alt_is_metro");
        double ruralRural = 0;
        if (combinedZones.getIndexedValueAt(origin, "alt_is_metro") == 0 && combinedZones.getIndexedValueAt(destination, "alt_is_metro") == 0) {
            ruralRural = 1;
        }


        double time = dataSet.getTravelTimeMatrix().get(m).getValueAt(origin, destination);
        double price = dataSet.getPriceMatrix().get(m).getValueAt(origin, destination);
        double frequency = dataSet.getPriceMatrix().get(m).getValueAt(origin, destination);

        double vot = mcOntarioCoefficients.getStringIndexedValueAt("vot", column);

        double impedance = 0;
        if (vot != 0) {
            impedance = price / (vot / 60) + time;
        }


        //todo solve intrazonal times
        if (origin == destination) {
            if (m.equals(ModeOntario.AUTO)) {
                time = 60;
                price = 20;
            }
        }

        //person-related variables
        PersonOntario p = trip.getTraveller();

        double incomeLow = p.getIncome() <= 50000 ? 1 : 0;
        double incomeHigh = p.getIncome() >= 100000 ? 1 : 0;

        double young = p.getAge() < 25 ? 1 : 0;
        double female = p.getGender() == 'F' ? 1 : 0;

        double educationUniv = p.getEducation() > 5 ? 1 : 0;

        //getCoefficients
        double k_calibration = mcOntarioCoefficients.getStringIndexedValueAt("k_calibration", column);
        double b_intercept = mcOntarioCoefficients.getStringIndexedValueAt("intercept", column);
        double b_frequency = mcOntarioCoefficients.getStringIndexedValueAt("frequency", column);
        double b_price = mcOntarioCoefficients.getStringIndexedValueAt("price", column);
        double b_time = mcOntarioCoefficients.getStringIndexedValueAt("time", column);
        double b_incomeLow = mcOntarioCoefficients.getStringIndexedValueAt("income_low", column);
        double b_incomeHigh = mcOntarioCoefficients.getStringIndexedValueAt("income_high", column);
        double b_young = mcOntarioCoefficients.getStringIndexedValueAt("young", column);
        double b_interMetro = mcOntarioCoefficients.getStringIndexedValueAt("inter_metro", column);
        double b_ruralRural = mcOntarioCoefficients.getStringIndexedValueAt("rural_rural", column);
        double b_female = mcOntarioCoefficients.getStringIndexedValueAt("female", column);
        double b_educationUniv = mcOntarioCoefficients.getStringIndexedValueAt("education_univ", column);
        double b_overnight = mcOntarioCoefficients.getStringIndexedValueAt("overnight", column);
        double b_party = mcOntarioCoefficients.getStringIndexedValueAt("party", column);
        double b_impedance = mcOntarioCoefficients.getStringIndexedValueAt("impedance", column);
        double alpha_impedance = mcOntarioCoefficients.getStringIndexedValueAt("alpha", column);

        //this updates calibration factor from during-runtime calibration matrix
        if (calibration) k_calibration = calibrationMatrix.get(trip.getTripPurpose()).get(trip.getMode());

        utility = b_intercept + k_calibration +
                b_frequency * frequency +
                b_price * price +
                b_time * time +
                b_incomeLow * incomeLow +
                b_incomeHigh * incomeHigh +
                b_young * young +
                b_interMetro * interMetro +
                b_ruralRural * ruralRural +
                b_female * female +
                b_educationUniv * educationUniv +
                b_overnight * overnight +
                b_party * party +
                b_impedance * Math.exp(alpha_impedance * impedance);


        if (time < 0) utility = Double.NEGATIVE_INFINITY;

        return utility;

    }

    public void updateCalibrationDomestic(Map<Purpose, Map<Mode, Double>> updatedMatrix) {

        for(Purpose purpose : PurposeOntario.values()){
            for (Mode mode : ModeOntario.values()){
                double newValue = this.calibrationMatrix.get(purpose).get(mode) + updatedMatrix.get(purpose).get(mode);
                calibrationMatrix.get(purpose).put(mode, newValue);
            }
        }

    }

    public void updateCalibrationDomesticVisitors(Map<Purpose, Map<Mode, Double>> updatedMatrix) {

        for(Purpose purpose : PurposeOntario.values()){
            for (Mode mode : ModeOntario.values()){
                double newValue = this.calibrationMatrixVisitors.get(purpose).get(mode) + updatedMatrix.get(purpose).get(mode);
                calibrationMatrixVisitors.get(purpose).put(mode, newValue);
            }
        }
    }

    public Map<Purpose, Map<Mode, Double>> getCalibrationMatrixVisitors() {
        return calibrationMatrixVisitors;
    }

    public Map<Purpose, Map<Mode, Double>> getCalibrationMatrix() {
        return calibrationMatrix;
    }

    public float getDomesticModalTravelTime(LongDistanceTripOntario trip){
        if (trip.getOrigZone().getZoneType().equals(ZoneTypeOntario.EXTOVERSEAS) || trip.getDestZoneType().equals(ZoneTypeOntario.EXTOVERSEAS) ){
            return -1.f;
        } else {
            return dataSet.getTravelTimeMatrix().get(trip.getMode()).getValueAt(trip.getOrigZone().getCombinedZoneId(), trip.getDestCombinedZoneId());
        }
    }

}
