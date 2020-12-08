package de.tum.bgu.msm.longDistance.data.trips;

/**
 *
 * Germany Model
 * Class to store trip types in the German implementation
 * Author: Ana Moreno, Technical University of Munich (TUM), ana.moreno@tum.de
 * Date: 8 December 2020
 * Version 1
 * Adapted from Ontario
 * Version 1
 *
 */

public enum TypeGermany implements Type {

    AWAY, DAYTRIP, INOUT;

//    public static final List<String> tripStates = Arrays.asList("away", "daytrip", "inout");


    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

    public static int getIndex(TypeGermany typeGermany) {
        switch (typeGermany) {
            case AWAY:
                return 0;
            case DAYTRIP:
                return 1;
            case INOUT:
                return 2;
            default:
                throw new RuntimeException("Type not defined");
        }

    }

}
