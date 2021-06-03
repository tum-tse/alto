package de.tum.bgu.msm.longDistance.data.sp;

public interface Household {
    void addPersonForInitialSetup(Person per);

    int getId();

    int getZoneId();

    int getHouseholdSize();
}
