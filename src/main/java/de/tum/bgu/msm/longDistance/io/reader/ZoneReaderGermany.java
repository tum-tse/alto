package de.tum.bgu.msm.longDistance.io.reader;

import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.zoneSystem.*;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneGermany;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * Germany Model
 * Class to read zones
 * Author: Ana Moreno, Technical University of Munich (TUM), ana.moreno@tum.de
 * Date: 8 December 2020
 * Version 1
 * Adapted from Ontario
 * Version 1
 *
 */

public class ZoneReaderGermany implements ZoneReader {
    private static Logger logger = Logger.getLogger(ZoneReaderGermany.class);
    private JSONObject prop;

    private DataSet dataSet;
    private String inputFolder;
    private String outputFolder;

    private TableDataSet zoneTable;
    private TableDataSet externalCanadaTable;
    private TableDataSet externalUsTable;
    private TableDataSet externalOverseasTable;
    private double scaleFactor;


    public ZoneReaderGermany() {
    }

    public void setup(JSONObject prop, String inputFolder, String outputFolder) {
        this.inputFolder = inputFolder;
        this.outputFolder = outputFolder;
        this.prop = prop;
        //autoFileMatrixLookup = new String[]{rb.getString("auto.skim.file"), rb.getString("auto.skim.matrix"), rb.getString("auto.skim.lookup")};
        //distanceFileMatrixLookup = new String[]{rb.getString("dist.skim.file"), rb.getString("dist.skim.matrix"), rb.getString("dist.skim.lookup")};

        //zoneTable = Util.readCSVfile(rb.getString("int.can"));
        zoneTable = Util.readCSVfile(inputFolder + JsonUtilMto.getStringProp(prop, "zone_system.internal_file"));
        zoneTable.buildIndex(zoneTable.getColumnPosition("TAZ_id"));

        //scaleFactor = JsonUtilMto.getFloatProp(prop, "synthetic_population.scale_factor");

        logger.info("Zonal data manager set up");


    }

    public void load(DataSet dataset) {

        this.dataSet = dataset;
        List<Zone> zoneList;
        List<Zone> internalZones = readInternalZones();
        //List<Zone> externalZones = readExternalZones();

        zoneList = new ArrayList<>();
        zoneList.addAll(internalZones);
        //zoneList.addAll(externalZones);

        dataSet.setZones(zoneList.stream().collect(Collectors.toMap(Zone::getId, x -> x)));



        logger.info("Zonal data loaded");
    }

    public void run(DataSet dataSet, int nThreads) {
    }




    private List<Zone> readInternalZones() {
        //create zones objects (empty) and a map to find them in hh zone assignment

        int[] zones;
        List<Zone> internalZoneList = new ArrayList<>();

        zones = zoneTable.getColumnAsInt("TAZ_id");
        for (int zone : zones) {
            int combinedZone = (int) zoneTable.getIndexedValueAt(zone, "TAZ_id");
            int population = (int) zoneTable.getIndexedValueAt(zone, "pop");
            String zoneTypeStr = zoneTable.getIndexedStringValueAt(zone,"type");
            int areaType = (int) zoneTable.getIndexedValueAt(zone, "areaType");
            int distanceToTransit = (int) zoneTable.getIndexedValueAt(zone, "distanceToTransit");
            int hotels = (int) zoneTable.getIndexedValueAt(zone, "hotels");
            ZoneTypeGermany zoneType = ZoneTypeGermany.GERMANY;
            if (zoneTypeStr.equals("eu")){
                zoneType = ZoneTypeGermany.EXTEU;
            } else if (zoneTypeStr.equals("ov")){
                zoneType = ZoneTypeGermany.EXTOVERSEAS;
            }
            int area = (int) zoneTable.getIndexedValueAt(zone, "Area");
            //zones are created as empty as they are filled out using sp
            Zone internalZone = new ZoneGermany(zone, population, 0, zoneType, area, AreaTypeGermany.valueOf(areaType), distanceToTransit);
            ((ZoneGermany)internalZone).setHotels(hotels);
            internalZoneList.add(internalZone);
        }

        return internalZoneList;

    }

    private ArrayList<Zone> readExternalZones() {

        ArrayList<Zone> externalZonesArray = new ArrayList<>();

        int[] externalZonesCanada;
        int[] externalZonesUs;
        int[] externalZonesOverseas;

        //read the external zones from files

        externalZonesCanada = externalCanadaTable.getColumnAsInt("treso_zone");
        for (int externalZone : externalZonesCanada) {
            int combinedZone = (int) externalCanadaTable.getIndexedValueAt(externalZone, "ldpm_zone");
            int population = (int) (externalCanadaTable.getIndexedValueAt(externalZone, "population") * scaleFactor);
            int employment = (int) (externalCanadaTable.getIndexedValueAt(externalZone, "employment") * scaleFactor);
            Zone zone = new ZoneOntario(externalZone, population,
                    employment, ZoneTypeOntario.EXTCANADA, combinedZone);
            externalZonesArray.add(zone);
        }


        externalZonesUs = externalUsTable.getColumnAsInt("treso_zone");
        for (int externalZone : externalZonesUs) {
            //int combinedZone = (int) externalCanadaTable.getIndexedValueAt(externalZone, "combinedZone");
            int population = (int) (externalUsTable.getIndexedValueAt(externalZone, "population") * scaleFactor);
            int employment = (int) (externalUsTable.getIndexedValueAt(externalZone, "employment") * scaleFactor);
            Zone zone = new ZoneOntario(externalZone, population,
                    employment, ZoneTypeOntario.EXTUS, (int) externalUsTable.getIndexedValueAt(externalZone, "ldpm_zone"));

            externalZonesArray.add(zone);
        }


        externalZonesOverseas = externalOverseasTable.getColumnAsInt("treso_zone");
        for (int externalZone : externalZonesOverseas) {
            //int combinedZone = (int) externalCanadaTable.getIndexedValueAt(externalZone, "combinedZone");
            long staticAttraction = (long) externalOverseasTable.getIndexedValueAt(externalZone, "static_attraction");
            int population = (int) (externalOverseasTable.getIndexedValueAt(externalZone, "population") * scaleFactor);
            int employment = (int) (externalOverseasTable.getIndexedValueAt(externalZone, "employment") * scaleFactor);
            ZoneOntario zone = new ZoneOntario(externalZone, population,
                    employment, ZoneTypeOntario.EXTOVERSEAS, (int) externalOverseasTable.getIndexedValueAt(externalZone, "ldpm_zone"));
            zone.setStaticAttraction(staticAttraction);
            externalZonesArray.add(zone);
        }

        return externalZonesArray;
    }


}

