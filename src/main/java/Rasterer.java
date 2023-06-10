import java.util.HashMap;
import java.util.Map;

/**
 * This class provides all code necessary to take a query box and produce
 * a query result. The getMapRaster method must return a Map containing all
 * seven of the required fields, otherwise the front end code will probably
 * not draw the output correctly.
 */
public class Rasterer {
    private static double ROOT_LRLON = -122.2119140625;
    private static double ROOT_ULLON = -122.2998046875;
    private static double ROOT_LRLAT = 37.82280243352756;
    private static double ROOT_ULLAT = 37.892195547244356;
    private String[][] render_grid;
    private double raster_ul_lon;
    private double raster_ul_lat;
    private double raster_lr_lon;
    private double raster_lr_lat;
    private int depth;
    private boolean query_success;

    public Rasterer() {
        // YOUR CODE HERE
        this.render_grid = null;
        this.raster_ul_lon = -1;
        this.raster_ul_lat = -1;
        this.raster_lr_lon = -1;
        this.raster_lr_lat = -1;
        this.depth = -1;
        this.query_success = false;
    }

    /**
     * Takes a user query and finds the grid of images that best matches the query. These
     * images will be combined into one big image (rastered) by the front end. <br>
     *
     *     The grid of images must obey the following properties, where image in the
     *     grid is referred to as a "tile".
     *     <ul>
     *         <li>The tiles collected must cover the most longitudinal distance per pixel
     *         (LonDPP) possible, while still covering less than or equal to the amount of
     *         longitudinal distance per pixel in the query box for the user viewport size. </li>
     *         <li>Contains all tiles that intersect the query bounding box that fulfill the
     *         above condition.</li>
     *         <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     *     </ul>
     *
     * @param params Map of the HTTP GET request's query parameters - the query box and
     *               the user viewport width and height.
     *
     * @return A map of results for the front end as specified: <br>
     * "render_grid"   : String[][], the files to display. <br>
     * "raster_ul_lon" : Number, the bounding upper left longitude of the rastered image. <br>
     * "raster_ul_lat" : Number, the bounding upper left latitude of the rastered image. <br>
     * "raster_lr_lon" : Number, the bounding lower right longitude of the rastered image. <br>
     * "raster_lr_lat" : Number, the bounding lower right latitude of the rastered image. <br>
     * "depth"         : Number, the depth of the nodes of the rastered image <br>
     * "query_success" : Boolean, whether the query was able to successfully complete; don't
     *                    forget to set this to true on success! <br>
     */
    public Map<String, Object> getMapRaster(Map<String, Double> params) {
        System.out.println(params);
        /* Use the params to initialize the Raster instance */
        rasterInit(params);
        /* Initialize an empty result map */
        Map<String, Object> results = new HashMap<>();
        /* put elements into result map with the parameters of Raster object */
        results.put("render_grid", render_grid);
        results.put("raster_ul_lon", raster_ul_lon);
        results.put("raster_ul_lat", raster_ul_lat);
        results.put("raster_lr_lon", raster_lr_lon);
        results.put("raster_lr_lat", raster_lr_lat);
        results.put("depth", depth);
        results.put("query_success", query_success);

        /*System.out.println("Since you haven't implemented getMapRaster, nothing is displayed in "
                           + "your browser.");*/
        return results;
    }

    /** Use the params to initialize the Raster instance */
    public void rasterInit(Map<String, Double> params) {
        depth = calDepth(params);
        raster_ul_lon = calRaster_ul_lon(params);
        raster_ul_lat = calRaster_ul_lat(params);
        raster_lr_lon = calRaster_lr_lon(params)[0];
        raster_lr_lat = calRaster_lr_lat(params)[0];
        render_grid = generateGrid(params);
        query_success = checkQuery(params);
    }

    /** helper function for rasterInit(...) to check the validation of query */
    private boolean checkQuery(Map<String, Double> params) {
        if (params.get("ullon") >= params.get("lrlon") || params.get("ullat") <= params.get("lrlat")) {
            return false;
        }
        if (params.get("ullon") < ROOT_ULLON && params.get("lrlon") < ROOT_ULLON) {
            return false;
        }
        if (params.get("ullon") > ROOT_LRLON && params.get("lrlon") > ROOT_LRLON) {
            return false;
        }
        if (params.get("ullat") > ROOT_ULLAT && params.get("lrlat") > ROOT_ULLAT) {
            return false;
        }
        if (params.get("ullat") < ROOT_LRLAT && params.get("lrlat") < ROOT_LRLAT) {
            return false;
        }
        return true;
    }

    /** helper function for rasterInit(...) to calculate depth
     * 1. need compute the LonDPP of the user's param (use helper function)
     * 2.  compare the LonDPP of each tile with the user's LonDPP
     * 3. if LonDPP <= user's LonDPP or depth reaches 7, then stop the iterative function
     * 4. return depth
     * */

