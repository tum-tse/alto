package de.tum.bgu.msm.longDistance.io.reader;

import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.grids.Grid;
import de.tum.bgu.msm.longDistance.data.grids.GridGermany;
import de.tum.bgu.msm.longDistance.data.zoneSystem.AreaTypeGermany;
import de.tum.bgu.msm.longDistance.data.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneGermany;
import de.tum.bgu.msm.longDistance.data.zoneSystem.ZoneTypeGermany;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GridReaderGermany implements GridReader{

    private static Logger logger = Logger.getLogger(GridReaderGermany.class);
    private JSONObject prop;

    private DataSet dataSet;
    private String inputFolder;
    private String outputFolder;

    private TableDataSet gridTable;

    @Override
    public void setup(JSONObject prop, String inputFolder, String outputFolder) {

        this.inputFolder = inputFolder;
        this.outputFolder = outputFolder;
        this.prop = prop;

        gridTable = Util.readCSVfile(inputFolder + JsonUtilMto.getStringProp(prop, "grid_system.internal_file"));
        gridTable.buildIndex(gridTable.getColumnPosition("id"));

        logger.info("Grid data manager set up");

    }

    @Override
    public void load(DataSet dataSet) {

        this.dataSet=dataSet;

        Map<Integer, List> gridMap = new HashMap<>();
        Map<Integer, List> internalGridMap = readInternalGrids();

        gridMap.putAll(internalGridMap);
        dataSet.setGrids(gridMap);

        logger.info("Grid data loaded");

    }

    @Override
    public void run(DataSet dataSet, int nThreads) {

    }

    private Map<Integer,List> readInternalGrids() {
        //create grids objects (empty) and a map to find them in trips microlocation

        Map<Integer,List> internalGridList = new HashMap<>();

        int zones = dataSet.getZones().size();
        int[] grids = gridTable.getColumnAsInt("id");

        for (int zone = 1; zone <= zones; zone++){

            List<Grid> tempGridList = new ArrayList<>();

            for (int grid : grids){

                int taz = (int) gridTable.getIndexedValueAt(grid, "zone");

                if (taz==zone){
                    int id = (int) gridTable.getIndexedValueAt(grid, "id");
                    String gridName = gridTable.getIndexedStringValueAt(grid, "name");
                    double popDensity = gridTable.getIndexedValueAt(grid, "popDensity");
                    double jobDensity = gridTable.getIndexedValueAt(grid, "jobDensity");

                    double coordX = gridTable.getIndexedValueAt(grid, "x_mp_31468");
                    double coordY = gridTable.getIndexedValueAt(grid, "y_mp_31468");

                    Grid internalGrid = new GridGermany(id, gridName, taz, popDensity, jobDensity, coordX, coordY);
                    tempGridList.add(internalGrid);

                }



            }

            internalGridList.put(zone, tempGridList);

        }
        return internalGridList;
    }
}
