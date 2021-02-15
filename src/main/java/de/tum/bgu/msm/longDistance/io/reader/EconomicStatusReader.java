package de.tum.bgu.msm.longDistance.io.reader;

import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.ModelComponent;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.sp.EconomicStatus;
import de.tum.bgu.msm.longDistance.data.sp.Household;
import de.tum.bgu.msm.longDistance.data.sp.HouseholdGermany;
import de.tum.bgu.msm.longDistance.io.CSVReader;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import javax.xml.crypto.Data;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class EconomicStatusReader implements ModelComponent {


    private static final Logger logger = Logger.getLogger(EconomicStatusReader.class);
    private BufferedReader reader;
    private Path pathTofileName;
    private final Map<String, Integer> economicStatusDefinition = new HashMap<>();
    private DataSet dataSet;
    private String inputFolder;
    private String outputFolder;
    private JSONObject prop;
    private int numberOfRecords = 0;

    private int hhSizeFactorIndex;
    private int inc0_500Index;
    private int inc500_900Index;
    private int inc900_1500Index;
    private int inc1500_2000Index;
    private int inc2000_2600Index;
    private int inc2600_3000Index;
    private int inc3000_3600Index;
    private int inc3600_4000Index;
    private int inc4000_4600Index;
    private int inc4600_5000Index;
    private int inc5000_5600Index;
    private int inc5600_6000Index;
    private int inc6000_6600Index;
    private int inc6600_7000Index;
    private int inc7000plusIndex;

    public EconomicStatusReader() {
    }

    @Override
    public void setup(JSONObject prop, String inputFolder, String outputFolder) {
        this.inputFolder = inputFolder;
        this.outputFolder = outputFolder;
        this.prop = prop;
        String fileName = JsonUtilMto.getStringProp(prop, "synthetic_population.economic_status_file");
        pathTofileName = Paths.get(inputFolder).getParent().resolve(fileName);

        logger.info("Economic status manager set up");
    }

    @Override
    public void load(DataSet dataset) {
        this.dataSet = dataset;
        read(pathTofileName, ",");
        assignEconomicStatusToAllHouseholds();
        logger.info("Economic status loaded");
    }

    @Override
    public void run(DataSet dataSet, int nThreads) {

    }

    private void read(Path filePath, String delimiter){
        initializeReader(filePath, delimiter);
        try {
            String record;
            while ((record = reader.readLine()) != null) {
                numberOfRecords++;
                processRecord(record.split(delimiter));
            }
        } catch (IOException e) {
            logger.error("Error parsing record number " + numberOfRecords + ": " + e.getMessage(), e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        logger.info(this.getClass().getSimpleName() + ": Read " + numberOfRecords + " records.");
    }

    private void initializeReader(Path filePath, String delimiter) {
        try {

            reader = Files.newBufferedReader(filePath,  StandardCharsets.ISO_8859_1);
            processHeader(reader.readLine().split(delimiter));
        } catch (IOException e) {
            logger.error("Error initializing csv reader: " + e.getMessage(), e);
        }
    }


    protected void processHeader(String[] header) {

        hhSizeFactorIndex = Util.findPositionInArray("hhSizeFactor", header);
        inc0_500Index = Util.findPositionInArray("Inc0-500", header);
        inc500_900Index = Util.findPositionInArray("Inc500-900", header);
        inc900_1500Index = Util.findPositionInArray("Inc900-1500", header);
        inc1500_2000Index = Util.findPositionInArray("Inc1500-2000", header);
        inc2000_2600Index = Util.findPositionInArray("Inc2000-2600", header);
        inc2600_3000Index = Util.findPositionInArray("Inc2600-3000", header);
        inc3000_3600Index = Util.findPositionInArray("Inc3000-3600", header);
        inc3600_4000Index = Util.findPositionInArray("Inc3600-4000", header);
        inc4000_4600Index = Util.findPositionInArray("Inc4000-4600", header);
        inc4600_5000Index = Util.findPositionInArray("Inc4600-5000", header);
        inc5000_5600Index = Util.findPositionInArray("Inc5000-5600", header);
        inc5600_6000Index = Util.findPositionInArray("Inc5600-6000", header);
        inc6000_6600Index = Util.findPositionInArray("Inc6000-6600", header);
        inc6600_7000Index = Util.findPositionInArray("Inc6600-7000", header);
        inc7000plusIndex = Util.findPositionInArray("Inc7000+", header);
    }

    protected void processRecord(String[] record) {
        float hhSizeFactor = Float.parseFloat(record[hhSizeFactorIndex]);
        int codeInc0_500 = Integer.parseInt(record[inc0_500Index]);
        int codeInc500_900 = Integer.parseInt(record[inc500_900Index]);
        int codeInc900_1500 = Integer.parseInt(record[inc900_1500Index]);
        int codeInc1500_2000 = Integer.parseInt(record[inc1500_2000Index]);
        int codeInc2000_2600 = Integer.parseInt(record[inc2000_2600Index]);
        int codeInc2600_3000 = Integer.parseInt(record[inc2600_3000Index]);
        int codeInc3000_3600 = Integer.parseInt(record[inc3000_3600Index]);
        int codeInc3600_4000 = Integer.parseInt(record[inc3600_4000Index]);
        int codeInc4000_4600 = Integer.parseInt(record[inc4000_4600Index]);
        int codeInc4600_5000 = Integer.parseInt(record[inc4600_5000Index]);
        int codeInc5000_5600 = Integer.parseInt(record[inc5000_5600Index]);
        int codeInc5600_6000 = Integer.parseInt(record[inc5600_6000Index]);
        int codeInc6000_6600 = Integer.parseInt(record[inc6000_6600Index]);
        int codeInc6600_7000 = Integer.parseInt(record[inc6600_7000Index]);
        int codeInc7000plus = Integer.parseInt(record[inc7000plusIndex]);
        economicStatusDefinition.put(hhSizeFactor + "_Inc0-500", codeInc0_500);
        economicStatusDefinition.put(hhSizeFactor + "_Inc500-900", codeInc500_900);
        economicStatusDefinition.put(hhSizeFactor + "_Inc900-1500", codeInc900_1500);
        economicStatusDefinition.put(hhSizeFactor + "_Inc1500-2000", codeInc1500_2000);
        economicStatusDefinition.put(hhSizeFactor + "_Inc2000-2600", codeInc2000_2600);
        economicStatusDefinition.put(hhSizeFactor + "_Inc2600-3000", codeInc2600_3000);
        economicStatusDefinition.put(hhSizeFactor + "_Inc3000-3600", codeInc3000_3600);
        economicStatusDefinition.put(hhSizeFactor + "_Inc3600-4000", codeInc3600_4000);
        economicStatusDefinition.put(hhSizeFactor + "_Inc4000-4600", codeInc4000_4600);
        economicStatusDefinition.put(hhSizeFactor + "_Inc4600-5000", codeInc4600_5000);
        economicStatusDefinition.put(hhSizeFactor + "_Inc5000-5600", codeInc5000_5600);
        economicStatusDefinition.put(hhSizeFactor + "_Inc5600-6000", codeInc5600_6000);
        economicStatusDefinition.put(hhSizeFactor + "_Inc6000-6600", codeInc6000_6600);
        economicStatusDefinition.put(hhSizeFactor + "_Inc6600-7000", codeInc6600_7000);
        economicStatusDefinition.put(hhSizeFactor + "_Inc7000+", codeInc7000plus);
    }

    private EconomicStatus getEconomicStatus(HouseholdGermany hh) {
    /*
    Defined as:
        1: Sehr niedrig
        2: Niedrig
        3: Mittel
        4: Hoch
        5: Sehr hoch
     */
        int countAdults = (int) hh.getAdultsEconomicStatusHh();
        int countChildren = (int) hh.getChildrenEconomicStatusHh();
        // Mobilität in Deutschland 2017
        // Die gewichtete Haushaltsgrösse wird aus der Anzahl un dem Alter der Haushaltsmitglieder bestimmt.
        //Kinder unter 14 Jahren gehen mit dem Faktor 0,3 ein. Die erste Person ab 14 Jahren im Haushalt erhält
        //den Gewichtungsfaktor 1, alle weiteren Personen ab 14 Jahren den Faktor 0,5.
        float weightedHhSize = Util.rounder(Math.min(4.4f, 1.0f + (countAdults - 1f) * 0.5f + countChildren * 0.3f), 1);
        String incomeCategory = getMidIncomeCategory(hh.getMonthlyIncome_EUR());
        int codeInc = economicStatusDefinition.get(weightedHhSize+"_"+incomeCategory);
        return EconomicStatus.getEconomicStatusFromCode(codeInc);
    }


    private String getMidIncomeCategory(int income) {

        final String[] incomeBrackets = {"Inc0-500","Inc500-900","Inc900-1500","Inc1500-2000","Inc2000-2600",
                "Inc2600-3000","Inc3000-3600","Inc3600-4000","Inc4000-4600","Inc4600-5000","Inc5000-5600",
                "Inc5600-6000","Inc6000-6600","Inc6600-7000","Inc7000+"};

        for (String incomeBracket : incomeBrackets) {
            String shortIncomeBrackets = incomeBracket.substring(3);
            try{
                String[] incomeBounds = shortIncomeBrackets.split("-");
                if (income >= Integer.parseInt(incomeBounds[0]) && income < Integer.parseInt(incomeBounds[1])) {
                    return incomeBracket;
                }
            } catch (Exception e) {
                if (income >= 7000) {
                    return incomeBrackets[incomeBrackets.length-1];
                }
            }
        }
        logger.error("Unrecognized income: " + income);
        return null;
    }

    private void assignEconomicStatusToAllHouseholds() {
        logger.info("  Assigning economic status to all households");
        Map<EconomicStatus, Integer> economicStatusCounts = new HashMap<>();
        for (Household hh: dataSet.getHouseholds().values()) {
            ((HouseholdGermany)hh).setEconomicStatus(getEconomicStatus((HouseholdGermany)hh));
            EconomicStatus economicStatus = ((HouseholdGermany) hh).getEconomicStatus();
            economicStatusCounts.putIfAbsent(economicStatus,0);
            economicStatusCounts.put(economicStatus, economicStatusCounts.get(economicStatus) + 1);
        }

        for (EconomicStatus es : economicStatusCounts.keySet()){
            logger.warn("Economic status: " + es + " count: " + economicStatusCounts.get(es));
        }
        logger.info("Assigned economic status to all households");
    }
}


