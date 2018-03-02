package networkpreprocessor;

/**
 *
 * @author nicolaus
 */
public class Point {
    
    /** Latitude */
    public Double lat;
    /** Longitude */
    public Double lng;
    
    public Point(double _lat, double _lng) {
        lat = _lat;
        lng = _lng;
    }
    
    /**
     * Checks whether two points are exactly the same
     * @param the point to compare to this
     * @return true if the same, else false
     */
    @Override
    public boolean equals(Object other){
        if (other == null) return false;
        if (other == this) return true;
        if (!(other instanceof Point))return false;
        Point myOther = (Point) other;
        if (myOther.lat.equals(this.lat) && myOther.lng.equals(this.lng))
            return true;
        return false;
    }
    
    @Override
    public String toString() {
        return lat + " " + lng;
    }
    
}
