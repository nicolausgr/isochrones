package processingserver;

import cern.colt.list.IntArrayList;
import java.util.ArrayList;
import java.util.HashSet;

/**
 *
 * @author nicolaus
 */
public class EdgesHull {
    
    public static void compute(int maxLngNode, ArrayList<IsochroneEdge>[] edgesOfNode, int timeLimit, IntArrayList nodesOfEdgesHull) {
        
        boolean notDoneYet = true;
        int MAXINT = Integer.MAX_VALUE;
        int minAngle, angle;
        int currNodeId, prevNodeId, brotherId;
        int lastFollowedEdgeFromMaxLat, lastEdgeInclinationFromMaxLat;
        int edgeCounter;
        int restrictionOriginalId;
        HashSet<Integer> allNodesToCheck;
        
        // First node: max lng node
        currNodeId = maxLngNode;
        nodesOfEdgesHull.add(currNodeId);
        minAngle = MAXINT;
        IsochroneEdge nextEdge = null, prevEdge;
        // Check edges of current node & whole restriction stack
        restrictionOriginalId = TrafficStore.nodes[currNodeId].restriction_original_id;
        if (restrictionOriginalId >= 0) {
            allNodesToCheck = new HashSet<Integer>(TrafficStore.newRestrictionNodesOverNodeWithId.get(restrictionOriginalId));
            allNodesToCheck.add(restrictionOriginalId);
        } else {
            if (TrafficStore.newRestrictionNodesOverNodeWithId.containsKey(currNodeId)) {
                allNodesToCheck = new HashSet<Integer>(TrafficStore.newRestrictionNodesOverNodeWithId.get(currNodeId));
            } else {
                allNodesToCheck = new HashSet<Integer>(1);
            }
            allNodesToCheck.add(currNodeId);
        }
        // Check edges of brother node & whole restriction stack
        brotherId = TrafficStore.nodes[currNodeId].brother_id;
        if (brotherId >= 0) {
            restrictionOriginalId = TrafficStore.nodes[brotherId].restriction_original_id;
            if (restrictionOriginalId >= 0) {
                allNodesToCheck.addAll(TrafficStore.newRestrictionNodesOverNodeWithId.get(restrictionOriginalId));
                allNodesToCheck.add(restrictionOriginalId);
            } else {
                if (TrafficStore.newRestrictionNodesOverNodeWithId.containsKey(brotherId)) {
                    allNodesToCheck.addAll(TrafficStore.newRestrictionNodesOverNodeWithId.get(brotherId));
                }
                allNodesToCheck.add(brotherId);
            }
        }
        for (Integer _id: allNodesToCheck) {
            if (edgesOfNode[_id] != null) {
                for (IsochroneEdge e : edgesOfNode[_id]) {
                    if (e.timeAtEnd <= timeLimit && e.edge.inclination_from < minAngle) {
                        minAngle = e.edge.inclination_from;
                        nextEdge = e;
                    }
                }
            }
        }
        if (nextEdge == null) {
            return;
        }
        
        prevNodeId = currNodeId;
        currNodeId = nextEdge.edge.en_id;
        
        nodesOfEdgesHull.add(currNodeId);
        
        prevEdge = nextEdge;
        lastFollowedEdgeFromMaxLat = nextEdge.edge.id;


        while (notDoneYet) {
            while (currNodeId != maxLngNode) {
                minAngle = MAXINT;
                nextEdge = null;
                edgeCounter = 0;
                
                // Check edges of current node & whole restriction stack
                restrictionOriginalId = TrafficStore.nodes[currNodeId].restriction_original_id;
                if (restrictionOriginalId >= 0) {
                    allNodesToCheck = new HashSet<Integer>(TrafficStore.newRestrictionNodesOverNodeWithId.get(restrictionOriginalId));
                    allNodesToCheck.add(restrictionOriginalId);
                } else {
                    if (TrafficStore.newRestrictionNodesOverNodeWithId.containsKey(currNodeId)) {
                        allNodesToCheck = new HashSet<Integer>(TrafficStore.newRestrictionNodesOverNodeWithId.get(currNodeId));
                    } else {
                        allNodesToCheck = new HashSet<Integer>(1);
                    }
                    allNodesToCheck.add(currNodeId);
                }
                // Check edges of brother node & whole restriction stack
                brotherId = TrafficStore.nodes[currNodeId].brother_id;
                if (brotherId >= 0) {
                    restrictionOriginalId = TrafficStore.nodes[brotherId].restriction_original_id;
                    if (restrictionOriginalId >= 0) {
                        allNodesToCheck.addAll(TrafficStore.newRestrictionNodesOverNodeWithId.get(restrictionOriginalId));
                        allNodesToCheck.add(restrictionOriginalId);
                    } else {
                        if (TrafficStore.newRestrictionNodesOverNodeWithId.containsKey(brotherId)) {
                            allNodesToCheck.addAll(TrafficStore.newRestrictionNodesOverNodeWithId.get(brotherId));
                        }
                        allNodesToCheck.add(brotherId);
                    }
                }
                for (Integer _id: allNodesToCheck) {
                    if (edgesOfNode[_id] != null) {
                        for (IsochroneEdge e: edgesOfNode[_id]) {
                            if (e.timeAtEnd <= timeLimit) {
                                edgeCounter++;

                                angle = e.edge.inclination_from + 180 - prevEdge.edge.inclination_to;
                                if (angle <= 0) {
                                    angle += 360;
                                }
                                if (angle > 360) {
                                    angle -= 360;
                                }

                                if (angle < minAngle && e.edge.en_id == prevNodeId) {
                                    if (edgeCounter == 1) {
                                        nextEdge = e;
                                    }
                                }
                                else if (angle < minAngle) {
                                    minAngle = angle;
                                    nextEdge = e;
                                }
                            }
                        }
                    }
                }
                
                if (nextEdge == null) {
                    return;
                }
                
                prevNodeId = currNodeId;
                currNodeId = nextEdge.edge.en_id;
                
                //Remove tails
                int size = nodesOfEdgesHull.size();
                if (size > 2 && currNodeId == nodesOfEdgesHull.getQuick(size - 2)) {
                    nodesOfEdgesHull.remove(size - 1);
                    nodesOfEdgesHull.remove(size - 2);
                }
                
                nodesOfEdgesHull.add(currNodeId);
                
                prevEdge = nextEdge;
            }
            
            /*
             * The following are in case two separate paths finish both on the same rightest node of isochrone
             */
            minAngle = MAXINT;
            nextEdge = null;
            lastEdgeInclinationFromMaxLat = -1;
            for (IsochroneEdge e: edgesOfNode[maxLngNode]) {
                if (e.edge.id == prevEdge.edge.id || e.edge.id == -prevEdge.edge.id) {
                    lastEdgeInclinationFromMaxLat = e.edge.inclination_from;
                }
            }
            for (IsochroneEdge e: edgesOfNode[maxLngNode]) {
                if (e.edge.id != lastFollowedEdgeFromMaxLat && e.timeAtEnd <= timeLimit
                        && e.edge.inclination_from < minAngle && e.edge.inclination_from > lastEdgeInclinationFromMaxLat) {
                    
                    minAngle = e.edge.inclination_from;
                    nextEdge = e;
                }
            }
            if (nextEdge == null) {
                // Remove rightest tail
                int size = nodesOfEdgesHull.size();
                while (size > 3 && nodesOfEdgesHull.getQuick(1) == nodesOfEdgesHull.getQuick(size - 2)) {
                    nodesOfEdgesHull.remove(size - 1);
                    nodesOfEdgesHull.remove(0);
                    size -= 2;
                }
                
                return;
            }
            
            prevNodeId = currNodeId;
            currNodeId = nextEdge.edge.en_id;    
            
            //Remove tails
            int size = nodesOfEdgesHull.size();
            if (size > 2 && currNodeId == nodesOfEdgesHull.getQuick(size - 2)) {
                nodesOfEdgesHull.remove(size - 1);
                nodesOfEdgesHull.remove(size - 2);
            }
            
            nodesOfEdgesHull.add(currNodeId);
            
            prevEdge = nextEdge;
            
            lastFollowedEdgeFromMaxLat = nextEdge.edge.id;
            
        }
    }

}
