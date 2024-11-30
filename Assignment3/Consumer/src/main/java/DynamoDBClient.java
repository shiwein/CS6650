import Model.LiftRide;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

public class DynamoDBClient {

    public static final DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
            .build();

    static {
        try {
            ListTablesResponse response = dynamoDbClient.listTables(ListTablesRequest.builder().build());
            System.out.println("Tables in DynamoDB: " + response.tableNames());
        } catch (DynamoDbException e) {
            System.err.println("Failed to list tables: " + e.getMessage());
        }
    }

    /**
     * 批量插入 LiftRide 数据
     */
    public static void insertLiftRidesBatch(List<LiftRide> liftRides) {
        List<WriteRequest> writeRequests = new ArrayList<>();

        for (LiftRide liftRide : liftRides) {
            Map<String, AttributeValue> item = new HashMap<>();
            // Primary Key: Partition Key (skierID) and Sort Key (dayID#time)
            item.put("skierID", AttributeValue.builder().n(String.valueOf(liftRide.getSkierID())).build());
            item.put("dayID#time", AttributeValue.builder()
                    .s(String.format("%s#%d", liftRide.getDayID(), liftRide.getTime()))
                    .build());

            // Attributes: resortID, seasonID, liftID, time
            item.put("resortID", AttributeValue.builder().n(String.valueOf(liftRide.getResortID())).build());
            item.put("seasonID", AttributeValue.builder().s(liftRide.getSeasonID()).build());
            item.put("liftID", AttributeValue.builder().n(String.valueOf(liftRide.getLiftID())).build());
            item.put("time", AttributeValue.builder().n(String.valueOf(liftRide.getTime())).build());

            writeRequests.add(WriteRequest.builder()
                    .putRequest(PutRequest.builder().item(item).build())
                    .build());

            // DynamoDB 批量写入限制：最多 25 项
            if (writeRequests.size() == 25) {
                flushBatch(writeRequests);
            }
        }

        if (!writeRequests.isEmpty()) {
            flushBatch(writeRequests);
        }
    }

    /**
     * 刷新批量写入到 DynamoDB
     */
    private static void flushBatch(List<WriteRequest> writeRequests) {
        try {
            dynamoDbClient.batchWriteItem(BatchWriteItemRequest.builder()
                    .requestItems(Collections.singletonMap("LiftRide", writeRequests))
                    .build());
        } catch (DynamoDbException e) {
            System.err.println("Batch write failed: " + e.getMessage());
        } finally {
            writeRequests.clear();
        }
    }

    /**
     * 根据 skierID 和 seasonID 查询滑雪天数和垂直总和
     */
    public static void querySkierSeasonData(String skierID, String seasonID) {
        try {
            Map<String, AttributeValue> attributeValues = new HashMap<>();
            attributeValues.put(":skierID", AttributeValue.builder().n(skierID).build());
            attributeValues.put(":seasonID", AttributeValue.builder().s(seasonID).build());

            QueryResponse response = dynamoDbClient.query(QueryRequest.builder()
                    .tableName("LiftRide")
                    .keyConditionExpression("skierID = :skierID AND begins_with(seasonID, :seasonID)")
                    .expressionAttributeValues(attributeValues)
                    .build());

            System.out.println("Query Results:");
            for (Map<String, AttributeValue> item : response.items()) {
                System.out.println(item);
            }
        } catch (DynamoDbException e) {
            System.err.println("Query failed: " + e.getMessage());
        }
    }

    /**
     * 根据 resortID 和 dayID 查询唯一滑雪者数量
     */
    public static void queryUniqueSkiersByResortDay(String resortID, String dayID) {
        try {
            Map<String, AttributeValue> attributeValues = new HashMap<>();
            attributeValues.put(":resortID", AttributeValue.builder().n(resortID).build());
            attributeValues.put(":dayID", AttributeValue.builder().s(dayID).build());

            QueryResponse response = dynamoDbClient.query(QueryRequest.builder()
                    .tableName("LiftRide")
                    .indexName("Resort-Day Index") // GSI 1
                    .keyConditionExpression("resortID = :resortID AND begins_with(dayID#time, :dayID)")
                    .expressionAttributeValues(attributeValues)
                    .build());

            Set<String> uniqueSkiers = new HashSet<>();
            for (Map<String, AttributeValue> item : response.items()) {
                uniqueSkiers.add(item.get("skierID").n());
            }

            System.out.println("Unique Skiers on " + dayID + ": " + uniqueSkiers.size());
        } catch (DynamoDbException e) {
            System.err.println("Query failed: " + e.getMessage());
        }
    }
}
