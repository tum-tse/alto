package de.tum.bgu.msm.longDistance.data.grids;

public class GridGermany implements Grid {

    int id;
    String gridName;
    int taz;
    double popDensity;
    double jobDensity;

    public GridGermany(int id, String gridName, int taz, double popDensity, double jobDensity) {
        this.id=id;
        this.gridName = gridName;
        this.taz = taz;
        this.popDensity = popDensity;
        this.jobDensity = jobDensity;
    }

    @Override
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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
