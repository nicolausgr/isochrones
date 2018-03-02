package networkpreprocessor;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.StrongConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

/**
 *
 * @author nicolaus
 */
public class NetworkPreprocessor {
    
    /** Number of nodes of the network */
    private static int numberOfNodes;
    /** Maximum node id of the network */
    private static int maxNodeId;
    /** Number of edges of the network */
    private static int numberOfEdges;
    /** Maximum edge id of the network */
    private static int maxEdgeId;
    /** Number of edge crossings in the network */
    private static int numberOfCrossings;
    /** Number of restriction records of the network */
    private static int numberOfRestrictions;
    
    /** node_id -> Node */
    private static HashMap<Integer,Node> nodes;
    /** edge_id -> Edge */
    private static HashMap<Integer,Edge> edges;
    /** node_id -> {Edges that have that start node}
    // Only used for restrictions. Created just before processing restrictions. New edges of restrictions are not added. */
    private static HashMap<Integer,ArrayList<Edge>> edgesOfNode;
    /** edge_id -> {Nodes to add} */
    private static HashMap<Integer,ArrayList<Node>> nodesToAddAtEdge;
    /** edge_id -> {Edge ids to which it is broken} */
    private static HashMap<Integer,ArrayList<Integer>> edgeIdsOfBrokenEdge;
    /** arraylist of restriction records */
    private static ArrayList<Restriction> restrictions;
    
    /** EPSILON */
    private static double E = 0.0000001;
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        // Connect to the database and initialize queries
        System.out.println("Connecting to the Database...");
        Database.initializeConnectionPool();
        Database.getConnection();
        Database.initializeQueries();
        
        // Count tables and find max node & edge id
        System.out.print("\nCounting nodes...");
        numberOfNodes = Database.countNodes();
        System.out.print(" " + numberOfNodes + "\nFinding max node id...");
        maxNodeId = Database.maxNodeId();
        System.out.print(" " + maxNodeId + "\nCounting edges...");
        numberOfEdges = Database.countEdges();
        System.out.print(" " + numberOfEdges + "\nFinding max edge id...");
        maxEdgeId = Database.maxEdgeId();
        System.out.print(" " + maxEdgeId + "\nCounting crossings...");
        numberOfCrossings = Database.countCrossings();
        System.out.print(" " + numberOfCrossings + "\nCounting restrictions...");
        numberOfRestrictions = Database.countRestrictions();
        
        /*
         * Load nodes
         */
        System.out.print(" " + numberOfRestrictions + "\nLoading nodes...");
        nodes = Database.loadNodes(numberOfNodes);
        /*
         * Load edges
         */
        System.out.print(" Done\nLoading edges...");
        edges = Database.loadEdges(numberOfEdges);
        
        /*
         * Process crossings
         */
        System.out.print(" Done\nProcessing crossings...");
        // nodesToAddAtEdge: a hashmap of arraylists of new nodes for each edge that will be broken (edgeId -> arraylist_of_new_nodes_to_be_broken_at)
        nodesToAddAtEdge = Database.processCrossings(maxNodeId, nodes, edges, numberOfCrossings);
        
        // Create an int array of edge ids to break
        int[] edgesToBreak = new int[nodesToAddAtEdge.size()];
        int i = 0;
        for (int key: nodesToAddAtEdge.keySet()) {
            edgesToBreak[i++] = key;
        }
        Arrays.sort(edgesToBreak);
        int nextEdgeId = maxEdgeId + 1;
        
        // Create hashmap of edge ids that consist the broken edges
        edgeIdsOfBrokenEdge = new HashMap<>(nodesToAddAtEdge.size());
        
        /*
         * Break edges
         */
        System.out.print(" Done\nBreaking edges...");
        breakEdges(edgesToBreak, nextEdgeId);
        nodesToAddAtEdge.clear();
        System.gc();
        
