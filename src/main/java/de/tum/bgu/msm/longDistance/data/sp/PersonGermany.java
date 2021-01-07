package de.tum.bgu.msm.longDistance.data.sp;


import de.tum.bgu.msm.longDistance.data.trips.Purpose;
import de.tum.bgu.msm.longDistance.data.trips.Type;

import java.util.Map;
import java.util.Optional;

/**
 *
 * Ontario Provincial Model
 * Class to store synthetic persons
 * Author: Ana Moreno, Technical University of Munich (TUM), ana.moreno@tum.de
 * Date: 8 December 2020
 * Version 1
 * Adapted from Ontario
 * Version 1
 *
 */

public class PersonGermany implements Person {

    private int id;
    private int age;
    private Gender gender;

    private HouseholdGermany hh;

    private boolean  isAway = false;
    private boolean  isDaytrip = false ;
    private boolean  isInOutTrip = false;

    private Map<Purpose, Map<Type, Double>> travelProbabilities = null;
// rows 1 to 3: away, daytrip, inOutTrip, home
// columns 1 to 3: visit, business, leisure

    //variables added to homogenize with survey data
    private boolean isYoung;
    private boolean isRetired;
    private boolean isFemale;
    private boolean isMale;
    private boolean isStudent;
    private boolean isEmployed;
    private boolean driversLicense;
    private boolean isBelow18;
    private boolean isBetween18and39;
    private boolean isBetween40and59;
    private boolean isOver60;


    public PersonGermany(int id, int hhId, int age, Gender gender, OccupationStatus occupation, boolean driversLicense, HouseholdGermany hh) {
        this.id = id;
        this.age = age;
        this.gender = gender;
        this.driversLicense = driversLicense;
        this.hh = hh;
        if (hh != null) hh.addPersonForInitialSetup(this);

        this.isYoung  = age < 25 ? true : false;
        this.isRetired = age > 64 ? true : false;
        this.isFemale = gender == Gender.FEMALE ? true : false;
        this.isMale = gender == Gender.MALE ? true : false;
        this.isEmployed = occupation == OccupationStatus.WORKER? true : false;
        this.isStudent = occupation ==  OccupationStatus.STUDENT ? true : false;
        this.isBelow18 = age < 17 ? true : false;
        this.isBetween18and39 = age < 40 && age > 17 ? true : false;
        this.isBetween40and59 = age < 60 && age > 39 ? true : false;
        this.isOver60 = age > 59 ? true : false;
    }



    @Override
    public int getPersonId() {return id;}

    public Gender getGender() {
        return gender;}

    public int getAge() {return age;}

    public int getIncome() {return hh.getHhInc();}

    public int getAdultsHh() {
        int adultsHh = 0;
        for (PersonGermany p : hh.getPersonsOfThisHousehold()) {
            if (p.getAge() >= 18) {
                adultsHh++;
            }
        }
        return adultsHh;
    }

    public int getKidsHh() {
        return hh.getHhSize()- getAdultsHh();
    }

    public HouseholdGermany getHousehold() {return hh;}

    public boolean isAway() {
        return isAway;
    }

    public boolean isDaytrip() {
        return isDaytrip;
    }

    public boolean isInOutTrip() {
        return isInOutTrip;
    }

    public void setAway(boolean away) {
        isAway = away;
    }

    public void setDaytrip(boolean daytrip) {
        isDaytrip = daytrip;
    }

    public void setInOutTrip(boolean inOutTrip) {
        isInOutTrip = inOutTrip;
    }

    public Map<Purpose, Map<Type, Double>> getTravelProbabilities() {
        return travelProbabilities;
    }

    public void setTravelProbabilities(Map<Purpose, Map<Type, Double>> travelProbabilities) {
        this.travelProbabilities = travelProbabilities;
    }

    public boolean isYoung() {
        return isYoung;
    }

    public boolean isRetired() {
        return isRetired;
    }

    public boolean isFemale() {
        return isFemale;
    }

    public boolean isMale() {
        return isMale;
    }

    public boolean isEmployed() {
        return isEmployed;
    }

    public boolean isDriversLicense() {
        return driversLicense;
    }

    public boolean isStudent() {
        return isStudent;
    }

    public boolean isBetween18and39() {
        return isBetween18and39;
    }

    public boolean isBetween40and59() {
        return isBetween40and59;
    }

    public boolean isOver60() {
        return isOver60;
    }

    public boolean isBelow18() {
        return isBelow18;
    }
}
