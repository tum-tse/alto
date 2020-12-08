package de.tum.bgu.msm.longDistance.data.trips;


/**
 *
 * Germany model
 * Class to store trip purposes
 * Author: Ana Moreno, Technical University of Munich (TUM), ana.moreno@tum.de
 * Date: 8 December 2020
 * Version 1
 * Adapted from Ontario
 * Version 1
 *
 */

public enum PurposeGermany implements Purpose {

    VISIT, BUSINESS, LEISURE;
    //public static final List<String> tripPurposes = Arrays.asList("visit", "business", "leisure");


    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
