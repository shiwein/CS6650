import Model.LiftRideEvent;
import Model.LiftRideEventGenerator;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MultithreadedLiftClient {
    private static final int INITIAL_THREADS = 32;
    private static final int REQUESTS_PER_THREAD = 1000;
    private static final int TOTAL_REQUESTS = 200_000;

    private BlockingQueue<LiftRideEvent> eventQueue = new LinkedBlockingQueue<>();
    private AtomicInteger successfulRequests = new AtomicInteger(0);
    private AtomicInteger failedRequests = new AtomicInteger(0);
    private List<Long> requestLatencies = new ArrayList<>(); // For in-memory storage of latencies

    public void start() throws InterruptedException {
        System.out.println("********************************************");
        long startTime = System.currentTimeMillis();
        ExecutorService executorService = Executors.newFixedThreadPool(INITIAL_THREADS);

        LiftRideEventGenerator generator = new LiftRideEventGenerator(eventQueue, TOTAL_REQUESTS);
        new Thread(generator).start();

        int totalRequestsSent = 0;

        // Start with initial threads
        List<Future<?>> futures = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(INITIAL_THREADS);

        // Submit initial 32 threads
        System.out.println("Launch 32 threads");
        for (int i = 0; i < INITIAL_THREADS; i++) {
            futures.add(executorService.submit(new EventSender(latch, REQUESTS_PER_THREAD)));
            totalRequestsSent += REQUESTS_PER_THREAD;
        }

        // Wait for the initial threads to complete and dynamically submit new threads until all requests are sent
        latch.await(); // Wait for the initial batch of threads to finish
        System.out.println("Total requests sent: " + totalRequestsSent);
        System.out.println("Total successful requests: " + successfulRequests.get());

        int remaining = TOTAL_REQUESTS - successfulRequests.get() - failedRequests.get();
        System.out.println("Remaining requests: " + remaining);

        System.out.println();
        System.out.println("********************************************");
        System.out.println("Launching " + INITIAL_THREADS + " threads");


        while (totalRequestsSent < TOTAL_REQUESTS) {
            // Calculate remaining requests
            int remainingRequests = TOTAL_REQUESTS - totalRequestsSent;
            int threadsToLaunch = (remainingRequests + REQUESTS_PER_THREAD - 1) / REQUESTS_PER_THREAD; // Ceiling of remaining requests / requests per thread

            // Dynamically adjust thread count if remaining threads would exceed initial limit
            threadsToLaunch = Math.min(INITIAL_THREADS, threadsToLaunch);
            latch = new CountDownLatch(threadsToLaunch);

            for (int i = 0; i < threadsToLaunch; i++) {
                futures.add(executorService.submit(new EventSender(latch, REQUESTS_PER_THREAD)));
                totalRequestsSent += REQUESTS_PER_THREAD;
            }

            latch.await(); // Wait for this batch to finish
        }

        // Shut down the executor service after all requests are sent
        executorService.shutdown();

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double throughput = (successfulRequests.get() / (totalTime / 1000.0));

        // Calculate and print stats

        System.out.println("All threads completed.");
        System.out.println("Total wall time: " + totalTime + "ms");
        System.out.println("Throughput (requests/second): " + throughput);

        System.out.println("Successful requests: " + successfulRequests.get());
        System.out.println("Failed requests: " + failedRequests.get());

        System.out.println("********************************************");
        calculateStats();
    }


    private class EventSender implements Runnable {
        private CountDownLatch latch;
        private int requestsToSend;

        public EventSender(CountDownLatch latch, int requestsToSend) {
            this.latch = latch;
            this.requestsToSend = requestsToSend;
        }

        @Override
        public void run() {
            ServerClient client = new ServerClient();
            for (int i = 0; i < requestsToSend; i++) {
                try {
                    LiftRideEvent event = eventQueue.take();
                    long startTime = System.currentTimeMillis();
                    boolean sent = client.sendLiftRideEvent(event);
                    long endTime = System.currentTimeMillis();
                    long latency = endTime - startTime;

                    try (FileWriter writer = new FileWriter("request_logs.csv", true)) {
                        writer.append(String.format("%d, POST, %d, %d\n", startTime, latency, 201));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    synchronized (requestLatencies) {
                        requestLatencies.add(latency);
                    }

                    if (sent) {
                        successfulRequests.incrementAndGet();
                    } else {
                        failedRequests.incrementAndGet();
                    }
                } catch (InterruptedException | IOException e) {
                    failedRequests.incrementAndGet();
                    e.printStackTrace();
                }
            }
            latch.countDown();
        }
    }

    private void calculateStats() {
        Collections.sort(requestLatencies);
        long totalLatency = requestLatencies.stream().mapToLong(Long::longValue).sum();
        long mean = totalLatency / requestLatencies.size();
        long median = requestLatencies.get(requestLatencies.size() / 2);
        long minLatency = Collections.min(requestLatencies);
        long maxLatency = Collections.max(requestLatencies);
        long p99 = requestLatencies.get((int) (requestLatencies.size() * 0.99) - 1);

        System.out.println("Mean Response Time (ms): " + mean);
        System.out.println("Median Response Time (ms): " + median);
        System.out.println("Min Response Time (ms): " + minLatency);
        System.out.println("Max Response Time (ms): " + maxLatency);
        System.out.println("99th Percentile Response Time (ms): " + p99);
    }

    private void writeMetricsToCSV(String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.append("Latency (ms)\n");
            for (Long latency : requestLatencies) {
                writer.append(latency.toString()).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        MultithreadedLiftClient client = new MultithreadedLiftClient();
        client.start();
    }
}
