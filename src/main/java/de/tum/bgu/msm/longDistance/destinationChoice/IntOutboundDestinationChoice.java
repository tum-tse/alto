package de.tum.bgu.msm.longDistance.destinationChoice;

//import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.common.datafile.TableDataSet;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.LDModelOntario;
import de.tum.bgu.msm.longDistance.data.trips.*;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneOntario;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneTypeOntario;
import de.tum.bgu.msm.longDistance.io.reader.ZoneReader;
import de.tum.bgu.msm.longDistance.modeChoice.IntModeChoice;
import de.tum.bgu.msm.longDistance.data.zoneSystem.Zone;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.*;

/**
 * Created by carlloga on 4/12/2017.
 */
public class IntOutboundDestinationChoice implements DestinationChoiceModule {

    private ResourceBundle rb;
    private static Logger logger = Logger.getLogger(DomesticDestinationChoice.class);
    private TableDataSet destCombinedZones;
    private TableDataSet coefficients;
    private int[] alternativesUS;
    private int[] alternativesOS;

    private TableDataSet origCombinedZones;
    private ZoneReader ldData;

    private IntModeChoice internationalModeChoiceForLogsums;
    Map<Integer, Zone> externalOsMap = new HashMap<>();

    boolean calibration;
    private Map<Purpose, Double> calibrationV;
    private DataSet dataSet;


    public IntOutboundDestinationChoice(JSONObject prop) {

        internationalModeChoiceForLogsums = new IntModeChoice(prop);


        //this.rb = rb;
        //coefficients = Util.readCSVfile(rb.getString("dc.int.out.coefs"));
        coefficients = Util.readCSVfile(JsonUtilMto.getStringProp(prop, "destination_choice.international.outbound.coef_file"));
        coefficients.buildStringIndex(1);

        //load alternatives
        //destCombinedZones = Util.readCSVfile(rb.getString("dc.us.combined"));
        destCombinedZones = Util.readCSVfile(JsonUtilMto.getStringProp(prop, "destination_choice.international.outbound.alternatives_file"));
        destCombinedZones.buildIndex(1);
        alternativesUS = destCombinedZones.getColumnAsInt("combinedZone");

        //load alternatives (origins, to read accessibility to US of the zone)
        //origCombinedZones = Util.readCSVfile(rb.getString("dc.combined.zones"));
        origCombinedZones = Util.readCSVfile(JsonUtilMto.getStringProp(prop, "destination_choice.domestic.alternatives_file"));
        origCombinedZones.buildIndex(1);


        //calibration = ResourceUtil.getBooleanProperty(rb, "dc.calibration", false);
        calibration = JsonUtilMto.getBooleanProp(prop, "destination_choice.calibration");
        this.calibrationV = new HashMap<>();

        logger.info("International DC (outbound) set up");

    }

    public void load(DataSet dataSet) {

        this.dataSet = dataSet;

        internationalModeChoiceForLogsums.loadIntModeChoice(dataSet);

        dataSet.getExternalZones().forEach(zone -> {
            if (zone.getZoneType() == ZoneTypeOntario.EXTOVERSEAS) {
                externalOsMap.put(((ZoneOntario) zone).getCombinedZoneId(), zone);
            }
        });

        alternativesOS = new int[externalOsMap.size()];
        int index = 0;
        for (Integer id : externalOsMap.keySet()) {
            alternativesOS[index] = id;
            index++;
        }

        logger.info("International DC (outbound) loaded");
    }

    public int selectDestination(LongDistanceTrip t) {

        LongDistanceTripOntario trip = (LongDistanceTripOntario) t;
        int destination;
        //0 visit, 1 business and 2 leisure

        Purpose tripPurpose = trip.getTripPurpose();

        if (selectUs(trip, tripPurpose)) {

            double[] expUtilities = Arrays.stream(alternativesUS).mapToDouble(a -> Math.exp(calculateUsZoneUtility(trip, tripPurpose, a))).toArray();
            double probability_denominator = Arrays.stream(expUtilities).sum();

            double[] probabilities = Arrays.stream(expUtilities).map(u -> u / probability_denominator).toArray();

            //destination = new EnumeratedIntegerDistribution(alternativesUS, probabilities).sample();
            destination = Util.select(probabilities, alternativesUS);

        } else {

            double[] expUtilitiesOs = Arrays.stream(alternativesOS).mapToDouble(a -> calculateOsZoneUtility(a)).toArray();

            double probability_denominator = Arrays.stream(expUtilitiesOs).sum();

            double[] probabilities = Arrays.stream(expUtilitiesOs).map(u -> u / probability_denominator).toArray();

            //destination = new EnumeratedIntegerDistribution(alternativesOS, probabilities).sample();
            destination = Util.select(probabilities, alternativesOS);
        }

        return destination;
    }

