package processingserver;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Random;

/**
 *
 * @author nicolaus
 */
public class Landmarks {

    /** The number of the landmarks */
    public static final int nofLandmarks = 32;
    /** The to & from distance arrays of the landmarks */
    public static int landmarkDistances[][];
    /** The ids of the nodes that represent landmarks */
    public static int[] landmarkIds = new int[nofLandmarks];

    /**
     * Initializes landmark distances to -1. Happens only once at server startup.
     * @param nofNodes 
     */
    public static void initLandmarks(int nofNodes) {
        landmarkDistances = new int[nofNodes * 2][nofLandmarks];
        for (int l = 0; l < 2 * nofNodes; l++) {
            for (int k = 0; k < nofLandmarks; k++) {
                landmarkDistances[l][k] = -1;
            }
        }
    }

    /**
     * Method that checks if landmarks are calculated correctly
     *
     * @param nofNodes
     * @return
     */
    public static boolean checkLandmarks(int nofNodes) {
//        int countDisconnected;
//
//        for (int k = 0; k < nofLandmarks; k++) {
//            countDisconnected = 0;
//            for (int l = 0; l < 2 * nofNodes; l++) {
//                if (landmarksDistances[k][l] == -1) {
//                    countDisconnected++;
//                }
//            }
//            System.out.println("Landmark " + k + ": " + (countDisconnected / 2) + " disconnected nodes");
//        }
        int countDisconnected;

        for (int k = 0; k < nofLandmarks; k++) {
            countDisconnected = 0;
            for (int l = 0; l < 2 * nofNodes; l++) {
                if (landmarkDistances[l][k] == -1) {
                    countDisconnected++;
                }
            }
            System.out.println("Landmark " + k + ":\t" + (countDisconnected / 2) + " disconnected nodes");
        }

        return true;

    }

    /**
     * @deprecated @param nodeId
     * @param nodeId
     * @param distGoalNodeId
     * @param distGoalNodeIdReverse
     * @param landmarkIndex
     * @return
     */
    public static int getOptimizedBoundsPerLandmark(int nodeId, int distGoalNodeId,
            int distGoalNodeIdReverse, int landmarkIndex) {
        /* assuming nodeId starts with zero */
        int distNodeId = landmarkDistances[landmarkIndex][2 * nodeId];
        int distNodeIdReverse = landmarkDistances[landmarkIndex][2 * nodeId + 1];

        int pplus = distGoalNodeId - distNodeId;
        int pminus = distNodeIdReverse - distGoalNodeIdReverse;
        return Math.max(pplus, pminus);

    }

    /**
     *
     * @param nodeId
     * @param goalNodeId
     * @param landmarkIndex
     * @return
     */
    public static int getBoundsPerLandmark(int nodeId, int goalNodeId, int landmarkIndex) {
        /* assuming nodeId starts with zero */
//        int distNodeId = landmarksDistances[landmarkIndex][2 * nodeId];
//        int distNodeIdReverse = landmarksDistances[landmarkIndex][2 * nodeId + 1];
//
//        int distGoalNodeId = landmarksDistances[landmarkIndex][2 * goalNodeId];
//        int distGoalNodeIdReverse = landmarksDistances[landmarkIndex][2 * goalNodeId + 1];
//
//        int pplus = distGoalNodeId - distNodeId;
//        int pminus = distNodeIdReverse - distGoalNodeIdReverse;
//        return Math.max(pplus, pminus);

        /* assuming nodeId starts with zero */
        int distNodeId = landmarkDistances[2 * nodeId][landmarkIndex];
        int distNodeIdReverse = landmarkDistances[2 * nodeId + 1][landmarkIndex];

        int distGoalNodeId = landmarkDistances[2 * goalNodeId][landmarkIndex];
        int distGoalNodeIdReverse = landmarkDistances[2 * goalNodeId + 1][landmarkIndex];

        int pplus = distGoalNodeId - distNodeId;
        int pminus = distNodeIdReverse - distGoalNodeIdReverse;
        return Math.max(pplus, pminus);

    }

