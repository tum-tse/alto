package de.tum.bgu.msm.longDistance.data.trips;


import de.tum.bgu.msm.longDistance.data.sp.HouseholdGermany;
import de.tum.bgu.msm.longDistance.data.sp.Person;
import de.tum.bgu.msm.longDistance.data.sp.PersonGermany;
import de.tum.bgu.msm.longDistance.data.zoneSystem.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Class to hold a long distance trip
 * Author: Ana Moreno, Technical University of Munich (TUM), ana.moreno@tum.de
 * Date: 8 December 2020
 * Version 1
 * Adapted from Ontario
 */
public class LongDistanceTripGermany implements LongDistanceTrip {

    private int tripId;
    private PersonGermany traveller;
    private boolean international;
    private Purpose tripPurpose;
    private Type tripState;
    private ZoneGermany origZone;
    private ZoneTypeGermany destZoneType;
    private Zone destZone;
    private Mode travelMode;
    private AccessEgressMode accessMode;
    private Mode egressMode;
    private float autoTravelDistance = -1;
    private float autoTravelTime = -1;
    private float distanceByMode = -1;
    private float travelTime = -1;
    private int departureTimesInMin = -999; // Alona
    private int departureTimeInHours = -999;
    private int departureTimeInHoursSecondSegment = -999; //this is the return trip of daytrips
    private boolean returnOvernightTrip = false;
    private Map<Pollutant, Float> emissions = new HashMap<>();
    private Map<String, Float> additionalAttributes = new HashMap<>();

    private double origX = -1;
    private double origY = -1;
    private double destX = -1;
    private double destY = -1;

    public LongDistanceTripGermany(int tripId, PersonGermany traveller, boolean international, Purpose tripPurpose, Type tripState, ZoneGermany origZone) {
        this.tripId = tripId;
        this.traveller = traveller;
        this.international = international;
        this.tripPurpose = tripPurpose;
        this.tripState = tripState;
        this.origZone = origZone;
    }

    public int getTripId() {
        return tripId;
    }

    public int getTravellerId() {
        return traveller.getPersonId();
    }

    public PersonGermany getTraveller() {
        return traveller;
    }

    public void setInternational(boolean international) {
        this.international = international;
    }

    public boolean isInternational() {
        return international;
    }

    public int getAdultsHhTravelPartySize() {
        return 0;
    }

    public int getKidsHhTravelPartySize() {
        return 0;
    }

    public ZoneGermany getOrigZone() {
        return origZone;
    }

    public void setMode(Mode travelMode) {
        this.travelMode = travelMode;
    }

    public ZoneTypeGermany getDestZoneType() {
        return destZoneType;
    }

    public void setDestZoneType(ZoneTypeGermany destZoneType) {
        this.destZoneType = destZoneType;
    }

    public Zone getDestZone() {
        return destZone;
    }

    public void setDestZone(Zone destZone) {
        this.destZone = destZone;
    }

    public float getAutoTravelDistance() {
        return autoTravelDistance;
    }

    public void setAutoTravelDistance(float autoTravelDistance) {
        this.autoTravelDistance = autoTravelDistance;
    }

    public float getAutoTravelTime() {
        return autoTravelTime;
    }

    public void setAutoTravelTime(float autoTravelTime) {
        this.autoTravelTime = autoTravelTime;
    }

    public float getDistanceByMode() {
        return distanceByMode;
    }

    public void setDistanceByMode(float distanceByMode) {
        this.distanceByMode = distanceByMode;
    }

    public int getDepartureTimeInHours() {
        return departureTimeInHours;
    }

    public void setDepartureTimeInHours(int departureTimeInHours) {
        this.departureTimeInHours = departureTimeInHours;
    }

    public int getDepartureTimesInMin() {
        return departureTimesInMin;
    }

    public void setDepartureTimesInMin(int departureTimesInMin) {
        this.departureTimesInMin = departureTimesInMin;
    }

    public int getDepartureTimeInHoursSecondSegment() {
        return departureTimeInHoursSecondSegment;
    }

    public void setDepartureTimeInHoursSecondSegment(int departureTimeInHoursSecondSegment) {
        this.departureTimeInHoursSecondSegment = departureTimeInHoursSecondSegment;
    }

    public boolean isReturnOvernightTrip() {
        return returnOvernightTrip;
    }

