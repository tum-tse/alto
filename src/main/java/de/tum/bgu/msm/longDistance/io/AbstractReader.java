package de.tum.bgu.msm.longDistance.io;


import de.tum.bgu.msm.longDistance.DataSet;

public abstract class AbstractReader {


    protected final DataSet dataSet;

    AbstractReader(DataSet dataSet) {
            this.dataSet = dataSet;
        }

    public abstract void read();


}
