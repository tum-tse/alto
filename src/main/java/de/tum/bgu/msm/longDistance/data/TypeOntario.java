package de.tum.bgu.msm.longDistance.data;


public enum TypeOntario implements Type {

    AWAY, DAYTRIP, INOUT;

//    public static final List<String> tripStates = Arrays.asList("away", "daytrip", "inout");


    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

    public static int getIndex(TypeOntario typeOntario) {
        switch (typeOntario) {
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
