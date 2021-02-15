package de.tum.bgu.msm.longDistance.data.airport;

public enum  AirportType {
    HUB, //airport with direct flights to most of the destinations. Can have flights and routes
    MAIN, //main airport of a zone. Can have flights and routes
    FEEDER_ZONE; //other airport in a zone. Can have flights but not routes


    public static AirportType valueOf(int code) {
        if(code == 1) {
            return HUB;
        } else if(code == 2) {
            return MAIN;
        } else if(code == 3) {
            return FEEDER_ZONE;
        } else {
            throw new RuntimeException("Undefined airport code given!");
        }
    }

}