    /** helper function to calculate tile width */
    private double calTileWidth(int depth) {
        double ROOT_WIDTH = ROOT_LRLON - ROOT_ULLON;
        return ROOT_WIDTH / Math.pow(2, depth);
    }

    private double calTileHeight(int depth) {
        double ROOT_HEIGHT = ROOT_ULLAT - ROOT_LRLAT;
        return ROOT_HEIGHT / Math.pow(2, depth);
    }

    /** helper function to calculate user's LonDPP */
    private double calUserLonDPP(Map<String, Double> params) {
        return (params.get("lrlon") - params.get("ullon")) / params.get("w");
    }

    /** helper function to calculate LonDPP of tiles */
    private double calTileLonDPP(int depth) {
        return calTileWidth(depth) / 256;
    }

    private int calDepth(Map<String, Double> params) {
        int depth = 0;
        double userLonDPP = calUserLonDPP(params);
        while (calTileLonDPP(depth) > userLonDPP) {
            depth++;
            if (depth == 7) {
                return 7;
            }
        }
        return depth;
    }

    /** helper function for rasterInit(...) to generate render_grid
     * 1. find x of the upperleft corner using depth
     * 2. find y of the upperleft corner using depth
     * 3. find the row's tile number of the query box
     * 4. generate grid
     * */

    private int getUlXNumber(Map<String, Double> params) {
        int depth = calDepth(params);
        double tileWidth = calTileWidth(depth);
        return (int) ((params.get("ullon") - ROOT_ULLON) / tileWidth);
    }

    private int getUlYNumber(Map<String, Double> params) {
        int depth = calDepth(params);
        double tileHeight = calTileHeight(depth);
        return (int) ((ROOT_ULLAT - params.get("ullat")) / tileHeight);
    }

    private double calRaster_ul_lon(Map<String, Double> params) {
        int x = getUlXNumber(params);
        int depth = calDepth(params);
        double tileWidth = calTileWidth(depth);
        return ROOT_ULLON + x * tileWidth;
    }

    private double calRaster_ul_lat(Map<String, Double> params) {
        int y = getUlYNumber(params);
        int depth = calDepth(params);
        double tileHeight = calTileHeight(depth);
        return ROOT_ULLAT - y * tileHeight;
    }

    private double[] calRaster_lr_lon(Map<String, Double> params) {
        double[] arr = new double[2];
        double tileWidth = calTileWidth(depth);
        int startX = getUlXNumber(params);
        int endX = startX;
        double lon = calRaster_ul_lon(params);
        while (params.get("lrlon") >= lon) {
            endX++;
            lon += tileWidth;
        }
        if (lon >= ROOT_LRLON) {
            lon = ROOT_LRLON;
            endX = (int) Math.pow(2, depth);
        }
        arr[0] = lon;
        arr[1] = endX;
        return arr;
    }

    private double[] calRaster_lr_lat(Map<String, Double> params) {
        double[] arr = new double[2];
        double tileHeight = calTileHeight(depth);
        int startY = getUlYNumber(params);
        int endY = startY;
        double lat = calRaster_ul_lat(params);
        while (params.get("lrlat") <= lat) {
            endY++;
            lat -= tileHeight;
        }
        if (lat <= ROOT_LRLAT) {
            lat = ROOT_LRLAT;
            endY = (int) Math.pow(2, depth);
        }
        arr[0] = lat;
        arr[1] = endY;
        return arr;
    }

    private String generateFilename(int depth, int x, int y) {
        return "d" + depth + "_x" + x + "_y" + y + ".png";
    }

    private String[][] generateGrid(Map<String, Double> params) {
        int startX = getUlXNumber(params);
        int startY = getUlYNumber(params);
        double[] arrX = calRaster_lr_lon(params);
        int endX = (int) arrX[1];
        double[] arrY = calRaster_lr_lat(params);
        int endY = (int) arrY[1];
        String[][] grid = new String[endY - startY][endX - startX];
        for (int y = 0; y < grid.length; y++) {
            for (int x = 0; x < grid[0].length; x++) {
                grid[y][x] = generateFilename(depth, startX + x, startY + y);
            }
        }
        return grid;
    }

//    public static void main(String[] args) {
//        Map<String, Double> testParams = new HashMap<>();
//        testParams.put("lrlon", -122.20908713544797);
//        testParams.put("ullon", -122.3027284165759);
//        testParams.put("w", 305.0);
//        testParams.put("h", 300.0);
//        testParams.put("ullat", 37.88708748276975);
//        testParams.put("lrlat", 37.848731523430196);
//        Rasterer test = new Rasterer();
//        test.rasterInit(testParams);
//        System.out.println(test.depth);
//        System.out.println(test.raster_ul_lat);
//        System.out.println(test.raster_ul_lon);
//        for (String[] i : test.render_grid) {
//            for (String j : i) {
//                System.out.println(j);
//            }
//        }
//        System.out.println(test.raster_lr_lat);
//        System.out.println(test.raster_lr_lon);
//        System.out.println(test.query_success);
//    }
}
