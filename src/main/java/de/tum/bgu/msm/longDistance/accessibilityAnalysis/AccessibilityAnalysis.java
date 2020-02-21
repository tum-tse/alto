package de.tum.bgu.msm.longDistance.accessibilityAnalysis;

import com.pb.common.util.ResourceUtil;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.DataSet;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneOntario;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneTypeOntario;
import de.tum.bgu.msm.longDistance.io.reader.SkimsReaderOntario;
import de.tum.bgu.msm.longDistance.tripGeneration.DomesticTripGeneration;
import de.tum.bgu.msm.longDistance.data.zoneSystem.Zone;

import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Created by carlloga on 4/26/2017.
 */
public class AccessibilityAnalysis {
    private ResourceBundle rb;
    private DataSet dataSet;
    static Logger logger = Logger.getLogger(DomesticTripGeneration.class);
    private List<String> fromZones;
    private List<String> toZones;

    private float alphaAuto;
    private float betaAuto;

    public AccessibilityAnalysis (ResourceBundle rb, SkimsReaderOntario zoneReader){
        this.rb = rb;
        this.dataSet = dataSet;




        zoneReader.readSkims();
        //zoneReader.readSkim("transit");
        //input parameters for accessibility calculations from mto properties
        alphaAuto = (float) ResourceUtil.getDoubleProperty(rb, "auto.accessibility.alpha");
        betaAuto = (float) ResourceUtil.getDoubleProperty(rb, "auto.accessibility.beta");
        fromZones = ResourceUtil.getListWithUserDefinedSeparator(rb, "orig.zone.type", ",");
        toZones = ResourceUtil.getListWithUserDefinedSeparator(rb, "dest.zone.type", ",");

    }

    public void calculateAccessibilityForAnalysis(){

        calculateAccessibility(dataSet, fromZones, toZones, alphaAuto, betaAuto);
        writeOutAccessibilities(dataSet);
        logger.info("Accessibility analysis completed using alpha = " + alphaAuto + " and beta = " + betaAuto);

    }

    public static void calculateAccessibility(DataSet dataSet, List<String> fromZones, List<String> toZones, float alphaAuto, float betaAuto) {

        //read alpha and beta parameters
        logger.info("   Calculating accessibilities");

        //create lists of origin and destination zones

        ArrayList<Zone> origZoneList = new ArrayList<>();
        for (String stringZoneType : fromZones) {
            for (Zone zone : dataSet.getZones().values()) {
                if (zone.getZoneType().equals(ZoneTypeOntario.getZoneType(stringZoneType))) origZoneList.add(zone);
            }
        }

        ArrayList<Zone> destZoneList = new ArrayList<>();
        for (String stringZoneType : toZones) {
            for (Zone zone : dataSet.getZones().values()) {
                if (zone.getZoneType().equals(ZoneTypeOntario.getZoneType(stringZoneType))) destZoneList.add(zone);
            }
        }

        double autoAccessibility;
        //calculate accessibilities
        for (Zone origZone : origZoneList) {
            autoAccessibility = 0;
            for (Zone destZone : destZoneList) {
                double autoImpedance;
                //limit the minimum travel time for accessibility calculations (namely long distance accessibility)
                //if (getAutoTravelTime(origZone.getId(), destZone.getId()) > 90) {
                if (dataSet.getAutoTravelTime(origZone.getId(), destZone.getId()) <= 0) {      // should never happen for auto, but has appeared for intrazonal trip length
                    autoImpedance = 0;
                } else {
                    autoImpedance = Math.exp(betaAuto * dataSet.getAutoTravelTime(origZone.getId(), destZone.getId()));
                }

                autoAccessibility += Math.pow(((ZoneOntario) destZone).getPopulation(), alphaAuto) * autoImpedance;

            }
            ((ZoneOntario) origZone).setAccessibility(autoAccessibility);


        }
        logger.info("Accessibility (raster zone level) calculated using alpha= " + alphaAuto + " and beta= " + betaAuto);
        //scaling accessibility (only for Ontario zones --> 100 is assigned to the highest value in Ontario)
        double[] autoAccessibilityArray = new double[dataSet.getZones().size()];

        int i = 0;
        double highestVal = 0;
        for (Zone zone : dataSet.getZones().values()) {
            ZoneOntario zoneOntario = (ZoneOntario) zone;
            autoAccessibilityArray[i] = zoneOntario.getAccessibility();
            if (autoAccessibilityArray[i] > highestVal & zone.getZoneType().equals(ZoneTypeOntario.ONTARIO)) {
                highestVal = autoAccessibilityArray[i];
            }
            i++;
        }
        i = 0;
        for (Zone zone : dataSet.getZones().values()) {
            ZoneOntario zoneOntario = (ZoneOntario) zone;
            zoneOntario.setAccessibility(autoAccessibilityArray[i] / highestVal * 100);
            i++;
        }

    }

    public void writeOutAccessibilities(DataSet dataSet) {
        //print out accessibilities - no longer used

        Collection<Zone> zoneList = dataSet.getZones().values();

        String fileName = rb.getString("access.out.file") + ".csv";
        PrintWriter pw = Util.openFileForSequentialWriting(fileName, false);
        pw.println("Zone,Accessibility,Population,Employments");

        logger.info("Print out data of accessibility");

        for (Zone zone : zoneList) {

            ZoneOntario zoneOntario = (ZoneOntario) zone;

            pw.println(zoneOntario.getId() + "," + zoneOntario.getAccessibility() + "," + zoneOntario.getPopulation() + "," + zoneOntario.getEmployment());
        }
        pw.close();
    }

}
