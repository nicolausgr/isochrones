package processingserver;

import java.util.HashSet;
import java.util.PriorityQueue;

/**
 *
 * @author nicolaus
 */
public class RoutingALT {
    
    int startNodeId;
    int endNodeId;
    int numberOfNodes;
    boolean outgoingEdges;
    
    PriorityQueue<RoutingALTNode> pQueue = null;
    int[] nodeCostFromStart;
    
    HashSet<Integer> endNodes;
    
    
    public RoutingALT(int _startNodeId, int _endNodeId, boolean _outgoingEdges) {
        
        startNodeId = _startNodeId;
        endNodeId = _endNodeId;
        outgoingEdges = _outgoingEdges;
        
        numberOfNodes = TrafficStore.nodes.length;
        
        pQueue = new PriorityQueue(5000);
        nodeCostFromStart = new int[numberOfNodes];
        
        if (TrafficStore.newRestrictionNodesOverNodeWithId.containsKey(endNodeId)) {
            endNodes = new HashSet<Integer>(TrafficStore.newRestrictionNodesOverNodeWithId.get(endNodeId));
            endNodes.add(endNodeId);
        } else {
            endNodes = new HashSet<Integer>(1);
            endNodes.add(endNodeId);
        }
        
    }
    
    
    public RoutingALTNode execute() {
        
        // ALT initialization
        int maxInt = Integer.MAX_VALUE;
        for (int i = 0; i < numberOfNodes; i++) {
            nodeCostFromStart[i] = maxInt;
        }
        
        int potFunStartNode;
        
        // If there are no restriction nodes over the start node, simply add the sart node
        if (!TrafficStore.newRestrictionNodesOverNodeWithId.containsKey(startNodeId)) {
            potFunStartNode = Landmarks.getBestBoundAllLandmarks(startNodeId, endNodes);
            
            pQueue.add(new RoutingALTNode(startNodeId, 0, potFunStartNode, 0, null, null));
            nodeCostFromStart[startNodeId] = 0;
        }
        // Else only add the restriction nodes over it (in case the driver has come from a restriction edge)
        else {
            for (int snode: TrafficStore.newRestrictionNodesOverNodeWithId.get(startNodeId)) {
                potFunStartNode = Landmarks.getBestBoundAllLandmarks(snode, endNodes);
                
                pQueue.add(new RoutingALTNode(snode, 0, potFunStartNode, 0, null, null));
                nodeCostFromStart[snode] = 0;
            }
        }
        
        
        RoutingALTNode dnode;
        int dnodeId;
        Node dnodeNode;
        int firstEdgeIndex, numberOfEdges;
        Edge edge, edgeFromParent;
        int enId;
        int newCostFromStart, newCost, newSecondaryCost;
        
        int result;
        
        while (!pQueue.isEmpty()) {
            dnode = pQueue.poll();
            dnodeId = dnode.id;
            
            // Continue if already visited node
            if (dnode.costFromStart > nodeCostFromStart[dnodeId]) {
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
            
            
            // Iterate on neighbours
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
                
                newCostFromStart = dnode.costFromStart + edge.tenthsOfSeconds;
                
                
                // Continue if neighbour has already been visited with lower cost
                if (newCostFromStart >= nodeCostFromStart[enId]) {
                    continue;
                }
                
                newSecondaryCost = dnode.secondaryCost + edge.meters;
                result = Landmarks.getBestBoundAllLandmarks(enId, endNodes);
                newCost = newCostFromStart + result;
                
                pQueue.add(new RoutingALTNode(enId, newCostFromStart, newCost, newSecondaryCost, dnode, edge));
                nodeCostFromStart[enId] = newCostFromStart;
            }
            
        }
        
        // If we end up here then destination node not accessible
        return new RoutingALTNode(-1, -1, -1, -1, null, null);
        
    }
}
