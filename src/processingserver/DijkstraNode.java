package processingserver;

/**
 *
 * @author nicolaus
 */
public class DijkstraNode implements Comparable {
    
    public int id;
    public int cost;
    
    
    public DijkstraNode(int _id, int _cost) {
        id = _id;
        cost = _cost;
    }
    
    @Override
    public int compareTo(Object o) {
        int my_r = Integer.compare(this.cost, ((DijkstraNode) o).cost);
        if (my_r == 0) {
            my_r = Integer.compare(this.id, ((DijkstraNode) o).id);
        }
        return my_r;
    }
    
}
