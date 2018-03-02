package networkpreprocessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.postgresql.ds.PGPoolingDataSource;

/**
 * Handles all communication with the database:
 * > Connection
 * > Query execution
 * > Storing nodes
 * > Storing edges
 * > Processing crossings
 * > Processing & storing restrictions
 * @author nicolaus
 */
public class Database {
    
    /** Connection pool */
    private static PGPoolingDataSource source;
    /** Single connection */
    private static Connection connection;
    /** The array of prepared statements */
    private static PreparedStatement[] pstmt = null;
    
    /** Enumeration of queries to be executed */
    private static enum Queries {
        COUNT_NODES, MAX_NODE_ID, SELECT_NODES,
        COUNT_EDGES, MAX_EDGE_ID, SELECT_EDGES,
        COUNT_CROSSINGS, SELECT_CROSSINGS,
        COUNT_RESTRICTIONS, SELECT_RESTRICTIONS, SIZE
    }
    
    /**
     * Initializes the connection pool
     */
    public static void initializeConnectionPool() {
        source = new PGPoolingDataSource();
        source.setDataSourceName("Road Network Database");
        source.setServerName("192.168.10.18");
        source.setUser("postgres");
        source.setPassword("mitsos1");
        source.setDatabaseName("mmdb2");
        source.setMaxConnections(5);
        source.setInitialConnections(3);
    }
    
