package de.tum.bgu.msm.longDistance.data.trips;

/**
 * Created by carlloga on 8/2/2017.
 */
public enum ModeGermany implements Mode {

    AUTO;
    //AUTO, AIR, RAIL, BUS;
//    private int[] modes = {0, 1, 2, 3};
//    private String[] modeNames = {"auto", "air", "rail", "bus"};

    public String toString() {
        return "auto";

    }


    public static Mode getMode(int m) {
        return ModeGermany.AUTO;
    }

/*    public String toString() {
        ModeGermany m = this;
        if (m.equals(ModeGermany.AUTO)) return "auto";
        else if (m.equals(ModeGermany.RAIL)) return "rail";
        else if (m.equals(ModeGermany.AIR)) return "air";
        else return "bus";

    }


    public static Mode getMode(int m) {
        if (m == 0) return ModeGermany.AUTO;
        else if (m == 1) return ModeGermany.AIR;
        else if (m == 2) return ModeGermany.RAIL;
        else return ModeGermany.BUS;
    }*/

}
