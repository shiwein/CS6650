import Model.LiftRideEvent;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import java.io.IOException;


public class ServerClient {
    private HttpClient httpClient;
    private String loadBalancer = "http://ServletBalancer-1748023418.us-west-2.elb.amazonaws.com";
    private String server1 = "http://54.214.38.101:8080";
    private String server2 = "http://52.42.160.104:8080";

    public ServerClient() {
        httpClient = HttpClients.createDefault();}

    public boolean sendLiftRideEvent(LiftRideEvent event) throws IOException {
        int skierID = event.getSkierID();
        int resortID = event.getResortID();
        String  seasonID = event.getSeasonID();
        String dayID = event.getDayID();

        HttpPost postRequest = new HttpPost(String.format(
                server1 + "/Server-1.0-SNAPSHOT/skiers/%d/seasons/%s/days/%s/skiers/%d",
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
        }

        if (statusCode == 201) {
            return true;
        } else {
            return false;
        }
    }
}

