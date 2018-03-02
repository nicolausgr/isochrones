package processingserver;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.JSONObject;

/**
 *
 * @author nicolaus
 */
public class ProcessingServlet extends HttpServlet {
    
    // Network variables
    private int numberOfNodes;
    private int numberOfEdges;
    
    /**
     * The override init method for the servlet.
     * Runs on server startup, calls its super.init for configuration & then the initialization method below.
     * @param config
     * @throws ServletException 
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            initialization();
        } catch (ServletException ex) {
            Logger.getLogger(ProcessingServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     *
     * @throws ServletException
     */
    public void initialization() throws ServletException {
        
        //Connect to the TrafficStore
        Reporting.startTask("Connecting to the TrafficStore");
        try {
            TrafficStore.connect();
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            Logger.getLogger(ProcessingServlet.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        Reporting.endTaskAndStartAnother("Initializing queries");
        
        // Initialize Queries
        TrafficStore.initializeQueries();
        Reporting.endTaskAndStartAnother("Counting nodes");
        
        // Load road network
        numberOfNodes = TrafficStore.countNodes();
        Reporting.endTaskAndStartAnother("Counting edges");
        numberOfEdges = TrafficStore.countEdges();
        Reporting.endTaskAndStartAnother("Loading nodes");
        TrafficStore.loadNodes(numberOfNodes);
        Reporting.endTaskAndStartAnother("Loading edges");
        TrafficStore.loadEdges(numberOfEdges);
        Reporting.endTaskAndStartAnother("Creating coordinates text file");
        
        // R* tree indexing of nodes
        TrafficStore.createCoordinatesTxt("/tmp/nodes_indexing.txt");   // this is needed for the R* tree creation
        Reporting.endTaskAndStartAnother("Creating R* tree");
        RTree.createRTree(numberOfNodes);
        Reporting.endTaskAndStartAnother("Loading landmarks & calculating distances");
        
        // Fetch & initialize landmarks
        TrafficStore.loadLandmarks();
        Landmarks.initLandmarks(numberOfNodes);
        LandmarkDijkstra objDijkstra;
        for (int l = 0; l < Landmarks.nofLandmarks; l++) {
            objDijkstra = new LandmarkDijkstra(l, Landmarks.landmarkIds[l], false);
            objDijkstra.execute();
            objDijkstra = new LandmarkDijkstra(l, Landmarks.landmarkIds[l], true);
            objDijkstra.execute();
        }
        Landmarks.checkLandmarks(numberOfNodes);
        Reporting.endTask();
        
        Reporting.print(numberOfNodes + " nodes\n" + numberOfEdges + " edges");
        Runtime runtime = Runtime.getRuntime();
        Reporting.print("Allocated memory: " + (runtime.totalMemory() / 1024 / 1024) + "MB");
        
    }

    /**
     * Processes requests for both HTTP
     * <code>GET</code> and
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        
        JSONObject requestResponse = null;
        
        boolean validRequest = false;
        
        // Isochrones request
        if (request.getParameter("type").equals("isochrones") && request.getParameterMap().containsKey("max_tt")) {
            
            validRequest = true;
            
            boolean reverseQuery = Boolean.parseBoolean(request.getParameter("reverse"));
            double x = Double.parseDouble(request.getParameter("x"));
            double y = Double.parseDouble(request.getParameter("y"));
            int numberOfIsochrones = Integer.parseInt(request.getParameter("num"));
            int maxTT = Integer.parseInt(request.getParameter("max_tt"));
            
            // Check limits
            if (maxTT > 30*60*10 || numberOfIsochrones < 1 || numberOfIsochrones > 6) {
                validRequest = false;
            } else {
                IsochroneRequest isochroneRequest = new IsochroneRequest(reverseQuery, numberOfIsochrones, maxTT, x, y);
                requestResponse = isochroneRequest.compute();
                isochroneRequest = null;
            }
            
        }
        // Routing 2-point request
        else if (request.getParameter("type").equals("routing") && !request.getParameterMap().containsKey("x3")) {
            
            validRequest = true;
            
            double x1 = Double.parseDouble(request.getParameter("x1"));
            double y1 = Double.parseDouble(request.getParameter("y1"));
            double x2 = Double.parseDouble(request.getParameter("x2"));
            double y2 = Double.parseDouble(request.getParameter("y2"));
            
            RoutingRequest routingRequest = new RoutingRequest(false, x1, y1, x2, y2, -1.0, -1.0);
            requestResponse = routingRequest.compute();
            routingRequest = null;
            
        }
        // Routing 3-point request
        else if (request.getParameter("type").equals("routing") && request.getParameterMap().containsKey("x3")) {
            
            validRequest = true;
            
            double x1 = Double.parseDouble(request.getParameter("x1"));
            double y1 = Double.parseDouble(request.getParameter("y1"));
            double x2 = Double.parseDouble(request.getParameter("x2"));
            double y2 = Double.parseDouble(request.getParameter("y2"));
            double x3 = Double.parseDouble(request.getParameter("x3"));
            double y3 = Double.parseDouble(request.getParameter("y3"));
            
            RoutingRequest routingRequest = new RoutingRequest(true, x1, y1, x2, y2, x3, y3);
            requestResponse = routingRequest.compute();
            routingRequest = null;
            
        }
        
        PrintWriter out = response.getWriter();
        try {
            if (validRequest) {
                out.print(requestResponse.toJSONString());
            } else {
                response.sendError(400);
            }
        } finally {
            out.close();
        }
        
    }
    
    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Computes and responds routing & isochrone requests";
    }// </editor-fold>
    
}
