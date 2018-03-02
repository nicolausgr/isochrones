package processingserver;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNResult;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndexFactory;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar.RStarTreeFactory;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * All interaction with the R* tree (creation, queries)
 * @author nicolaus
 */
public class RTree {
    
    /** The database variable */
    private static Database db;
    /** The distance query variable */
    private static DistanceQuery<DoubleVector, DoubleDistance> dist;
    
    /**
     * Creation of the R* tree index of the nodes by their coordinates
     * @param numberOfNodes the number of network nodes to check after tree creation
     */
    public static void createRTree(int numberOfNodes) {
        ListParameterization spatparams = new ListParameterization();
        spatparams.addParameter(StaticArrayDatabase.INDEX_ID, RStarTreeFactory.class);
        spatparams.addParameter(TreeIndexFactory.PAGE_SIZE_ID, 1000);
        spatparams.addParameter(FileBasedDatabaseConnection.INPUT_ID, "/tmp/nodes_indexing.txt");
        // Get database
        db = ClassGenericsUtil.parameterizeOrAbort(StaticArrayDatabase.class, spatparams);
        db.initialize();
        Relation<DoubleVector> rep = db.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD);
        dist = db.getDistanceQuery(rep, EuclideanDistanceFunction.STATIC);
        if (rep.size() != numberOfNodes) {
            System.err.println("Error on R* tree indexing (TreeSize != numberOfNodes)");
            System.exit(1);
        }
    }
    
    /**
     * Looks up the R* tree to detect the closest network node to the given coordinates
     * @param x longitude
     * @param y latitude
     * @return the id of the closest node
     */
    public static int closestNodeToCoords(double x, double y) {
        double[] point = new double[] {y, x};
        DoubleVector dv = new DoubleVector(point);
        KNNQuery<DoubleVector, DoubleDistance> knnq = db.getKNNQuery(dist, 1);
        KNNResult<DoubleDistance> ids = knnq.getKNNForObject(dv, 1);
        /*
         * Minus 1 because R* tree index starts counting from 1.
         * In the TrafficStore node IDs are also from 1 but in the
         * SELECT queries we put a "-1" to start counting from 0
         */
        return ids.get(0).getDBID().getIntegerID() - 1;
    }
    
}
