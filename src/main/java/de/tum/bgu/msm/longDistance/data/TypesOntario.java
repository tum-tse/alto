package de.tum.bgu.msm.longDistance.data;


public enum TypesOntario implements Type {

    AWAY, DAYTRIP, INOUT;

//    public static final List<String> tripStates = Arrays.asList("away", "daytrip", "inout");


    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
