package de.tum.bgu.msm.longDistance.tripGeneration;

import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.trips.LongDistanceTripGermany;
import de.tum.bgu.msm.longDistance.data.trips.LongDistanceTripOntario;
import de.tum.bgu.msm.longDistance.io.reader.SyntheticPopulationReader;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.ResourceBundle;


/**
 * Germany Model
 * Module to simulate long-distance travel
 * Author: Ana Moreno, Technische Universität München (TUM), ana.moreno@tum.de
 * Date: 17 December 2020
 * Version 1
 * Adapted from Ontario Provincial Model
 */
public class TripGenerationGermany implements TripGeneration {
    private ResourceBundle rb;
    private JSONObject prop;
    private DataSet dataSet;
    static Logger logger = Logger.getLogger(TripGenerationGermany.class);
    private SyntheticPopulationReader synPop;

    //trip gen models
    private DomesticTripGenerationGermany domesticTripGeneration;
    //private InternationalTripGenerationGermany internationalTripGeneration;
    //private VisitorsTripGeneration visitorsTripGeneration;
    //private ExtCanToIntTripGeneration extCanToIntTripGeneration;

    public TripGenerationGermany() {
    }


    public void setup(JSONObject prop, String inputFolder, String outputFolder ){
        this.prop = prop;

//        this.synPop = synPop;

        //create the trip generation models
        domesticTripGeneration = new DomesticTripGenerationGermany(prop, inputFolder, outputFolder);
        //internationalTripGeneration = new InternationalTripGenerationGermany(prop, inputFolder, outputFolder);
        //visitorsTripGeneration = new VisitorsTripGeneration(prop, inputFolder, outputFolder);
        //extCanToIntTripGeneration = new ExtCanToIntTripGeneration(rb);

        logger.info("Trip TripGeneration model set up");
    }

    public void load(DataSet dataSet){
        this.dataSet = dataSet;

        domesticTripGeneration.load(dataSet);
        //internationalTripGeneration.load(dataSet);
        //visitorsTripGeneration.load(dataSet);

        logger.info("Trip generation loaded");
    }

    public void run(DataSet dataSet, int nThreads){
        generateTrips();
    }



    public void generateTrips() {

        //initialize list of trips
        ArrayList<LongDistanceTripGermany> trips_dom_ontarian; //trips from Ontario to all Canada - sp based
        ArrayList<LongDistanceTripGermany> trips_int_ontarian; //trips from Ontario to other countries - sp based
        //ArrayList<LongDistanceTrip> trips_int_canadian; //trips from non-Ontario to other countries
        ArrayList<LongDistanceTripGermany> trips_visitors; //trips from non-Ontario to all Canada, and trips from other country to Canada


        //generate domestic trips
        trips_dom_ontarian = domesticTripGeneration.run();
        dataSet.getAllTrips().addAll(trips_dom_ontarian);
        logger.info("  " + trips_dom_ontarian.size() + " domestic trips from Ontario generated");

        //generate international trips (must be done after domestic)
        //trips_int_ontarian = internationalTripGeneration.run();
        //dataSet.getAllTrips().addAll(trips_int_ontarian);
        //logger.info("  " + trips_int_ontarian.size() + " international trips from Ontario generated");

        //generate visitors
        //trips_visitors = visitorsTripGeneration.run();
       // dataSet.getAllTrips().addAll(trips_visitors);
        //logger.info("  Visitor trips to Canada generated");


    }

}
