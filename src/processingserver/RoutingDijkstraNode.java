package processingserver;

/**
 *
 * @author nicolaus
 */
public class RoutingDijkstraNode implements Comparable {
    
    public int id;
    public int cost;
    public int secondaryCost;
    public RoutingDijkstraNode parent;
    public Edge edgeFromParent;
    
    
    public RoutingDijkstraNode(int _id, int _cost, int _secondaryCost, RoutingDijkstraNode _parent, Edge _edgeFromParent) {
        id = _id;
        cost = _cost;
        secondaryCost = _secondaryCost;
        parent = _parent;
        edgeFromParent = _edgeFromParent;
    }
    
    @Override
    public int compareTo(Object o) {
        int my_r = Integer.compare(this.cost, ((RoutingDijkstraNode) o).cost);
        if (my_r == 0) {
            my_r = Integer.compare(this.secondaryCost, ((RoutingDijkstraNode) o).secondaryCost);
        }
        if (my_r == 0) {
            my_r = Integer.compare(this.id, ((RoutingDijkstraNode) o).id);
        }
        return my_r;
    }
    
}
