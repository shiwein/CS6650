import Model.LiftRideEvent;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import java.io.IOException;
import com.google.gson.Gson;

public class ServerClient {
    private HttpClient httpClient;

    public ServerClient() throws IOException {
        httpClient = HttpClients.createDefault();
    }

    public boolean sendLiftRideEvent(LiftRideEvent event) throws IOException {
        int skierID = event.getSkierID();
        int resortID = event.getResortID();
        String  seasonID = event.getSeasonID();
        String dayID = event.getDayID();

        HttpPost postRequest = new HttpPost(String.format(
                "http://localhost:8080/Server_war_exploded/" +
                        "skiers/%d/seasons/%s/days/%s/skiers/%d",
                resortID, seasonID, dayID, skierID));

        String json = "{" + "\"time\": " + event.getTime() + "," +
                "\"liftID\": " + event.getLiftID() + "}";

        StringEntity entity = new StringEntity(json);
        postRequest.setEntity(entity);
        postRequest.setHeader("Content-Type", "application/json");


        HttpResponse response = httpClient.execute(postRequest);
        int statusCode = response.getStatusLine().getStatusCode();

        try {
            String responseBody = EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
//            System.err.println("Invalid Body: " + e.getMessage());
        }

        if (statusCode == 201) {
            return true;
        } else {
            return false;
        }
    }
}