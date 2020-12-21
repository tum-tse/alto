package de.tum.bgu.msm.longDistance.data.sp;


import java.util.Arrays;
import java.util.Collection;

public enum EconomicStatus {
    VERYLOW,LOW,MEDIUM,HIGH,VERYHIGH;

    public static EconomicStatus getEconomicStatusType(String stringZoneType) {
        if (stringZoneType.equals("Verylow")) return EconomicStatus.VERYLOW;
        else if (stringZoneType.equals("Low")) return EconomicStatus.LOW;
        else if (stringZoneType.equals("Medium")) return EconomicStatus.MEDIUM;
        else if (stringZoneType.equals("High")) return EconomicStatus.HIGH;
        else return EconomicStatus.VERYHIGH;

    }

    public static EconomicStatus getEconomicStatusFromCode(int intEconomicStatus) {
        if (intEconomicStatus == 1) return EconomicStatus.VERYLOW;
        else if (intEconomicStatus == 2) return EconomicStatus.LOW;
        else if (intEconomicStatus == 3) return EconomicStatus.MEDIUM;
        else if (intEconomicStatus == 4) return EconomicStatus.HIGH;
        else return EconomicStatus.VERYHIGH;

    }

    @Override
    public String toString(){
        EconomicStatus zt = this;
        if (zt.equals(EconomicStatus.VERYLOW)) return "Verylow";
        else if (zt.equals(EconomicStatus.LOW)) return "Low";
        else if (zt.equals(EconomicStatus.MEDIUM)) return "Medium";
        else if (zt.equals(EconomicStatus.HIGH)) return "High";
        else return "Veryhigh";

    }

    public static Collection<EconomicStatus> ZoneTypes(){
        return Arrays.asList(EconomicStatus.VERYLOW, EconomicStatus.LOW, EconomicStatus.MEDIUM, EconomicStatus.HIGH, EconomicStatus.VERYHIGH);
    }
}
