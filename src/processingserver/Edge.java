/*
 * TODO
 * Linestring in what form?
 */
package processingserver;

import java.util.ArrayList;

/**
 * All necessary data for each edge of the network
 * @author nicolaus
 */
public class Edge {
    
    public int id;
    public int sn_id;
    public int en_id;
    public int meters;
    public int tenthsOfSeconds;
    public boolean outgoing;
    public boolean incoming;
    public int inclination_from;
    public int inclination_to;
    public ArrayList<Point> linestring;
    
    public Edge(int _id, int _sn_id, int _en_id, int _meters, int _tenthsOfSeconds,
                boolean _outgoing, boolean _incoming, int _inclination_from, int _inclination_to,
                ArrayList<Point> _linestring) {
        id = _id;
        sn_id = _sn_id;
        en_id = _en_id;
        meters = _meters;
        tenthsOfSeconds = _tenthsOfSeconds;
        outgoing = _outgoing;
        incoming = _incoming;
        inclination_from = _inclination_from;
        inclination_to = _inclination_to;
        linestring = _linestring;
    }
    
    @Override
    public String toString() {
        return id + "[" + sn_id + "->" + en_id + "]: " + meters + "m | outgoing/incoming: " + outgoing + "/" + incoming + " | " + tenthsOfSeconds + "'";
    }
    
}
