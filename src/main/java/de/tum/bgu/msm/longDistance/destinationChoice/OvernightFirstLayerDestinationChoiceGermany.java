package de.tum.bgu.msm.longDistance.destinationChoice;

//import com.pb.common.datafile.TableDataSet;
//import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.common.datafile.TableDataSet;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.sp.EconomicStatus;
import de.tum.bgu.msm.longDistance.data.sp.HouseholdGermany;
import de.tum.bgu.msm.longDistance.data.sp.PersonGermany;
import de.tum.bgu.msm.longDistance.data.trips.*;
import de.tum.bgu.msm.longDistance.data.zoneSystem.AreaTypeGermany;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneType;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneTypeGermany;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneTypeOntario;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Germany wide travel demand model
 * Class to select the first layer of destinations(DE, EU, Overseas) of overnight trips
 * Author: Wei-Chieh Huang, Technical University of Munich (TUM), ana.moreno@tum.de
 * Date: 23 March 2021
 * Version 1
 * Adapted from Destination Choice Model from Joe, Created by Joe on 26/10/2016.
 */


public class OvernightFirstLayerDestinationChoiceGermany implements DestinationChoiceModule{

    private ResourceBundle rb;
    private static Logger logger = Logger.getLogger(OvernightFirstLayerDestinationChoiceGermany.class);
    private TableDataSet coefficients;
    private Map<Purpose, Map<ZoneType, Double>> calibrationOvernightFirstLayerDcMatrix;
    private int[] destinations;
    private DataSet dataSet;
    private boolean calibrationOvernightFirstLayerDc;

    public OvernightFirstLayerDestinationChoiceGermany(JSONObject prop, String inputFolder) {
        coefficients = Util.readCSVfile(inputFolder + JsonUtilMto.getStringProp(prop, "destination_choice.overnightFirstLayer.coef_file"));
        coefficients.buildStringIndex(1);
        this.calibrationOvernightFirstLayerDcMatrix = new HashMap<>();
        calibrationOvernightFirstLayerDc = JsonUtilMto.getBooleanProp(prop,"destination_choice.calibration.overnightFirstLayer");
        logger.info("Overnight First Layer DC set up");
    }

    @Override
    public void load(DataSet dataSet) {

        this.dataSet = dataSet;

        for(Purpose purpose : PurposeGermany.values()){
            this.calibrationOvernightFirstLayerDcMatrix.put(purpose, new HashMap<>());
            for (ZoneType zoneType : ZoneTypeGermany.values()){
                this.calibrationOvernightFirstLayerDcMatrix.get(purpose).putIfAbsent(zoneType, 0.0);
            }
        }
        logger.info("Overnight First Layer DC loaded");

    }

    @Override
    public int selectDestination(LongDistanceTrip trip) {
        return 0;
    }

    public ZoneTypeGermany selectFirstLayerDestination(LongDistanceTrip trip) {

        //initialize utility vectors
        double[] expUtilities = new double[ZoneTypeGermany.values().length];
        ZoneTypeGermany selectedZoneType = null;

        //calculate exp(Ui) for each zone type
        expUtilities = Arrays.stream(ZoneTypeGermany.values()).mapToDouble(z -> Math.exp(calculateUtility(trip, z))).toArray();

        double probability_denominator = Arrays.stream(expUtilities).sum();
        double[] probabilities = Arrays.stream(expUtilities).map(u -> u / probability_denominator).toArray();

        selectedZoneType = (ZoneTypeGermany) Util.selectGermany(probabilities, ZoneTypeGermany.values());
        return selectedZoneType;

    }

