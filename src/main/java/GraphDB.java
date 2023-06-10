import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.*;

/**
 * Graph for storing all of the intersection (vertex) and road (edge) information.
 * Uses your GraphBuildingHandler to convert the XML files into a graph. Your
 * code must include the vertices, adjacent, distance, closest, lat, and lon
 * methods. You'll also need to include instance variables and methods for
 * modifying the graph (e.g. addNode and addEdge).
 *
 * @author Alan Yao, Josh Hug
 */
public class GraphDB {
    /** Your instance variables for storing the graph. You should consider
     * creating helper classes, e.g. Node, Edge, etc. */
    private HashMap<Long, Node> nodeMap = new HashMap<>();
    private HashMap<Long, Way> connectedNodeMap = new HashMap<>();

    /**
     * Example constructor shows how to create and start an XML parser.
     * You do not need to modify this constructor, but you're welcome to do so.
     * @param dbPath Path to the XML file to be parsed.
     */
    public GraphDB(String dbPath) {
        try {
            File inputFile = new File(dbPath);
            FileInputStream inputStream = new FileInputStream(inputFile);
            // GZIPInputStream stream = new GZIPInputStream(inputStream);

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            GraphBuildingHandler gbh = new GraphBuildingHandler(this);
            saxParser.parse(inputStream, gbh);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        clean();
    }

    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     * @param s Input string.
     * @return Cleaned string.
     */
    static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

    /**
     *  Remove nodes with no connections from the graph.
     *  While this does not guarantee that any two nodes in the remaining graph are connected,
     *  we can reasonably assume this since typically roads are connected.
     */
    private void clean() {
        // TODO: Your code here.
        ArrayList<Long> removeList = new ArrayList<>();
        for( Node node : nodeMap.values()) {
            if (node.adj.isEmpty()) {
                removeList.add(node.id);
            }
        }
//        System.out.println(nodeMap.size());
//        System.out.println(removeList.size());
        for (Long id : removeList) {
            nodeMap.remove(id);
        }
    }

    /**
     * Returns an iterable of all vertex IDs in the graph.
     * @return An iterable of id's of all vertices in the graph.
     */
    Iterable<Long> vertices() {
        //YOUR CODE HERE, this currently returns only an empty list.
        return nodeMap.keySet();
    }

    /**
     * Returns ids of all vertices adjacent to v.
     * @param v The id of the vertex we are looking adjacent to.
     * @return An iterable of the ids of the neighbors of v.
     */
    Iterable<Long> adjacent(long v) {
        return nodeMap.get(v).adj;
    }

    /**
     * Returns the great-circle distance between vertices v and w in miles.
     * Assumes the lon/lat methods are implemented properly.
     * <a href="https://www.movable-type.co.uk/scripts/latlong.html">Source</a>.
     * @param v The id of the first vertex.
     * @param w The id of the second vertex.
     * @return The great-circle distance between the two locations from the graph.
     */
    double distance(long v, long w) {
        return distance(lon(v), lat(v), lon(w), lat(w));
    }

    static double distance(double lonV, double latV, double lonW, double latW) {
        double phi1 = Math.toRadians(latV);
        double phi2 = Math.toRadians(latW);
        double dphi = Math.toRadians(latW - latV);
        double dlambda = Math.toRadians(lonW - lonV);

        double a = Math.sin(dphi / 2.0) * Math.sin(dphi / 2.0);
        a += Math.cos(phi1) * Math.cos(phi2) * Math.sin(dlambda / 2.0) * Math.sin(dlambda / 2.0);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 3963 * c;
    }

    /**
     * Returns the initial bearing (angle) between vertices v and w in degrees.
     * The initial bearing is the angle that, if followed in a straight line
     * along a great-circle arc from the starting point, would take you to the
     * end point.
     * Assumes the lon/lat methods are implemented properly.
     * <a href="https://www.movable-type.co.uk/scripts/latlong.html">Source</a>.
     * @param v The id of the first vertex.
     * @param w The id of the second vertex.
     * @return The initial bearing between the vertices.
     */
    double bearing(long v, long w) {
        return bearing(lon(v), lat(v), lon(w), lat(w));
    }

    static double bearing(double lonV, double latV, double lonW, double latW) {
        double phi1 = Math.toRadians(latV);
        double phi2 = Math.toRadians(latW);
        double lambda1 = Math.toRadians(lonV);
        double lambda2 = Math.toRadians(lonW);

        double y = Math.sin(lambda2 - lambda1) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2);
        x -= Math.sin(phi1) * Math.cos(phi2) * Math.cos(lambda2 - lambda1);
        return Math.toDegrees(Math.atan2(y, x));
    }

