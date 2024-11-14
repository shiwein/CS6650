import Model.LiftRide;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;

public class Consumer {
    private final static String QUEUE_NAME = "LiftRide";
    private final static Integer NUM_THREAD = 100;
    private final static String LOCAL = "localhost";
    private final static String AWS = "44.231.229.60";
    private final static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final static ConcurrentHashMap<Integer, CopyOnWriteArrayList<String>> map = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(AWS);
        factory.setUsername("admin");
        factory.setPassword("adminPassword");
        Connection connection = factory.newConnection();

        Runnable runnable = () -> {
            Channel channel;
            try{
                channel = connection.createChannel();
                channel.queueDeclare(QUEUE_NAME, false, false, false, null);
                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    LiftRide liftRide = gson.fromJson(message, LiftRide.class);
                    System.out.println(" [x] Received '" + liftRide.toString() + "'");

                    Integer skierID = liftRide.getSkierID();
                    String liftID = liftRide.getLiftID();

                    map.putIfAbsent(skierID, new CopyOnWriteArrayList<>());
                    map.get(liftRide.getSkierID()).add(liftID);

                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                };
                channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> { });
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        for (int i = 0; i < NUM_THREAD; i++) {
            Thread thread = new Thread(runnable);
            thread.start();
        }
    }
}