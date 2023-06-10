import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This class provides a shortestPath method for finding routes between two points
 * on the map. Start by using Dijkstra's, and if your code isn't fast enough for your
 * satisfaction (or the autograder), upgrade your implementation by switching it to A*.
 * Your code will probably not be fast enough to pass the autograder unless you use A*.
 * The difference between A* and Dijkstra's is only a couple of lines of code, and boils
 * down to the priority you use to order your vertices.
 */
public class Router {
    /**
     * Return a List of longs representing the shortest path from the node
     * closest to a start location and the node closest to the destination
     * location.
     * @param g The graph to use.
     * @param stlon The longitude of the start location.
     * @param stlat The latitude of the start location.
     * @param destlon The longitude of the destination location.
     * @param destlat The latitude of the destination location.
     * @return A list of node id's in the order visited on the shortest path.
     *
     * 0. compute the heuristic using distance (already done)
     * 1. find the closest node of start point and the destination
     * 2. create distTo information "list"
     * 3. create edgeTo information "list"
     * 4. insert all the vertices into PQ in order of distTo + heuristic (now all are infinite, except for the start) ???
     * 5. remove the best vertex v from PQ and relax all the vertices iteratively until the target is reached
     */


    public static List<Long> shortestPath(GraphDB g, double stlon, double stlat,
                                          double destlon, double destlat) {
        /* find the closest node of the start and the destination */
        long startID = g.closest(stlon, stlat);
        long destID = g.closest(destlon, destlat);

        /* create distTo information "list" */
        HashMap<Long, Double> distTo = new HashMap<>();
        for (long v : g.vertices()) {
            distTo.put(v, Double.MAX_VALUE);
        }

        /* set the distTo of the start to 0 */
        distTo.put(startID, (double) 0);

        /* create edgeTo information "list" */
        HashMap<Long, Long> edgeTo = new HashMap<>();

        /* set the edgeTo of the start to itself */
        edgeTo.put(startID, startID);

        /* create marked information "list" */
        HashMap<Long, Boolean> marked = new HashMap<>();
        for (long v : g.vertices()) {
            marked.put(v, false);
        }

        /* create a PQ in order of distTo + heuristic and insert all the vertices into the PQ */
        PriorityQueue<EntryFringe> fringe = new PriorityQueue<>();
        EntryFringe start = new EntryFringe(startID, distTo.get(startID), g.distance(stlon, stlat, destlon, destlat));
        fringe.add(start);

        while (!fringe.isEmpty()) {
            EntryFringe curr = fringe.poll();
            if (marked.get(curr.id)) {
                continue;
            }
            if (curr.id == destID) {
                break;
            }
            marked.put(curr.id, true);
            for (long v : g.adjacent(curr.id)) {
                if (distTo.get(v) > distTo.get(curr.id) + g.distance(curr.id, v)) {
                    double newDistToV = distTo.get(curr.id) + g.distance(curr.id, v);
                    EntryFringe entry = new EntryFringe(v, newDistToV, g.distance(v, destID));

                    distTo.put(v, newDistToV);

                    fringe.add(entry);
                    edgeTo.put(v, curr.id);
                }
            }
        }

        /* create the list for return */
        LinkedList<Long> route = new LinkedList<>();
        long v = destID;
        while (v != startID) {
            if (edgeTo.get(v) == null) {
                return new LinkedList<>();
            }
            route.addFirst(v);
            v = edgeTo.get(v);
        }
        route.addFirst(startID);

        return route;
    }

    private static class EntryFringe implements Comparable<EntryFringe> {
        private long id;
        private double value;
        public EntryFringe(long id, double distTo, double heuristic) {
            this.id = id;
            this.value = distTo + heuristic;
        }

        @Override
        public int compareTo(EntryFringe o) {
            return Double.compare(this.value, o.value);
        }
    }

