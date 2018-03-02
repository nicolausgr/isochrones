package networkpreprocessor;

/**
 *
 * @author nicolaus
 */
public class Restriction {
    
    /** A serial id of the restriction record */
    public int serial_id;
    /** An id for the restriction */
    public int restriction_id;
    /** A sequence number of the record inside the whole restriction */
    public int seqnr;
    /** The id of the edge */
    public int edge_id;
    /** The id of the start node of the edge */
    public int sn_id;
    /** The id of the end node of the edge */
    public int en_id;
    /** The number of edges in the whole restriction */
    public int numberOfEdgesOfRestriction;
    /** Whether the restriction is active (false if any of its edges is outgoing:false) */
    public boolean active;
    /** The maneuver id in the original maneuver table */
    public Double original_mn_id;
    
    public Restriction(int _serial_id, int _restriction_id, int _seqnr,
                       int _edge_id, int _sn_id, int _en_id, double _original_mn_id) {
        serial_id = _serial_id;
        restriction_id = _restriction_id;
        seqnr = _seqnr;
        edge_id = _edge_id;
        sn_id = _sn_id;
        en_id = _en_id;
        original_mn_id = _original_mn_id;
        numberOfEdgesOfRestriction = -1;
        active = true;
    }
    
    @Override
    public String toString() {
        return serial_id + "\t" + restriction_id + "\t" + seqnr + "\t" + edge_id + "\t"
               + sn_id + "\t" + en_id + "\t" + numberOfEdgesOfRestriction + "\t" + active + "\t" + original_mn_id;
    }
    
}
