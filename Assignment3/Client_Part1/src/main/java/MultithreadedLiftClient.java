import Model.LiftRideEvent;
import Model.LiftRideEventGenerator;

import java.util.concurrent.*;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;


public class MultithreadedLiftClient {
    private static final int INITIAL_THREADS = 32;
    private static final int REQUESTS_PER_THREAD = 1000;
    private static final int TOTAL_REQUESTS = 200_000;
    private static final int SECOND_PHASE_THREADS = 100;

    private BlockingQueue<LiftRideEvent> eventQueue;
    private static AtomicInteger successfulRequests = new AtomicInteger(0);
    private AtomicInteger failedRequests = new AtomicInteger(0);

    public MultithreadedLiftClient() {
        eventQueue = new LinkedBlockingQueue<>();
    }

    public void start() throws IOException, InterruptedException {
        Long startTime = System.currentTimeMillis();

        ExecutorService executorService = Executors.newFixedThreadPool(INITIAL_THREADS);

        LiftRideEventGenerator generator = new LiftRideEventGenerator(eventQueue, TOTAL_REQUESTS);
        new Thread(generator).start();

        CountDownLatch latch = new CountDownLatch(INITIAL_THREADS);

        for (int i = 0; i < INITIAL_THREADS; i++) {
            executorService.submit(new EventSender(latch, REQUESTS_PER_THREAD));
        }
        latch.await();
        System.out.println("32 threads completed. Total requests sent: " + INITIAL_THREADS * REQUESTS_PER_THREAD);
        int remainingRequests = TOTAL_REQUESTS - successfulRequests.get() - failedRequests.get();
        System.out.println("Left requests: " + remainingRequests);
        System.out.println("Now the number of threads will increase to ***100***");
        System.out.println("Please wait for remaining processing...");
        System.out.println("Number of threads: " + INITIAL_THREADS);
        System.out.println("Request/Threads: " + remainingRequests/INITIAL_THREADS);
        System.out.println("*********************************************************");
        executorService.shutdown();

        ExecutorService secondExecutorService = Executors.newFixedThreadPool(SECOND_PHASE_THREADS);
        while (remainingRequests > 0) {
            CountDownLatch remainingLatch = new CountDownLatch(INITIAL_THREADS);
            int requestsPerThread = remainingRequests / SECOND_PHASE_THREADS;
            int extraRequests = remainingRequests % SECOND_PHASE_THREADS;
            int totalRequestsForThisBatch = 0;

            for (int i = 0; i < INITIAL_THREADS; i++) {
                int requestsForThisThread = requestsPerThread + (i < extraRequests ? 1 : 0);
                totalRequestsForThisBatch += requestsForThisThread;
                secondExecutorService.submit(new EventSender(remainingLatch, requestsForThisThread));
            }
            remainingRequests -= totalRequestsForThisBatch;
            remainingLatch.await();
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        System.out.println("All threads completed.");
        System.out.println("Successful requests: " + successfulRequests.get());
        System.out.println("Failed requests: " + failedRequests.get());
        System.out.println("Total run time (ms): " + totalTime);

        double throughput = (successfulRequests.get() / (totalTime / 1000.0));
        System.out.println("Throughput (requests/second): " + throughput);

        executorService.shutdown();
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
            try {
                ServerClient client = new ServerClient();
                for (int i = 0; i < requestsToSend; i++) {
                    try {
                        LiftRideEvent event = eventQueue.take();
                        if (event.getResortID() == 11) {
                            break;
                        }
                        int attempts = 0;
                        boolean sent = false;
                        while (attempts < 5 && !sent) {
                            sent = client.sendLiftRideEvent(event);
                            attempts++;
                        }

                        if (sent) {
                            synchronized (MultithreadedLiftClient.this) {
                                successfulRequests.getAndIncrement();
                            }
                        } else {
                            synchronized (MultithreadedLiftClient.this) {
                                failedRequests.getAndIncrement();
                            }
                        }
                    } catch (InterruptedException | IOException e) {
                        e.printStackTrace();
                        synchronized (MultithreadedLiftClient.this) {
                            failedRequests.getAndIncrement();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            latch.countDown();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        MultithreadedLiftClient client = new MultithreadedLiftClient();
        try {
            client.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}