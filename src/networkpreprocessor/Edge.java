package networkpreprocessor;

import java.util.ArrayList;

/**
 * 
 * @author nicolaus
 */
public class Edge {
    
    /** Edge id */
    public int id;
    /** Start node id */
    public int sn_id;
    /** End node id */
    public int en_id;
    /** Length of the edge in meters */
    public double meters;
    /** Cost of the edge in minutes */
    public double minutes;
    /** Whether the edge can be traversed the forward way by car */
    public boolean outgoing;
    /** Whether the edge can be traversed the reverse way by car */
    public boolean incoming;
    /** The arraylist of points that consist the linestring of the edge */
    public ArrayList<Point> linestring;
    /** The id in the original edge table */
    public int original_id;
    /** The inclination on the starting point of the edge (between the first two points) */
    public double inclination_from;
    /** The inclination on the ending point of the edge (between the last two points) */
    public double inclination_to;
    
    public Edge(int _id, int _sn_id, int _en_id, double _meters, double _minutes,
                boolean _outgoing, boolean _incoming, ArrayList<Point> _linestring) {
        id = _id;
        sn_id = _sn_id;
        en_id = _en_id;
        meters = _meters;
        minutes = _minutes;
        outgoing = _outgoing;
        incoming = _incoming;
        linestring = _linestring;
        original_id = _id;
    }
    
    public Edge(int _id, int _sn_id, int _en_id, double _meters, double _minutes,
                boolean _outgoing, boolean _incoming, ArrayList<Point> _linestring, int _original_id) {
        id = _id;
        sn_id = _sn_id;
        en_id = _en_id;
        meters = _meters;
        minutes = _minutes;
        outgoing = _outgoing;
        incoming = _incoming;
        linestring = _linestring;
        original_id = _original_id;
    }
    
    public Edge(Edge e) {
        id = e.id;
        sn_id = e.sn_id;
        en_id = e.en_id;
        meters = e.meters;
        minutes = e.minutes;
        outgoing = e.outgoing;
        incoming = e.incoming;
        linestring = new ArrayList<>(e.linestring);
        original_id = e.original_id;
        inclination_from = e.inclination_from;
        inclination_to = e.inclination_to;
    }
    
    @Override
    public String toString() {
        return id + "\t" + sn_id + "\t" + en_id + "\t" + meters + "\t" + minutes
                  + "\t" + outgoing + "\t" + incoming + "\t" + inclination_from + "\t" + inclination_to
                  + "\t" + original_id + "\t" + Database.printLinestring(linestring);
    }
    
}
