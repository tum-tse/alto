package de.tum.bgu.msm.longDistance.data;

public enum PurposesOntario implements Purpose {

    VISIT, BUSINESS, LEISURE;
    //public static final List<String> tripPurposes = Arrays.asList("visit", "business", "leisure");


    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