    public void setReturnOvernightTrip(boolean returnOvernightTrip) {
        this.returnOvernightTrip = returnOvernightTrip;
    }

    public float getTravelTime() {
        return travelTime;
    }

    public void setTravelTimeByMode(float travelTime) {
        this.travelTime = travelTime;
    }

    public double getOrigX() {
        return origX;
    }

    public void setOrigX(double origX) {
        this.origX = origX;
    }

    public double getOrigY() {
        return origY;
    }

    public void setOrigY(double origY) {
        this.origY = origY;
    }

    public double getDestX() {
        return destX;
    }

    public void setDestX(double destX) {
        this.destX = destX;
    }

    public double getDestY() {
        return destY;
    }

    public void setDestY(double destY) {
        this.destY = destY;
    }

    public AccessEgressMode getAccessMode() {
        return accessMode;
    }

    public void setAccessMode(AccessEgressMode accessMode) {
        this.accessMode = accessMode;
    }

    public Mode getEgressMode() {
        return egressMode;
    }

    public void setEgressMode(Mode egressMode) {
        this.egressMode = egressMode;
    }

    public static String getHeader() {
        return "tripId,personId" +
                ",international,tripPurpose,tripState,tripOriginZone,tripOriginType" +
                ",tripDestZone,tripDestType,travelDistanceByCar_km,travelTimeByCar_h,travelDistance_km" +
                ",tripMode,travelTimeByMode_h" +
                ",departureTimeMin,departureTimeReturnDaytrip,ReturnOvernightTrip" +
                ",CO2emissions_kg" +
                ",origX,origY,destX,destY" +
                ",selPos"+
                ",utility_" + ModeGermany.getMode(0) +
                ",utility_" + ModeGermany.getMode(1) +
                ",utility_" + ModeGermany.getMode(2) +
                ",utility_" + ModeGermany.getMode(3) +
                ",utility_" + ModeGermany.getMode(4) +
                ",utility_" + ModeGermany.getMode(5) +
                ",impedance_" + ModeGermany.getMode(0) +
                ",impedance_" + ModeGermany.getMode(1) +
                ",impedance_" + ModeGermany.getMode(2) +
                ",impedance_" + ModeGermany.getMode(3) +
                ",impedance_" + ModeGermany.getMode(4) +
                ",impedance_" + ModeGermany.getMode(5) +
                ",costTotal_" + ModeGermany.getMode(0) +
                ",costTotal_" + ModeGermany.getMode(1) +
                ",costTotal_" + ModeGermany.getMode(2) +
                ",costTotal_" + ModeGermany.getMode(3) +
                ",costTotal_" + ModeGermany.getMode(4) +
                ",costTotal_" + ModeGermany.getMode(5) +
                ",costAccess_" + ModeGermany.getMode(0)+
                ",costAccess_" + ModeGermany.getMode(1)+
                ",costAccess_" + ModeGermany.getMode(2)+
                ",costAccess_" + ModeGermany.getMode(3)+
                ",costAccess_" + ModeGermany.getMode(4)+
                ",costAccess_" + ModeGermany.getMode(5)+
                ",costEgress_" + ModeGermany.getMode(0)+
                ",costEgress_" + ModeGermany.getMode(1)+
                ",costEgress_" + ModeGermany.getMode(2)+
                ",costEgress_" + ModeGermany.getMode(3)+
                ",costEgress_" + ModeGermany.getMode(4)+
                ",costEgress_" + ModeGermany.getMode(5)+
                ",timeTotal_" + ModeGermany.getMode(0) +
                ",timeTotal_" + ModeGermany.getMode(1) +
                ",timeTotal_" + ModeGermany.getMode(2) +
                ",timeTotal_" + ModeGermany.getMode(3) +
                ",timeTotal_" + ModeGermany.getMode(4) +
                ",timeTotal_" + ModeGermany.getMode(5) +
                ",timeAccess_" + ModeGermany.getMode(0) +
                ",timeAccess_" + ModeGermany.getMode(1) +
                ",timeAccess_" + ModeGermany.getMode(2) +
                ",timeAccess_" + ModeGermany.getMode(3) +
                ",timeAccess_" + ModeGermany.getMode(4) +
                ",timeAccess_" + ModeGermany.getMode(5) +
                ",timeEgress_" + ModeGermany.getMode(0) +
                ",timeEgress_" + ModeGermany.getMode(1) +
                ",timeEgress_" + ModeGermany.getMode(2) +
                ",timeEgress_" + ModeGermany.getMode(3) +
                ",timeEgress_" + ModeGermany.getMode(4) +
                ",timeEgress_" + ModeGermany.getMode(5) +
                ",distance_" + ModeGermany.getMode(0) +
                ",distance_" + ModeGermany.getMode(1) +
                ",distance_" + ModeGermany.getMode(2) +
                ",distance_" + ModeGermany.getMode(3) +
                ",distance_" + ModeGermany.getMode(4) +
                ",distance_" + ModeGermany.getMode(5) +
                ",distanceAccess_" + ModeGermany.getMode(0)+
                ",distanceAccess_" + ModeGermany.getMode(1)+
                ",distanceAccess_" + ModeGermany.getMode(2)+
                ",distanceAccess_" + ModeGermany.getMode(3)+
                ",distanceAccess_" + ModeGermany.getMode(4)+
                ",distanceAccess_" + ModeGermany.getMode(5)+
                ",distanceEgress_" + ModeGermany.getMode(0)+
                ",distanceEgress_" + ModeGermany.getMode(1)+
                ",distanceEgress_" + ModeGermany.getMode(2)+
                ",distanceEgress_" + ModeGermany.getMode(3)+
                ",distanceEgress_" + ModeGermany.getMode(4)+
                ",distanceEgress_" + ModeGermany.getMode(5)+
                ",personalIncome"+
                ",householdIncome"+
                ",economicStatus"+
                ",householdCarOwnership"+
                ",householdAreaType"
//                + ",personAge,personGender," +
                //        "personEducation,personWorkStatus,personIncome,adultsInHh,kidsInHh"
                ;
    }

