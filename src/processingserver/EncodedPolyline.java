/**
 * Reimplementation of Mark McClures Javascript PolylineEncoder All the
 * mathematical logic is more or less copied by McClure
 *
 * @author Mark Rambow
 * @e-mail markrambow[at]gmail[dot]com
 * @version 0.1
 *
 */
package processingserver;

import cern.colt.list.IntArrayList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

/**
 *
 * @author nicolaus
 */
public class EncodedPolyline {
    
    private static double verySmall = 0.0013;
    
    
    /**
     * distance(p0, p1, p2) computes the distance between the point p0 and the
     * segment [p1,p2]. This could probably be replaced with something that is a
     * bit more numerically stable.
     *
     * @param p0
     * @param p1
     * @param p2
     * @return
     */
    public static double distance(Point p0, Point p1, Point p2) {
        double u, out = 0.0;

        if (p1.lat == p2.lat
                && p1.lng == p2.lng) {
            out = Math.sqrt(Math.pow(p2.lat - p0.lat, 2)
                    + Math.pow(p2.lng - p0.lng, 2));
        } else {
            u = ((p0.lat - p1.lat)
                    * (p2.lat - p1.lat) + (p0
                    .lng - p1.lng)
                    * (p2.lng - p1.lng))
                    / (Math.pow(p2.lat - p1.lat, 2) + Math
                    .pow(p2.lng - p1.lng, 2));

            if (u <= 0) {
                out = Math.sqrt(Math.pow(p0.lat - p1.lat,
                        2)
                        + Math.pow(p0.lng - p1.lng, 2));
            }
            if (u >= 1) {
                out = Math.sqrt(Math.pow(p0.lat - p2.lat,
                        2)
                        + Math.pow(p0.lng - p2.lng, 2));
            }
            if (0 < u && u < 1) {
                out = Math.sqrt(Math.pow(p0.lat - p1.lat
                        - u * (p2.lat - p1.lat), 2)
                        + Math.pow(p0.lng - p1.lng - u
                        * (p2.lng - p1.lng), 2));
            }
        }
        return out;
    }
    
    private static int floor1e5(double coordinate) {
        return (int) Math.floor(coordinate * 1e5);
    }

    private static String encodeSignedNumber(int num) {
        int sgn_num = num << 1;
        if (num < 0) {
            sgn_num = ~(sgn_num);
        }
        return (encodeNumber(sgn_num));
    }

    private static String encodeNumber(int num) {

        StringBuilder encodeString = new StringBuilder();

        while (num >= 0x20) {
            int nextValue = (0x20 | (num & 0x1f)) + 63;
            encodeString.append((char) (nextValue));
            num >>= 5;
        }

        num += 63;
        encodeString.append((char) (num));

        return encodeString.toString();
    }
    
    private static String createEncodings(IntArrayList nodeIds, double[] dists) {
        StringBuffer encodedPoints = new StringBuffer();

        double maxlat = 0, minlat = 0, maxlon = 0, minlon = 0;

        int plat = 0;
        int plng = 0;

        Node nodei;

        for (int i = 0; i < nodeIds.size(); i++) {

            nodei = TrafficStore.nodes[nodeIds.getQuick(i)];

            // determin bounds (max/min lat/lon)
            if (i == 0) {
                maxlat = minlat = nodei.lat;
                maxlon = minlon = nodei.lng;
            } else {
                if (nodei.lat > maxlat) {
                    maxlat = nodei.lat;
                } else if (nodei.lat < minlat) {
                    minlat = nodei.lat;
                } else if (nodei.lng > maxlon) {
                    maxlon = nodei.lng;
                } else if (nodei.lng < minlon) {
                    minlon = nodei.lng;
                }
            }

            if (dists[i] != 0 || i == 0 || i == nodeIds.size() - 1) {

                int late5 = floor1e5(nodei.lat);
                int lnge5 = floor1e5(nodei.lng);

                int dlat = late5 - plat;
                int dlng = lnge5 - plng;

                plat = late5;
                plng = lnge5;

                encodedPoints.append(encodeSignedNumber(dlat));
                encodedPoints.append(encodeSignedNumber(dlng));

            }
        }

        return encodedPoints.toString();
    }
    
    /**
     *
     * @param nodeIds
     * @return
     */
    public static String createIsochroneEncodings(IntArrayList nodeIds) {
        int arraySize = nodeIds.size();
        StringBuilder encodedPoints = new StringBuilder(3 * arraySize);
        
        int plat = 0;
        int plng = 0;
        int late5;
        int lnge5;
        int dlat;
        int dlng;
        
        Node currNode;
        for (int i = 0; i < arraySize; i++) {
            currNode = TrafficStore.nodes[nodeIds.getQuick(i)];
            late5 = floor1e5(currNode.lat);
            lnge5 = floor1e5(currNode.lng);
            dlat = late5 - plat;
            dlng = lnge5 - plng;
            encodedPoints.append(encodeSignedNumber(dlat)).append(encodeSignedNumber(dlng));
            plat = late5;
            plng = lnge5;
        }
        
        return encodedPoints.toString();
    }


