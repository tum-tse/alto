package de.tum.bgu.msm.longDistance.data.trips;


import de.tum.bgu.msm.longDistance.data.sp.Person;
import de.tum.bgu.msm.longDistance.data.sp.PersonGermany;
import de.tum.bgu.msm.longDistance.data.zoneSystem.*;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * Class to hold a long distance trip
 * Author: Ana Moreno, Technical University of Munich (TUM), ana.moreno@tum.de
 * Date: 8 December 2020
 * Version 1
 * Adapted from Ontario
 *
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
    private float travelDistanceLevel2 = -1;
    private float travelTimeLevel2 = -1;
    private float travelDistanceLevel1 = -1;
    private int departureTimeInHours = -999;
    private int departureTimeInHoursSecondSegment = -999; //this is the return trip of daytrips
    private boolean returnOvernightTrip = false;
    private Map<Pollutant, Float> emissions = new HashMap<>();


    public LongDistanceTripGermany(int tripId, PersonGermany traveller, boolean international, Purpose tripPurpose, Type tripState, ZoneGermany origZone ) {
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

    public boolean isInternational() {
        return international;
    }

    public int getAdultsHhTravelPartySize() { return 0; }

    public int getKidsHhTravelPartySize() { return 0; }

    public ZoneGermany getOrigZone() { return origZone; }

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

    public float getTravelDistanceLevel2() {
        return travelDistanceLevel2;
    }

    public void setTravelDistanceLevel2(float travelDistanceLevel2) {
        this.travelDistanceLevel2 = travelDistanceLevel2;
    }

    public float getTravelDistanceLevel1() {
        return travelDistanceLevel1;
    }

    public void setTravelDistanceLevel1(float travelDistanceLevel1) {
        this.travelDistanceLevel1 = travelDistanceLevel1;
    }

    public int getDepartureTimeInHours() {
        return departureTimeInHours;
    }

    public void setDepartureTimeInHours(int departureTimeInHours) {
        this.departureTimeInHours = departureTimeInHours;
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

    public float getTravelTimeLevel2() {
        return travelTimeLevel2;
    }

    public void setTravelTimeLevel2(float travelTimeLevel2) {
        this.travelTimeLevel2 = travelTimeLevel2;
    }

    public static String getHeader() {
        return "tripId,personId,international,tripPurpose,tripState,tripOriginZone,tripOriginCombinedZone,tripOriginType," +
                "tripDestCombinedZone,tripMode,"
                + "numberOfNights,hhAdultsTravelParty,hhKidsTravelParty,nonHhTravelParty,destZoneType,destZone,travelDistanceLvl2,travelDistanceLvl1" +
                ",departureTime,departureTimeReturnDaytrip,ReturnOvernightTrip,travelTimeLevel2ByMode"
//                + ",personAge,personGender," +
        //        "personEducation,personWorkStatus,personIncome,adultsInHh,kidsInHh"
                ;
    }
    @Override
    public String toString() {
        LongDistanceTripGermany tr = this;
        String str = null;
        if (tr.getOrigZone().getZoneType().equals(ZoneTypeGermany.GERMANY)) {
            Person traveller = tr.getTraveller();

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
                    + "," + tr.getTravelDistanceLevel2()
                    + "," + tr.getTravelDistanceLevel1()
                    + "," + tr.getDepartureTimeInHours()
                    + "," + tr.getDepartureTimeInHoursSecondSegment()
                    + "," + tr.isReturnOvernightTrip()
                    + "," + tr.getTravelTimeLevel2()
                    /*+ "," + traveller.getAge()
                    + "," + Character.toString(traveller.getGender())
                    + "," + traveller.getEducation()
                    + "," + traveller.getWorkStatus()
                    + "," + traveller.getIncome()
                    + "," + traveller.getAdultsHh()
                    + "," + traveller.getKidsHh()*/
            );
        } else {
            str =  (tr.getTripId()
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
                    + "," + tr.getTravelDistanceLevel2()
                    + "," + tr.getTravelDistanceLevel1()
                    + "," + tr.getDepartureTimeInHours()
                    + "," + tr.getDepartureTimeInHoursSecondSegment()
                    + "," + tr.isReturnOvernightTrip()
                    + "," + tr.getTravelTimeLevel2()
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

    public void setEmissions(Map<Pollutant, Float>  emissions) {
        this.emissions = emissions;
    }
}
