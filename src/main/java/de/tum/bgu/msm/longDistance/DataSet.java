package de.tum.bgu.msm.longDistance;

import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.longDistance.data.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.data.Mode;
import de.tum.bgu.msm.longDistance.destinationChoice.Distribution;
import de.tum.bgu.msm.longDistance.destinationChoice.DomesticDestinationChoice;
import de.tum.bgu.msm.longDistance.destinationChoice.IntInboundDestinationChoice;
import de.tum.bgu.msm.longDistance.destinationChoice.IntOutboundDestinationChoice;
import de.tum.bgu.msm.longDistance.modeChoice.DomesticModeChoice;
import de.tum.bgu.msm.longDistance.modeChoice.IntModeChoice;
import de.tum.bgu.msm.longDistance.modeChoice.McModel;
import de.tum.bgu.msm.longDistance.data.sp.Household;
import de.tum.bgu.msm.longDistance.data.sp.Person;
import de.tum.bgu.msm.longDistance.zoneSystem.Zone;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.util.*;

/**
 * Created by carlloga on 8/1/2017.
 */
public class DataSet {

    private static Logger logger = LogManager.getLogger(DataSet.class);



    //ZONAL DATA
    private Map<Integer, Zone> zones = new HashMap<>();
    private ArrayList<Zone> internalZones = new ArrayList<>();
    private ArrayList<Zone> externalZones = new ArrayList<>();

    //SKIMS GA-zones
    private Matrix autoTravelTime;
    private Matrix autoTravelDistance;

    //SKIMS level-2 zones
    private Map<Mode, Matrix> travelTimeMatrix;;
    private Map<Mode, Matrix> priceMatrix;
    private Map<Mode, Matrix> transferMatrix;

    public Map<Mode, Matrix> getTravelTimeMatrix() {
        return travelTimeMatrix;
    }

    public void setTravelTimeMatrix(Map<Mode, Matrix> travelTimeMatrix) {
        this.travelTimeMatrix = travelTimeMatrix;
    }

    public Map<Mode, Matrix> getPriceMatrix() {
        return priceMatrix;
    }

    public void setPriceMatrix(Map<Mode, Matrix> priceMatrix) {
        this.priceMatrix = priceMatrix;
    }

    public Map<Mode, Matrix> getTransferMatrix() {
        return transferMatrix;
    }

    public void setTransferMatrix(Map<Mode, Matrix> transferMatrix) {
        this.transferMatrix = transferMatrix;
    }

    public Map<Mode, Matrix> getFrequencyMatrix() {
        return frequencyMatrix;
    }

    public void setFrequencyMatrix(Map<Mode, Matrix> frequencyMatrix) {
        this.frequencyMatrix = frequencyMatrix;
    }

    private Map<Mode, Matrix> frequencyMatrix;

    //SYNYHETIC POPULATION
    private Map<Integer, Person> persons = new HashMap<>();
    private Map<Integer, Household> households = new HashMap<>();

    //TRIPS
    private ArrayList<LongDistanceTrip> allTrips;

    //MODELS
    //todo probably the data to be interchanged between models should be here instead
    private Distribution destinationChoiceModel;
    private DomesticDestinationChoice dcDomestic;
    private IntOutboundDestinationChoice dcIntOutbound;
    private IntInboundDestinationChoice dcIntInbound;

    private McModel modeChoiceModel;
    private DomesticModeChoice mcDomestic;
    private IntModeChoice mcInt;


    public Map<Integer, Zone> getZones() {
        return zones;
    }

    public void setZones(Map<Integer, Zone> zones) {
        this.zones = zones;
    }

    public ArrayList<Zone> getInternalZones() {
        return internalZones;
    }

    public void setInternalZones(ArrayList<Zone> internalZones) {
        this.internalZones = internalZones;
    }

    public ArrayList<Zone> getExternalZones() {
        return externalZones;
    }

    public void setExternalZones(ArrayList<Zone> externalZones) {
        this.externalZones = externalZones;
    }

    public void setAutoTravelTime(Matrix autoTravelTime) {
        this.autoTravelTime = autoTravelTime;
    }

    public void setAutoTravelDistance(Matrix autoTravelDistance) {
        this.autoTravelDistance = autoTravelDistance;
    }

    public Map<Integer, Person> getPersons() {
        return persons;
    }

    public void setPersons(Map<Integer, Person> persons) {
        this.persons = persons;
    }

    public Map<Integer, Household> getHouseholds() {
        return households;
    }

    public void setHouseholds(Map<Integer, Household> households) {
        this.households = households;
    }

    public float getAutoTravelTime(int orig, int dest) {
            return autoTravelTime.getValueAt(orig, dest);
    }

    public float getAutoTravelDistance(int orig, int dest) {
            return autoTravelDistance.getValueAt(orig, dest);
    }

    public Person getPersonFromId(int personId) {
        return persons.get(personId);
    }

    public Household getHouseholdFromId(int hhId) {
        return households.get(hhId);
    }

    public DomesticModeChoice getMcDomestic() {
        return mcDomestic;
    }

    public void setMcDomestic(DomesticModeChoice mcDomestic) {
        this.mcDomestic = mcDomestic;
    }

    public DomesticDestinationChoice getDcDomestic() {
        return dcDomestic;
    }

    public void setDcDomestic(DomesticDestinationChoice dcDomestic) {
        this.dcDomestic = dcDomestic;
    }

    public IntModeChoice getMcInt() {
        return mcInt;
    }

    public void setMcInt(IntModeChoice mcInt) {
        this.mcInt = mcInt;
    }

    public IntOutboundDestinationChoice getDcIntOutbound() {
        return dcIntOutbound;
    }

    public void setDcIntOutbound(IntOutboundDestinationChoice dcIntOutbound) {
        this.dcIntOutbound = dcIntOutbound;
    }

    public ArrayList<LongDistanceTrip> getAllTrips() {
        return allTrips;
    }

    public void setAllTrips(ArrayList<LongDistanceTrip> allTrips) {
        this.allTrips = allTrips;
    }

    public Distribution getDestinationChoiceModel() {
        return destinationChoiceModel;
    }

    public void setDestinationChoiceModel(Distribution destinationChoiceModel) {
        this.destinationChoiceModel = destinationChoiceModel;
    }

    public McModel getModeChoiceModel() {
        return modeChoiceModel;
    }

    public void setModeChoiceModel(McModel modeChoiceModel) {
        this.modeChoiceModel = modeChoiceModel;
    }

    public IntInboundDestinationChoice getDcIntInbound() {
        return dcIntInbound;
    }

    public void setDcIntInbound(IntInboundDestinationChoice dcIntInbound) {
        this.dcIntInbound = dcIntInbound;
    }
}
