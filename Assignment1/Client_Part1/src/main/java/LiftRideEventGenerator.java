import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LiftRideEventGenerator {
    private static final Random random = new Random();

    public static void generateEvents(int totalRequests, ConcurrentLinkedQueue<LiftRideEvent> eventQueue) {
        for (int i = 0; i < totalRequests; i++) {
            eventQueue.add(generateLiftRideEvent());
        }
    }

    private static LiftRideEvent generateLiftRideEvent() {
        int skierID = random.nextInt(100000) + 1;
        int resortID = random.nextInt(10) + 1;
        int liftID = random.nextInt(40) + 1;
        int seasonID = 2024;
        int dayID = 1;
        int time = random.nextInt(360) + 1;
        return new LiftRideEvent(skierID, resortID, liftID, seasonID, dayID, time);
    }
}