package de.tum.bgu.msm.longDistance.data.trips;



import de.tum.bgu.msm.longDistance.data.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneType;
import de.tum.bgu.msm.longDistance.data.sp.Person;

import java.util.ArrayList;

/**
 * Created by Carlos Llorca  on 7/5/2016.
 * Technical University of Munich
 *
 * Class to hold a long distance trip
 *
 */
public class LongDistanceTrip {

    private int tripId;
    private Person traveller;
    private boolean international;
    private Purpose tripPurpose;
    private Type tripState;
    private int nights;
    private ArrayList<Person> hhTravelParty;
    private int nonHhTravelPartySize;
    private Zone origZone;
    private int destCombinedZoneId = -1;
    private ZoneType destZoneType;
    private Zone destZone;
    private Mode travelMode;
    private float travelDistanceLevel2 = -1;
    private float travelTimeLevel2 = -1;
    private float travelDistanceLevel1 = -1;
    private int departureTimeInHours = -999;
    private int departureTimeInHoursSecondSegment = -999; //this is the return trip of daytrips
    private boolean returnOvernightTrip = false;


    public void setHhTravelParty(ArrayList<Person> hhTravelParty) {
        this.hhTravelParty = hhTravelParty;
    }

    public LongDistanceTrip(int tripId, Person traveller, boolean international, Purpose tripPurpose, Type tripState, Zone origZone, int nights, int nonHhTravelPartySize ) {
        this.tripId = tripId;
        this.traveller = traveller;
        this.international = international;
        this.tripPurpose = tripPurpose;
        this.tripState = tripState;
        this.origZone = origZone;
        this.nights = nights;
        this.hhTravelParty = new ArrayList<>();
        this.nonHhTravelPartySize = nonHhTravelPartySize;
    }

    public int getTripId() {
        return tripId;
    }

    public int getTravellerId() {
        return traveller.getPersonId();
    }

    public Person getTraveller() {
        return traveller;
    }

    public boolean isInternational() {
        return international;
    }

    public Type getTripState() {
        return tripState;
    }

    public Purpose getTripPurpose() {
        return tripPurpose;
    }

    public int getNights() {
        return nights;
    }

    public int getAdultsHhTravelPartySize() { return 0; }

    public int getKidsHhTravelPartySize() { return 0; }

    public int getNonHhTravelPartySize() {
        return nonHhTravelPartySize;
    }

    public Zone getOrigZone() { return origZone; }

    public int getDestCombinedZoneId() { return destCombinedZoneId; }

    public void setCombinedDestZoneId(int destinationZoneId) {
        this.destCombinedZoneId = destinationZoneId;
    }

    public void setMode(Mode travelMode) {
        this.travelMode = travelMode;
    }

    public Mode getMode() {
        return travelMode;
    }

    public ZoneType getDestZoneType() {
        return destZoneType;
    }

    public void setDestZoneType(ZoneType destZoneType) {
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
        LongDistanceTrip tr = this;
        String str = null;
        if (tr.getOrigZone().getZoneType().equals(ZoneType.ONTARIO)) {
            Person traveller = tr.getTraveller();

            str = (tr.getTripId()
                    + "," + tr.getTravellerId()
                    + "," + tr.isInternational()
                    + "," + tr.tripPurpose.toString()
                    + "," + tr.tripState.toString()
                    + "," + tr.getOrigZone().getId()
                    + "," + tr.getOrigZone().getCombinedZoneId()
                    + "," + tr.getOrigZone().getZoneType()
                    + "," + tr.getDestCombinedZoneId()
                    + "," + tr.getMode()
                    + "," + tr.getNights()
                    + "," + tr.getAdultsHhTravelPartySize()
                    + "," + tr.getKidsHhTravelPartySize()
                    + "," + tr.getNonHhTravelPartySize()
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
                    + "," + tr.getOrigZone().getCombinedZoneId()
                    + "," + tr.getOrigZone().getZoneType()
                    + "," + tr.getDestCombinedZoneId()
                    + "," + tr.getMode()
                    + "," + tr.getNights()
                    + "," + tr.getAdultsHhTravelPartySize()
                    + "," + tr.getKidsHhTravelPartySize()
                    + "," + tr.getNonHhTravelPartySize()
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

}
