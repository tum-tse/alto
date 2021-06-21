package de.tum.bgu.msm.longDistance.io.writer;

import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.trips.*;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneGermany;
import org.json.simple.JSONObject;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Random;

public class TripsToPlans {

    private String outputFile;
    private String outputFolder;
    private String outputFileName;

    private Scenario scenario;
    private Population pop;

    private float  BUSINESS_AUTO_OCCUPANCY;
    private float  LEISURE_AUTO_OCCUPANCY;
    private float  PRIVATE_AUTO_OCCUPANCY;

    double boardingTime_sec;
    double postProcessingTime_sec = 15*60;

    public void setup(JSONObject prop, String inputFolder, String outputFolderInput) {
        outputFolder = outputFolderInput;
        outputFileName = JsonUtilMto.getStringProp(prop, "output.trip_file") ;

        BUSINESS_AUTO_OCCUPANCY = JsonUtilMto.getFloatProp(prop, "tripAssignment.tripParty_autoTrips.business");
        LEISURE_AUTO_OCCUPANCY = JsonUtilMto.getFloatProp(prop, "tripAssignment.tripParty_autoTrips.leisure");
        PRIVATE_AUTO_OCCUPANCY = JsonUtilMto.getFloatProp(prop, "tripAssignment.tripParty_autoTrips.private");

        boardingTime_sec = JsonUtilMto.getFloatProp(prop, "airport.boardingTime_min") * 60;
        postProcessingTime_sec = JsonUtilMto.getFloatProp(prop, "airport.postProcessTime_min") * 60;
    }

    public void load(DataSet dataSet) {
        this.outputFile = outputFolder + dataSet.getPopulationSection() + "_" + outputFileName;

        Config config = ConfigUtils.createConfig();
        scenario = ScenarioUtils.loadScenario(config);
        pop = scenario.getPopulation();
    }

    public void run(DataSet dataSet, int nThreads) {
        Random rmd = new Random(1);

        for (LongDistanceTrip tr : dataSet.getAllTrips()) {
            generatePlans(tr, rmd.nextDouble());
        }

        PopulationWriter pw = new PopulationWriter(pop);
        pw.write(outputFile);

    }



