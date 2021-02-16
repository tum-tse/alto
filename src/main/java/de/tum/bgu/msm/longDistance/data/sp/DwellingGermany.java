package de.tum.bgu.msm.longDistance.data.sp;

import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneGermany;
import org.apache.log4j.Logger;

public class DwellingGermany {

    static Logger logger = Logger.getLogger(DwellingGermany.class);

    private int id;
    private int hhId;
    private int taz;
    private ZoneGermany zone;
    private double coordX;
    private double coordY;


    public DwellingGermany(int id, int hhId, int taz, ZoneGermany zone, double coordX, double coordY) {
        this.id = id;
        this.hhId = hhId;
        this.taz = taz;
        this.zone = zone;
        this.coordX = coordX;
        this.coordY = coordY;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getHhId() {
        return hhId;
    }

    public void setHhId(int hhId) {
        this.hhId = hhId;
    }

    public int getTaz() {
        return taz;
    }

    public void setTaz(int taz) {
        this.taz = taz;
    }

    public ZoneGermany getZone() {
        return zone;
    }

    public void setZone(ZoneGermany zone) {
        this.zone = zone;
    }

    public double getCoordX() {
        return coordX;
    }

    public void setCoordX(double coordX) {
        this.coordX = coordX;
    }

    public double getCoordY() {
        return coordY;
    }

    public void setCoordY(double coordY) {
        this.coordY = coordY;
    }
}