    /**
     *
     * @param nodeId
     * @param goalNodeId
     * @param landmarkIndex
     * @return
     */
    public static int getUpperBoundsPerLandmark(int nodeId, int goalNodeId, int landmarkIndex) {
        /* assuming nodeId starts with zero */
//        int distNodeId = landmarksDistances[landmarkIndex][2 * nodeId];
//        int distNodeIdReverse = landmarksDistances[landmarkIndex][2 * nodeId + 1];
//
//        int distGoalNodeId = landmarksDistances[landmarkIndex][2 * goalNodeId];
//        int distGoalNodeIdReverse = landmarksDistances[landmarkIndex][2 * goalNodeId + 1];
//
//        int pplus = distGoalNodeId - distNodeId;
//        int pminus = distNodeIdReverse - distGoalNodeIdReverse;
//        return Math.max(pplus, pminus);

        /* assuming nodeId starts with zero */
        int distNodeId = landmarkDistances[2 * nodeId][landmarkIndex];
        int distNodeIdReverse = landmarkDistances[2 * nodeId + 1][landmarkIndex];

        int distGoalNodeId = landmarkDistances[2 * goalNodeId][landmarkIndex];
        int distGoalNodeIdReverse = landmarkDistances[2 * goalNodeId + 1][landmarkIndex];

//        int pplus = distGoalNodeId + distNodeId;
//        int pminus = distNodeIdReverse + distGoalNodeIdReverse;
//        return Math.max(pplus, pminus);
        return distNodeIdReverse + distGoalNodeId;

    }

    /**
     * @deprecated Method that returns the best landmark and the corresponding
     * lowerBound for startNodeId and goalNodeId
     *
     * @param startNodeId
     * @param goalNodeId
     * @return
     */
    public static int[] getBestBoundAllLandmark(int startNodeId, int goalNodeId) {
        int result[] = new int[4];
        result[0] = -1;
        result[1] = -1;
        result[2] = -1;
        result[3] = -1;
        int landmarkBound;

        for (int k = 0; k < nofLandmarks; k++) {
            landmarkBound = getBoundsPerLandmark(startNodeId, goalNodeId, k);
            if (landmarkBound > result[1]) {
                result[0] = k;
                result[1] = landmarkBound;
                result[2] = landmarkDistances[k][2 * goalNodeId];
                result[3] = landmarkDistances[k][2 * goalNodeId + 1];
            }
        }
        return result;
    }

    /**
     * Method that returns the best landmark and the corresponding lowerBound
     * for startNodeId and goalNodeId
     *
     * @param nodeId
     * @param goalNodeId
     * @return
     */
    public static int getBestBoundAllLandmarks(int nodeId, int goalNodeId) {
        int result = 0;
        int landmarkBound;

        for (int k = 0; k < nofLandmarks; k++) {
            landmarkBound = getBoundsPerLandmark(nodeId, goalNodeId, k);
            if (landmarkBound > result) {
                result = landmarkBound;
            }
        }
        return result;
    }
    
    /**
     * Method that returns the best landmark and the corresponding lowerBound
     * for startNodeId and all end Nodes. NEEDS CORRECTION
     * 
     * @param nodeId
     * @param endNodes
     * @return 
     */
    public static int getBestBoundAllLandmarks(int nodeId, HashSet<Integer> endNodes) {
        int nodeResult = 0;
        int totalResult = Integer.MAX_VALUE;
        int landmarkBound;
        
        for (int endNodeId: endNodes) {
            for (int k = 0; k < nofLandmarks; k++) {
                landmarkBound = getBoundsPerLandmark(nodeId, endNodeId, k);
                if (landmarkBound > nodeResult) {
                    nodeResult = landmarkBound;
                }
            }
            if (nodeResult < totalResult) {
                totalResult = nodeResult;
            }
        }
        
        return totalResult;
    }
    
