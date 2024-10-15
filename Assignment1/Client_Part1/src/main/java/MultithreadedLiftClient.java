import java.util.concurrent.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MultithreadedLiftClient {
    private static final int INITIAL_THREADS = 32;
    private static final int REQUESTS_PER_THREAD = 1000;
    private static final int TOTAL_REQUESTS = 200_000;
    private static final int QUEUE_CAPACITY = 10_000;
    private static final ConcurrentLinkedQueue<LiftRideEvent> eventQueue = new ConcurrentLinkedQueue<>();
    private static final List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
    private static final List<Integer> responseCodes = Collections.synchronizedList(new ArrayList<>());
    private static final AtomicInteger successfulRequests = new AtomicInteger(0);
    private static final AtomicInteger failedRequests = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException, IOException {
        System.out.println("Starting Lift Ride Event generation...");
        long startTime = System.currentTimeMillis();

        // Generate all lift ride events in advance
        LiftRideEventGenerator.generateEvents(TOTAL_REQUESTS, eventQueue);

        ThreadPoolExecutor initialExecutor = new ThreadPoolExecutor(
                INITIAL_THREADS, INITIAL_THREADS, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(QUEUE_CAPACITY));
        ThreadPoolExecutor dynamicExecutor = new ThreadPoolExecutor(
                1, 64, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());

        // Create initial threads
        for (int i = 0; i < INITIAL_THREADS; i++) {
            initialExecutor.submit(() -> LiftRideRequestSender.sendPostRequests(REQUESTS_PER_THREAD, eventQueue));
        }

        initialExecutor.shutdown();
        if (!initialExecutor.awaitTermination(60, TimeUnit.MINUTES)) {
            initialExecutor.shutdownNow();
        }

        // Submit remaining tasks to dynamicExecutor
        int remainingRequests = TOTAL_REQUESTS - (INITIAL_THREADS * REQUESTS_PER_THREAD);
        int remainingThreads = (remainingRequests + REQUESTS_PER_THREAD - 1) / REQUESTS_PER_THREAD; // Ceiling of division

        for (int i = 0; i < remainingThreads; i++) {
            dynamicExecutor.submit(() -> LiftRideRequestSender.sendPostRequests(REQUESTS_PER_THREAD, eventQueue));
        }

        dynamicExecutor.shutdown();
        if (!dynamicExecutor.awaitTermination(60, TimeUnit.MINUTES)) {
            dynamicExecutor.shutdownNow();
        }

        long endTime = System.currentTimeMillis();
        double totalTimeInSeconds = (endTime - startTime) / 1000.0;

        // Calculate performance metrics
        calculateAndPrintMetrics(totalTimeInSeconds);

        // Write data to CSV
        writeCsvFile("request_data.csv");
    }

    public static void incrementSuccessfulRequests() {
        successfulRequests.incrementAndGet();
    }

    public static void incrementFailedRequests() {
        failedRequests.incrementAndGet();
    }

    public static void recordLatency(long latency) {
        latencies.add(latency);
    }

    public static void recordResponseCode(int code) {
        responseCodes.add(code);
    }

    private static void calculateAndPrintMetrics(double totalTimeInSeconds) {
        // Calculate mean response time
        double meanLatency = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);

        // Calculate median response time
        List<Long> sortedLatencies = new ArrayList<>(latencies);
        Collections.sort(sortedLatencies);
        double medianLatency = sortedLatencies.size() % 2 == 0 ?
                (sortedLatencies.get(sortedLatencies.size() / 2 - 1) + sortedLatencies.get(sortedLatencies.size() / 2)) / 2.0 :
                sortedLatencies.get(sortedLatencies.size() / 2);

        // Calculate p99 response time
        int p99Index = (int) Math.ceil(0.99 * sortedLatencies.size()) - 1;
        long p99Latency = sortedLatencies.get(p99Index);

        // Calculate min and max response time
        long minLatency = sortedLatencies.get(0);
        long maxLatency = sortedLatencies.get(sortedLatencies.size() - 1);

        // Print metrics
        System.out.println("Number of successful requests sent: " + successfulRequests);
        System.out.println("Number of unsuccessful requests: " + failedRequests);
        System.out.println("Total run time: " + totalTimeInSeconds + " seconds");
        System.out.println("Total throughput: " + (TOTAL_REQUESTS / totalTimeInSeconds) + " requests/second");
        System.out.println("Mean response time: " + meanLatency + " ms");
        System.out.println("Median response time: " + medianLatency + " ms");
        System.out.println("99th percentile response time: " + p99Latency + " ms");
        System.out.println("Min response time: " + minLatency + " ms");
        System.out.println("Max response time: " + maxLatency + " ms");
    }

    private static void writeCsvFile(String fileName) throws IOException {
        try (FileWriter csvWriter = new FileWriter(fileName)) {
            csvWriter.append("Start Time,Request Type,Latency,Response Code\n");
            for (int i = 0; i < latencies.size(); i++) {
                csvWriter.append(System.currentTimeMillis() + ",POST," + latencies.get(i) + "," + responseCodes.get(i) + "\n");
            }
        }
    }
}