        /*
         * Duplicate edges
         */
        System.out.print(" Done\nDuplicating edges...");
        int[] edgesBeforeDuplication = new int[edges.size()];
        int j = 0;
        for (int key: edges.keySet()) {
            edgesBeforeDuplication[j++] = key;
        }
        Arrays.sort(edgesBeforeDuplication);
        for (int currEdgeId: edgesBeforeDuplication) {
            Edge currEdge = edges.get(currEdgeId);
            ArrayList<Point> newLinestring = new ArrayList<>(currEdge.linestring);
            Collections.reverse(newLinestring);
            Edge newEdge = new Edge(-currEdge.id, currEdge.en_id, currEdge.sn_id, currEdge.meters, currEdge.minutes,
                                    currEdge.incoming, currEdge.outgoing, newLinestring, currEdge.original_id);
            edges.put(newEdge.id, newEdge);
        }
        
        /*
         * Calculate & set edge inclinations of all edges
         */
        System.out.print(" Done\nCalculating edge inclinations...");
        for (int key: edges.keySet()) {
            setEdgeInclination(edges.get(key));
        }
        
        /*
         * Store restrictions
         */
        System.out.print(" Done\nStoring restrictions...");
        restrictions = Database.storeRestrictions(numberOfRestrictions, edges, edgeIdsOfBrokenEdge);
        edgeIdsOfBrokenEdge.clear();
        System.gc();
        
        /*
         * Process restrictions
         */
        System.out.print(" Done\nProcessing restrictions...");
        // Create a hash map of the edges of each node
        edgesOfNode = new HashMap<>(nodes.size());
        for (int eid: edges.keySet()) {
            addInEdgesOfNode(edges.get(eid));
        }
        Node en;
        Edge fwdEdge, revEdge, newEdge;
        int firstEdgeIdOfCurrentRestriction = -1;
        int nextNodeId = Collections.max(nodes.keySet()) + 1;
        nextEdgeId = Collections.max(edges.keySet()) + 1;
        HashMap<Integer,NewNodesOfRestrictionsWithSpecificFirstEdgeId> newNodesOfRestrictionsWithThisFirstId = new HashMap<>();
        NewNodesOfRestrictionsWithSpecificFirstEdgeId alreadyCreatedNewNodes;
        
