package processingserver;

/**
 * All necessary data for each node of the network.
 * Includes the index of the first adjacent edge & the number of edges
 * @author nicolaus
 */
public class Node {
    
    public int id;
    public int brother_id;
    public int restriction_original_id;
    public boolean enabledInRtree;
    public double lng;
    public double lat;
    public int firstEdgeIndex;
    public int numberOfEdges;
    
    public Node(int _id, int _brother_id, int _restriction_original_id, boolean _enabledInRtree, double _lng, double _lat) {
        id = _id;
        brother_id = _brother_id;
        restriction_original_id = _restriction_original_id;
        enabledInRtree = _enabledInRtree;
        lng = _lng;
        lat = _lat;
        firstEdgeIndex = -1;
        numberOfEdges = 0;
    }
    
    @Override
    public String toString() {
        return id + "(" + lng + " , " + lat + ")";
    }
    
}
