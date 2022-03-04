package de.tum.bgu.msm.longDistance.modeChoice;

//import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.common.datafile.TableDataSet;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.trips.*;

import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneTypeOntario;
import org.json.simple.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 * Created by carlloga on 4/26/2017.
 */
public class IntModeChoice {

    private static Logger logger = Logger.getLogger(IntModeChoice.class);

    private DataSet dataSet;

    private TableDataSet mcIntOutboundCoefficients;
    private TableDataSet mcIntInboundCoefficients;

    private boolean calibration;
    private Map<Purpose, Map<Mode, Double>> calibrationMatrixOutbound;
    private Map<Purpose, Map<Mode, Double>> calibrationMatrixInbound;





    public IntModeChoice(JSONObject prop) {
        //this.rb = rb;

       // mcIntOutboundCoefficients = Util.readCSVfile(rb.getString("mc.int.outbound.coefs"));
        mcIntOutboundCoefficients = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"mode_choice.international.outbound.coef_file"));
        mcIntOutboundCoefficients.buildStringIndex(1);

        mcIntInboundCoefficients = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"mode_choice.international.outbound.coef_file"));
        mcIntInboundCoefficients.buildStringIndex(1);


        calibration = JsonUtilMto.getBooleanProp(prop,"mode_choice.calibration");


        logger.info("International MC set up");

    }


    public void loadIntModeChoice(DataSet dataSet){
        this.dataSet = dataSet;
        calibrationMatrixOutbound = new HashMap<>();
        calibrationMatrixInbound = new HashMap<>();
        //fill map
        for(Purpose purpose : PurposeOntario.values()){
            calibrationMatrixOutbound.put(purpose, new HashMap<>());
            calibrationMatrixInbound.put(purpose, new HashMap<>());
            for (Mode mode : ModeOntario.values()){
                calibrationMatrixOutbound.get(purpose).put(mode, 0.);
                calibrationMatrixInbound.put(purpose, new HashMap<>());
            }
        }


        logger.info("International MC loaded");
    }

    public Mode selectMode(LongDistanceTripOntario trip){

        double[] expUtilities;

        Mode[] modes = ModeOntario.values();

        if(trip.getOrigZone().getZoneType().equals(ZoneTypeOntario.ONTARIO) || trip.getOrigZone().getZoneType().equals(ZoneTypeOntario.EXTCANADA)){
            expUtilities = Arrays.stream(modes)
                    //calculate exp(Ui) for each destination
                    .mapToDouble(m -> Math.exp(calculateUtilityFromCanada(trip, m, trip.getDestCombinedZoneId()))).toArray();
        } else {
            expUtilities = Arrays.stream(modes)
                    //calculate exp(Ui) for each destination
                    .mapToDouble(m -> Math.exp(calculateUtilityToCanada(trip, m, trip.getDestCombinedZoneId()))).toArray();
        }


        //choose one destination, weighted at random by the probabilities
        //return new EnumeratedIntegerDistribution(modes, expUtilities).sample();
        return (Mode) Util.select(expUtilities, modes);


    }

    public double calculateUtilityToCanada(LongDistanceTripOntario trip, Mode m, int destination) {

        double utility;
        String tripPurpose =trip.getTripPurpose().toString().toLowerCase();
        String column = m.toString().toLowerCase() + "." + tripPurpose;
        String tripState = trip.getTripState().toString().toLowerCase();

        //trip-related variables
        int party = trip.getAdultsHhTravelPartySize() + trip.getKidsHhTravelPartySize() + trip.getNonHhTravelPartySize();

        int overnight = 1;
        if (tripState.equals("daytrip")){
            overnight = 0;
        }

        int origin = trip.getOrigZone().getCombinedZoneId();



        //zone-related variables

        double time = dataSet.getTravelTimeMatrix().get(m).getValueAt(origin, destination);
        double price = dataSet.getPriceMatrix().get(m).getValueAt(origin, destination);
        double frequency = dataSet.getFrequencyMatrix().get(m).getValueAt(origin, destination);

        double vot= mcIntInboundCoefficients.getStringIndexedValueAt("vot", column);


        double impedance = 0;
        if (vot != 0){
            impedance = price/(vot/60) + time;
        }


        //todo solve intrazonal times
        if (origin==destination){
            if (m.equals(ModeOntario.AUTO)) {
                time = 60;
                price = 20;
            }
        }

        //getCoefficients
        double b_intercept = mcIntInboundCoefficients.getStringIndexedValueAt("intercept", column);
        double b_frequency= mcIntInboundCoefficients.getStringIndexedValueAt("frequency", column);
        double b_price= mcIntInboundCoefficients.getStringIndexedValueAt("price", column);
        double b_time= mcIntInboundCoefficients.getStringIndexedValueAt("time", column);
        double b_overnight= mcIntInboundCoefficients.getStringIndexedValueAt("overnight", column);
        double b_party= mcIntInboundCoefficients.getStringIndexedValueAt("party", column);
        double b_impedance= mcIntInboundCoefficients.getStringIndexedValueAt("impedance", column);
        double beta_time = mcIntInboundCoefficients.getStringIndexedValueAt("beta_time", column);
        double k_calibration = mcIntInboundCoefficients.getStringIndexedValueAt("k_calibration", column);

        //calibration factor update during runtime
        if (calibration) {
            k_calibration = calibrationMatrixInbound.get(trip.getTripPurpose()).get(trip.getMode());
        }


                utility = b_intercept + k_calibration +
                        b_frequency*frequency +
                b_price * price +
                b_time * time +
                b_overnight * overnight +
                b_party * party +
                b_impedance * Math.exp(beta_time*impedance);


        if (time < 0 ) utility = Double.NEGATIVE_INFINITY;


        return utility;
    }

    public double calculateUtilityFromCanada(LongDistanceTripOntario trip, Mode m, int destination){

        double utility;
        String tripPurpose =trip.getTripPurpose().toString().toLowerCase();
        String column = m.toString().toLowerCase() + "." + tripPurpose;
        String tripState = trip.getTripState().toString().toLowerCase();

        //trip-related variables
        int party = trip.getAdultsHhTravelPartySize() + trip.getKidsHhTravelPartySize() + trip.getNonHhTravelPartySize();

        int overnight = 1;
        if (tripState.equals("daytrip")){
            overnight = 0;
        }

        int origin = trip.getOrigZone().getCombinedZoneId();


        //zone-related variables

        double time = dataSet.getTravelTimeMatrix().get(m).getValueAt(origin, destination);
        double price = dataSet.getPriceMatrix().get(m).getValueAt(origin, destination);
        double frequency = dataSet.getFrequencyMatrix().get(m).getValueAt(origin, destination);

        double vot= mcIntOutboundCoefficients.getStringIndexedValueAt("vot", column);

        double impedance = 0;
        if (vot != 0){
            impedance = price/(vot/60) + time;
        }


        //todo solve intrazonal times
        if (origin==destination){
            if (m.equals(ModeOntario.AUTO)) {
                time = 60;
                price = 20;
            }
        }

        //getCoefficients
        double b_intercept = mcIntOutboundCoefficients.getStringIndexedValueAt("intercept", column);
        double b_frequency= mcIntOutboundCoefficients.getStringIndexedValueAt("frequency", column);
        double b_price= mcIntOutboundCoefficients.getStringIndexedValueAt("price", column);
        double b_time= mcIntOutboundCoefficients.getStringIndexedValueAt("time", column);
        double b_overnight= mcIntOutboundCoefficients.getStringIndexedValueAt("overnight", column);
        double b_party= mcIntOutboundCoefficients.getStringIndexedValueAt("party", column);
        double b_impedance= mcIntOutboundCoefficients.getStringIndexedValueAt("impedance", column);
        double beta_time = mcIntOutboundCoefficients.getStringIndexedValueAt("beta_time", column);

        double k_calibration = mcIntOutboundCoefficients.getStringIndexedValueAt("k_calibration", column);

        //calibration during runtime
        if (calibration) {
            k_calibration = calibrationMatrixOutbound.get(trip.getTripPurpose()).get(trip.getMode());
        }

        utility = b_intercept + k_calibration +
                b_frequency*frequency +
                b_price * price +
                b_time * time +
                b_overnight * overnight +
                b_party * party +
                b_impedance * Math.exp(beta_time*impedance);


        if (time < 0 ) utility = Double.NEGATIVE_INFINITY;


        return utility;

    }

    public Map<Purpose, Map<Mode, Double>> getCalibrationMatrixOutbound() {
        return calibrationMatrixOutbound;
    }

    public Map<Purpose, Map<Mode, Double>> getCalibrationMatrixInbound() {
        return calibrationMatrixInbound;
    }


    public void updateCalibrationOutbound(Map<Purpose, Map<Mode, Double>> updatedMatrix) {

        for(Purpose purpose : PurposeOntario.values()){
            for (Mode mode : ModeOntario.values()){
                double newValue = this.calibrationMatrixOutbound.get(purpose).get(mode) + updatedMatrix.get(purpose).get(mode);
                calibrationMatrixOutbound.get(purpose).put(mode, newValue);
            }
        }

    }

    public void updateCalibrationInbound(Map<Purpose, Map<Mode, Double>> updatedMatrix) {

        for(Purpose purpose : PurposeOntario.values()){
            for (Mode mode : ModeOntario.values()){
                double newValue = this.calibrationMatrixInbound.get(purpose).get(mode) + updatedMatrix.get(purpose).get(mode);
                calibrationMatrixInbound.get(purpose).put(mode, newValue);
            }
        }
    }



    public float getInternationalModalTravelTime(LongDistanceTripOntario trip){
        if (trip.getOrigZone().getZoneType().equals(ZoneTypeOntario.EXTOVERSEAS) || trip.getDestZoneType().equals(ZoneTypeOntario.EXTOVERSEAS) ){
            return -1.f;
        } else {
            return dataSet.getTravelTimeMatrix().get(trip.getMode()).getValueAt(trip.getOrigZone().getCombinedZoneId(), trip.getDestCombinedZoneId());
        }

    }
}
