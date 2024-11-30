import Model.ChannelPool;
import Model.LiftRide;
import Model.Message;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rabbitmq.client.Channel;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

@WebServlet(name = "SkiersServlet", urlPatterns = "/skiers/*", loadOnStartup = 1)
public class SkiersServlet extends HttpServlet {

    private ChannelPool channelPool;
    private static final String QUEUE_NAME = "LiftRide";
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void init() throws ServletException {
        System.out.println("Servlet initializing...");
        super.init();
        try {
            this.channelPool = new ChannelPool();
//            Channel channel = channelPool.takeChannel();
//            channel.queueDeclare(QUEUE_NAME, false, false, false, null); // 设置队列持久化
//            channelPool.add(channel);
            System.out.println("Queue declared and ChannelPool initialized successfully.");
        } catch (IOException | TimeoutException e) {
            throw new ServletException("Failed to initialize ChannelPool or declare queue", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        String urlPath = req.getPathInfo();
        Message message = new Message();
        String responseJson;

        try (PrintWriter out = res.getWriter()) {
            if (urlPath == null || urlPath.isEmpty()) {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                message.setMessage("Missing Path Parameters");
                out.write(gson.toJson(message));
                return;
            }

            if (!isValUrlPath(urlPath)) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                message.setMessage("Invalid URL Format");
                out.write(gson.toJson(message));
                return;
            }

            String json = readRequestBody(req);
            if (!isValLiftRide(json)) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                message.setMessage("Invalid LiftRide Inputs");
                out.write(gson.toJson(message));
                return;
            }

            String[] pathData = handlePathData(urlPath);
            LiftRide liftRide = gson.fromJson(json, LiftRide.class);
            liftRide.setResortID(pathData[0]);
            liftRide.setSeasonID(pathData[1]);
            liftRide.setDayID(pathData[2]);
            liftRide.setSkierID(pathData[3]);

            try {
                Channel channel = channelPool.takeChannel();
                channel.basicPublish("", QUEUE_NAME, null, gson.toJson(liftRide).getBytes(StandardCharsets.UTF_8));
                channelPool.add(channel);

                res.setStatus(HttpServletResponse.SC_CREATED);
                message.setMessage("Lift ride successfully added to the queue");
                responseJson = gson.toJson(message);
                System.out.println("Message sent to queue: " + gson.toJson(liftRide));
            } catch (IOException | InterruptedException e) {
                res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                message.setMessage("Failed to send message to queue");
                responseJson = gson.toJson(message);
                e.printStackTrace();
            }
            out.write(responseJson);
        }
    }

    private String readRequestBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/plain");
        String urlPath = req.getPathInfo();

        try (PrintWriter out = res.getWriter()) {
            if (urlPath == null || urlPath.isEmpty()) {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write("Missing Path Parameters");
                return;
            }

            String[] urlParts = urlPath.split("/");
            if (!isUrlValid(urlParts)) {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write("Invalid Path");
            } else {
                res.setStatus(HttpServletResponse.SC_OK);
                out.write("It works!");
            }
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (channelPool != null) {
            try {
                channelPool.close();
                System.out.println("ChannelPool closed successfully.");
            } catch (Exception e) {
                System.err.println("Error closing ChannelPool: " + e.getMessage());
            }
        }
    }

    private boolean isUrlValid(String[] urlPath) {
        return urlPath.length == 8 && "seasons".equals(urlPath[2]) && "days".equals(urlPath[4]) && "skiers".equals(urlPath[6]);
    }

    protected boolean isValUrlPath(String urlPath) {
        String[] pathParts = urlPath.split("/");
        if (pathParts.length == 8) {
            try {
                Integer.parseInt(pathParts[1]);  // resortID
                Integer.parseInt(pathParts[3]);  // seasonID
                Integer.parseInt(pathParts[5]);  // dayID
                Integer.parseInt(pathParts[7]);  // skierID
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    protected boolean isValLiftRide(String json) {
        LiftRide liftRide = gson.fromJson(json, LiftRide.class);
        return liftRide != null && liftRide.getLiftID() != null && liftRide.getTime() != null;
    }

    protected String[] handlePathData(String urlPath) {
        String[] pathParts = urlPath.split("/");
        return new String[]{pathParts[1], pathParts[3], pathParts[5], pathParts[7]};
    }
}