    /**
     * 
     * @param snodeDistances
     * @param goalNodeId
     * @return 
     */
    public static int getBestBoundAllLandmarksGivenStartNode(int[] snodeDistances, int goalNodeId) {
        int result = 0;
        int landmarkBound;

        for (int k = 0; k < nofLandmarks; k++) {
            landmarkBound = getBoundsPerLandmarkGivenStartNode(snodeDistances[2*k], snodeDistances[2*k+1], goalNodeId, k);
            if (landmarkBound > result) {
                result = landmarkBound;
            }
        }
        return result;
    }
    
    /**
     * 
     * @param forwardDistance
     * @param reverseDistance
     * @param goalNodeId
     * @param landmarkIndex
     * @return 
     */
    public static int getBoundsPerLandmarkGivenStartNode(int forwardDistance, int reverseDistance, int goalNodeId, int landmarkIndex) {
        /* assuming nodeId starts with zero */
        int distNodeId = forwardDistance;
        int distNodeIdReverse = reverseDistance;

        int distGoalNodeId = landmarkDistances[2 * goalNodeId][landmarkIndex];
        int distGoalNodeIdReverse = landmarkDistances[2 * goalNodeId + 1][landmarkIndex];

        int pplus = distGoalNodeId - distNodeId;
        int pminus = distNodeIdReverse - distGoalNodeIdReverse;
        return Math.max(pplus, pminus);
    }

    /**
     * Method that returns the best landmark and the corresponding lowerBound
     * for startNodeId and goalNodeId
     *
     * @param nodeId
     * @param goalNodeId
     * @return
     */
    public static int getBestUpperBoundAllLandmarks(int nodeId, int goalNodeId) {
        int result = Integer.MAX_VALUE;
        int landmarkBound;

        for (int k = 0; k < nofLandmarks; k++) {
            landmarkBound = getUpperBoundsPerLandmark(nodeId, goalNodeId, k);
            if (landmarkBound < result) {
                result = landmarkBound;
            }
        }
        return result;
    }
    
    public static int getBestUpperBoundAllLandmarksGivenStartNode(int[] snodeDistances, int goalNodeId) {
        int result = Integer.MAX_VALUE;
        int landmarkBound;

        for (int k = 0; k < nofLandmarks; k++) {
            landmarkBound = getUpperBoundsPerLandmarkGivenStartNode(snodeDistances[2*k], snodeDistances[2*k+1], goalNodeId, k);
            if (landmarkBound < result) {
                result = landmarkBound;
            }
        }
        return result;
    }
    
    public static int getUpperBoundsPerLandmarkGivenStartNode(int forwardDistance, int reverseDistance, int goalNodeId, int landmarkIndex) {
        /* assuming nodeId starts with zero */
//        int distNodeId = forwardDistance;
        int distNodeIdReverse = reverseDistance;

        int distGoalNodeId = landmarkDistances[2 * goalNodeId][landmarkIndex];
//        int distGoalNodeIdReverse = landmarkDistances[2 * goalNodeId + 1][landmarkIndex];

//        int pplus = distGoalNodeId + distNodeId;
//        int pminus = distNodeIdReverse + distGoalNodeIdReverse;
//        return Math.max(pplus, pminus);
        return distNodeIdReverse + distGoalNodeId;

    }

    /**
     * Method that returns the best landmark and the corresponding lowerBound
     * for startNodeId and goalNodeId
     *
     * @param nodeId
     * @param goalNodeId
     * @param activeLandmarks
     * @param nofActiveLandmarks
     * @return
     */
    public static int getBestBoundAllLandmarks(int nodeId, int goalNodeId, boolean[] activeLandmarks, int nofActiveLandmarks) {
        int result = 0;
        int landmarkBound;
        int seq = 0;

//        for (int k = 0; k < nofLandmarks; k++) {
        for (int k = 0; k < nofLandmarks; k++) {
            if (seq == nofActiveLandmarks) {
                break;
            }
            if (!activeLandmarks[k]) {
                continue;
            }
            landmarkBound = getBoundsPerLandmark(nodeId, goalNodeId, k);
            seq++;
            if (landmarkBound > result) {
                result = landmarkBound;

            }
        }
        return result;
    }

