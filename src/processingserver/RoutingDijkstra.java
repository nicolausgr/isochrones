package processingserver;

import java.util.HashSet;
import java.util.PriorityQueue;

/**
 *
 * @author nicolaus
 */
public class RoutingDijkstra {
    
    int startNodeId;
    int endNodeId;
    int numberOfNodes;
    boolean outgoingEdges;
    
    PriorityQueue<RoutingDijkstraNode> pQueue = null;
    int[] nodeCost;
    
    HashSet<Integer> endNodes;
    
    
    public RoutingDijkstra(int _startNodeId, int _endNodeId, boolean _outgoingEdges) {
        
        startNodeId = _startNodeId;
        endNodeId = _endNodeId;
        outgoingEdges = _outgoingEdges;
        
        numberOfNodes = TrafficStore.nodes.length;
        
        pQueue = new PriorityQueue(5000);
        nodeCost = new int[numberOfNodes];
        
        if (TrafficStore.newRestrictionNodesOverNodeWithId.containsKey(endNodeId)) {
            endNodes = new HashSet<Integer>(TrafficStore.newRestrictionNodesOverNodeWithId.get(endNodeId));
            endNodes.add(endNodeId);
        } else {
            endNodes = new HashSet<Integer>(1);
            endNodes.add(endNodeId);
        }
        
    }
    
    public RoutingDijkstraNode execute() {
        
        // Dijkstra initialization
        int maxInt = Integer.MAX_VALUE;
        for (int i = 0; i < numberOfNodes; i++) {
            nodeCost[i] = maxInt;
        }
        
        // If there are no restriction nodes over the start node, simply add the sart node
        if (!TrafficStore.newRestrictionNodesOverNodeWithId.containsKey(startNodeId)) {
            pQueue.add(new RoutingDijkstraNode(startNodeId, 0, 0, null, null));
            nodeCost[startNodeId] = 0;
        }
        // Else only add the restriction nodes over it (in case the driver has come from a restriction edge)
        else {
            for (int snode: TrafficStore.newRestrictionNodesOverNodeWithId.get(startNodeId)) {
                pQueue.add(new RoutingDijkstraNode(snode, 0, 0, null, null));
                nodeCost[snode] = 0;
            }
        }
        
        
        RoutingDijkstraNode dnode;
        int dnodeId;
        Node dnodeNode;
        int firstEdgeIndex, numberOfEdges;
        Edge edge, edgeFromParent;
        int enId, newEnCost, newEnSecondaryCost;
        
        
        while (!pQueue.isEmpty()) {
            dnode = pQueue.poll();
            dnodeId = dnode.id;

            // If it has been expanded (with lower cost), continue
            if (dnode.cost > nodeCost[dnodeId]) {
                continue;
            }

            // Return if this node is the destination node (original or restriction node over it)
            if (endNodes.contains(dnodeId)) {
                return dnode;
            }
            
            
            dnodeNode = TrafficStore.nodes[dnodeId];
            
            
            // Scan edges
            firstEdgeIndex = dnodeNode.firstEdgeIndex;
            numberOfEdges = dnodeNode.numberOfEdges;
            
            for (int i = firstEdgeIndex; i < firstEdgeIndex + numberOfEdges; i++) {
                edge = TrafficStore.edges[i];
                
                // Process only outgoing edges (or incoming if this is selected)
                if ((outgoingEdges && !edge.outgoing) || (!outgoingEdges && !edge.incoming)) {
                    continue;
                }
                
                enId = edge.en_id;
                
                // If we came here from a restriction node we can't go back to its original node
                edgeFromParent = dnode.edgeFromParent;
                if (edgeFromParent != null &&
                        TrafficStore.nodes[edgeFromParent.sn_id].restriction_original_id == enId) {
                    continue;
                }
                
                newEnCost = dnode.cost + edge.tenthsOfSeconds;
                
                // Continue if neighbour has already been visited with lower cost
                if (newEnCost >= nodeCost[enId]) {
                    continue;
                }
                
                newEnSecondaryCost = dnode.secondaryCost + edge.meters;
                
                pQueue.add(new RoutingDijkstraNode(enId, newEnCost, newEnSecondaryCost, dnode, edge));
                nodeCost[enId] = newEnCost;
            }
            
        }
        
        // If we end up here then destination node not accessible
        return new RoutingDijkstraNode(-1, -1, -1, null, null);
        
    }
}
