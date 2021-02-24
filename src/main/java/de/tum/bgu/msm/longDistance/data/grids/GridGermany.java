package de.tum.bgu.msm.longDistance.data.grids;

public class GridGermany implements Grid {

    int id;
    String gridName;
    int taz;
    double popDensity;
    double jobDensity;
    double coordX;
    double coordY;

    public GridGermany(int id, String gridName, int taz, double popDensity, double jobDensity, double coordX, double coordY) {
        this.id=id;
        this.gridName = gridName;
        this.taz = taz;
        this.popDensity = popDensity;
        this.jobDensity = jobDensity;
        this.coordX = coordX;
        this.coordY = coordY;
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
