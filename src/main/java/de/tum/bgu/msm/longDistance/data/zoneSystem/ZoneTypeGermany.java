package de.tum.bgu.msm.longDistance.data.zoneSystem;

import java.util.Arrays;
import java.util.Collection;

/**
 *
 * Germany Model
 * Class to store zones types in the German implementation
 * Author: Ana Moreno, Technical University of Munich (TUM), ana.moreno@tum.de
 * Date: 8 December 2020
 * Version 1
 * Adapted from Ontario
 * Version 1
 *
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