    /**
     * Method that returns the best landmarks for a particular search
     *
     * @param startNodeId
     * @param goalNodeId
     * @param nofActiveLandmarks
     * @return
     */
    public static void getBestLandmarks(int startNodeId, int goalNodeId, int nofActiveLandmarks, boolean[] activeLandmarks) {
        PriorityQueue<DijkstraNode> pqueue = new PriorityQueue<DijkstraNode>(nofLandmarks);

        int landmarkBound;
//        int result = 0;

        for (int k = 0; k < nofLandmarks; k++) {
            landmarkBound = getBoundsPerLandmark(startNodeId, goalNodeId, k);
            /* We add -cost, so that larger cost polls first */
            pqueue.add(new DijkstraNode(k, -landmarkBound));
//            if (landmarkBound > result) {
//                result = landmarkBound;
//            }
        }


//        for (int k = 0; k < nofLandmarks; k++) {
//            landmarkBound = getBoundsPerLandmark(goalNodeId, startNodeId, k);
//            /* We add -cost, so that larger cost polls first */
//            pqueue.add(new DijkstraNode(k, -landmarkBound));
////            if (landmarkBound > result) {
////                result = landmarkBound;
////            }
//        }



        DijkstraNode n;
        for (int k = 0; k < nofActiveLandmarks; k++) {
            n = pqueue.poll();
            activeLandmarks[n.id] = true;
//            System.out.println("Landmark "+ n.id +" with cost: " + (-n.cost));
        }

//        for (int k = 0; k < nofActiveLandmarks; k++) {
//            System.out.println("Landmark "+ activeLandmarks[k]);
//        }



    }

    /**
     * Method that copies landmarks preprocessing for permanent storage
     *
     * @param nodeCost
     * @param nofNodes
     * @param landmarkIndex
     * @param isReverseGraph
     */
    public static void copyNodeCostToLandmarks(int nodeCost[], int nofNodes, int landmarkIndex, boolean isReverseGraph) {
//        for (int l = 0; l < nofNodes; l++) {
//            if (!isReverseGraph) {
//                if (nodeCost[l] < Integer.MAX_VALUE) {
//                    landmarksDistances[landmarkIndex][2 * l] = nodeCost[l];
//                }
//
//            } else {
//                if (nodeCost[l] < Integer.MAX_VALUE) {
//                    landmarksDistances[landmarkIndex][2 * l + 1] = nodeCost[l];
//                }
//            }
//        }
        for (int l = 0; l < nofNodes; l++) {
            if (!isReverseGraph) {
                if (nodeCost[l] < Integer.MAX_VALUE) {
                    landmarkDistances[2 * l][landmarkIndex] = nodeCost[l];
                }

            } else {
                if (nodeCost[l] < Integer.MAX_VALUE) {
                    landmarkDistances[2 * l + 1][landmarkIndex] = nodeCost[l];
                }
            }
        }
    }

    /**
     * Getting start node - goal node pairs
     *
     * @param numberOfPairs
     * @param numberOfNodes
     * @return
     */
    public static int[][] getRandomNodePairs(int numberOfPairs, int numberOfNodes) {

        int[][] pair = new int[numberOfPairs][2];

        Random rndNumbers = new Random(184);
        for (int k = 0; k < numberOfPairs; k++) {
            pair[k][0] = rndNumbers.nextInt(numberOfNodes);
            pair[k][1] = rndNumbers.nextInt(numberOfNodes);
        }

        return pair;


    }
}
