package de.tum.bgu.msm.longDistance.data.zoneSystem;

public enum AreaTypeGermany {
    CORE_CITY(10),
    CITY (20),
    MEDIUM_SIZED_CITY(30),
    TOWN(40),
    RURAL(50),
    NOT_AVAILABLE(-999);

    private final int code;

    AreaTypeGermany(int code) {
        this.code = code;
    }

    public static AreaTypeGermany valueOf(int code) {
        switch (code) {
            case 10:
                return CORE_CITY;
            case 20:
                return CITY;
            case 30:
                return MEDIUM_SIZED_CITY;
            case 40:
                return TOWN;
            case 50:
                return RURAL;
            case -999:
                return NOT_AVAILABLE;
            default:
                throw new RuntimeException("Area Type for code " + code + " not specified in SGtyp classification.");
        }
    }

    public int code() {
        return code;
    }
}
