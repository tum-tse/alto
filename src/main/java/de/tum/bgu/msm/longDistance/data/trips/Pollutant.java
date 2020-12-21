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


public enum Pollutant {

    //CO2, NOX;
    CO2;
    private int[] pollutants = {0};
    private String[] pollutantsName = {"CO2"};

    public String toString() {
        Pollutant m = this;
        if (m.equals(Pollutant.CO2)) return "co2";
        else return "co2";

    }

    public static Pollutant getPollutant(int m) {
        if (m == 0) return Pollutant.CO2;
        else return Pollutant.CO2;
    }
}