        for (Restriction r: restrictions) {
            
            // If inactive restriction, ignore
            if (!r.active) {
                continue;
            }
            
            // 1st edge of restriction
            if (r.seqnr == 1) {
                firstEdgeIdOfCurrentRestriction = r.edge_id;
                
                // If a restriction of this 1st edge id has not be scanned yet
                if (!newNodesOfRestrictionsWithThisFirstId.containsKey(r.edge_id)) {
                    en = nodes.get(r.en_id);

                    // Create new node for the end node of the 1st restriction edge
                    nodes.put(nextNodeId, new Node(nextNodeId, en.brother, en.id, false, en.point));
                    NewNodesOfRestrictionsWithSpecificFirstEdgeId newNodes = new NewNodesOfRestrictionsWithSpecificFirstEdgeId(r.edge_id, nextNodeId, en.id);
                    newNodesOfRestrictionsWithThisFirstId.put(r.edge_id, newNodes);
                    // "Restrict" this edge because we will create it right below
                    newNodes.restrictThisEdgeEnIdFromThisOldNodeId(r.sn_id, r.en_id);

                    // Create a new forward edge (copy of the old but with the new node as end node)
                    fwdEdge = edges.get(r.edge_id);
                    newEdge = new Edge(fwdEdge);
                    newEdge.id = nextEdgeId;
                    newEdge.en_id = nextNodeId;
                    edges.put(nextEdgeId, newEdge);

                    // Create a new opposite edge (copy of the old but with the new node as start node)
                    revEdge = edges.get(-r.edge_id);
                    newEdge = new Edge(revEdge);
                    newEdge.id = -nextEdgeId;
                    newEdge.sn_id = nextNodeId;
                    edges.put(-nextEdgeId, newEdge);

                    // Remove from the old edges the direction from the start node to the end node
                    fwdEdge.outgoing = false;
                    revEdge.incoming = false;
                    
                    nextNodeId++;
                    nextEdgeId++;
                }
            }
            
            // Last edge of restriction
            else if (r.seqnr == r.numberOfEdgesOfRestriction) {
                alreadyCreatedNewNodes = newNodesOfRestrictionsWithThisFirstId.get(firstEdgeIdOfCurrentRestriction);
                
                // "Restrict" this edge because it is restricted!
                alreadyCreatedNewNodes.restrictThisEdgeEnIdFromThisOldNodeId(r.en_id, r.sn_id);
            }
            
            // Middle edge of restriction
            else {
                en = nodes.get(r.en_id);
                
                alreadyCreatedNewNodes = newNodesOfRestrictionsWithThisFirstId.get(firstEdgeIdOfCurrentRestriction);
                
                // If a new node for this end node has not yet been created
                if (!alreadyCreatedNewNodes.oldNodeIdAlreadyContained(r.en_id)) {
                    
                    // Create new node for the end node of the restriction edge
                    nodes.put(nextNodeId, new Node(nextNodeId, en.brother, en.id, false, en.point));
                    alreadyCreatedNewNodes.addNewNode(nextNodeId, en.id);
                    // "Restrict" this edge because we will create it right below (from both sides)
                    alreadyCreatedNewNodes.restrictThisEdgeEnIdFromThisOldNodeId(r.sn_id, r.en_id);
                    alreadyCreatedNewNodes.restrictThisEdgeEnIdFromThisOldNodeId(r.en_id, r.sn_id);

                    // Create a new forward edge (copy of the old but with the previous node as start node & the new node as end node)
                    fwdEdge = edges.get(r.edge_id);
                    newEdge = new Edge(fwdEdge);
                    newEdge.id = nextEdgeId;
                    newEdge.sn_id = alreadyCreatedNewNodes.newNodeIdFromOldNodeId(fwdEdge.sn_id);
                    newEdge.en_id = nextNodeId;
                    edges.put(nextEdgeId, newEdge);

                    // Create a new opposite edge (copy of the old but with the previous node as end node & the new node as start node)
                    revEdge = edges.get(-r.edge_id);
                    newEdge = new Edge(revEdge);
                    newEdge.id = -nextEdgeId;
                    newEdge.sn_id = nextNodeId;
                    newEdge.en_id = alreadyCreatedNewNodes.newNodeIdFromOldNodeId(revEdge.en_id);
                    edges.put(-nextEdgeId, newEdge);
                    
                    nextEdgeId++;
                    nextNodeId++;
                }
            }
        }
        // Now create all outgoing edges from the new nodes except for the ones stored in EdgeEnIdsNotToCreate (estricted or already created)
        for (Integer _firstEdgeId: newNodesOfRestrictionsWithThisFirstId.keySet()) {
            alreadyCreatedNewNodes = newNodesOfRestrictionsWithThisFirstId.get(_firstEdgeId);
            for (NewNodesOfRestrictionsWithSpecificFirstEdgeId.NewNodeOfRestriction _newNode: alreadyCreatedNewNodes.newNodes) {
                for (Edge e: edgesOfNode.get(_newNode.oldNodeId)) {
                    if (_newNode.EdgeEnIdsNotToCreate.contains(e.en_id)) {
                        continue;
                    }
                    if (e.outgoing == false) {
                        continue;
                    }
                    fwdEdge = e;
                    newEdge = new Edge(fwdEdge);
                    newEdge.id = nextEdgeId;
                    newEdge.sn_id = _newNode.newNodeId;
                    newEdge.incoming = false;
                    edges.put(nextEdgeId, newEdge);
                    
                    revEdge = edges.get(-e.id);
                    newEdge = new Edge(revEdge);
                    newEdge.id = -nextEdgeId;
                    newEdge.en_id = _newNode.newNodeId;
                    newEdge.outgoing = false;
                    edges.put(-nextEdgeId, newEdge);

                    nextEdgeId++;
                }
            }
        }
        
