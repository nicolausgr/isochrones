package processingserver;

/**
 *
 * @author nicolaus
 */
public class RoutingALTNode extends DijkstraNode implements Comparable {
    
    public int secondaryCost = -1;
    public RoutingALTNode parent = null;
    public int costFromStart = -1;
    public Edge edgeFromParent = null;
    
    public RoutingALTNode(int _id, int _costFromStart, int _cost, int _secondaryCost, RoutingALTNode _parent, Edge _edgeFromParent) {
        super(_id, _cost);
        secondaryCost = _secondaryCost;
        parent = _parent;
        costFromStart = _costFromStart;
        edgeFromParent = _edgeFromParent;
    }
    
    @Override
    public int compareTo(Object o) {
        int my_r = Integer.compare(this.cost, ((RoutingALTNode) o).cost);
        if (my_r == 0) {
            my_r = Integer.compare(this.costFromStart, ((RoutingALTNode) o).costFromStart);
            if (my_r == 0) {
                my_r = Integer.compare(this.secondaryCost, ((RoutingALTNode) o).secondaryCost);
                if (my_r == 0) {
                    my_r = Integer.compare(this.id, ((RoutingALTNode) o).id);
                }
            }
        }
        return my_r;
    }
    
}
