package networkpreprocessor;

/**
 *
 * @author nicolaus
 */
public class Node implements Comparable {
    
    /** Node id */
    public int id;
    /** Id of the brother node (for nodes on crossing points) */
    public int brother;
    /** The original id if this node was created due to restrictions over another */
    public int restrictionOriginalNode;
    /** Whether the node will be enabled in the R* Tree (false for disconnected nodes) */
    public boolean enabledInRtree = true;
    /** The point of the node (coordinates) */
    public Point point;
    /** The index on edge (among all points of the edge) if it is a node that will break an edge */
    public int indexOnEdge = -1;
     
    public Node(int _id, int _brother, double _lng, double _lat) {
        id = _id;
        brother = _brother;
        point = new Point(_lat, _lng);
        enabledInRtree = true;
        restrictionOriginalNode = -1;
    }
    
    public Node(int _id, int _brother, Point _point) {
        id = _id;
        brother = _brother;
        point = _point;
        enabledInRtree = true;
        restrictionOriginalNode = -1;
    }
    
    public Node(int _id, int _brother, int _restrictionOriginalNode, boolean _enabledInRtree, Point _point) {
        id = _id;
        brother = _brother;
        restrictionOriginalNode = _restrictionOriginalNode;
        enabledInRtree = _enabledInRtree;
        point = _point;
    }
    
    @Override
    public String toString() {
        return id + "\t" + brother + "\t" + restrictionOriginalNode + "\t" + enabledInRtree + "\t"
                  + point.lng + "\t" + point.lat + "\tSRID=4326;POINT(" + point.lng + " " + point.lat + ")";
    }
    
    /**
     * Compares the value indexOnEdge (used to sort new nodes on their sequence in the edge to break)
     * @param o
     * @return 
     */
    @Override
    public int compareTo(Object o) {
        int compareRes = Integer.compare(this.indexOnEdge, ((Node) o).indexOnEdge);
        return compareRes;
    }
    
}
