package processingserver;

import cern.colt.list.IntArrayList;
import java.util.ArrayList;
import org.json.simple.JSONObject;

/**
 *
 * @author nicolaus
 */
public class IsochroneRequest {
    
    private boolean reverseQuery;
    private int numberOfIsochrones;
    private int maxTT;
    private double x;
    private double y;
    private int rootNodeId;
    private Node rootNode;
    private int[] isochroneLimits;
    
    private ArrayList<IsochroneEdge>[] edgesOfNode;
    private int[] maxLngNodes;
    
    public IsochroneRequest(boolean _reverseQuery, int _numberOfIsochrones,
                            int _maxTT, double _x, double _y) {
        reverseQuery = _reverseQuery;
        numberOfIsochrones = _numberOfIsochrones;
        maxTT = _maxTT;
        x = _x;
        y = _y;
        isochroneLimits = new int[numberOfIsochrones];
        for (int i = 0; i < numberOfIsochrones; i++) {
            isochroneLimits[i] = (i + 1) * (maxTT / numberOfIsochrones);
        }
        Reporting.print("ISOCΗRONE(" + x + "," + y + ") " + reverseQuery + " " + numberOfIsochrones + "@" + (maxTT/600) + "mins");
    }
    
    public JSONObject compute() {
        
        // Determine the root node by looking up the R* tree
        rootNodeId = RTree.closestNodeToCoords(x, y);
        rootNode = TrafficStore.nodes[rootNodeId];
        Reporting.print("RootNode: " + rootNodeId + " (" + rootNode.lng + "," + rootNode.lat + ")");
        
        // Initialize Dijkstra result arrays
        edgesOfNode = (ArrayList<IsochroneEdge>[]) new ArrayList[TrafficStore.nodes.length];
        maxLngNodes = new int[isochroneLimits.length];
        
        // Execute Dijkstra
        IsochroneDijkstra dijkstra = new IsochroneDijkstra(rootNodeId, maxTT, reverseQuery, isochroneLimits);
        dijkstra.execute(edgesOfNode, maxLngNodes);
        
        // Create JSON object & fill it with information
        JSONObject jsonIsochroneResponse = new JSONObject();
        jsonIsochroneResponse.put("num", isochroneLimits.length);
        jsonIsochroneResponse.put("x", rootNode.lng);
        jsonIsochroneResponse.put("y", rootNode.lat);
        
        // Compute Edges' Hulls, encode them & put them in the JSON object
        IntArrayList nodesOfEdgesHull;
        for (int i = 0; i < isochroneLimits.length; i++) {
            nodesOfEdgesHull = new IntArrayList(10000);
            Reporting.print(i + "(" + (isochroneLimits[i]/600) +  ") maxLngNodeId: " + maxLngNodes[i]);
            EdgesHull.compute(maxLngNodes[i], edgesOfNode, isochroneLimits[i], nodesOfEdgesHull);
            Reporting.print(i + "(" + (isochroneLimits[i]/600) + ") nodesOfEdgesHull: #" + nodesOfEdgesHull.size());
            
            jsonIsochroneResponse.put("isochrone" + (i+1), EncodedPolyline.createIsochroneEncodings(nodesOfEdgesHull));
        }
        for (int i = isochroneLimits.length; i < 6; i++) {
            jsonIsochroneResponse.put("isochrone" + (i+1), "");
        }
        
        return jsonIsochroneResponse;
        
    }
    
}