    /**
     * Create the list of directions corresponding to a route on the graph.
     * @param g The graph to use.
     * @param route The route to translate into directions. Each element
     *              corresponds to a node from the graph in the route.
     * @return A list of NavigatiionDirection objects corresponding to the input
     * route.
     *
     * 1. dequeue List one by one
     * 2. calculate the direction, extract way, and calculate the distance
     * 3. create the NavigationDirection object
     */
    public static List<NavigationDirection> routeDirections(GraphDB g, List<Long> route) {
        List<NavigationDirection> res = new ArrayList<>();

        NavigationDirection cur = new NavigationDirection();
        cur.direction = NavigationDirection.START;
        cur.way = getWayName(g, route.get(0), route.get(1));
        cur.distance += g.distance(route.get(0), route.get(1));

        for (int i = 1, j = 2; j < route.size(); i++, j++) {
            if (!getWayName(g, route.get(i), route.get(j)).equals(cur.way)) {
                res.add(cur);
                cur = new NavigationDirection();
                cur.way = getWayName(g, route.get(i), route.get(j));

                double prevBearing = g.bearing(route.get(i - 1), route.get(i));
                double curBearing = g.bearing(route.get(i), route.get(j));
                cur.direction = convertBearingToDirection(prevBearing, curBearing);

                cur.distance += g.distance(route.get(i), route.get(j));
                continue;
            }
            cur.distance += g.distance(route.get(i), route.get(j));
        }
        res.add(cur);
        return res;
//        long startID = route.remove(0);
//        long nodeID = startID;
//        boolean isStart = true;
//        int direction = 0;
//        double distance = 0;
//        while (!route.isEmpty()) {
//            long endID = route.remove(0);
//            String way = "unknown road";
//            int newDirection = getDirection(g, nodeID, endID, isStart);
//            isStart = false;
//            if (direction == newDirection) {
//                nodeID = endID;
//                continue;
//            } else {
//                distance = g.distance(startID, nodeID);
//                HashSet<Long> startNodeWay = g.getNodeWay(startID);
//                HashSet<Long> nodeWay = g.getNodeWay(nodeID);
//                for (long wayID : startNodeWay) {
//                    if (nodeWay.contains(wayID)) {
//                        way = g.getWayName(wayID);
//                    }
//                }
//                direction = newDirection;
//                startID = nodeID;
//            }
    }

    private static int convertBearingToDirection(double prevBearing, double curBearing) {
        double relativeBearing = curBearing - prevBearing;
        if (relativeBearing > 180) {
            relativeBearing -= 360;
        } else if (relativeBearing < -180) {
            relativeBearing += 360;
        }

        if (relativeBearing < -100) {
            return NavigationDirection.SHARP_LEFT;
        } else if (relativeBearing < -30) {
            return NavigationDirection.LEFT;
        } else if (relativeBearing < -15) {
            return NavigationDirection.SLIGHT_LEFT;
        } else if (relativeBearing < 15) {
            return NavigationDirection.STRAIGHT;
        } else if (relativeBearing < 30) {
            return NavigationDirection.SLIGHT_RIGHT;
        } else if (relativeBearing < 100) {
            return NavigationDirection.RIGHT;
        } else {
            return NavigationDirection.SHARP_RIGHT;
        }
    }

    private static String getWayName(GraphDB g, long node1, long node2) {
        String noName = "";
        HashSet<Long> nodeWay1 = g.getNodeWay(node1);
        HashSet<Long> nodeWay2 = g.getNodeWay(node2);
        List<Long> intersection =
                nodeWay1.stream().filter(nodeWay2::contains).collect(Collectors.toList());

        if (!intersection.isEmpty()) {
            if (g.getWayName(intersection.get(0)) == null) {
                return noName;
            } else {
                return g.getWayName(intersection.get(0));
            }
        }

        return noName;
    }


//    private static int getDirection(GraphDB g, long startID, long nodeID, boolean isStart) {
//        if (isStart) {
//            return 0;
//        }
//        double direction = g.bearing(startID, nodeID);
//        if (-15 <= direction && direction <= 15) {
//            return 1;
//        }
//        if (-30 <= direction && direction < -15) {
//            return 2;
//        }
//        if (15 < direction && direction <= 30) {
//            return 3;
//        }
//        if (30 < direction && direction <= 100) {
//            return 4;
//        }
//        if (-100 <= direction && direction < -30) {
//            return 5;
//        }
//        if (-180 < direction && direction < -100) {
//            return 6;
//        }
//        if (100 < direction && direction < 180) {
//            return 7;
//        } else {
//            throw new RuntimeException("error!");
//        }
//    }

