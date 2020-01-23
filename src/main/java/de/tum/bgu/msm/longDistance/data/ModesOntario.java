package de.tum.bgu.msm.longDistance.data;

/**
 * Created by carlloga on 8/2/2017.
 */
public enum ModesOntario implements Mode {

    AUTO, AIR, RAIL, BUS;

//    private int[] modes = {0, 1, 2, 3};
//    private String[] modeNames = {"auto", "air", "rail", "bus"};

    public String toString() {
        ModesOntario m = this;
        if (m.equals(ModesOntario.AUTO)) return "auto";
        else if (m.equals(ModesOntario.RAIL)) return "rail";
        else if (m.equals(ModesOntario.AIR)) return "air";
        else return "bus";

    }


    public static Mode getMode(int m) {
        if (m == 0) return ModesOntario.AUTO;
        else if (m == 1) return ModesOntario.AIR;
        else if (m == 2) return ModesOntario.RAIL;
        else return ModesOntario.BUS;
    }

}
