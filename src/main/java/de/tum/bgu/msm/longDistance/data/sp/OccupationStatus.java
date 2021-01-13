package de.tum.bgu.msm.longDistance.data.sp;

import de.tum.bgu.msm.longDistance.data.trips.ModeGermany;

public enum OccupationStatus {
    WORKER,
    UNEMPLOYED,
    STUDENT;

    public static OccupationStatus valueOf(int occupationCode) {
        if(occupationCode == 1) {
            return WORKER;
        } else if(occupationCode == 2 || occupationCode == 0 || occupationCode == 4) {
            return UNEMPLOYED;
        } else if(occupationCode == 3) {
            return STUDENT;
        } else {
            throw new RuntimeException("Undefined occupation code given!");
        }
    }

    public int codeOf() {
        OccupationStatus m = this;
        if (m.equals(OccupationStatus.WORKER)) return 1;
        else if (m.equals(OccupationStatus.UNEMPLOYED)) return 2;
        else if (m.equals(OccupationStatus.STUDENT)) return 3;
        else return 1;

    }
}
