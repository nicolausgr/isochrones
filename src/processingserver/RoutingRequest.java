package processingserver;

import java.util.Random;
import org.json.simple.JSONObject;

/**
 *
 * @author nicolaus
 */
public class RoutingRequest {
    
    private boolean threePoint;
    private double x1;
    private double y1;
    private double x2;
    private double y2;
    private double x3;
    private double y3;
    private int node1;
    private int node2;
    private int node3;
    
    public RoutingRequest(boolean _threePoint, double _x1, double _y1,
                                               double _x2, double _y2,
                                               double _x3, double _y3) {
        threePoint = _threePoint;
        x1 = _x1;
        y1 = _y1;
        x2 = _x2;
        y2 = _y2;
        x3 = _x3;
        y3 = _y3;
    }
    
    public JSONObject compute() {
        
        // Determine closest nodes by looking up the R* tree
        node1 = RTree.closestNodeToCoords(x1, y1);
        node2 = RTree.closestNodeToCoords(x2, y2);
        if (threePoint) {
            node3 = RTree.closestNodeToCoords(x3, y3);
            Reporting.print("ROUTING: " + node1 + " (" + TrafficStore.nodes[node1].lng + "," + TrafficStore.nodes[node1].lat + ")  ->  "
                                        + node2 + " (" + TrafficStore.nodes[node2].lng + "," + TrafficStore.nodes[node2].lat + ")  ->  "
                                        + node3 + " (" + TrafficStore.nodes[node3].lng + "," + TrafficStore.nodes[node3].lat + ")");
        } else {
            Reporting.print("ROUTING: " + node1 + " (" + TrafficStore.nodes[node1].lng + "," + TrafficStore.nodes[node1].lat + ")  ->  "
                                        + node2 + " (" + TrafficStore.nodes[node2].lng + "," + TrafficStore.nodes[node2].lat + ")");
        }
        
        JSONObject jsonRoutingResponse = new JSONObject();
        
        if (!threePoint) {
            
//            // Execute Dijkstra
//            RoutingDijkstra dijkstra = new RoutingDijkstra(node1, node2, true);
//            RoutingDijkstraNode finalDnode = dijkstra.execute();
            
            // Execute ALT
            RoutingALT alt = new RoutingALT(node1, node2, true);
            RoutingALTNode finalALTnode = alt.execute();
            
            // Fill JSON object
            jsonRoutingResponse.put("x1", TrafficStore.nodes[node1].lng);
            jsonRoutingResponse.put("y1", TrafficStore.nodes[node1].lat);
            jsonRoutingResponse.put("x2", TrafficStore.nodes[node2].lng);
            jsonRoutingResponse.put("y2", TrafficStore.nodes[node2].lat);
            jsonRoutingResponse.put("tt", finalALTnode.cost);
            jsonRoutingResponse.put("distance", finalALTnode.secondaryCost);
            String encodedString = EncodedPolyline.createRoutingEncodings(finalALTnode);
            jsonRoutingResponse.put("path", encodedString);
            
        } else {
            
//            // Execute 1st Dijkstra
//            RoutingDijkstra dijkstra1 = new RoutingDijkstra(node1, node2, true);
//            RoutingDijkstraNode finalDnode1 = dijkstra1.execute();
//            // Execute 2nd Dijkstra
//            RoutingDijkstra dijkstra2 = new RoutingDijkstra(node2, node3, true);
//            RoutingDijkstraNode finalDnode2 = dijkstra2.execute();
            
            // Execute 1st ALT
            RoutingALT alt1 = new RoutingALT(node1, node2, true);
            RoutingALTNode finalALTnode1 = alt1.execute();
            // Execute 2nd ALT
            RoutingALT alt2 = new RoutingALT(node2, node3, true);
            RoutingALTNode finalALTnode2 = alt2.execute();
            
            // Fill JSON object
            jsonRoutingResponse.put("x1", TrafficStore.nodes[node1].lng);
            jsonRoutingResponse.put("y1", TrafficStore.nodes[node1].lat);
            jsonRoutingResponse.put("x2", TrafficStore.nodes[node2].lng);
            jsonRoutingResponse.put("y2", TrafficStore.nodes[node2].lat);
            jsonRoutingResponse.put("x3", TrafficStore.nodes[node3].lng);
            jsonRoutingResponse.put("y3", TrafficStore.nodes[node3].lat);
            jsonRoutingResponse.put("tt1_2", finalALTnode1.cost);
            jsonRoutingResponse.put("distance1_2", finalALTnode1.secondaryCost);
            jsonRoutingResponse.put("path1_2", EncodedPolyline.createRoutingEncodings(finalALTnode1));
            jsonRoutingResponse.put("tt2_3", finalALTnode2.cost);
            jsonRoutingResponse.put("distance2_3", finalALTnode2.secondaryCost);
            jsonRoutingResponse.put("path2_3", EncodedPolyline.createRoutingEncodings(finalALTnode2));
            
        }
        
//        executeTests();
        
        return jsonRoutingResponse;
        
    }
    
    private void executeTests() {
        
        int a, b;
        long timeA, timeB;
        long timeDijkstra = 0, timeALT = 0;
        
        for (int i = 0; i < 10000; i++) {
            Random rand = new Random();
            
            a = rand.nextInt(TrafficStore.nodes.length);
            while (!TrafficStore.nodes[a].enabledInRtree) {
                a = rand.nextInt(TrafficStore.nodes.length);
            }
            b = rand.nextInt(TrafficStore.nodes.length);
            while (!TrafficStore.nodes[b].enabledInRtree) {
                b = rand.nextInt(TrafficStore.nodes.length);
            }
            
            timeA = System.nanoTime();
            RoutingDijkstra dijkstra = new RoutingDijkstra(a, b, true);
            RoutingDijkstraNode finalDnode = dijkstra.execute();
            timeB = System.nanoTime();
            timeDijkstra += (timeB - timeA);
            
            timeA = System.nanoTime();
            RoutingALT alt = new RoutingALT(a, b, true);
            RoutingALTNode finalALTnode = alt.execute();
            timeB = System.nanoTime();
            timeALT += (timeB - timeA);

            System.out.println("[" + i + "]: " + a + ";" + b + ";" + finalDnode.cost + ";" + finalALTnode.cost + ";" + (finalDnode.cost == finalALTnode.cost));
        }
        
        System.out.println("Dijkstra: " + ((timeDijkstra/10000.0)/1e9) + " sec");
        System.out.println("ALT:      " + ((timeALT/10000.0)/1e9) + " sec");
    }
}
