package processingserver;

import java.util.PriorityQueue;

/**
 * Dijkstra from a landmark to the whole network
 * @author nicolaus
 */
public class LandmarkDijkstra {
    
    int landmarkIndex;
    int landmarkId;
    boolean reverseGraph;
    int numberOfNodes;
    
    PriorityQueue<DijkstraNode> pQueue;
    int[] nodeCost;

    public LandmarkDijkstra(int _landmarkIndex, int _landmarkId, boolean _reverseGraph) {
        
        landmarkIndex = _landmarkIndex;
        landmarkId = _landmarkId;
        reverseGraph = _reverseGraph;
        
        numberOfNodes = TrafficStore.nodes.length;
        
        pQueue = new PriorityQueue(5000);
        nodeCost = new int[numberOfNodes];
        
    }

    /**
     *
     * @param landmarkNodeId
     */
    public void execute() {
        
        // Dijkstra initialization
        int maxInt = Integer.MAX_VALUE;
        for (int i = 0; i < numberOfNodes; i++) {
            nodeCost[i] = maxInt;
        }
        
        pQueue.add(new DijkstraNode(landmarkId, 0));
        nodeCost[landmarkId] = 0;
        
        
        DijkstraNode dnode;
        Node dnodeNode;
        int dnodeId, dnodeCost;
        int firstEdgeIndex, numberOfEdges;
        Edge edge;
        int enId, enCost;
        
        
        while (!pQueue.isEmpty()) {
            dnode = pQueue.poll();
            dnodeId = dnode.id;
            dnodeCost = dnode.cost;
            
            // Continue if already visited node
            if (dnode.cost > nodeCost[dnodeId]) {
                continue;
            }
            
            // Extract dnode's information
            dnodeNode = TrafficStore.nodes[dnodeId];
            firstEdgeIndex = dnodeNode.firstEdgeIndex;
            numberOfEdges = dnodeNode.numberOfEdges;

            // Iterate on neighbours
            for (int i = firstEdgeIndex; i < firstEdgeIndex + numberOfEdges; i++) {
                edge = TrafficStore.edges[i];

                // Process only incoming edges if onReverseGraph else outgoing
                if (!reverseGraph) {
                    if (!edge.outgoing) {
                        continue;
                    }
                } else {
                    if (!edge.incoming) {
                        continue;
                    }
                }
                
                enId = edge.en_id;
                
                enCost = dnodeCost + edge.tenthsOfSeconds;
                
                // Continue if neighbour has already been visited with lower cost
                if (enCost >= nodeCost[enId]) {
                    continue;
                }

                pQueue.add(new DijkstraNode(enId, enCost));
                nodeCost[enId] = enCost;
            }

        }

        // We end up here when the entire network is traversed
        Landmarks.copyNodeCostToLandmarks(nodeCost, numberOfNodes, landmarkIndex, reverseGraph);

    }
}
