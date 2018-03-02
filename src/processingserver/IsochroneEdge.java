package processingserver;

/**
 *
 * @author nicolaus
 */
public class IsochroneEdge {
    
    public Edge edge = null;
    public int timeAtEnd = Integer.MAX_VALUE;

    public IsochroneEdge(Edge _edge, int _timeAtEnd) {
        edge = _edge;
        timeAtEnd = _timeAtEnd;
    }
    
}
