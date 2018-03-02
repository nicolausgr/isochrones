/*
 * TODO
 * Read linestring? What form? How to save it?
 */
package processingserver;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * All interaction with the TrafficStore
 * @author nicolaus
 */
public class TrafficStore {
    
    /** The connection variable */
    private static Connection connection = null;
    /** The array of the prepared statements */
    private static PreparedStatement[] pstmt = null;
    
    /** The nodes of the road network publicly available */
    public static Node[] nodes;
    /** The edges of the road network publicly available */
    public static Edge[] edges;
    /** A hash map for finding edge index by edge id */
    public static HashMap<Integer,Integer> edgeIndexOfEdgeWithId;
    /** A hash map for storing all created restriction nodes over a node */
    public static HashMap<Integer,HashSet<Integer>> newRestrictionNodesOverNodeWithId;
    
    /** Enumeration of queries to be executed */
    private static enum Queries {
        COUNT_NODES, COUNT_EDGES, SELECT_NODES, SELECT_EDGES, SELECT_LANDMARKS, SIZE
    }
    
    /**
     * Gets a connection with the TrafficStore
     * @throws NamingException
     * @throws SQLException
     * @throws Exception 
     */
    public static void connect() throws NamingException, SQLException, Exception {
        
        InitialContext cxt = new InitialContext();
        if (cxt == null) {
            throw new Exception("No context found!");
        }
        
        DataSource ds = (DataSource) cxt.lookup( "java:/comp/env/TrafficStore" );
        if (ds == null) {
            throw new Exception("Data source not found!");
        }
        
        connection = ds.getConnection();
    }
    
