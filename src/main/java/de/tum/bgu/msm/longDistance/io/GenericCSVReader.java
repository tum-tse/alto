package de.tum.bgu.msm.longDistance.io;

import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.data.DataSet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenericCSVReader extends CSVReader {

    private final String delimiter;
    private List<String> fields;
    private String fileName;
    private int counter = 0;

    private Map<String, Integer> indexes;
    private Map<Integer, Map<String, String>> values;

    public GenericCSVReader(DataSet dataSet, List<String> fields, String fileName, String delimiter) {
        super(dataSet);
        this.fields = fields;
        this.fileName = fileName;
        this.delimiter = delimiter;

        indexes = new HashMap<>();
        values = new HashMap<>();

    }

    @Override
    protected void processHeader(String[] header) {
        for (String field : fields){
            indexes.put(field, Util.findPositionInArray(field, header));
        }
    }

    @Override
    protected void processRecord(String[] record) {

        Map<String, String> thisObjectAttribute = new HashMap<>();
        for (String field : fields){
            thisObjectAttribute.put(field, record[indexes.get(field)]);
        }
        values.put(counter, thisObjectAttribute);
        counter ++;
    }

    @Override
    public void read() {
        super.read(fileName, delimiter);
    }

    public Map<Integer, Map<String, String>> readAndReturnResults(){
        read();
        return values;

    }

}
