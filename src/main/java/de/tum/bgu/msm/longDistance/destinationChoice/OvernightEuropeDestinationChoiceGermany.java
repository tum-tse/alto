package de.tum.bgu.msm.longDistance.destinationChoice;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.trips.*;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneGermany;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneType;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneTypeGermany;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class OvernightEuropeDestinationChoiceGermany implements DestinationChoiceModule{

    private ResourceBundle rb;
    private static Logger logger = Logger.getLogger(OvernightEuropeDestinationChoiceGermany.class);
    public static int longDistanceThreshold;
    private TableDataSet coefficients;
    protected Matrix autoDist;
    private boolean calibration;
    private Map<Purpose, Map<ZoneType, Double>> calibrationOvernightEuropeDcMatrix;
    private int[] destinations;
    private DataSet dataSet;
    private boolean calibrationOvernightEuropeDc;

    public OvernightEuropeDestinationChoiceGermany(JSONObject prop, String inputFolder) {
        coefficients = Util.readCSVfile(inputFolder + JsonUtilMto.getStringProp(prop, "destination_choice.overnightEurope.coef_file"));
        coefficients.buildStringIndex(1);

        //calibration = JsonUtilMto.getBooleanProp(prop, "destination_choice.calibration");
        this.calibrationOvernightEuropeDcMatrix = new HashMap<>();
        calibrationOvernightEuropeDc = JsonUtilMto.getBooleanProp(prop,"destination_choice.calibration_overnightEurope");
        logger.info("Overnight Europe DC set up");
    }

    @Override
    public void load(DataSet dataSet) {

        this.dataSet = dataSet;

        for(Purpose purpose : PurposeGermany.values()){
            this.calibrationOvernightEuropeDcMatrix.put(purpose, new HashMap<>());
            for (ZoneType zoneType : ZoneTypeGermany.values()){
                this.calibrationOvernightEuropeDcMatrix.get(purpose).putIfAbsent(zoneType, 1.0);
            }
        }
        logger.info("Overnight Europe DC loaded");

    }

    @Override
    public int selectDestination(LongDistanceTrip trip) {
        LongDistanceTripGermany t = (LongDistanceTripGermany) trip;
        return selectDestination(t, dataSet);
    }

    //given a trip, calculate the utility of each destination
    public int selectDestination(LongDistanceTripGermany trip, DataSet dataSet) {

        int[] alternatives = dataSet.getZones().keySet().stream().mapToInt(u ->u).toArray(); //select all the zones

        double[] expUtilities = Arrays.stream(alternatives)
                //calculate exp(Ui) for each destination
                .mapToDouble(a -> Math.exp(calculateUtility(trip, a))).toArray();
        //calculate the probability for each trip, based on the destination utilities
        double probability_denominator = Arrays.stream(expUtilities).sum();

        //calculate the probability for each trip, based on the destination utilities
        double[] probabilities = Arrays.stream(expUtilities).map(u -> u / probability_denominator).toArray();

        //choose one destination, weighted at random by the probabilities
        return Util.selectGermany(probabilities, alternatives);
        //return new EnumeratedIntegerDistribution(alternatives, probabilities).sample();

    }

    private double calculateUtility(LongDistanceTripGermany trip, int destination) {
        // Method to calculate utility of all possible destinations for LongDistanceTrip trip

        int origin = trip.getOrigZone().getId();
        float distance = autoDist.getValueAt(origin, destination) / 1000; //to convert meters to km
        ZoneGermany destinationZone = (ZoneGermany) dataSet.getZones().get(destination);
        boolean populatedZone = !destinationZone.getEmptyZone();
        boolean isEurope = destinationZone.getZoneType().equals(ZoneTypeGermany.EXTEU);

        if (distance > longDistanceThreshold && populatedZone && isEurope) {


            double population = destinationZone.getPopulation();
            if(population<=0){
                population=0;
            }

            double employment = destinationZone.getEmployment();
            if(employment<=0){
                employment=0;
            }

            double touristsAtHotel = destinationZone.getTouristsAtHotel();
            if(touristsAtHotel<=0){
                touristsAtHotel=0;
            }

            Purpose tripPurpose = trip.getTripPurpose();
            TypeGermany tripState = (TypeGermany) trip.getTripState();
            //Coefficients
            String coefficientColumn = tripState + "." + tripPurpose;
            double b_distance_log = coefficients.getStringIndexedValueAt("log_distance", coefficientColumn);
            double b_popEmployment = coefficients.getStringIndexedValueAt("popEmployment", coefficientColumn);
            double b_touristAtHotel = coefficients.getStringIndexedValueAt("hotel", coefficientColumn);
            double k_calibration = coefficients.getStringIndexedValueAt("k_calibration", coefficientColumn);

            //log conversions
            double log_distance = distance > 0 ? Math.log10(distance) : 0;

            if (calibrationOvernightEuropeDc) {
                k_calibration = k_calibration * calibrationOvernightEuropeDcMatrix.get(tripPurpose).get(tripState);
            }

            double u =
                    b_distance_log * k_calibration * log_distance +
                            b_touristAtHotel * touristsAtHotel / 1000 +  //touristsAtHotel in thousands
                            b_popEmployment * (population + employment) / 1000000;

            return u;
        } else {
            return Double.NEGATIVE_INFINITY;
        }
    }

    public Map<Purpose, Map<ZoneType, Double>> getOvernightEuropeDcCalibration() {
        return calibrationOvernightEuropeDcMatrix;
    }

    public void updateOvernightEuropeDcCalibration(Map<Purpose, Map<Type, Double>> updatedMatrix) {

        for (Purpose purpose : PurposeGermany.values()){
            for (ZoneType zoneType : ZoneTypeGermany.values()){
                double newValue = calibrationOvernightEuropeDcMatrix.get(purpose).get(zoneType) * updatedMatrix.get(purpose).get(zoneType);
                calibrationOvernightEuropeDcMatrix.get(purpose).put(zoneType, newValue);
                System.out.println("k-factor: " + purpose + "\t" + zoneType + "\t" + calibrationOvernightEuropeDcMatrix.get(purpose).get(zoneType));
            }
        }
    }

}