    /**
     * Initializes prepared statements (queries) that will be executed.
     * Node IDs get a -1 in order to start counting from 0 and not from 1 as in the TrafficStore
     */
    public static void initializeQueries() {
        
        String sql[] = new String[Queries.SIZE.ordinal()];
        
        // Count all nodes of network
        sql[Queries.COUNT_NODES.ordinal()] = "SELECT COUNT(*)" + "\n"
                                            + "  FROM wigeogis_processing_nodes" + "\n";
        
        // Count all edges of network
        sql[Queries.COUNT_EDGES.ordinal()] = "SELECT COUNT(*)" + "\n"
                                           + "  FROM wigeogis_processing_edges" + "\n"
                                           + " WHERE outgoing = true OR incoming = true" + "\n";
        
        // Fetch all nodes of network
        // node ids - 1
        sql[Queries.SELECT_NODES.ordinal()] = "  SELECT id - 1, brother_id - 1, restriction_original_id - 1, enabled_in_rtree, lng, lat" + "\n"
                                            + "    FROM wigeogis_processing_nodes" + "\n"
                                            + "ORDER BY id" + "\n";
        
        // Fetch all edges of network
        // node ids - 1
        sql[Queries.SELECT_EDGES.ordinal()] = "  SELECT id, sn_id - 1, en_id - 1, meters, minutes," + "\n"
                                            + "         outgoing, incoming, inclination_from, inclination_to," + "\n"
                                            + "         ST_AsText(the_geom)" + "\n"
                                            + "    FROM wigeogis_processing_edges" + "\n"
                                            + "   WHERE outgoing = true OR incoming = true" + "\n"
                                            + "ORDER BY sn_id, outgoing DESC, incoming ASC" + "\n";
        
        // Fetch all 32 landmarks
        sql[Queries.SELECT_LANDMARKS.ordinal()] = "  SELECT landmark1 - 1, landmark2 - 1, landmark3 - 1, landmark4 - 1" + "\n"
                                                + "    FROM wigeogis_processing_landmarks" + "\n"
                                                + "ORDER BY cell_p8" + "\n";
        
        try {
            pstmt = new PreparedStatement[Queries.SIZE.ordinal()];
            connection.setAutoCommit(false);
            System.out.println("-------------------------");
            for (int k = 0; k < pstmt.length; k++) {
                System.out.print(sql[k]);
                System.out.println("-------------------------");
                pstmt[k] = connection.prepareStatement(sql[k]);
                pstmt[k].setFetchSize(4000);
            }
        } catch (SQLException ex) {
            Logger.getLogger(TrafficStore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Calls countTableRows
     * @return number of nodes of the network
     */
    public static int countNodes() {
        return countTableRows(Queries.COUNT_NODES.ordinal());
    }
    
    /**
     * Calls countTableRows
     * @return number of edges of the network
     */
    public static int countEdges() {
        return countTableRows(Queries.COUNT_EDGES.ordinal());
    }
    
    /**
     * Executes COUNT(*) queries
     * @param whichQuery either count nodes or edges
     * @return 
     */
    private static int countTableRows(int whichQuery) {
        ResultSet rs;
        int numberOfRecords = -1;
        try {
            rs = pstmt[whichQuery].executeQuery();
            while (rs.next()) {
                numberOfRecords = rs.getInt(1);
            }
            rs.close();
            pstmt[whichQuery].close();
        } catch (Exception ex) {
            Logger.getLogger(TrafficStore.class.getName()).log(Level.SEVERE, null, ex);
        }
        return numberOfRecords;
    }
    
    /**
     * Fetches all nodes of the road network & stores them. It also fills the hash map of the restriction nodes over a node.
     * @param numberOfNodes the number of nodes for the array to be initialized
     */
    public static void loadNodes(int numberOfNodes) {
        ResultSet rs;
        int _id, _brother_id, _restriction_original_id;
        boolean _connected;
        double _lng, _lat;
        int i = 0;
        
        nodes = new Node[numberOfNodes];
        newRestrictionNodesOverNodeWithId = new HashMap<Integer,HashSet<Integer>>();

        try {
            rs = pstmt[Queries.SELECT_NODES.ordinal()].executeQuery();
            while (rs.next()) {
                _id = rs.getInt(1);
                _brother_id = rs.getInt(2);
                _restriction_original_id = rs.getInt(3);
                _connected = rs.getBoolean(4);
                _lng = rs.getDouble(5);
                _lat = rs.getDouble(6);
                nodes[i] = new Node(_id, _brother_id, _restriction_original_id, _connected, _lng, _lat);
                if (_restriction_original_id >= 0) {
                    if (newRestrictionNodesOverNodeWithId.containsKey(_restriction_original_id)) {
                        newRestrictionNodesOverNodeWithId.get(_restriction_original_id).add(_id);
                    } else {
                        HashSet<Integer> newSet = new HashSet<Integer>(1);
                        newSet.add(_id);
                        newRestrictionNodesOverNodeWithId.put(_restriction_original_id, newSet);
                    }
                }
                i++;
            }
            rs.close();
            pstmt[Queries.SELECT_NODES.ordinal()].close();
        } catch (Exception ex) {
            Logger.getLogger(TrafficStore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Fetches all edges of the road network & stores them along with nodes' edge indexes.
     * Each node gets the index of its first adjacent edge & the number of its adjacent edges
     * @param numberOfEdges the number of edges for the array to be initialized
     */
    public static void loadEdges(int numberOfEdges) {
        ResultSet rs;
        int _id, _sn_id, _en_id;
        double _meters, _minutes, _inclination_from, _inclination_to;
        boolean _outgoing, _incoming;
        String _linestring;
        int prevSnId = -1;
        int edgeIndex = 0;
        
        edges = new Edge[numberOfEdges];
        edgeIndexOfEdgeWithId = new HashMap<Integer,Integer>(numberOfEdges);
        
        try {
            rs = pstmt[Queries.SELECT_EDGES.ordinal()].executeQuery();
            while (rs.next()) {
                _id = rs.getInt(1);
                _sn_id = rs.getInt(2);
                _en_id = rs.getInt(3);
                _meters = rs.getDouble(4);
                _minutes = rs.getDouble(5);
                _outgoing = rs.getBoolean(6);
                _incoming = rs.getBoolean(7);
                _inclination_from = rs.getDouble(8);
                _inclination_to = rs.getDouble(9);
                _linestring = rs.getString(10);
                
                if (_sn_id != prevSnId) {
                    nodes[_sn_id].firstEdgeIndex = edgeIndex;
                }
                prevSnId = _sn_id;
                nodes[_sn_id].numberOfEdges++;
                // Cast meters and minutes (-> tenths of seconds) to integers
                edges[edgeIndex] = new Edge(_id, _sn_id, _en_id, (int) Math.round(_meters), (int) Math.round(_minutes*60.0*10.0),
                                            _outgoing, _incoming, (int) Math.round(_inclination_from), (int) Math.round(_inclination_to),
                                            parseTextGeom(_linestring));
                edgeIndexOfEdgeWithId.put(_id, edgeIndex);
                edgeIndex++;
                if (edgeIndex % 100000 == 0) {
                    System.out.println("Edge: #" + edgeIndex);
                }
            }
            rs.close();
            pstmt[Queries.SELECT_EDGES.ordinal()].close();
        } catch (Exception ex) {
            Logger.getLogger(TrafficStore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Fetches landmark ids from the TrafficStore and stores them in the static Landmarks class
     */
    public static void loadLandmarks() {
        ResultSet rs;
        int counter = 0;
        try {
            rs = pstmt[Queries.SELECT_LANDMARKS.ordinal()].executeQuery();
            while (rs.next()) {
                Landmarks.landmarkIds[counter++] = rs.getInt(1);
                Landmarks.landmarkIds[counter++] = rs.getInt(2);
                Landmarks.landmarkIds[counter++] = rs.getInt(3);
                Landmarks.landmarkIds[counter++] = rs.getInt(4);
            }
            if (!(counter == Landmarks.nofLandmarks)) {
                System.err.println("ERROR: Landmarks.nofLandmarks and DB #landmarks don't match");
            }
            rs.close();
            pstmt[Queries.SELECT_LANDMARKS.ordinal()].close();
        } catch (Exception ex) {
            Logger.getLogger(TrafficStore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Creates a text file of all node coordinates needed for the initialization of the R* tree.
     * For the not-strongly-connected nodes it inserts inexistent coordinates not to be ever chosen.
     * @param filename the full path of the text file to be created
     */
    public static void createCoordinatesTxt(String filename) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(filename, "UTF-8");
            for (int i = 0; i < nodes.length; i++) {
                if (nodes[i].enabledInRtree) {
                    writer.println(nodes[i].lat + "\t" + nodes[i].lng);
                }
                // If not-strongly-connected, they should never be chosen
                else {
                    writer.println(-1000.0 + "\t" + -1000.0);
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(TrafficStore.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(TrafficStore.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            writer.close();
        }
    }
    
    /**
     * Transforms a geometry in text format to an arraylist of its containing points
     * @param textGeom a POINT, MULTIPOINT or LINESTRING geometry in text format
     * @return an arraylist containing the points (coordinates) that consist the geometry
     */
    public static ArrayList<Point> parseTextGeom(String textGeom) {
        ArrayList<Point> points = new ArrayList<Point>();
        
        String pointsString = textGeom.substring(textGeom.indexOf("(") + 1, textGeom.indexOf(")"));
        
        String[] pointsArray = pointsString.split(",");
        
        for (int i = 0; i < pointsArray.length; i++) {
            String[] singlePoint = pointsArray[i].split(" ");
            points.add(new Point(Double.parseDouble(singlePoint[1]), Double.parseDouble(singlePoint[0])));
        }
        
        return points;
    }
    
}
