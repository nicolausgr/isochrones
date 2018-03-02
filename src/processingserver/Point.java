package processingserver;

/**
 *
 * @author nicolaus
 */
public class Point {
    
    public Double lat;
    public Double lng;
    
    public Point(double _lat, double _lng) {
        lat = _lat;
        lng = _lng;
    }
    
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