    @Override
    public String toString() {
        LongDistanceTripGermany tr = this;
        String str = null;
        if (tr.getOrigZone().getZoneType().equals(ZoneTypeGermany.GERMANY)) {
            PersonGermany traveller = tr.getTraveller();
            HouseholdGermany household = tr.getTraveller().getHousehold();

            str = (tr.getTripId()
                    + "," + tr.getTravellerId()
                    + "," + tr.isInternational()
                    + "," + tr.tripPurpose.toString()
                    + "," + tr.tripState.toString()
                    + "," + tr.getOrigZone().getId()
                    + "," + tr.getOrigZone().getZoneType()
                    + "," + tr.getDestZone().getId()
                    + "," + tr.getDestZone().getZoneType()
                    + "," + tr.getAutoTravelDistance() / 1000
                    + "," + tr.getAutoTravelTime() / 3600
                    + "," + tr.getDistanceByMode() / 1000
                    + "," + tr.getMode()
                    + "," + tr.getTravelTime() / 3600
                    + "," + tr.getDepartureTimesInMin() // Alona
                    + "," + tr.getDepartureTimeInHoursSecondSegment()
                    + "," + tr.isReturnOvernightTrip()
                    + "," + tr.getEmissions().get(Pollutant.CO2)
                    + "," + tr.getOrigX()
                    + "," + tr.getOrigY()
                    + "," + tr.getDestX()
                    + "," + tr.getDestY()
                    + "," + tr.getAdditionalAttributes().get("selPos")
                    + "," + tr.getAdditionalAttributes().get("utility_" + ModeGermany.getMode(0))
                    + "," + tr.getAdditionalAttributes().get("utility_" + ModeGermany.getMode(1))
                    + "," + tr.getAdditionalAttributes().get("utility_" + ModeGermany.getMode(2))
                    + "," + tr.getAdditionalAttributes().get("utility_" + ModeGermany.getMode(3))
                    + "," + tr.getAdditionalAttributes().get("utility_" + ModeGermany.getMode(4))
                    + "," + tr.getAdditionalAttributes().get("utility_" + ModeGermany.getMode(5))
                    + "," + tr.getAdditionalAttributes().get("impedance_" + ModeGermany.getMode(0))
                    + "," + tr.getAdditionalAttributes().get("impedance_" + ModeGermany.getMode(1))
                    + "," + tr.getAdditionalAttributes().get("impedance_" + ModeGermany.getMode(2))
                    + "," + tr.getAdditionalAttributes().get("impedance_" + ModeGermany.getMode(3))
                    + "," + tr.getAdditionalAttributes().get("impedance_" + ModeGermany.getMode(4))
                    + "," + tr.getAdditionalAttributes().get("impedance_" + ModeGermany.getMode(5))
                    + "," + tr.getAdditionalAttributes().get("costTotal_" + ModeGermany.getMode(0))
                    + "," + tr.getAdditionalAttributes().get("costTotal_" + ModeGermany.getMode(1))
                    + "," + tr.getAdditionalAttributes().get("costTotal_" + ModeGermany.getMode(2))
                    + "," + tr.getAdditionalAttributes().get("costTotal_" + ModeGermany.getMode(3))
                    + "," + tr.getAdditionalAttributes().get("costTotal_" + ModeGermany.getMode(4))
                    + "," + tr.getAdditionalAttributes().get("costTotal_" + ModeGermany.getMode(5))
                    + "," + tr.getAdditionalAttributes().get("costAccess_"+ ModeGermany.getMode(0))
                    + "," + tr.getAdditionalAttributes().get("costAccess_"+ ModeGermany.getMode(1))
                    + "," + tr.getAdditionalAttributes().get("costAccess_"+ ModeGermany.getMode(2))
                    + "," + tr.getAdditionalAttributes().get("costAccess_"+ ModeGermany.getMode(3))
                    + "," + tr.getAdditionalAttributes().get("costAccess_"+ ModeGermany.getMode(4))
                    + "," + tr.getAdditionalAttributes().get("costAccess_"+ ModeGermany.getMode(5))
                    + "," + tr.getAdditionalAttributes().get("costEgress_"+ ModeGermany.getMode(0))
                    + "," + tr.getAdditionalAttributes().get("costEgress_"+ ModeGermany.getMode(1))
                    + "," + tr.getAdditionalAttributes().get("costEgress_"+ ModeGermany.getMode(2))
                    + "," + tr.getAdditionalAttributes().get("costAccess_"+ ModeGermany.getMode(3))
                    + "," + tr.getAdditionalAttributes().get("costAccess_"+ ModeGermany.getMode(4))
                    + "," + tr.getAdditionalAttributes().get("costAccess_"+ ModeGermany.getMode(5))
                    + "," + tr.getAdditionalAttributes().get("timeTotal_" + ModeGermany.getMode(0))
                    + "," + tr.getAdditionalAttributes().get("timeTotal_" + ModeGermany.getMode(1))
                    + "," + tr.getAdditionalAttributes().get("timeTotal_" + ModeGermany.getMode(2))
                    + "," + tr.getAdditionalAttributes().get("timeTotal_" + ModeGermany.getMode(3))
                    + "," + tr.getAdditionalAttributes().get("timeTotal_" + ModeGermany.getMode(4))
                    + "," + tr.getAdditionalAttributes().get("timeTotal_" + ModeGermany.getMode(5))
                    + "," + tr.getAdditionalAttributes().get("timeAccess_" + ModeGermany.getMode(0))
                    + "," + tr.getAdditionalAttributes().get("timeAccess_" + ModeGermany.getMode(1))
                    + "," + tr.getAdditionalAttributes().get("timeAccess_" + ModeGermany.getMode(2))
                    + "," + tr.getAdditionalAttributes().get("timeAccess_" + ModeGermany.getMode(3))
                    + "," + tr.getAdditionalAttributes().get("timeAccess_" + ModeGermany.getMode(4))
                    + "," + tr.getAdditionalAttributes().get("timeAccess_" + ModeGermany.getMode(5))
                    + "," + tr.getAdditionalAttributes().get("timeEgress_" + ModeGermany.getMode(0))
                    + "," + tr.getAdditionalAttributes().get("timeEgress_" + ModeGermany.getMode(1))
                    + "," + tr.getAdditionalAttributes().get("timeEgress_" + ModeGermany.getMode(2))
                    + "," + tr.getAdditionalAttributes().get("timeEgress_" + ModeGermany.getMode(3))
                    + "," + tr.getAdditionalAttributes().get("timeEgress_" + ModeGermany.getMode(4))
                    + "," + tr.getAdditionalAttributes().get("timeEgress_" + ModeGermany.getMode(5))
                    + "," + tr.getAdditionalAttributes().get("distance_" + ModeGermany.getMode(0))
                    + "," + tr.getAdditionalAttributes().get("distance_" + ModeGermany.getMode(1))
                    + "," + tr.getAdditionalAttributes().get("distance_" + ModeGermany.getMode(2))
                    + "," + tr.getAdditionalAttributes().get("distance_" + ModeGermany.getMode(3))
                    + "," + tr.getAdditionalAttributes().get("distance_" + ModeGermany.getMode(4))
                    + "," + tr.getAdditionalAttributes().get("distance_" + ModeGermany.getMode(5))
                    + "," + tr.getAdditionalAttributes().get("distanceAccess_" + ModeGermany.getMode(0))
                    + "," + tr.getAdditionalAttributes().get("distanceAccess_" + ModeGermany.getMode(1))
                    + "," + tr.getAdditionalAttributes().get("distanceAccess_" + ModeGermany.getMode(2))
                    + "," + tr.getAdditionalAttributes().get("distanceAccess_" + ModeGermany.getMode(3))
                    + "," + tr.getAdditionalAttributes().get("distanceAccess_" + ModeGermany.getMode(4))
                    + "," + tr.getAdditionalAttributes().get("distanceAccess_" + ModeGermany.getMode(5))
                    + "," + tr.getAdditionalAttributes().get("distanceEgress_" + ModeGermany.getMode(0))
                    + "," + tr.getAdditionalAttributes().get("distanceEgress_" + ModeGermany.getMode(1))
                    + "," + tr.getAdditionalAttributes().get("distanceEgress_" + ModeGermany.getMode(2))
                    + "," + tr.getAdditionalAttributes().get("distanceEgress_" + ModeGermany.getMode(3))
                    + "," + tr.getAdditionalAttributes().get("distanceEgress_" + ModeGermany.getMode(4))
                    + "," + tr.getAdditionalAttributes().get("distanceEgress_" + ModeGermany.getMode(5))
                    + "," + traveller.getPpInc()
                    + "," + traveller.getIncome()
                    + "," + household.getEconomicStatus().toString()
                    + "," + household.getHhAutos()
                    + "," + household.getZone().getAreatype().toString()
                    /*
                    /*+ "," + traveller.getAge()
                    + "," + Character.toString(traveller.getGender())
                    + "," + traveller.getEducation()
                    + "," + traveller.getWorkStatus()
                    + "," + traveller.getIncome()
                    + "," + traveller.getAdultsHh()
                    + "," + traveller.getKidsHh()*/
            );
        } else {
            str = (tr.getTripId()
                    + "," + tr.getTravellerId()
                    + "," + tr.isInternational()
                    + "," + tr.tripPurpose.toString()
                    + "," + tr.tripState.toString()
                    + "," + tr.getOrigZone().getId()
                    + "," + tr.getOrigZone().getZoneType()
                    + "," + tr.getMode()
                    + "," + tr.getAdultsHhTravelPartySize()
                    + "," + tr.getKidsHhTravelPartySize()
                    + "," + tr.getDestZoneType()
                    + "," + tr.getDestZone().getId()
                    + "," + tr.getAutoTravelDistance()
                    + "," + tr.getDepartureTimesInMin() // Alona
                    + "," + tr.getDepartureTimeInHours()
                    + "," + tr.getDepartureTimeInHoursSecondSegment()
                    + "," + tr.isReturnOvernightTrip()
                    + "," + tr.getTravelTime()
                    //+ ",-1,,-1,-1,-1,-1,-1"
            );

        }
        return str;
    }

    @Override
    public Mode getMode() {
        return travelMode;
    }

    @Override
    public Type getTripState() {
        return tripState;
    }

    @Override
    public Purpose getTripPurpose() {
        return tripPurpose;
    }

    @Override
    public Map<Pollutant, Float> getEmissions() {
        return emissions;
    }

    @Override
    public Float getCO2emissions() {

        return emissions.get(Pollutant.CO2);
    }

    public Map<String, Float> getAdditionalAttributes() {
        return additionalAttributes;
    }

    public void setAdditionalAttributes(Map<String, Float> additionalAttributes) {
        this.additionalAttributes = additionalAttributes;
    }

    public void setEmissions(Map<Pollutant, Float> emissions) {
        this.emissions = emissions;
    }

}
