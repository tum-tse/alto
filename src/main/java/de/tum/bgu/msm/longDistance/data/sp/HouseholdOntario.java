package de.tum.bgu.msm.longDistance.data.sp;
 import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneOntario;
 import org.apache.log4j.Logger;

/**
 *
 * Ontario Provincial Model
 * Class to store synthetic households
 * Author: Rolf Moeckel, Technical University of Munich (TUM), rolf.moeckel@tum.de
 * Date: 22 April 2016
 * Version 1
 *
 */

public class HouseholdOntario implements Household {

    static Logger logger = Logger.getLogger(HouseholdOntario.class);

    private int id;
    private int hhSize;
    private int hhInc;
    private int ddType;
    private int numWrks;
    private int numKids;
    private int taz;
    private ZoneOntario zone;
    private PersonOntario[] persons;


    public HouseholdOntario(int id, int hhInc, int ddType, int taz, ZoneOntario zone) {
        this.id      = id;
        this.hhSize  = 0;
        this.hhInc   = hhInc;
        this.ddType  = ddType;
//        this.numWrks = numWrks;
//        this.numKids = numKids;
//        persons = new Person[hhSize];
        this.taz = taz;
        this.zone = zone;
        this.persons = new PersonOntario[0];
    }


    @Override
    public void addPersonForInitialSetup(Person per) {
        // This method adds a person to the household (only used for initial setup)

        Person[] personsAddedSoFar = persons;
        persons = new PersonOntario[personsAddedSoFar.length + 1];
        System.arraycopy(personsAddedSoFar, 0, persons, 0, persons.length-1);
        persons[persons.length-1] = (PersonOntario) per;
        hhSize++;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getZoneId() {
        return taz;
    }

    @Override
    public int getHouseholdSize() {
        return hhSize;
    }

    public int getHhSize() {
        return hhSize;
    }

    public int getHhInc() {
        return hhInc;
    }

    public int getTaz() {
        return taz;
    }

    public ZoneOntario getZone() {return zone;}

    public PersonOntario[] getPersonsOfThisHousehold() {
        return persons;
    }
}
