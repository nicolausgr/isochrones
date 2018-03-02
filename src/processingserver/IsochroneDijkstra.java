/*
 * TODO
 * Change PriorityQueue to PriorityQueueDial
 */
package processingserver;

import java.util.ArrayList;
import java.util.PriorityQueue;

/**
 *
 * @author nicolaus
 */
public class IsochroneDijkstra {
    
    int rootNode;
    int maxLimit;
    boolean reverseGraph;
    int[] isochroneLimits;
    int numberOfNodes;
    
    PriorityQueue<DijkstraNode> pQueue;
    int[] nodeCost;
    
    public IsochroneDijkstra(int _rootNode, int _maxLimit, boolean _reverseGraph, int[] _isochroneLimits) {
        
        rootNode = _rootNode;
        maxLimit = _maxLimit;
        reverseGraph = _reverseGraph;
        isochroneLimits = _isochroneLimits;
        
        numberOfNodes = TrafficStore.nodes.length;
        
        pQueue = new PriorityQueue(5000);
        nodeCost = new int[numberOfNodes];
        
    }
    
    public void execute(ArrayList<IsochroneEdge>[] edgesOfNode, int[] maxLngNodes) {
        
        // Dijkstra initialization
        int maxInt = Integer.MAX_VALUE;
        for (int i = 0; i < numberOfNodes; i++) {
            nodeCost[i] = maxInt;
        }
        
        pQueue.add(new DijkstraNode(rootNode, 0));
        nodeCost[rootNode] = 0;
        
        
        // Max longitude nodes needed for Edges' Hulls: initialization
        double[] maxLng = new double[isochroneLimits.length];
        for (int i = 0; i < isochroneLimits.length; i++) {
            maxLngNodes[i] = -1;
            maxLng[i] = Double.MIN_VALUE;
        }
        
        DijkstraNode dnode;
        int dnodeId, dnodeCost;
        Node dnodeNode;
        int firstEdgeIndex, numberOfEdges;
        Edge edge, oppositeEdge;
        int enId, edgeCost, enCost;
        int isochroneIndex = 0;
        
        edgesOfNode[rootNode] = new ArrayList<IsochroneEdge>(4);
        
        
        while (!pQueue.isEmpty()) {
            dnode = pQueue.poll();
            dnodeId = dnode.id;
            dnodeCost = dnode.cost;
            
            // If it has been expanded (with lower cost), continue
            if (dnodeCost > nodeCost[dnodeId]) {
                continue;
            }
            
            // Adjust the isochrone index (for the max lats)
            while (dnodeCost > isochroneLimits[isochroneIndex]) {
                isochroneIndex++;
            }
            
            
            dnodeNode = TrafficStore.nodes[dnodeId];
            
            
            // Process to find max longitude nodes
            // Also, a loop in the end for cases of maxLng[i] > maxLng[i+1]
            if (dnodeNode.lng > maxLng[isochroneIndex]) {
                maxLng[isochroneIndex] = dnodeNode.lng;
                maxLngNodes[isochroneIndex] = dnodeId;
            }
            
            // Scan edges
            firstEdgeIndex = dnodeNode.firstEdgeIndex;
            numberOfEdges = dnodeNode.numberOfEdges;
            
            for (int i = firstEdgeIndex; i < firstEdgeIndex + numberOfEdges; i++) {
                edge = TrafficStore.edges[i];
                
                // If reverse query and forward edge or the opposite, continue
                if ((reverseGraph && !edge.incoming) || (!reverseGraph && !edge.outgoing)) {
                    continue;
                }
                
                enId = edge.en_id;
                
                // Calculate edge's isochrone time (time to traverse it starting from root)
                edgeCost = dnodeCost + edge.tenthsOfSeconds;
                
                // If isochrone time exceeds max limit, continue
                if (edgeCost > maxLimit){
                    continue;
                }
                
                // Get cost of neighbor to see if
                //   a) we improve cost to add to priority queue
                //   b) the neighbor has been expanded before
                enCost = nodeCost[enId];
                
                
                /*
                 * Save isochrone Edge
                 */
                // We add only iso edges that lead to not already expanded nodes (current expansion limit: dnodeCost),
                // because those edges leading to expanded nodes have already been added. We put = in case we have
                // same costs in order to at least add the edge (even if it will be added to both nodes).
                if (dnodeCost <= enCost) {
                    int edgeId = edge.id;
                    // NOTE: edgesOfNode[dnodeId] has already been initialized
                    if (edgesOfNode[enId] == null) {
                        edgesOfNode[enId] = new ArrayList<IsochroneEdge>(6);
                    }
                    // Add edge to the start node's edge catalog
                    edgesOfNode[dnodeId].add(new IsochroneEdge(edge, edgeCost));
                    
                    // Add edge (with opposite inclination) to the end node's edge catalog
                    oppositeEdge = TrafficStore.edges[TrafficStore.edgeIndexOfEdgeWithId.get(-edgeId)];
                    edgesOfNode[enId].add(new IsochroneEdge(oppositeEdge, edgeCost));
                }
                
                // If end node already added in PQ with lower cost, don't add to pQueue
                if (edgeCost >= enCost) {
                    continue;
                }
                
                pQueue.add(new DijkstraNode(enId, edgeCost));
                nodeCost[enId] = edgeCost;
            }
        }
        
        
        // Loop for cases of maxLng[i] > maxLng[i+1]
        for (int j = 0; j < isochroneLimits.length-1; j++) {
            if (maxLng[j] > maxLng[j+1]) {
                maxLng[j+1] = maxLng[j];
                maxLngNodes[j+1] = maxLngNodes[j];
            }
        }
        
    }
    
}