    private double calculateUtility(LongDistanceTrip t, ZoneTypeGermany z) {

        LongDistanceTripGermany trip = (LongDistanceTripGermany) t;

        double utility;
        String tripPurpose = trip.getTripPurpose().toString().toLowerCase();
        String zoneType = z.toString();
        String column = zoneType + "." + tripPurpose;

        //zone-related variables
        PersonGermany pers = (PersonGermany) dataSet.getPersons().get(trip.getTravellerId());
        HouseholdGermany hh = pers.getHousehold();

        //getCoefficients
        double b_intercept = coefficients.getStringIndexedValueAt("(intercept)", column);
        double b_mediumEconomicStatus = coefficients.getStringIndexedValueAt("economicStatusMedium", column);
        double b_highEconomicStatus = coefficients.getStringIndexedValueAt("economicStatusHigh", column);
        double b_veryHighEconomicStatus = coefficients.getStringIndexedValueAt("economicStatusVeryHigh", column);
        double b_adult18_60 = coefficients.getStringIndexedValueAt("isBetween18and60", column);
        double b_olderThan60 = coefficients.getStringIndexedValueAt("olderThan60", column);
        double b_male = coefficients.getStringIndexedValueAt("male", column);
        double b_employed = coefficients.getStringIndexedValueAt("employed", column);
        double b_student = coefficients.getStringIndexedValueAt("student", column);
        double b_hhSize = coefficients.getStringIndexedValueAt("householdSize", column);
        double b_cityOrMidSizeCity = coefficients.getStringIndexedValueAt("city_mediumCity", column);
        double b_ruralOrTown = coefficients.getStringIndexedValueAt("town_rural", column);
        double k_calibration = coefficients.getStringIndexedValueAt("k_calibration", column);

        if (calibrationOvernightFirstLayerDc) k_calibration = k_calibration + calibrationOvernightFirstLayerDcMatrix.get(trip.getTripPurpose()).get(z);

        utility = b_intercept +
                b_mediumEconomicStatus * Boolean.compare(hh.getEconomicStatus().equals(EconomicStatus.MEDIUM), false) +
                b_highEconomicStatus * Boolean.compare(hh.getEconomicStatus().equals(EconomicStatus.HIGH), false) +
                b_veryHighEconomicStatus * Boolean.compare(hh.getEconomicStatus().equals(EconomicStatus.VERYHIGH), false) +
                b_adult18_60 * Boolean.compare(pers.getAge() < 60 && pers.getAge() >= 18, false) +
                b_olderThan60 * Boolean.compare(pers.getAge() >= 60, false) +
                b_male * Boolean.compare(pers.isMale(), false) +
                b_employed * Boolean.compare(pers.isEmployed(), false) +
                b_student * Boolean.compare(pers.isStudent(), false) +
                b_hhSize * hh.getHhSize() +
                b_cityOrMidSizeCity * Boolean.compare(hh.getZone().getAreatype().equals(AreaTypeGermany.CITY), false) +
                b_cityOrMidSizeCity * Boolean.compare(hh.getZone().getAreatype().equals(AreaTypeGermany.MEDIUM_SIZED_CITY), false) +
                b_ruralOrTown * Boolean.compare(hh.getZone().getAreatype().equals(AreaTypeGermany.RURAL), false) +
                b_ruralOrTown * Boolean.compare(hh.getZone().getAreatype().equals(AreaTypeGermany.TOWN), false)+
                k_calibration
        ;

        return utility;
    }

    public Map<Purpose, Map<ZoneType, Double>> getDomesticDcCalibration() {
        return calibrationOvernightFirstLayerDcMatrix;
    }

    public void updateOvernightFirstLayerDcCalibration(Map<Purpose, Map<ZoneType, Double>> updatedMatrix) {

        for (Purpose purpose : PurposeGermany.values()){
            for (ZoneType zoneType : ZoneTypeGermany.values()){
                double newValue = calibrationOvernightFirstLayerDcMatrix.get(purpose).get(zoneType) + updatedMatrix.get(purpose).get(zoneType);
                calibrationOvernightFirstLayerDcMatrix.get(purpose).put(zoneType, newValue);
                System.out.println("k-factor: " + purpose + "\t" + zoneType + "\t" + calibrationOvernightFirstLayerDcMatrix.get(purpose).get(zoneType));
            }
        }
    }
}