    /**
     * Class to represent a navigation direction, which consists of 3 attributes:
     * a direction to go, a way, and the distance to travel for.
     */
    public static class NavigationDirection {

        /** Integer constants representing directions. */
        public static final int START = 0;
        public static final int STRAIGHT = 1;
        public static final int SLIGHT_LEFT = 2;
        public static final int SLIGHT_RIGHT = 3;
        public static final int RIGHT = 4;
        public static final int LEFT = 5;
        public static final int SHARP_LEFT = 6;
        public static final int SHARP_RIGHT = 7;

        /** Number of directions supported. */
        public static final int NUM_DIRECTIONS = 8;

        /** A mapping of integer values to directions.*/
        public static final String[] DIRECTIONS = new String[NUM_DIRECTIONS];

        /** Default name for an unknown way. */
        public static final String UNKNOWN_ROAD = "unknown road";
        
        /** Static initializer. */
        static {
            DIRECTIONS[START] = "Start";
            DIRECTIONS[STRAIGHT] = "Go straight";
            DIRECTIONS[SLIGHT_LEFT] = "Slight left";
            DIRECTIONS[SLIGHT_RIGHT] = "Slight right";
            DIRECTIONS[LEFT] = "Turn left";
            DIRECTIONS[RIGHT] = "Turn right";
            DIRECTIONS[SHARP_LEFT] = "Sharp left";
            DIRECTIONS[SHARP_RIGHT] = "Sharp right";
        }

        /** The direction a given NavigationDirection represents.*/
        int direction;
        /** The name of the way I represent. */
        String way;
        /** The distance along this way I represent. */
        double distance;

        /**
         * Create a default, anonymous NavigationDirection.
         */
        public NavigationDirection() {
            this.direction = STRAIGHT;
            this.way = UNKNOWN_ROAD;
            this.distance = 0.0;
        }

        public String toString() {
            return String.format("%s on %s and continue for %.3f miles.",
                    DIRECTIONS[direction], way, distance);
        }

        /**
         * Takes the string representation of a navigation direction and converts it into
         * a Navigation Direction object.
         * @param dirAsString The string representation of the NavigationDirection.
         * @return A NavigationDirection object representing the input string.
         */
        public static NavigationDirection fromString(String dirAsString) {
            String regex = "([a-zA-Z\\s]+) on ([\\w\\s]*) and continue for ([0-9\\.]+) miles\\.";
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(dirAsString);
            NavigationDirection nd = new NavigationDirection();
            if (m.matches()) {
                String direction = m.group(1);
                if (direction.equals("Start")) {
                    nd.direction = NavigationDirection.START;
                } else if (direction.equals("Go straight")) {
                    nd.direction = NavigationDirection.STRAIGHT;
                } else if (direction.equals("Slight left")) {
                    nd.direction = NavigationDirection.SLIGHT_LEFT;
                } else if (direction.equals("Slight right")) {
                    nd.direction = NavigationDirection.SLIGHT_RIGHT;
                } else if (direction.equals("Turn right")) {
                    nd.direction = NavigationDirection.RIGHT;
                } else if (direction.equals("Turn left")) {
                    nd.direction = NavigationDirection.LEFT;
                } else if (direction.equals("Sharp left")) {
                    nd.direction = NavigationDirection.SHARP_LEFT;
                } else if (direction.equals("Sharp right")) {
                    nd.direction = NavigationDirection.SHARP_RIGHT;
                } else {
                    return null;
                }

                nd.way = m.group(2);
                try {
                    nd.distance = Double.parseDouble(m.group(3));
                } catch (NumberFormatException e) {
                    return null;
                }
                return nd;
            } else {
                // not a valid nd
                return null;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof NavigationDirection) {
                return direction == ((NavigationDirection) o).direction
                    && way.equals(((NavigationDirection) o).way)
                    && distance == ((NavigationDirection) o).distance;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(direction, way, distance);
        }
    }
}