        /*
         * Find & mark disconnected (not-strongly-connected) nodes using JGraphT
         */
        System.out.print(" Done\nFinding disconnected nodes...");
        ArrayList<Integer> notStronglyConnectedNodes = findDisconnectedNodes();
        for (Integer nodeId: notStronglyConnectedNodes) {
            nodes.get(nodeId).enabledInRtree = false;
        }
        
        /*
         * Write nodes to file
         */
        System.out.print(" #" + notStronglyConnectedNodes.size() + "\nWriting nodes file...");
        PrintWriter writer = null;
        try {
            writer = new PrintWriter("/tmp/processing_nodes.txt", "UTF-8");
            for (int nid: nodes.keySet()) {
                writer.println(nodes.get(nid).toString());
            }
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger(NetworkPreprocessor.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            writer.close();
        }
        
        /*
         * Write edges to file
         */
        System.out.print(" Done\nWriting edges file...");
        writer = null;
        try {
            writer = new PrintWriter("/tmp/processing_edges.txt", "UTF-8");
            for (int edgeId: edges.keySet()) {
                writer.println(edges.get(edgeId).toString());
            }
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger(NetworkPreprocessor.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            writer.close();
        }
        
        /*
         * Write restrictions to file
         */
        System.out.print(" Done\nWriting restrictions file...");
        writer = null;
        try {
            writer = new PrintWriter("/tmp/processing_restrictions.txt", "UTF-8");
            for (Restriction r: restrictions) {
                writer.println(r.toString());
            }
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger(NetworkPreprocessor.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            writer.close();
        }
        
        Runtime runtime = Runtime.getRuntime();
        System.out.println(" Done\n\nAllocated memory: " + (runtime.totalMemory() / 1024 / 1024) + "MB");
        
    }
    
    /**
     * Calculates the from & to inclination of an edge (inclination on starting point & inclination on ending point)
     * @param e the edge of which to calculate inclinations
     */
    private static void setEdgeInclination(Edge e) {
        Point point1, point2;
        double x1, y1, x2, y2;
        int i;
        
        point1 = e.linestring.get(0);
        x1 = point1.lng;
        y1 = point1.lat;
        point2 = e.linestring.get(1);
        i = 2;
        while (point2.equals(point1)) {
            point2 = e.linestring.get(i++);
        }
        x2 = point2.lng;
        y2 = point2.lat;
        e.inclination_from = inclinationOfPoints(x1, y1, x2, y2);
        
        point1 = e.linestring.get(e.linestring.size()-2);
        x1 = point1.lng;
        y1 = point1.lat;
        point2 = e.linestring.get(e.linestring.size()-1);
        i = e.linestring.size()-3;
        while (point2.equals(point1)) {
            point1 = e.linestring.get(i--);
        }
        x2 = point2.lng;
        y2 = point2.lat;
        e.inclination_to = inclinationOfPoints(x1, y1, x2, y2);
    }
    
    /**
     * Returns the inclination of the two given points
     * @param x1 starting point lng
     * @param y1 starting point lat
     * @param x2 ending point lng
     * @param y2 ending point lat
     * @return 
     */
    private static double inclinationOfPoints(double x1, double y1, double x2, double y2) {
        double inclination;
        
        // If x1 != x2
        if (x1 - x2 > E || x1 - x2 < -E) {
            // If y1 = y2
            if (y1 - y2 < E && y1 - y2 > -E) {
                if (x2 < x1) {
                    inclination = 180.0;
                } else {
                    inclination = 0.0;
                }
            }
            // If y1 != y2
            else {
                inclination = Math.toDegrees(Math.atan((y2-y1)/(x2-x1)));
                // If inclination < 0
                if (inclination < 0.0) {
                    inclination += 180.0;
                }
                // If y2 < y1
                if (y2 < y1) {
                    inclination += 180.0;
                }
            }
        }
        // If x1 = x2
        else {
            if (y2 > y1 + E) {
                inclination = 90.0;
            } else if (y2 < y1 - E) {
                inclination = 270.0;
            } else {
                inclination = 0.0;
                System.out.println("ERROR on inclination: x1=x2 & y1=y2");
            }
        }
        
        return inclination;
    }
    
    /**
     * Creates a human readable string of an arraylist of points
     * @param points the arraylist of points to print
     * @return the human readable string created
     */
    private static String printLinestring(ArrayList<Point> points) {
        String str = "";
        Point curr;
        
        for (int i = 0; i < points.size(); i++) {
            curr = points.get(i);
            str += "(" + curr.lng + " , " + curr.lat + ") -> ";
        }
        
        return str;
    }
    
    /**
     * Calculates the total distance of a linestring
     * @param points the arraylist of points that consist the linestring
     * @return the total distance of the linestring
     */
    private static double totalDistanceOfALinestring(ArrayList<Point> points) {
        double distance = 0.0;
        Point prevPoint, currPoint;
        
        if (points.size() < 1) {
            System.out.println("ERROR: linestring of zero points");
            System.exit(1);
        } else if (points.size() == 1) {
            return 0.0;
        }
        
        prevPoint = points.get(0);
        for (int i = 1; i < points.size(); i++) {
            currPoint = points.get(i);
            distance += Math.sqrt(Math.pow(currPoint.lng-prevPoint.lng, 2.0) +
                                  Math.pow(currPoint.lat-prevPoint.lat, 2.0));
            prevPoint = currPoint;
        }
        
        return distance;
    }
    
    /**
     * Using JGraphT, it detects all not strongly connected nodes of the network
     * @return the arraylist of not strongly connected node ids
     */
    private static ArrayList<Integer> findDisconnectedNodes() {
        DirectedGraph<Integer, DefaultEdge> g;
        ArrayList<Integer> notStronglyConnectedNodes = new ArrayList<>();
        Edge edge;
        int k;
        
        g = new SimpleDirectedGraph<>(DefaultEdge.class);
        
        for (int edgeId: edges.keySet()) {
            if (edgeId < 0) {
                continue;
            }
            edge = edges.get(edgeId);
            if (!g.containsVertex(edge.sn_id)) {
                g.addVertex(edge.sn_id);
            }
            if (!g.containsVertex(edge.en_id)) {
                g.addVertex(edge.en_id);
            }

            if (edge.outgoing && !g.containsEdge(edge.sn_id, edge.en_id)) {
                g.addEdge(edge.sn_id, edge.en_id);
            }
            if (edge.incoming && !g.containsEdge(edge.en_id, edge.sn_id)) {
                g.addEdge(edge.en_id, edge.sn_id);
            }
        }
        
        StrongConnectivityInspector<Integer, DefaultEdge> strongConnInspector = new StrongConnectivityInspector<>(g);
        List<Set<Integer>> s = strongConnInspector.stronglyConnectedSets();
        Set<Integer> s1;
        int maxSize = 0;
        int largest = -1;
        for (k = 0; k < s.size(); k++) {
            s1 = s.get(k);
            if (s1.size() > maxSize) {
                maxSize = s1.size();
                largest = k;
            }
        }
        
        for (k = 0; k < s.size(); k++) {
            if (k != largest) {
                s1 = s.get(k);
                for (Integer nodeId: s1) {
                    notStronglyConnectedNodes.add(nodeId);
                }
            }
        }
        
        return notStronglyConnectedNodes;
    }
    
    /**
     * Adds an edge to the edgesOfNode hashmap in the id of the starting node
     * @param e the edge to add
     */
    private static void addInEdgesOfNode(Edge e) {
        int snId = e.sn_id;
        ArrayList<Edge> list;
        
        if (edgesOfNode.get(snId) == null) {
            list = new ArrayList<>(1);
            list.add(e);
            edgesOfNode.put(snId, list);
        } else {
            edgesOfNode.get(snId).add(e);
        }
    }
    
    /**
     * Breaks the edges in many parts (on the new nodes that have been created on crossing points)
     * @param edgesToBreak an array of edges that will be broken
     * @param nextEdgeId the id of the next edge to be created
     */
    private static void breakEdges(int[] edgesToBreak, int nextEdgeId) {
        // For each one of these edge ids, break the edge
        for (int edgeIdToBreak: edgesToBreak) {
            
            Edge originalEdge = edges.get(edgeIdToBreak);
            
            // Sort the nodes that will break the edge on their distance from the start node
            ArrayList<Node> nodesToAdd = nodesToAddAtEdge.get(edgeIdToBreak);
            for (Node n: nodesToAdd) {
                n.indexOnEdge = originalEdge.linestring.indexOf(n.point);
            }
            Collections.sort(nodesToAdd);
            
            // Store original edge information
            int originalEnId = originalEdge.en_id;
            double originalMeters = originalEdge.meters;
            double originalMinutes = originalEdge.minutes;
            ArrayList<Point> originalLinestring = new ArrayList<>(originalEdge.linestring);
            double originalDistance = totalDistanceOfALinestring(originalLinestring);
            
            int _id, _sn_id, _en_id;
            double currDistance, _meters, _minutes;
            ArrayList<Point> _linestring;
            Node currNode, prevNode;
            
            // Edit original edge
            currNode = nodesToAdd.get(0);
            originalEdge.en_id = currNode.id;
            originalEdge.linestring = new ArrayList<>(originalLinestring.subList(0, currNode.indexOnEdge+1));
            
            currDistance = totalDistanceOfALinestring(originalEdge.linestring);
            originalEdge.meters = (currDistance / originalDistance) * originalMeters;
            originalEdge.minutes = (currDistance / originalDistance) * originalMinutes;
            prevNode = currNode;
            
            ArrayList<Integer> listOfEdgeIdsOfBrokenEdge = new ArrayList<>(nodesToAdd.size()+1);
            listOfEdgeIdsOfBrokenEdge.add(originalEdge.id);
            
            // Create the rest of the edges
            for (int k = 1; k < nodesToAdd.size(); k++) {
                currNode = nodesToAdd.get(k);
                _id = nextEdgeId++;
                _sn_id = prevNode.id;
                _en_id = currNode.id;
                _linestring = new ArrayList<>(originalLinestring.subList(prevNode.indexOnEdge, currNode.indexOnEdge+1));
                currDistance = totalDistanceOfALinestring(_linestring);
                _meters = (currDistance / originalDistance) * originalMeters;
                _minutes = (currDistance / originalDistance) * originalMinutes;
                edges.put(_id, new Edge(_id, _sn_id, _en_id, _meters, _minutes, originalEdge.outgoing,
                                        originalEdge.incoming, _linestring, originalEdge.id));
                listOfEdgeIdsOfBrokenEdge.add(_id);
                prevNode = currNode;
            }
            
            // Create the last edge
            _id = nextEdgeId++;
            _sn_id = prevNode.id;
            _en_id = originalEnId;
            _linestring = new ArrayList<>(originalLinestring.subList(prevNode.indexOnEdge, originalLinestring.size()));
            currDistance = totalDistanceOfALinestring(_linestring);
            _meters = (currDistance / originalDistance) * originalMeters;
            _minutes = (currDistance / originalDistance) * originalMinutes;
            edges.put(_id, new Edge(_id, _sn_id, _en_id, _meters, _minutes, originalEdge.outgoing,
                                    originalEdge.incoming, _linestring, originalEdge.id));
            listOfEdgeIdsOfBrokenEdge.add(_id);
            
            edgeIdsOfBrokenEdge.put(originalEdge.id, listOfEdgeIdsOfBrokenEdge);
        }
    }
    
}
