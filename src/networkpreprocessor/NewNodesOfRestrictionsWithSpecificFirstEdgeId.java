package networkpreprocessor;

import java.util.ArrayList;

/**
 * Each one of these objects holds all the new nodes created for every restriction with a specific first edge
 * @author nicolaus
 */
public class NewNodesOfRestrictionsWithSpecificFirstEdgeId {
    
    public int firstRestrictionEdgeId;
    public ArrayList<NewNodeOfRestriction> newNodes;
    
    // This constructor is called in the first occurence of a restriction with this first edge id (in the first edge)
    public NewNodesOfRestrictionsWithSpecificFirstEdgeId(int _firstRestrictionEdgeId, int _newNodeId, int _oldNodeId) {
        firstRestrictionEdgeId = _firstRestrictionEdgeId;
        newNodes = new ArrayList<>();
        newNodes.add(new NewNodeOfRestriction(_newNodeId, _oldNodeId));
    }
    
    public void addNewNode(int _newNodeId, int _oldNodeId) {
        newNodes.add(new NewNodeOfRestriction(_newNodeId, _oldNodeId));
    }
    
    public void restrictThisEdgeEnIdFromThisOldNodeId(int nodeIdToRestrict, int fromThisOldNodeId) {
        newNodeWithThisOldId(fromThisOldNodeId).addEnIdOfNotToCreateEdge(nodeIdToRestrict);
    }
    
    public int newNodeIdFromOldNodeId(int _oldNodeId) {
        NewNodeOfRestriction _newNode = newNodeWithThisOldId(_oldNodeId);
        if (_newNode != null) {
            return _newNode.newNodeId;
        } else {
            System.out.println("ERROR: No node found in NewNodesOfRestrictionsWithSpecificFirstEdgeId with this old id");
            return -1;
        }
    }
    
    public boolean oldNodeIdAlreadyContained(int _oldNodeId) {
        if (newNodeWithThisOldId(_oldNodeId) != null) {
            return true;
        } else {
            return false;
        }
    }
    
    public NewNodeOfRestriction newNodeWithThisNewId(int _newNodeId) {
        for (NewNodeOfRestriction n: newNodes) {
            if (n.newNodeId == _newNodeId) {
                return n;
            }
        }
        
        return null;
    }
    
    private NewNodeOfRestriction newNodeWithThisOldId(int _oldNodeId) {
        for (NewNodeOfRestriction n: newNodes) {
            if (n.oldNodeId == _oldNodeId) {
                return n;
            }
        }
        
        return null;
    }
    
    public class NewNodeOfRestriction {
        public int newNodeId;
        public int oldNodeId;
        ArrayList<Integer> EdgeEnIdsNotToCreate;
        
        public NewNodeOfRestriction(int _newNodeId, int _oldNodeId) {
            newNodeId = _newNodeId;
            oldNodeId = _oldNodeId;
            EdgeEnIdsNotToCreate = new ArrayList<>();
        }
        
        public void addEnIdOfNotToCreateEdge(int _nodeId) {
            if (EdgeEnIdsNotToCreate == null) {
                EdgeEnIdsNotToCreate = new ArrayList<>();
            }
            EdgeEnIdsNotToCreate.add(_nodeId);
        }
    }
    
}