    public ZoneTypeOntario getDestinationZoneType(int destinationZoneId) {
        //method to give the destination zone type from a destination

        if (externalOsMap.keySet().contains(destinationZoneId)) {
            return ZoneTypeOntario.EXTOVERSEAS;
        } else {
            return ZoneTypeOntario.EXTUS;
        }
    }

    public boolean selectUs(LongDistanceTripOntario trip, Purpose tripPurpose) {

        double utility;
        //binary choice model for US/OS (per purpose)
        if (trip.getOrigZone().getZoneType().equals(ZoneTypeOntario.ONTARIO)) {
            //trips from Ontario = use accessibility to get the probability
            double b_intercept = coefficients.getStringIndexedValueAt("isUs", tripPurpose.toString());
            double b_usAccess = coefficients.getStringIndexedValueAt("isUsAcc", tripPurpose.toString());

            double usAccess = origCombinedZones.getIndexedValueAt(trip.getOrigZone().getCombinedZoneId(), "usAccess");

            utility = Math.exp(b_intercept + b_usAccess * usAccess);
        } else {
            //if from external canada do not have accessibility to us in the choice model
            double b_intercept = coefficients.getStringIndexedValueAt("isUsExternal", tripPurpose.toString().toLowerCase());
            utility = Math.exp(b_intercept);
        }

        double probability = utility / (1 + utility);

        if (trip.getTripState().equals(TypeOntario.DAYTRIP)) {
            //daytrips are always to US
            return true;
        } else {
            if (LDModelOntario.rand.nextDouble() < probability) {
                return true;
            } else {
                return false;
            }
        }
    }


    private double calculateUsZoneUtility(LongDistanceTripOntario trip, Purpose tripPurpose, int destination) {

        //read coefficients

        Type tripState = trip.getTripState();

        double b_population = coefficients.getStringIndexedValueAt("population", tripPurpose.toString());
        double b_log_population = coefficients.getStringIndexedValueAt("log_population", tripPurpose.toString());
        double dtLogsum = coefficients.getStringIndexedValueAt("dtLogsum", tripPurpose.toString());
        double onLogsum = coefficients.getStringIndexedValueAt("onLogsum", tripPurpose.toString());

        double k_dtLogsum = coefficients.getStringIndexedValueAt("k_dtLogsum", tripPurpose.toString());
        double k_onLogsum = coefficients.getStringIndexedValueAt("k_onLogsum", tripPurpose.toString());

        if (calibration) {
            k_dtLogsum = calibrationV.getOrDefault(tripPurpose, 1.);
            k_onLogsum = k_dtLogsum;
        }


        double logsum = 0;
        Mode[] modes = ModeOntario.values();
        for (Mode m : modes) {
            logsum += Math.exp(internationalModeChoiceForLogsums.calculateUtilityFromCanada(trip, m, destination));
        }
        if (logsum == 0) {
            return Double.NEGATIVE_INFINITY;
            //deals with trips that logsum == 0 --> means that no mode is available
            //logger.info(trip.getOrigZone().getCombinedZoneId() + " to " + destination);
        } else {
            logsum = Math.log(logsum);
        }

        //read destination data
        double population = destCombinedZones.getIndexedValueAt(destination, "population");

        double log_population = population > 0 ? Math.log(destCombinedZones.getIndexedValueAt(destination, "population")) : 0;


        int overnight = 1;
        if (tripState.equals("daytrip")) {
            overnight = 0;
        }

        return b_population * population +
                b_log_population * log_population +
                dtLogsum * (1 - overnight) * logsum * k_dtLogsum +
                onLogsum * overnight * logsum * k_onLogsum;
    }


    private double calculateOsZoneUtility(int destination) {
        return ((ZoneOntario) externalOsMap.get(destination)).getStaticAttraction();
    }

    public void updateIntOutboundCalibrationV(Map<Purpose, Double> b_calibrationVector) {

        for (Purpose purpose : PurposeOntario.values()) {
            double newValue = calibrationV.get(purpose) * b_calibrationVector.get(purpose);
            calibrationV.put(purpose, newValue);
        }
    }

    public Map<Purpose, Double> getCalibrationV() {
        return calibrationV;
    }

    public TableDataSet getOrigCombinedZones() {
        return origCombinedZones;
    }
}
