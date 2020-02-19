package de.tum.bgu.msm.longDistance.tripGeneration;

import de.tum.bgu.msm.longDistance.DataSet;
import de.tum.bgu.msm.longDistance.data.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.ModelComponent;
import de.tum.bgu.msm.longDistance.data.sp.SyntheticPopulation;

import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.ResourceBundle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;



/**
 * Created by Joe on 28/10/2016.
 */
public class Generation implements ModelComponent {
    private ResourceBundle rb;
    private JSONObject prop;
    private DataSet dataSet;
    static Logger logger = LogManager.getLogger(Generation.class);
    private SyntheticPopulation synPop;

    //trip gen models
    private TripGenerationModule domesticTripGeneration;
    private TripGenerationModule internationalTripGeneration;
    private TripGenerationModule visitorsTripGeneration;
    //private ExtCanToIntTripGeneration extCanToIntTripGeneration;

    public Generation() {
    }


    public void setup(JSONObject prop, String inputFolder, String outputFolder ){
        this.prop = prop;

//        this.synPop = synPop;

        //create the trip generation models
        domesticTripGeneration = new DomesticTripGeneration(prop);
        internationalTripGeneration = new InternationalTripGeneration(prop);
        visitorsTripGeneration = new VisitorsTripGeneration(prop);
        //extCanToIntTripGeneration = new ExtCanToIntTripGeneration(rb);

        logger.info("Trip Generation model set up");
    }

    public void load(DataSet dataSet){
        this.dataSet = dataSet;

        domesticTripGeneration.load(dataSet);
        internationalTripGeneration.load(dataSet);
        visitorsTripGeneration.load(dataSet);

        logger.info("Trip generation loaded");
    }

    public void run(DataSet dataSet, int nThreads){
        dataSet.setAllTrips(generateTrips());
    }



    public ArrayList<LongDistanceTrip> generateTrips() {

        //initialize list of trips
        ArrayList<LongDistanceTrip> trips_dom_ontarian; //trips from Ontario to all Canada - sp based
        ArrayList<LongDistanceTrip> trips_int_ontarian; //trips from Ontario to other countries - sp based
        //ArrayList<LongDistanceTrip> trips_int_canadian; //trips from non-Ontario to other countries
        ArrayList<LongDistanceTrip> trips_visitors; //trips from non-Ontario to all Canada, and trips from other country to Canada

        //generate domestic trips
        trips_dom_ontarian = domesticTripGeneration.run();
        logger.info("  " + trips_dom_ontarian.size() + " domestic trips from Ontario generated");

        //generate international trips (must be done after domestic)
        trips_int_ontarian = internationalTripGeneration.run();
        logger.info("  " + trips_int_ontarian.size() + " international trips from Ontario generated");

        //generate visitors
        trips_visitors = visitorsTripGeneration.run();
        //logger.info("  Visitor trips to Canada generated");

        //trips_int_canadian = extCanToIntTripGeneration.runExtCanInternationalTripGeneration(zonalData.getExternalZoneList());
        //logger.info("  International trips from non-Ontarian zones generated");

        //join all the trips
        ArrayList<LongDistanceTrip> allTrips = new ArrayList<>();
        allTrips.addAll(trips_int_ontarian);
        allTrips.addAll(trips_dom_ontarian);
        allTrips.addAll(trips_visitors);
        //allTrips.addAll(trips_int_canadian);

        return allTrips;

    }

}
