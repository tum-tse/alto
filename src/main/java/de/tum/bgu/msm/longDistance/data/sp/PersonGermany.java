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
    private boolean isStudent;
    private boolean isEmployed;
    private boolean isEconomicStatusLow = false;
    private boolean isEconomicStatusMedium = false;
    private boolean isEconomicStatusHigh = false;
    private boolean isEconomicStatusVeryHigh = false;
    private boolean driversLicense;
    private int workplace;


    public PersonGermany(int id, int hhId, int age, Gender gender, OccupationStatus occupation, int workplace, boolean driversLicense, HouseholdGermany hh) {
        this.id = id;
        this.age = age;
        this.gender = gender;
        this.workplace = workplace;
        this.driversLicense = driversLicense;
        this.hh = hh;
        if (hh != null) hh.addPersonForInitialSetup(this);

        this.isYoung  = age < 25 ? true : false;
        this.isRetired = age > 64 ? true : false;
        this.isFemale = gender == Gender.FEMALE ? true : false;
        this.isEmployed = occupation == OccupationStatus.WORKER? true : false;
        this.isStudent = occupation ==  OccupationStatus.STUDENT ? true : false;
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

    public boolean isEmployed() {
        return isEmployed;
    }

    public boolean isDriversLicense() {
        return driversLicense;
    }

    public boolean isStudent() {
        return isStudent;
    }

    public int getPersonWorkplace() {
        return workplace;
    }

}