    /**
     * Douglas-Peucker algorithm, adapted for encoding
     *
     * @param nodeIds
     * @return
     */
    public static String createDPIsochroneEncodings(IntArrayList nodeIds) {
        int i, maxLoc = 0;
        Stack<int[]> stack = new Stack<int[]>();
        int arraySize = nodeIds.size();
        double[] dists = new double[arraySize];
        double maxDist, absMaxDist = 0.0, temp = 0.0;
        int[] current;
        String encodedPoints;
        Node nodei, nodec0, nodec1;

        if (arraySize > 2) {
            int[] stackVal = new int[]{0, (arraySize - 1)};
            stack.push(stackVal);

            while (stack.size() > 0) {
                current = stack.pop();
                maxDist = 0;

                for (i = current[0] + 1; i < current[1]; i++) {
                    nodei = TrafficStore.nodes[nodeIds.getQuick(i)];
                    nodec0 = TrafficStore.nodes[nodeIds.getQuick(current[0])];
                    nodec1 = TrafficStore.nodes[nodeIds.getQuick(current[1])];
                    temp = distance(new Point(nodei.lat, nodei.lng),
                                    new Point(nodec0.lat, nodec0.lng),
                                    new Point(nodec1.lat, nodec1.lng));
                    if (temp > maxDist) {
                        maxDist = temp;
                        maxLoc = i;
                        if (maxDist > absMaxDist) {
                            absMaxDist = maxDist;
                        }
                    }
                }
                if (maxDist > verySmall) {
                    dists[maxLoc] = maxDist;
                    int[] stackValCurMax = {current[0], maxLoc};
                    stack.push(stackValCurMax);
                    int[] stackValMaxCur = {maxLoc, current[1]};
                    stack.push(stackValMaxCur);
                }
            }
        }

        encodedPoints = createEncodings(nodeIds, dists);
//        encodedPoints = replace(encodedPoints, "\\", "\\\\");

        return encodedPoints;
    }

    
    /**
     *
     * @param finalDnode
     * @return
     */
    public static String createRoutingEncodings(RoutingDijkstraNode finalDnode) {
        StringBuilder encodedPoints = new StringBuilder(1500);

        int plat = 0;
        int plng = 0;
        int late5;
        int lnge5;
        int dlat;
        int dlng;
        
        RoutingDijkstraNode currNode = finalDnode;
        
        while (currNode != null) {
            
            if (currNode.edgeFromParent != null) {
                for (int i = currNode.edgeFromParent.linestring.size() - 1; i >= 0; i--) {
                    
                    late5 = floor1e5(currNode.edgeFromParent.linestring.get(i).lat);
                    lnge5 = floor1e5(currNode.edgeFromParent.linestring.get(i).lng);
                    dlat = late5 - plat;
                    dlng = lnge5 - plng;
                    if (dlat == 0 && dlng == 0) {
                        continue;
                    }
                    encodedPoints.append(encodeSignedNumber(dlat)).append(encodeSignedNumber(dlng));
                    plat = late5;
                    plng = lnge5;
                }
            }
            
            currNode = currNode.parent;
        }
        
        return encodedPoints.toString();
    }

    
    /**
     *
     * @param finalALTnode
     * @return
     */
    public static String createRoutingEncodings(RoutingALTNode finalALTnode) {
        StringBuilder encodedPoints = new StringBuilder(1500);

        int plat = 0;
        int plng = 0;
        int late5;
        int lnge5;
        int dlat;
        int dlng;
        
        RoutingALTNode currNode = finalALTnode;
        
        while (currNode != null) {
            
            if (currNode.edgeFromParent != null) {
                for (int i = currNode.edgeFromParent.linestring.size() - 1; i >= 0; i--) {
                    
                    late5 = floor1e5(currNode.edgeFromParent.linestring.get(i).lat);
                    lnge5 = floor1e5(currNode.edgeFromParent.linestring.get(i).lng);
                    dlat = late5 - plat;
                    dlng = lnge5 - plng;
                    if (dlat == 0 && dlng == 0) {
                        continue;
                    }
                    encodedPoints.append(encodeSignedNumber(dlat)).append(encodeSignedNumber(dlng));
                    plat = late5;
                    plng = lnge5;
                }
            }
            
            currNode = currNode.parent;
        }
        
        return encodedPoints.toString();
    }

    /**
     *
     * @param track
     * @param level
     * @param step
     * @return
     */
    public static HashMap createEncodings(ArrayList<Point> trackpointList, int level, int step) {
        
        int listSize = trackpointList.size();
        
        HashMap<String,String> resultMap = new HashMap<String,String>(1);
        StringBuilder encodedPoints = new StringBuilder(listSize * 2);
        
        int plat = 0;
        int plng = 0;
        
        Point trackpoint;
        int late5;
        int lnge5;
        int dlat;
        int dlng;
        
        for (int i = 0; i < listSize; i += step) {
            trackpoint = trackpointList.get(i);
            
            late5 = floor1e5(trackpoint.lat);
            lnge5 = floor1e5(trackpoint.lng);
            
            dlat = late5 - plat;
            dlng = lnge5 - plng;
            
            encodedPoints.append(encodeSignedNumber(dlat)).append(
            encodeSignedNumber(dlng));
            plat = late5;
            plng = lnge5;
            
        }
        
        resultMap.put("encodedPoints", encodedPoints.toString());
        
        return resultMap;
    }
    
    // by geekyblogger
    public static ArrayList decode(String encoded) {
        
        ArrayList poly = new ArrayList();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            Point p = new Point((((double) lat / 1E5)), (((double) lng / 1E5)));
            poly.add(p);
            
        }
        
        return poly;
        
    }
    
}
