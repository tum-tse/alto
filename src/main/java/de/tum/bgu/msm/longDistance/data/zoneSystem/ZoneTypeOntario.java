package de.tum.bgu.msm.longDistance.data.zoneSystem;

import java.util.Arrays;
import java.util.Collection;

/**
 * Created by carlloga on 8/10/2016.
 */
public enum ZoneTypeOntario implements ZoneType {
    ONTARIO, EXTCANADA, EXTUS, EXTOVERSEAS;

    public static ZoneType getZoneType(String stringZoneType) {
        if (stringZoneType.equals("ONTARIO")) return ZoneTypeOntario.ONTARIO;
        else if (stringZoneType.equals("EXTCANADA")) return ZoneTypeOntario.EXTCANADA;
        else if (stringZoneType.equals("EXTUS")) return ZoneTypeOntario.EXTUS;
        else return ZoneTypeOntario.EXTOVERSEAS;

    }

    @Override
    public String toString(){
        ZoneTypeOntario zt = this;
        if (zt.equals(ZoneTypeOntario.ONTARIO)) return "ONTARIO";
        else if (zt.equals(ZoneTypeOntario.EXTCANADA)) return "EXTCANADA";
        else if (zt.equals(ZoneTypeOntario.EXTUS)) return "EXTUS";
        else return "EXTOVERSEAS";

    }

    public static Collection<ZoneType> ZoneTypes(){
        return Arrays.asList(ZoneTypeOntario.ONTARIO, ZoneTypeOntario.EXTCANADA, ZoneTypeOntario.EXTUS, ZoneTypeOntario.EXTOVERSEAS);
    }


}



