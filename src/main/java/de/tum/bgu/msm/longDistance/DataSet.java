package de.tum.bgu.msm.longDistance;

import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.longDistance.data.sp.PersonOntario;
import de.tum.bgu.msm.longDistance.data.trips.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.data.trips.LongDistanceTripOntario;
import de.tum.bgu.msm.longDistance.data.trips.Mode;
import de.tum.bgu.msm.longDistance.data.sp.Household;
import de.tum.bgu.msm.longDistance.data.sp.Person;
import de.tum.bgu.msm.longDistance.data.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneTypeOntario;
import org.apache.log4j.Logger;


import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by carlloga on 8/1/2017.
 */
public class DataSet {

    private static Logger logger = Logger.getLogger(DataSet.class);


    //ZONAL DATA
    private Map<Integer, Zone> zones = new HashMap<>();

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
    private ArrayList<LongDistanceTripOntario> allTrips = new ArrayList<>();

    public Map<Integer, Zone> getZones() {
        return zones;
    }

    public void setZones(Map<Integer, Zone> zones) {
        this.zones = zones;
    }

    public List<Zone> getExternalZones() {
        return zones.values().stream().filter(zone -> !zone.getZoneType().equals(ZoneTypeOntario.ONTARIO)).collect(Collectors.toList());
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

    public ArrayList<LongDistanceTripOntario> getAllTrips() {
        return allTrips;
    }
}
