package de.tum.bgu.msm.longDistance.data.grids;

public class GridGermany implements Grid {

    String gridName;
    int taz;
    double popDensity;
    double jobDensity;


    public GridGermany(String gridName, int taz, double popDensity, double jobDensity) {
        this.gridName = gridName;
        this.taz = taz;
        this.popDensity = popDensity;
        this.jobDensity = jobDensity;
    }


    public String getGridName() {
        return gridName;
    }

    public void setGridName(String gridName) {
        this.gridName = gridName;
    }

    public int getTaz() {
        return taz;
    }

    public void setTaz(int taz) {
        this.taz = taz;
    }

    public double getPopDensity() {
        return popDensity;
    }

    public void setPopDensity(double popDensity) {
        this.popDensity = popDensity;
    }

    public double getJobDensity() {
        return jobDensity;
    }

    public void setJobDensity(double jobDensity) {
        this.jobDensity = jobDensity;
    }
}
