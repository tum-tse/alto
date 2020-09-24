package de.tum.bgu.msm.longDistance.tripGeneration;

import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.trips.LongDistanceTripOntario;
import de.tum.bgu.msm.longDistance.io.reader.SyntheticPopulationReader;

import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;



/**
 * Created by Joe on 28/10/2016.
 */
public class TripGenerationOntario implements TripGeneration {
    private ResourceBundle rb;
    private JSONObject prop;
    private DataSet dataSet;
    static Logger logger = Logger.getLogger(TripGenerationOntario.class);
    private SyntheticPopulationReader synPop;

    //trip gen models
    private DomesticTripGeneration domesticTripGeneration;
    private InternationalTripGeneration internationalTripGeneration;
    private VisitorsTripGeneration visitorsTripGeneration;
    //private ExtCanToIntTripGeneration extCanToIntTripGeneration;

    public TripGenerationOntario() {
    }


    public void setup(JSONObject prop, String inputFolder, String outputFolder ){
        this.prop = prop;

//        this.synPop = synPop;

        //create the trip generation models
        domesticTripGeneration = new DomesticTripGeneration(prop, inputFolder, outputFolder);
        internationalTripGeneration = new InternationalTripGeneration(prop, inputFolder, outputFolder);
        visitorsTripGeneration = new VisitorsTripGeneration(prop, inputFolder, outputFolder);
        //extCanToIntTripGeneration = new ExtCanToIntTripGeneration(rb);

        logger.info("Trip TripGeneration model set up");
    }

    public void load(DataSet dataSet){
        this.dataSet = dataSet;

        domesticTripGeneration.load(dataSet);
        internationalTripGeneration.load(dataSet);
        visitorsTripGeneration.load(dataSet);

        logger.info("Trip generation loaded");
    }

    public void run(DataSet dataSet, int nThreads){
        generateTrips();
    }



    public void generateTrips() {

        //initialize list of trips
        ArrayList<LongDistanceTripOntario> trips_dom_ontarian; //trips from Ontario to all Canada - sp based
        ArrayList<LongDistanceTripOntario> trips_int_ontarian; //trips from Ontario to other countries - sp based
        //ArrayList<LongDistanceTrip> trips_int_canadian; //trips from non-Ontario to other countries
        ArrayList<LongDistanceTripOntario> trips_visitors; //trips from non-Ontario to all Canada, and trips from other country to Canada


        //generate domestic trips
        trips_dom_ontarian = domesticTripGeneration.run();
        dataSet.getAllTrips().addAll(trips_dom_ontarian);
        logger.info("  " + trips_dom_ontarian.size() + " domestic trips from Ontario generated");

        //generate international trips (must be done after domestic)
        trips_int_ontarian = internationalTripGeneration.run();
        dataSet.getAllTrips().addAll(trips_int_ontarian);
        logger.info("  " + trips_int_ontarian.size() + " international trips from Ontario generated");

        //generate visitors
        trips_visitors = visitorsTripGeneration.run();
        dataSet.getAllTrips().addAll(trips_visitors);
        //logger.info("  Visitor trips to Canada generated");


    }

}