    /**
     * Returns the vertex closest to the given longitude and latitude.
     * @param lon The target longitude.
     * @param lat The target latitude.
     * @return The id of the node in the graph closest to the target.
     */
    long closest(double lon, double lat) {
        long closestNode = -1;
        double minDistance = Double.MAX_VALUE;
        for (Node node : nodeMap.values()) {
            double dis = distance(lon, lat, node.lon, node.lat);
            if (dis < minDistance) {
                minDistance = dis;
                closestNode = node.id;
            }
        }
        return closestNode;
    }

    /**
     * Gets the longitude of a vertex.
     * @param v The id of the vertex.
     * @return The longitude of the vertex.
     */
    double lon(long v) {
        return nodeMap.get(v).lon;
    }

    /**
     * Gets the latitude of a vertex.
     * @param v The id of the vertex.
     * @return The latitude of the vertex.
     */
    double lat(long v) {
        return nodeMap.get(v).lat;
    }

    /** create a graph-like subclass
     * 1. use hashmap to store node: key is the node id, value is the node object
     * 2. each node is an object with id, lon, lat variables and a hashset for adjacent node
     */
     static class Node {
        private long id;
        private double lon;
        private double lat;
        private String name;
        private HashSet<Long> connectedWay;
        private HashSet<Long> adj;

        public Node(long id, double lon, double lat) {
            this.id = id;
            this.lon = lon;
            this.lat = lat;
            this.connectedWay = new HashSet<>();
            this.adj = new HashSet<>();
        }
     }

    public void putNewNode(long id, double lon, double lat) {
        Node node = new Node(id, lon, lat);
        nodeMap.put(id, node);
    }
    public void setNodeName(long nodeID, String name) {
        this.nodeMap.get(nodeID).name = name;
    }
    public HashSet<Long> getNodeWay(long nodeID) {return nodeMap.get(nodeID).connectedWay;}

    /** create a subclass for storing the possible connections
     * 1. add the way the node belongs to into the connectedWay variable in Node object
     * 2. initialize a hashmap in connectedNodeMap (backup) with the way id
     * 3. add node id into the corresponding way's hashset
     */
    static class Way {
        private long id;
        private String maxSpeed;
        private boolean isValidWay;
        private String name;
        private ArrayList<Long> connectedNodes;

        public Way(long id) {
            this.id = id;
            connectedNodes = new ArrayList<>();
        }
    }

    public String getWayName(long wayID) {
        Way way = connectedNodeMap.get(wayID);
        if (way.name == null) {
            return "";
        }
        return way.name;
    }

    public void initNewWay(long wayID) {
        Way way = new Way(wayID);
        this.connectedNodeMap.put(wayID, way);
    }

    public void addConnectedNodeToBackup(long wayID, long nodeID) {
        connectedNodeMap.get(wayID).connectedNodes.add(nodeID);
    }

    public void addMaxSpeed(long wayID, String maxSpeed) {
        connectedNodeMap.get(wayID).maxSpeed = maxSpeed;
    }

    public void setFlag(long wayID, boolean b) {
        connectedNodeMap.get(wayID).isValidWay = b;
    }

    public void setWayName(long wayID, String name) {
        connectedNodeMap.get(wayID).name = name;
    }

    public boolean isValidWay(long wayID) {
        return connectedNodeMap.get(wayID).isValidWay;
    }
    public void addConnectedWayToNode(long wayID) {
        ArrayList<Long> nodeList = connectedNodeMap.get(wayID).connectedNodes;
        for (int i = 0; i < nodeList.size(); i++) {
            nodeMap.get(nodeList.get(i)).connectedWay.add(wayID);
            if (nodeList.size() == 1) {
                continue;
            } else if (i == 0) {
                nodeMap.get(nodeList.get(0)).adj.add(nodeList.get(1));
            } else if (i == nodeList.size() - 1) {
                nodeMap.get(nodeList.get(nodeList.size() - 1)).adj.add(nodeList.get(nodeList.size() - 2));
            } else {
                nodeMap.get(nodeList.get(i)).adj.add(nodeList.get(i - 1));
                nodeMap.get(nodeList.get(i)).adj.add(nodeList.get(i + 1));
            }
        }
    }
}