    /**
     * Gets a connection from the connection pool
     */
    public static void getConnection() {
        Connection conn = null;
        if (source == null) {
            System.out.println("No connection pool");
            System.exit(1);
        }
        try {
            conn = source.getConnection();
        } catch (SQLException e) {
        }
        if (conn == null) {
            System.out.println("No connection from connection pool");
            System.exit(1);
        }
        try {
            conn.setReadOnly(true);
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
        connection = conn;
    }
    
    /**
     * Initializes all queries to be executed
     */
    public static void initializeQueries() {
        
        String sql[] = new String[Queries.SIZE.ordinal()];
        
        // Count all nodes of network
        sql[Queries.COUNT_NODES.ordinal()] = "SELECT COUNT(*)" + "\n"
                                           + "  FROM wigeogis_nodes" + "\n";
        
        // Find the max node id (to create new nodes starting from the next integer)
        sql[Queries.MAX_NODE_ID.ordinal()] = "SELECT MAX(serial_node_id)" + "\n"
                                           + "  FROM wigeogis_nodes" + "\n";
        
        // Fetch all edges of network
        sql[Queries.SELECT_NODES.ordinal()] = "  SELECT serial_node_id, x, y" + "\n"
                                            + "    FROM wigeogis_nodes" + "\n"
                                            + "ORDER BY serial_node_id" + "\n";
        
        // Count all edges of network
        sql[Queries.COUNT_EDGES.ordinal()] = "SELECT COUNT(*)" + "\n"
                                           + "  FROM wigeogis_nw" + "\n";
        
        // Find the max edge id (to create new edges starting from the next integer)
        sql[Queries.MAX_EDGE_ID.ordinal()] = "SELECT MAX(gid)" + "\n"
                                           + "  FROM wigeogis_nw" + "\n";
        
        // Fetch all edges of network
        sql[Queries.SELECT_EDGES.ordinal()] = "  SELECT gid, f_jnct_serial_id, t_jnct_serial_id," + "\n"
                                            + "         meters, minutes, oneway, privaterd, f_bp, t_bp, ST_AsText(geom_linestring)" + "\n"
                                            + "    FROM wigeogis_nw" + "\n"
                                            + "ORDER BY gid" + "\n";
        
        // Count edge crossings of the network
        sql[Queries.COUNT_CROSSINGS.ordinal()] = "SELECT COUNT(*)" + "\n"
                                               + "  FROM wigeogis_processing_crossings" + "\n";
        
        // Fetch all edge crossings of the network
        sql[Queries.SELECT_CROSSINGS.ordinal()] = "  SELECT gid1, ST_AsText(geom_linestring1), gid2, ST_AsText(geom_linestring2)," + "\n"
                                                + "         ST_AsText(ST_Intersection(geom_linestring1, geom_linestring2))" + "\n"
                                                + "    FROM wigeogis_processing_crossings" + "\n"
                                                + "ORDER BY gid1, gid2" + "\n";
        
        // Count all restrictions of the network
        sql[Queries.COUNT_RESTRICTIONS.ordinal()] = "  SELECT COUNT(*)" + "\n"
                                                   + "    FROM wigeogis_mp_dbf" + "\n"
                                                   + "   WHERE mn_feattyp != 9401 AND mn_feattyp != 2104 AND mn_feattyp != 2199" + "\n";;
        
        // Fetch all restrictions of the network
        sql[Queries.SELECT_RESTRICTIONS.ordinal()] = "  SELECT id, seqnr, nw_gid, nw_f_jnct_serial_id, nw_t_jnct_serial_id" + "\n"
                                                   + "    FROM wigeogis_mp_dbf" + "\n"
                                                   + "   WHERE mn_feattyp != 9401 AND mn_feattyp != 2104 AND mn_feattyp != 2199" + "\n"
                                                   + "ORDER BY id, seqnr" + "\n";
        
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
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Calls countTableRowsOrMaxId
     * @return number of nodes of the network
     */
    public static int countNodes() {
        return countTableRowsOrMaxId(Queries.COUNT_NODES.ordinal());
    }
    
    /**
     * Calls countTableRowsOrMaxId
     * @return max id of the nodes
     */
    public static int maxNodeId() {
        return countTableRowsOrMaxId(Queries.MAX_NODE_ID.ordinal());
    }
    
    /**
     * Calls countTableRowsOrMaxId
     * @return number of edges of the network
     */
    public static int countEdges() {
        return countTableRowsOrMaxId(Queries.COUNT_EDGES.ordinal());
    }
    
    /**
     * Calls countTableRowsOrMaxId
     * @return max id of the edges
     */
    public static int maxEdgeId() {
        return countTableRowsOrMaxId(Queries.MAX_EDGE_ID.ordinal());
    }
    
    /**
     * Calls countTableRowsOrMaxId
     * @return number of edge crossings of the network
     */
    public static int countCrossings() {
        return countTableRowsOrMaxId(Queries.COUNT_CROSSINGS.ordinal());
    }
    
    /**
     * Calls countTableRowsOrMaxId
     * @return number of edge crossings of the network
     */
    public static int countRestrictions() {
        return countTableRowsOrMaxId(Queries.COUNT_RESTRICTIONS.ordinal());
    }
    
    /**
     * Executes COUNT(*) or MAX(id) queries
     * @param whichQuery either count nodes/edges/crossings or max node/edge id
     * @return the requested integer (count or max id)
     */
    private static int countTableRowsOrMaxId(int whichQuery) {
        ResultSet rs;
        int returnValue = -1;
        try {
            rs = pstmt[whichQuery].executeQuery();
            while (rs.next()) {
                returnValue = rs.getInt(1);
            }
            rs.close();
            pstmt[whichQuery].close();
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
        return returnValue;
    }
    
    /**
     * Fetches all nodes of the road network & stores them in a hashmap (nodeId -> Node).
     * @param numberOfNodes the number of the nodes to initialize the hashmap
     * @return the hashmap that contains all fetched nodes
     */
    public static HashMap<Integer,Node> loadNodes(int numberOfNodes) {
        ResultSet rs;
        int _id;
        double _x, _y;
        
        HashMap<Integer,Node> nodes = new HashMap<>(numberOfNodes);

        try {
            rs = pstmt[Queries.SELECT_NODES.ordinal()].executeQuery();
            while (rs.next()) {
                _id = rs.getInt(1);
                _x = rs.getDouble(2);
                _y = rs.getDouble(3);
                
                nodes.put(_id, new Node(_id, -1, _x, _y));
            }
            rs.close();
            pstmt[Queries.SELECT_NODES.ordinal()].close();
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
        return nodes;
    }
    
    /**
     * Fetches all nodes of the road network & stores them in a hashmap (edgeId -> Edge).
     * Outgoing & incoming (possible both false) are determined by looking at many columns of the original table.
     * @param numberOfEdges the number of the edges to initialize the hashmap
     * @return the hashmap that contains all fetched edges
     */
    public static HashMap<Integer,Edge> loadEdges(int numberOfEdges) {
        ResultSet rs;
        int id, sn_id, en_id;
        double meters, minutes;
        String oneway, linestring;
        int privaterd, f_bp, t_bp;
        
        HashMap<Integer,Edge> edges = new HashMap<>(numberOfEdges);
        
        try {
            rs = pstmt[Queries.SELECT_EDGES.ordinal()].executeQuery();
            while (rs.next()) {
                id = rs.getInt(1);
                sn_id = rs.getInt(2);
                en_id = rs.getInt(3);
                meters = rs.getDouble(4);
                minutes = rs.getDouble(5);
                oneway = rs.getString(6);
                privaterd = rs.getInt(7);
                f_bp = rs.getInt(8);
                t_bp = rs.getInt(9);
                linestring = rs.getString(10);
                
                boolean outgoing, incoming;
                outgoing = incoming = false;
                
                if (privaterd == 0 && f_bp == 0 && t_bp == 0) {
                    if (oneway == null) {
                        outgoing = incoming = true;
                    } else if (oneway.equals("FT")) {
                        outgoing = true;
                        incoming = false;
                    } else if (oneway.equals("TF")) {
                        outgoing = false;
                        incoming = true;
                    } else if (oneway.equals("N")) {
                        outgoing = incoming = false;
                    } else {
                        System.out.println("ERROR on oneway field");
                        System.exit(1);
                    }
                }
                
                edges.put(id, new Edge(id, sn_id, en_id, meters, minutes,
                                       outgoing, incoming, parseTextGeom(linestring)));
            }
            rs.close();
            pstmt[Queries.SELECT_EDGES.ordinal()].close();
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
        return edges;
    }
    
    /**
     * Transforms a geometry in text format to an arraylist of its containing points
     * @param textGeom a POINT, MULTIPOINT or LINESTRING geometry in text format
     * @return an arraylist containing the points (coordinates) that consist the geometry
     */
    public static ArrayList<Point> parseTextGeom(String textGeom) {
        ArrayList<Point> points = new ArrayList<>();
        
        String pointsString = textGeom.substring(textGeom.indexOf("(") + 1, textGeom.indexOf(")"));
        
        String[] pointsArray = pointsString.split(",");
        
        for (int i = 0; i < pointsArray.length; i++) {
            String[] singlePoint = pointsArray[i].split(" ");
            points.add(new Point(Double.parseDouble(singlePoint[1]), Double.parseDouble(singlePoint[0])));
        }
        
        return points;
    }
    
    /**
     * Transforms an arraylist of points a geometry to text format
     * @param points an arraylist of points (coordinates) that consist a linestring geometry
     * @return a LINESTRING geometry in text format
     */
    public static String printLinestring(ArrayList<Point> points) {
        String str = "SRID=4326;LINESTRING(";
        Point curr;
        
        curr = points.get(0);
        str += curr.lng + " " + curr.lat;
        for (int i = 0; i < points.size(); i++) {
            curr = points.get(i);
            str += "," + curr.lng + " " + curr.lat;
        }
        str += ")";
        
        return str;
    }
    
    /**
     * Fetches crossings and for each one:
     * > Creates two new nodes (if crossings not already on existing nodes) and adds them at the nodes hashmap
     * > Adds each node to the arraylist of new nodes for the respective edge
     * @param maxNodeId the max node id to start creating nodes with the next int
     * @param nodes the nodes of the network as fetched from the database
     * @param edges the edges of the network as fetched from the database
     * @param numberOfCrossings the number of crossings to initialize the nodesToAddAtEdge hashmap
     * @return a hashmap of arraylists of new nodes for each edge that will be broken (edgeId -> arraylist_of_new_nodes_to_be_broken_at)
     */
    public static HashMap<Integer,ArrayList<Node>> processCrossings(int maxNodeId, HashMap<Integer,Node> nodes,
                                                                    HashMap<Integer,Edge> edges, int numberOfCrossings) {
        ResultSet rs;
        int edge_id1, edge_id2;
        Edge edge1, edge2;
        int brother1, brother2;
        boolean createNode1, createNode2;
        String text_crossings;
        int nextNodeId = maxNodeId + 1;
        
        HashMap<Integer,ArrayList<Node>> nodesToAddAtEdge = new HashMap<>(numberOfCrossings);
        
        try {
            rs = pstmt[Queries.SELECT_CROSSINGS.ordinal()].executeQuery();
            while (rs.next()) {
                edge_id1 = rs.getInt(1);
                edge_id2 = rs.getInt(3);
                text_crossings = rs.getString(5);
                
                ArrayList<Point> crossings = parseTextGeom(text_crossings);
                edge1 = edges.get(edge_id1);
                edge2 = edges.get(edge_id2);
                // For each crossing...
                for (Point c: crossings) {
                    // ... check if point is on edge's start/end node or on a previously created node.
                    // Determine node id of crossings (or create new node id).
                    createNode1 = false;
                    brother1 = -1;
                    if (edge1.linestring.indexOf(c) == 0) {
                        brother1 = edge1.sn_id;
                    } else if (edge1.linestring.indexOf(c) == edge1.linestring.size()-1) {
                        brother1 = edge1.en_id;
                    } else if (nodesToAddAtEdge.containsKey(edge_id1)) {
                        brother1 = searchPointInNodesArrayList(nodesToAddAtEdge.get(edge_id1), c);
                    }
                    if (brother1 == -1) {
                        brother1 = nextNodeId++;
                        createNode1 = true;
                    }
                    
                    createNode2 = false;
                    brother2 = -1;
                    if (edge2.linestring.indexOf(c) == 0) {
                        brother2 = edge2.sn_id;
                    } else if (edge2.linestring.indexOf(c) == edge2.linestring.size()-1) {
                        brother2 = edge2.en_id;
                    } else if (nodesToAddAtEdge.containsKey(edge_id2)) {
                        brother2 = searchPointInNodesArrayList(nodesToAddAtEdge.get(edge_id2), c);
                    }
                    if (brother2 == -1) {
                        brother2 = nextNodeId++;
                        createNode2 = true;
                    }
                    
                    // If new nodes, add them to the arraylist of nodes that each edge will be broken at
                    // Either way, update brother info
                    if (createNode1) {
                        Node newNode1 = new Node(brother1, brother2, c);
                        nodes.put(brother1, newNode1);
                        
                        if (!nodesToAddAtEdge.containsKey(edge_id1)) {
                            ArrayList<Node> newList = new ArrayList<>(1);
                            newList.add(newNode1);
                            nodesToAddAtEdge.put(edge_id1, newList);
                        } else {
                            nodesToAddAtEdge.get(edge_id1).add(newNode1);
                        }
                    } else {
                        nodes.get(brother1).brother = brother2;
                    }
                    if (createNode2) {
                        Node newNode2 = new Node(brother2, brother1, c);
                        nodes.put(brother2, newNode2);
                        
                        if (!nodesToAddAtEdge.containsKey(edge_id2)) {
                            ArrayList<Node> newList = new ArrayList<>(1);
                            newList.add(newNode2);
                            nodesToAddAtEdge.put(edge_id2, newList);
                        } else {
                            nodesToAddAtEdge.get(edge_id2).add(newNode2);
                        }
                    } else {
                        nodes.get(brother2).brother = brother1;
                    }
                       
                }
            }
            rs.close();
            pstmt[Queries.SELECT_CROSSINGS.ordinal()].close();
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
        return nodesToAddAtEdge;
    }
    
    /**
     * Determines if a given point is contained inside an arraylist of points
     * @param nodesArrayList the arraylist of points to search in
     * @param p the point to search
     * @return the index of the point in the arraylist if found, else -1
     */
    private static int searchPointInNodesArrayList(ArrayList<Node> nodesArrayList, Point p) {
        for (Node n: nodesArrayList) {
            if (n.point.equals(p)) {
                return n.id;
            }
        }
        return -1;
    }
    
    /**
     * Fetches all restrictions of the network and adapts them to the processing network.
     * Edges of the restrictions will be outgoing and correctly from the start node to the end node
     * (possibly change some edges to their opposite ones).
     * @param numberOfRestrictions the number of the restriction records in the database
     * @param edges the edges of the network
     * @param edgeIdsOfBrokenEdge the hashmap of arraylists of edges that each edge was broken at
     * @return an arraylist containing the adapted restriction records
     */
    public static ArrayList<Restriction> storeRestrictions(int numberOfRestrictions, HashMap<Integer,Edge> edges,
                                                             HashMap<Integer,ArrayList<Integer>> edgeIdsOfBrokenEdge) {
        ResultSet rs;
        int serial_id = 1;
        int prev_restriction_id = -1, curr_restriction_id = -1;
        double prev_id = -1, curr_id;
        int prev_seqnr = -1, curr_seqnr;
        int prev_edge = -1, curr_edge;
        int prev_sn = -1, curr_sn;
        int prev_en = -1, curr_en;
        int temp;
        
        ArrayList<Restriction> restrictions = new ArrayList<>(numberOfRestrictions);

        try {
            rs = pstmt[Queries.SELECT_RESTRICTIONS.ordinal()].executeQuery();
            if (rs.next()) {
                prev_id = curr_id = rs.getDouble(1);
                prev_seqnr = curr_seqnr = rs.getInt(2);
                prev_edge = curr_edge = rs.getInt(3);
                prev_sn = curr_sn = rs.getInt(4);
                prev_en = curr_en = rs.getInt(5);
                prev_restriction_id = curr_restriction_id = 1;
            }
            while (rs.next()) {
                curr_id = rs.getDouble(1);
                curr_seqnr = rs.getInt(2);
                curr_edge = rs.getInt(3);
                curr_sn = rs.getInt(4);
                curr_en = rs.getInt(5);
                
                if (curr_seqnr != 1) {
                    if (prev_en != curr_sn && prev_en != curr_en) {
                        prev_edge = -prev_edge;
                        temp = prev_en;
                        prev_en = prev_sn;
                        prev_sn = temp;
                    }
                    if (curr_sn != prev_en) {
                        curr_edge = -curr_edge;
                        temp = curr_en;
                        curr_en = curr_sn;
                        curr_sn = temp;
                    }
                } else {
                    curr_restriction_id++;
                }
                
                // If the edge of the restriction has been broken, we have to break the restriction to the broken edges as well
                if (edgeIdsOfBrokenEdge.containsKey(prev_edge) || edgeIdsOfBrokenEdge.containsKey(-prev_edge)) {
                    if (prev_edge < 0) {
                        ArrayList<Integer> listOfEdgeIdsOfBrokenEdge = new ArrayList(edgeIdsOfBrokenEdge.get(-prev_edge));
                        Collections.reverse(listOfEdgeIdsOfBrokenEdge);
                        for (int edge_id: listOfEdgeIdsOfBrokenEdge) {
                            restrictions.add(new Restriction(serial_id++, prev_restriction_id, prev_seqnr++,
                                                             -edge_id, edges.get(-edge_id).sn_id, edges.get(-edge_id).en_id, prev_id));
                        }
                    } else {
                        ArrayList<Integer> listOfEdgeIdsOfBrokenEdge = new ArrayList(edgeIdsOfBrokenEdge.get(prev_edge));
                        for (int edge_id: listOfEdgeIdsOfBrokenEdge) {
                            restrictions.add(new Restriction(serial_id++, prev_restriction_id, prev_seqnr++,
                                                             edge_id, edges.get(edge_id).sn_id, edges.get(edge_id).en_id, prev_id));
                        }
                    }
                } else {
                    restrictions.add(new Restriction(serial_id++, prev_restriction_id, prev_seqnr++,
                                                     prev_edge, prev_sn, prev_en, prev_id));
                }
                
                prev_id = curr_id;
                prev_edge = curr_edge;
                prev_sn = curr_sn;
                prev_en = curr_en;
                prev_restriction_id = curr_restriction_id;
                if (curr_seqnr == 1) {
                    prev_seqnr = 1;
                }
            }
            
            restrictions.add(new Restriction(serial_id++, prev_restriction_id, prev_seqnr,
                                             prev_edge, prev_sn, prev_en, prev_id));
            
            rs.close();
            pstmt[Queries.SELECT_NODES.ordinal()].close();
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // Update numberOfEdgesOfRestriction & mark whether restriction is active (this in 2 steps - reverse now and then forward)
        boolean lastEdgeOfRestriction = true;
        boolean prevRestrictionIsActive = true;
        int seqnrOfLastEdge = -1;
        for (int i = restrictions.size()-1; i >= 0; i--) {
            if (lastEdgeOfRestriction) {
                seqnrOfLastEdge = restrictions.get(i).seqnr;
                lastEdgeOfRestriction = false;
                prevRestrictionIsActive = true;
            }
            if (edges.get(restrictions.get(i).edge_id).outgoing == false) {
                prevRestrictionIsActive = false;
            }
            restrictions.get(i).numberOfEdgesOfRestriction = seqnrOfLastEdge;
            if (restrictions.get(i).seqnr == 1) {
                lastEdgeOfRestriction = true;
            }
            if (!prevRestrictionIsActive) {
                restrictions.get(i).active = false;
            }
        }
        
        // Second step (forward) for marking whether a restriction is active
        for (int i = 0; i < restrictions.size(); i++) {
            if (restrictions.get(i).seqnr == 1) {
                prevRestrictionIsActive = true;
            }
            if (edges.get(restrictions.get(i).edge_id).outgoing == false) {
                prevRestrictionIsActive = false;
            }
            if (!prevRestrictionIsActive) {
                restrictions.get(i).active = false;
            }
        }
        
        return restrictions;
    }
    
}
