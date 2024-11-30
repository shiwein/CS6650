package Model;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;

public class ChannelPool {
    private Connection connection;
    private BlockingQueue<Channel> pool;
    private final static int capacity = 100;
    private final static String LOCAL = "localhost";
    private final static String AWS = "44.231.229.60";
    private final static String QUEUE_NAME = "LiftRide";

    public ChannelPool() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(AWS);  // 添加
        factory.setUsername("admin");
        factory.setPassword("adminPassword");

        try {
            this.connection = factory.newConnection();
            System.out.println("Connected to RabbitMQ");
        } catch (IOException | TimeoutException e) {
            System.err.println("Something Went Wrong in Connection");
            e.printStackTrace();
            throw e;
        }

        this.pool = new LinkedBlockingQueue<>();

        for (int i = 0; i < capacity; i++) {
            try {
                Channel channel = this.connection.createChannel();
                channel.queueDeclare(QUEUE_NAME, false, false, false, null);
                this.pool.add(channel);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Channel takeChannel() throws InterruptedException {
        return this.pool.take();
    }

    public void add(Channel channel) {
        this.pool.offer(channel);
    }

    public void close() {
        for (Channel channel : pool) {
            try {
                if (channel != null && channel.isOpen()) {
                    channel.close();
                }
                this.pool.remove(channel);  //确保关闭的channel被移除
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            if (connection != null && connection.isOpen()) {
                connection.close();
                System.out.println("Connection to RabbitMQ closed");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