    private void generatePlans(LongDistanceTrip tr, double prob) {

        LongDistanceTripGermany trip = (LongDistanceTripGermany) tr;

        int tId = trip.getTripId();
        int pId = trip.getTravellerId();

        double origX = trip.getOrigZone().getZoneX();
        double origY = trip.getOrigZone().getZoneY();
        Coord origin = new Coord(origX, origY);

        double destX = ((ZoneGermany)trip.getDestZone()).getZoneX();
        double destY = ((ZoneGermany)trip.getDestZone()).getZoneY();
        Coord destination = new Coord(destX, destY);

        double originAirportX = trip.getAdditionalAttributes().get("originAirportX");
        double originAirportY = trip.getAdditionalAttributes().get("originAirportY");
        Coord originAirport = new Coord(originAirportX, originAirportY);

        double destinationAirportX = trip.getAdditionalAttributes().get("destinationAirportX");
        double destinationAirportY = trip.getAdditionalAttributes().get("destinationAirportY");
        Coord destinationAirport = new Coord(destinationAirportX, destinationAirportY);

        Mode mode = trip.getMode();
        Purpose purpose = trip.getTripPurpose();

        int departureTime_sec = trip.getDepartureTimesInMin() * 60;
        double departureTimeReturningDaytrip_sec = trip.getDepartureTimeInHoursSecondSegment() * 3600;

        double time_air_sec = trip.getAdditionalAttributes().get("time_air") * 3600;
        double timeAccess_air_sec = trip.getAdditionalAttributes().get("timeAccess_air") * 3600;
        double timeEgress_air_sec = trip.getAdditionalAttributes().get("timeEgress_air") * 3600;

        double selPosition;

        if (purpose.equals(PurposeGermany.BUSINESS)) {
            selPosition = 1 / BUSINESS_AUTO_OCCUPANCY;
        } else if(purpose.equals(PurposeGermany.LEISURE)) {
            selPosition = 1 / LEISURE_AUTO_OCCUPANCY;
        } else {
            selPosition = 1 / PRIVATE_AUTO_OCCUPANCY;
        }

        if (prob <= selPosition){
            if (mode.equals(ModeGermany.AUTO)){

                if (trip.getTripState().equals(TypeGermany.DAYTRIP)){

                    //1st trip leg
                    Id<Person> personId1 = Id.createPersonId(tId);
                    Person person1 = scenario.getPopulation().getFactory().createPerson(personId1);
                    Plan plan1 = scenario.getPopulation().getFactory().createPlan();

                    Activity activity1 = scenario.getPopulation().getFactory().createActivityFromCoord("home", origin);
                    activity1.setEndTime(departureTime_sec);
                    plan1.addActivity(activity1);

                    Leg leg1 = scenario.getPopulation().getFactory().createLeg(mode.toString());
                    plan1.addLeg(leg1);

                    Activity activity2 = scenario.getPopulation().getFactory().createActivityFromCoord(purpose.toString(), destination);
                    plan1.addActivity(activity2);

                    person1.addPlan(plan1);
                    pop.addPerson(person1);
                    System.out.println("PersonID " + personId1);

                    //2nd trip leg
                    Id<Person> personId2 = Id.createPersonId(1_000_000 + tId);
                    Person person2 = scenario.getPopulation().getFactory().createPerson(personId2);
                    Plan plan2 = scenario.getPopulation().getFactory().createPlan();

                    activity2.setEndTime(departureTimeReturningDaytrip_sec);
                    plan2.addActivity(activity2);

                    Leg leg2 = scenario.getPopulation().getFactory().createLeg(mode.toString());
                    plan2.addLeg(leg2);

                    Activity activity3 = scenario.getPopulation().getFactory().createActivityFromCoord("home", origin);
                    plan2.addActivity(activity3);

                    person2.addPlan(plan2);
                    pop.addPerson(person2);
                    System.out.println("PersonID " + personId2);

                } else if(trip.getTripState().equals(TypeGermany.OVERNIGHT)){

                    if(!trip.isReturnOvernightTrip()){

                        //1st trip leg
                        Id<Person> personId = Id.createPersonId(tId);
                        Person person = scenario.getPopulation().getFactory().createPerson(personId);
                        Plan plan = scenario.getPopulation().getFactory().createPlan();

                        Activity activity1 = scenario.getPopulation().getFactory().createActivityFromCoord("home", origin);
                        activity1.setEndTime(departureTime_sec);
                        plan.addActivity(activity1);

                        Leg leg1 = scenario.getPopulation().getFactory().createLeg(mode.toString());
                        plan.addLeg(leg1);

                        Activity activity2 = scenario.getPopulation().getFactory().createActivityFromCoord(purpose.toString(), destination);
                        plan.addActivity(activity2);

                        person.addPlan(plan);
                        pop.addPerson(person);
                        System.out.println("PersonID " + personId);

                        if (trip.isInternational()) {

                            //visitor leg
                            Id<Person> visitorId = Id.createPersonId(6_000_000 + tId);
                            Person visitor = scenario.getPopulation().getFactory().createPerson(visitorId);
                            Plan visitorPlan = scenario.getPopulation().getFactory().createPlan();

                            Activity activityVisitor1 = scenario.getPopulation().getFactory().createActivityFromCoord("home", destination);
                            activityVisitor1.setEndTime(departureTime_sec);
                            visitorPlan.addActivity(activityVisitor1);

                            Leg legVisitor1 = scenario.getPopulation().getFactory().createLeg(mode.toString());
                            visitorPlan.addLeg(legVisitor1);

                            Activity activityVisitor2 = scenario.getPopulation().getFactory().createActivityFromCoord("visitor", origin);
                            visitorPlan.addActivity(activityVisitor2);

                            visitor.addPlan(visitorPlan);
                            pop.addPerson(visitor);
                            System.out.println("PersonID " + visitorId);

                        }

                    }else{

                        //1st trip leg
                        Id<Person> personId = Id.createPersonId(tId);
                        Person person = scenario.getPopulation().getFactory().createPerson(personId);
                        Plan plan = scenario.getPopulation().getFactory().createPlan();

                        Activity activity1 = scenario.getPopulation().getFactory().createActivityFromCoord(purpose.toString(), destination);
                        activity1.setEndTime(departureTime_sec);
                        plan.addActivity(activity1);

                        Leg leg1 = scenario.getPopulation().getFactory().createLeg(mode.toString());
                        plan.addLeg(leg1);

                        Activity activity2 = scenario.getPopulation().getFactory().createActivityFromCoord("home", origin);
                        plan.addActivity(activity2);

                        person.addPlan(plan);
                        pop.addPerson(person);
                        System.out.println("PersonID " + personId);

                        if (trip.isInternational()) {

                            //visitor leg
                            Id<Person> visitorId = Id.createPersonId(6_000_000 + tId);
                            Person visitor = scenario.getPopulation().getFactory().createPerson(visitorId);
                            Plan visitorPlan = scenario.getPopulation().getFactory().createPlan();

                            Activity activityVisitor1 = scenario.getPopulation().getFactory().createActivityFromCoord("visitor", origin);
                            activityVisitor1.setEndTime(departureTime_sec);
                            visitorPlan.addActivity(activityVisitor1);

                            Leg legVisitor1 = scenario.getPopulation().getFactory().createLeg(mode.toString());
                            visitorPlan.addLeg(legVisitor1);

                            Activity activityVisitor2 = scenario.getPopulation().getFactory().createActivityFromCoord("home", destination);
                            visitorPlan.addActivity(activityVisitor2);

                            visitor.addPlan(visitorPlan);
                            pop.addPerson(visitor);
                            System.out.println("PersonID " + visitorId);

                        }

                    }

                } else{
                    System.out.println("Away trip is skipped.");
                }

            }else if(mode.equals(ModeGermany.AIR)){

                if (trip.getTripState().equals(TypeGermany.DAYTRIP)){

                    if (!trip.isInternational()){

                        //1st trip leg
                        Id<Person> personId1 = Id.createPersonId(tId); //get initial person id to new
                        Person person1 = scenario.getPopulation().getFactory().createPerson(personId1);
                        Plan plan1 = scenario.getPopulation().getFactory().createPlan();

                        Activity activity1 = scenario.getPopulation().getFactory().createActivityFromCoord("home", origin);
                        activity1.setEndTime(departureTime_sec);
                        plan1.addActivity(activity1);

                        Leg leg1 = scenario.getPopulation().getFactory().createLeg("auto");
                        plan1.addLeg(leg1);

                        Activity airport1 = scenario.getPopulation().getFactory().createActivityFromCoord(purpose.toString(), originAirport);
                        plan1.addActivity(airport1);

                        person1.addPlan(plan1);
                        pop.addPerson(person1);
                        System.out.println("PersonID " + personId1);

                        //2nd trip leg
                        Id<Person> personId2 = Id.createPersonId(1_000_000 + tId); //get initial person id to new
                        Person person2 = scenario.getPopulation().getFactory().createPerson(personId2);
                        Plan plan2 = scenario.getPopulation().getFactory().createPlan();

                        Activity airport2 = scenario.getPopulation().getFactory().createActivityFromCoord(purpose.toString(), destinationAirport);
                        airport2.setEndTime(departureTime_sec + timeAccess_air_sec + time_air_sec);
                        plan2.addActivity(airport2);

                        Leg airLeg2 = scenario.getPopulation().getFactory().createLeg("auto");
                        plan2.addLeg(airLeg2);

                        Activity activity2 = scenario.getPopulation().getFactory().createActivityFromCoord(purpose.toString(), destination);
                        plan2.addActivity(activity2);

                        person2.addPlan(plan2);
                        pop.addPerson(person2);
                        System.out.println("PersonID " + personId2);

                        //3rd trip leg
                        Id<Person> personId3 = Id.createPersonId(2_000_000 + tId); //get initial person id to new
                        Person person3 = scenario.getPopulation().getFactory().createPerson(personId3);
                        Plan plan3 = scenario.getPopulation().getFactory().createPlan();

                        activity2.setEndTime(departureTimeReturningDaytrip_sec);
                        plan3.addActivity(activity2);

                        Leg airLeg3 = scenario.getPopulation().getFactory().createLeg("auto");
                        plan3.addLeg(airLeg3);

                        Activity airport3 = scenario.getPopulation().getFactory().createActivityFromCoord(purpose.toString(), destinationAirport);
                        plan3.addActivity(airport3);

                        person3.addPlan(plan3);
                        pop.addPerson(person3);
                        System.out.println("PersonID " + personId3);

                        //4th trip leg
                        Id<Person> personId4 = Id.createPersonId(3_000_000 + tId); //get initial person id to new
                        Person person4 = scenario.getPopulation().getFactory().createPerson(personId4);
                        Plan plan4 = scenario.getPopulation().getFactory().createPlan();

                        Activity airport4 = scenario.getPopulation().getFactory().createActivityFromCoord(purpose.toString(), originAirport);
                        airport4.setEndTime(departureTimeReturningDaytrip_sec + timeEgress_air_sec + time_air_sec);
                        plan4.addActivity(airport4);

                        Leg leg2 = scenario.getPopulation().getFactory().createLeg("auto");
                        plan4.addLeg(leg2);

                        Activity activity3 = scenario.getPopulation().getFactory().createActivityFromCoord("home", origin);
                        plan4.addActivity(activity3);

                        person4.addPlan(plan4);
                        pop.addPerson(person4);
                        System.out.println("PersonID " + personId4);

                    }else{

                        //1st trip leg
                        Id<Person> personId1 = Id.createPersonId(tId); //get initial person id to new
                        Person person1 = scenario.getPopulation().getFactory().createPerson(personId1);
                        Plan plan1 = scenario.getPopulation().getFactory().createPlan();

                        Activity activity1 = scenario.getPopulation().getFactory().createActivityFromCoord("home", origin);
                        activity1.setEndTime(departureTime_sec);
                        plan1.addActivity(activity1);

                        Leg leg1 = scenario.getPopulation().getFactory().createLeg("auto");
                        plan1.addLeg(leg1);

                        Activity airport1 = scenario.getPopulation().getFactory().createActivityFromCoord(purpose.toString(), originAirport);
                        plan1.addActivity(airport1);

                        person1.addPlan(plan1);
                        pop.addPerson(person1);
                        System.out.println("PersonID " + personId1);

                        //2nd trip leg
                        Id<Person> personId2 = Id.createPersonId(1_000_000 + tId); //get initial person id to new
                        Person person2 = scenario.getPopulation().getFactory().createPerson(personId2);
                        Plan plan2 = scenario.getPopulation().getFactory().createPlan();

                        airport1.setEndTime(departureTimeReturningDaytrip_sec + timeEgress_air_sec + time_air_sec);
                        plan2.addActivity(airport1);

                        Leg leg2 = scenario.getPopulation().getFactory().createLeg("auto");
                        plan2.addLeg(leg2);

                        Activity activity3 = scenario.getPopulation().getFactory().createActivityFromCoord("home", origin);
                        plan2.addActivity(activity3);

                        person2.addPlan(plan2);
                        pop.addPerson(person2);
                        System.out.println("PersonID " + personId2);
                    }


                }else if(trip.getTripState().equals(TypeGermany.OVERNIGHT)){

                    Id<Person> personId1 = Id.createPersonId(tId); //get initial person id to new
                    Person person1 = scenario.getPopulation().getFactory().createPerson(personId1);
                    Plan plan1 = scenario.getPopulation().getFactory().createPlan();

                    Id<Person> personId2 = Id.createPersonId(1_000_000 + tId); //get initial person id to new
                    Person person2 = scenario.getPopulation().getFactory().createPerson(personId2);
                    Plan plan2 = scenario.getPopulation().getFactory().createPlan();

                    if(!trip.isReturnOvernightTrip()){

                        if (!trip.isInternational()){

                            //1st trip leg
                            Activity activity1 = scenario.getPopulation().getFactory().createActivityFromCoord("home", origin);
                            activity1.setEndTime(departureTime_sec);
                            plan1.addActivity(activity1);

                            Leg leg1 = scenario.getPopulation().getFactory().createLeg("auto");
                            plan1.addLeg(leg1);

                            Activity airport1 = scenario.getPopulation().getFactory().createActivityFromCoord(purpose.toString(), originAirport);
                            airport1.setEndTime(departureTime_sec + timeAccess_air_sec + boardingTime_sec);
                            plan1.addActivity(airport1);

                            person1.addPlan(plan1);
                            pop.addPerson(person1);
                            System.out.println("PersonID " + personId1);

                            //2nd trip leg
                            Activity airport2 = scenario.getPopulation().getFactory().createActivityFromCoord(purpose.toString(), destinationAirport);
                            airport2.setEndTime(departureTime_sec + timeAccess_air_sec + time_air_sec);
                            plan2.addActivity(airport2);

                            Leg leg2 = scenario.getPopulation().getFactory().createLeg("auto");
                            plan2.addLeg(leg2);

                            Activity activity2 = scenario.getPopulation().getFactory().createActivityFromCoord(purpose.toString(), destination);
                            plan2.addActivity(activity2);

                            person2.addPlan(plan2);
                            pop.addPerson(person2);
                            System.out.println("PersonID " + personId2);

                        }else{

                            //1st trip leg
                            Activity activity1 = scenario.getPopulation().getFactory().createActivityFromCoord("home", origin);
                            activity1.setEndTime(departureTime_sec);
                            plan1.addActivity(activity1);

                            Leg leg1 = scenario.getPopulation().getFactory().createLeg("auto");
                            plan1.addLeg(leg1);

                            Activity airport1 = scenario.getPopulation().getFactory().createActivityFromCoord(purpose.toString(), originAirport);
                            plan1.addActivity(airport1);

                            person1.addPlan(plan1);
                            pop.addPerson(person1);
                            System.out.println("PersonID " + personId1);

                            //visitor leg
                            Id<Person> visitorId = Id.createPersonId(6_000_000 + tId);
                            Person visitor = scenario.getPopulation().getFactory().createPerson(visitorId);
                            Plan visitorPlan = scenario.getPopulation().getFactory().createPlan();

                            Activity activityVisitor1 = scenario.getPopulation().getFactory().createActivityFromCoord("visitor", originAirport);
                            activityVisitor1.setEndTime(departureTime_sec);
                            visitorPlan.addActivity(activityVisitor1);

                            Leg legVisitor1 = scenario.getPopulation().getFactory().createLeg(mode.toString());
                            visitorPlan.addLeg(legVisitor1);

                            Activity activityVisitor2 = scenario.getPopulation().getFactory().createActivityFromCoord("home", origin);
                            visitorPlan.addActivity(activityVisitor2);

                            visitor.addPlan(visitorPlan);
                            pop.addPerson(visitor);
                            System.out.println("PersonID " + visitorId);

                        }

                    }else{

                        if (!trip.isInternational()){

                            //1st trip leg
                            Activity activity1 = scenario.getPopulation().getFactory().createActivityFromCoord(purpose.toString(), destination);
                            activity1.setEndTime(departureTime_sec);
                            plan1.addActivity(activity1);

                            Leg leg1 = scenario.getPopulation().getFactory().createLeg("auto");
                            plan1.addLeg(leg1);

                            Activity airport1 = scenario.getPopulation().getFactory().createActivityFromCoord(purpose.toString(), destinationAirport);
                            plan1.addActivity(airport1);

                            person1.addPlan(plan1);
                            pop.addPerson(person1);
                            System.out.println("PersonID " + personId1);

                            //2nd trip leg
                            Activity airport2 = scenario.getPopulation().getFactory().createActivityFromCoord(purpose.toString(), originAirport);
                            airport2.setEndTime(departureTime_sec + timeEgress_air_sec + time_air_sec);
                            plan2.addActivity(airport2);

                            Leg leg2 = scenario.getPopulation().getFactory().createLeg("auto");
                            plan2.addLeg(leg2);

                            Activity activity2 = scenario.getPopulation().getFactory().createActivityFromCoord("home", origin);
                            plan2.addActivity(activity2);

                            person2.addPlan(plan2);
                            pop.addPerson(person2);
                            System.out.println("PersonID " + personId2);

                        }else{

                            //1st trip leg
                            Activity airport1 = scenario.getPopulation().getFactory().createActivityFromCoord(purpose.toString(), originAirport);
                            airport1.setEndTime(departureTime_sec + timeEgress_air_sec + time_air_sec);
                            plan1.addActivity(airport1);

                            Leg leg1 = scenario.getPopulation().getFactory().createLeg("auto");
                            plan1.addLeg(leg1);

                            Activity activity1 = scenario.getPopulation().getFactory().createActivityFromCoord("home", origin);
                            plan1.addActivity(activity1);

                            person1.addPlan(plan1);
                            pop.addPerson(person1);
                            System.out.println("PersonID " + personId1);

                            //visitor leg
                            Id<Person> visitorId = Id.createPersonId(6_000_000 + tId);
                            Person visitor = scenario.getPopulation().getFactory().createPerson(visitorId);
                            Plan visitorPlan = scenario.getPopulation().getFactory().createPlan();

                            Activity activityVisitor1 = scenario.getPopulation().getFactory().createActivityFromCoord("home", origin);
                            activityVisitor1.setEndTime(departureTime_sec);
                            visitorPlan.addActivity(activityVisitor1);

                            Leg legVisitor1 = scenario.getPopulation().getFactory().createLeg(mode.toString());
                            visitorPlan.addLeg(legVisitor1);

                            Activity activityVisitor2 = scenario.getPopulation().getFactory().createActivityFromCoord("visitor", originAirport);
                            visitorPlan.addActivity(activityVisitor2);

                            visitor.addPlan(visitorPlan);
                            pop.addPerson(visitor);
                            System.out.println("PersonID " + visitorId);
                        }
                    }
                }else{
                    System.out.println("Away trip is skipped.");
                }

            }else{
                System.out.println("Bus or rail trip is skipped.");
            }
        } else {
            System.out.println("Traveling with someone else.");
        }

    }

}
