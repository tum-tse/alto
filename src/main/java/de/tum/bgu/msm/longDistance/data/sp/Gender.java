package de.tum.bgu.msm.longDistance.data.sp;

public enum Gender {
    MALE,
    FEMALE;

    public static Gender valueOf(int code) {
        if(code == 2) {
            return FEMALE;
        } else if(code == 1) {
            return MALE;
        } else {
            throw new RuntimeException("Undefined gender code given!");
        }
    }

    public int codeOf() {
        Gender g = this;
        if(g.equals(Gender.MALE)) {
            return 1;
        } else if(g.equals(Gender.FEMALE)) {
            return 2;
        } else {
            throw new RuntimeException("Undefined gender code given!");
        }
    }
}
