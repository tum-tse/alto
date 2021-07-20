package de.tum.bgu.msm.longDistance.data.trips;

/**
 *
 * Germany Model
 * Class to store long distance main modes
 * Author: Ana Moreno, Technical University of Munich (TUM), ana.moreno@tum.de
 * Date: 8 December 2020
 * Version 1
 * Adapted from Ontario
 * Version 1
 *
 */


public enum ModeGermany implements Mode {

    //AUTO;
    AUTO, AIR, RAIL, BUS, RAIL_SHUTTLE;
   private int[] modes = {0, 1, 2, 3, 4};
   private String[] modeNames = {"auto", "air", "rail", "bus", "rail_shuttle"};

   public String toString() {
        ModeGermany m = this;
        if (m.equals(ModeGermany.AUTO)) return "auto";
        else if (m.equals(ModeGermany.RAIL)) return "rail";
        else if (m.equals(ModeGermany.AIR)) return "air";
        else if (m.equals(ModeGermany.BUS)) return "bus";
        else return "rail_shuttle";

    }

    public static Mode getMode(int m) {
        if (m == 0) return ModeGermany.AUTO;
        else if (m == 1) return ModeGermany.AIR;
        else if (m == 2) return ModeGermany.RAIL;
        else if (m == 3) return ModeGermany.BUS;
        else return ModeGermany.RAIL_SHUTTLE;
    }

}
