package de.tum.bgu.msm.longDistance.data.zoneSystem;

import java.util.Arrays;
import java.util.Collection;

/**
 * Created by carlloga on 8/10/2016.
 */
public enum ZoneTypeGermany implements ZoneType {
    GERMANY, EXTEU, EXTOVERSEAS;

    public static ZoneType getZoneType(String stringZoneType) {
        if (stringZoneType.equals("GERMANY")) return ZoneTypeGermany.GERMANY;
        else if (stringZoneType.equals("EXTEU")) return ZoneTypeGermany.EXTEU;
        else return ZoneTypeGermany.EXTOVERSEAS;

    }

    @Override
    public String toString(){
        ZoneTypeGermany zt = this;
        if (zt.equals(ZoneTypeGermany.GERMANY)) return "GERMANY";
        else if (zt.equals(ZoneTypeGermany.EXTEU)) return "EXTEU";
        else return "EXTOVERSEAS";

    }

    public static Collection<ZoneType> ZoneTypes(){
        return Arrays.asList(ZoneTypeGermany.GERMANY, ZoneTypeGermany.EXTEU, ZoneTypeGermany.EXTOVERSEAS);
    }


}



