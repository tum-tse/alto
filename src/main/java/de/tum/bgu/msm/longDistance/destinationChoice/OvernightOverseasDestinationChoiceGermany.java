package de.tum.bgu.msm.longDistance.destinationChoice;

import de.tum.bgu.msm.common.datafile.TableDataSet;
import de.tum.bgu.msm.common.matrix.Matrix;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.trips.*;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneType;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneTypeGermany;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class OvernightOverseasDestinationChoiceGermany implements DestinationChoiceModule{

    private ResourceBundle rb;
    private static Logger logger = Logger.getLogger(OvernightOverseasDestinationChoiceGermany.class);
    public static int longDistanceThreshold;
    private TableDataSet proportionsByContinents;
    protected Matrix autoDist;
    private Map<Type, Map<ZoneType, Map<Purpose, Double>>> calibrationOvernightOverseasDcMatrix;
    private int[] destinations;
    private DataSet dataSet;
    private boolean calibrationOvernightOverseasDc;

    public OvernightOverseasDestinationChoiceGermany(JSONObject prop, String inputFolder) {
        proportionsByContinents = Util.readCSVfile(inputFolder + JsonUtilMto.getStringProp(prop, "destination_choice.overnightOverseas.coef_file"));
        proportionsByContinents.buildStringIndex(3);

        this.calibrationOvernightOverseasDcMatrix = new HashMap<>();
        calibrationOvernightOverseasDc = JsonUtilMto.getBooleanProp(prop,"destination_choice.calibration.overnightOverseas");
        logger.info("Overnight Overseas DC set up");
    }

    @Override
    public void load(DataSet dataSet) {

        this.dataSet = dataSet;

        this.calibrationOvernightOverseasDcMatrix.put(TypeGermany.OVERNIGHT, new HashMap<>());
        this.calibrationOvernightOverseasDcMatrix.get(TypeGermany.OVERNIGHT).putIfAbsent(ZoneTypeGermany.EXTOVERSEAS,new HashMap<>());
        for (Purpose purpose : PurposeGermany.values()){
            this.calibrationOvernightOverseasDcMatrix.get(TypeGermany.OVERNIGHT).get(ZoneTypeGermany.EXTOVERSEAS).putIfAbsent(purpose,1.0);
        }

        logger.info("Overnight Overseas DC loaded");

    }

    @Override
    public int selectDestination(LongDistanceTrip trip) {
        LongDistanceTripGermany t = (LongDistanceTripGermany) trip;
        return selectDestination(t, dataSet);
    }

    //given a trip, calculate the utility of each destination
    public int selectDestination(LongDistanceTripGermany trip, DataSet dataSet) {
        String columnName = "Share";
        int[] alternatives = new int[4];
        int countAlternatives = 0;
        for (int i = 1; i<=dataSet.getZones().size(); i++){
            if(dataSet.getZones().get(i).getZoneType().equals(ZoneTypeGermany.EXTOVERSEAS)){
                alternatives[countAlternatives] = dataSet.getZones().get(i).getId();
                countAlternatives = countAlternatives+1;
            }
        }

        double[] probabilities = proportionsByContinents.getColumnAsDouble(columnName);

        return Util.selectGermany(probabilities, alternatives);

    }

    public Map<Type, Map<ZoneType, Map<Purpose, Double>>> getOvernightOverseasDcCalibration() {
        return calibrationOvernightOverseasDcMatrix;
    }

    public void updateOvernightOverseasDcCalibration(Map<Type, Map<ZoneType, Map<Purpose, Double>>> updatedMatrix) {

        for (Purpose purpose : PurposeGermany.values()){
            double newValue = calibrationOvernightOverseasDcMatrix.get(TypeGermany.OVERNIGHT).get(ZoneTypeGermany.EXTOVERSEAS).get(purpose) * updatedMatrix.get(TypeGermany.OVERNIGHT).get(ZoneTypeGermany.EXTOVERSEAS).get(purpose);
            calibrationOvernightOverseasDcMatrix.get(TypeGermany.OVERNIGHT).get(ZoneTypeGermany.EXTOVERSEAS).put(purpose, newValue);
            System.out.println("k-factor: " + TypeGermany.OVERNIGHT + "\t" + ZoneTypeGermany.EXTOVERSEAS + "\t" + purpose + "\t" + calibrationOvernightOverseasDcMatrix.get(TypeGermany.OVERNIGHT).get(ZoneTypeGermany.EXTOVERSEAS).get(purpose));
        }
    }


}
