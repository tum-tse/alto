package de.tum.bgu.msm.longDistance.data.airport;

public enum AirportDestinationType {

    DOMESTIC_AND_INTERNATIONAL, //airport with direct flights to most of the destinations. Can have flights and routes
    ONLY_INTERNATIONAL; //other airport in a zone. Can have flights but not routes


    public static AirportDestinationType valueOf(int code) {
        if(code == 1) {
            return DOMESTIC_AND_INTERNATIONAL;
        } else if(code == 2) {
            return ONLY_INTERNATIONAL;
        } else {
            throw new RuntimeException("Undefined airport code given!");
        }
    }

}
