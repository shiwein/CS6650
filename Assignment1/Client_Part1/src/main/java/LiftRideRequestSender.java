import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LiftRideRequestSender {
    private static final String SERVER_URL = "http://52.87.211.94:8080/Upic-1.0-SNAPSHOT/skiers";

    public static void sendPostRequests(int requestCount, ConcurrentLinkedQueue<LiftRideEvent> eventQueue) {
        HttpClient client = HttpClient.newHttpClient();
        for (int i = 0; i < requestCount; i++) {
            try {
                LiftRideEvent event = eventQueue.poll();
                if (event == null) {
                    continue;
                }
                long startTime = System.currentTimeMillis();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(SERVER_URL))
                        .POST(HttpRequest.BodyPublishers.ofString(event.toJson()))
                        .header("Content-Type", "application/json")
                        .build();

                int retries = 0;
                boolean success = false;
                while (retries < 5 && !success) {
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    long endTime = System.currentTimeMillis();
                    long latency = endTime - startTime;
                    int statusCode = response.statusCode();
                    MultithreadedLiftClient.recordLatency(latency);
                    MultithreadedLiftClient.recordResponseCode(statusCode);
                    if (statusCode == 201) {
                        success = true;
                        MultithreadedLiftClient.incrementSuccessfulRequests();
                    } else if (statusCode >= 500 && statusCode < 600) {
                        retries++;
                    } else {
                        retries = 5; // Do not retry for client errors (4xx)
                    }
                }

                if (!success) {
                    MultithreadedLiftClient.incrementFailedRequests();
                }
            } catch (Exception e) {
                MultithreadedLiftClient.incrementFailedRequests();
                e.printStackTrace();
            }
        }
    }
}