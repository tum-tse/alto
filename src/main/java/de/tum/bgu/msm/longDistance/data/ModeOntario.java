package de.tum.bgu.msm.longDistance.data;

/**
 * Created by carlloga on 8/2/2017.
 */
public enum ModeOntario implements Mode {

    AUTO, AIR, RAIL, BUS;

//    private int[] modes = {0, 1, 2, 3};
//    private String[] modeNames = {"auto", "air", "rail", "bus"};

    public String toString() {
        ModeOntario m = this;
        if (m.equals(ModeOntario.AUTO)) return "auto";
        else if (m.equals(ModeOntario.RAIL)) return "rail";
        else if (m.equals(ModeOntario.AIR)) return "air";
        else return "bus";

    }


    public static Mode getMode(int m) {
        if (m == 0) return ModeOntario.AUTO;
        else if (m == 1) return ModeOntario.AIR;
        else if (m == 2) return ModeOntario.RAIL;
        else return ModeOntario.BUS;
    }


}
