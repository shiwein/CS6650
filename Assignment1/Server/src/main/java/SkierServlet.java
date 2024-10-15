import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


@WebServlet(name = "skierServlet", urlPatterns = "/skiers/*")
public class SkierServlet extends HttpServlet {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        String urlPath = req.getPathInfo();
        Message message = new Message();
        if (urlPath == null || urlPath.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            message.setMessage("Data Not Found");
            return;
        }
        System.out.println(urlPath);

        if (!valPostUrl(urlPath)) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            message.setMessage("Invalid URL format");
            res.getWriter().write(gson.toJson(message));
            return;
        }

        // check req body
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        String json = sb.toString();
        if (!valLiftRide(json)) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            message.setMessage("Invalid JSON body");
        } else {
            res.setStatus(HttpServletResponse.SC_CREATED);
            message.setMessage("Lift ride recorded successfully");
        }
        System.out.println(json);

        res.getWriter().write(gson.toJson(message));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

    }

    protected boolean valPostUrl(String urlPath) {
        String[] urlArr = urlPath.split("/");
        return urlArr.length == 8 && valSkiersUrl(urlArr);
    }

    protected boolean valSkiersUrl(String[] urlArr) {
        if (!"seasons".equals(urlArr[2]) || !"days".equals(urlArr[4]) || !"skiers".equals(urlArr[6])) {
            return false;
        }
        try {
            Integer.parseInt(urlArr[1]);  // resortID
            Integer.parseInt(urlArr[3]);  // seasonID
            Integer.parseInt(urlArr[5]);  // dayID
            Integer.parseInt(urlArr[7]);  // skierID
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    protected boolean valLiftRide(String json) {
        try {
            LiftRide liftRide = gson.fromJson(json, LiftRide.class);
            return liftRide != null && liftRide.getLiftID() != null && liftRide.getLiftID() > 0 &&
                    liftRide.getTime() != null && liftRide.getTime() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void sendJsonResponse(HttpServletResponse res, int statusCode, String message) throws IOException {
        res.setStatus(statusCode);
        Message msg = new Message();
        msg.setMessage(message);
        try (PrintWriter out = res.getWriter()) {
            out.write(gson.toJson(msg));
            out.flush();
        }
    }
}

