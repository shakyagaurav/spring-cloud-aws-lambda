package com.notification.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification.entity.UserConnectionId;
import com.notification.repository.UserConnectionsRepository;
import com.notification.response.NotificationResponse;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.apigatewaymanagementapi.ApiGatewayManagementApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.apigatewaymanagementapi.model.PostToConnectionRequest;
import software.amazon.awssdk.services.apigatewaymanagementapi.model.PostToConnectionResponse;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
public class NotificationService {

    private static final String API_GATEWAY_WEBSOCKET_URI = "https://<API-GATEWAY-WEBSOCKET-ENDPOINT>";

    private final ApiGatewayManagementApiClient apiGatewayManagementApiClient;
    private final UserConnectionsRepository repository;

    public NotificationService(UserConnectionsRepository repository) {
        this.repository = repository;
        this.apiGatewayManagementApiClient = ApiGatewayManagementApiClient.builder()
                .endpointOverride(URI.create(API_GATEWAY_WEBSOCKET_URI))
                .region(Region.US_EAST_1)
                .build();
    }

    public String handleRequest(Map<String, Object> event) {
        return processNotification(event);
    }

    private String processNotification(Map<String, Object> event) {
        log.info("processNotification() invoked for request: {}", event);
        try {
            // Extract records from the event
            Map<String, List<Map<String, Object>>> records = (Map<String, List<Map<String, Object>>>) event.get("records");

            // Iterate through the records
            for (Map.Entry<String, List<Map<String, Object>>> entry : records.entrySet()) {
                List<Map<String, Object>> messages = entry.getValue();

                // Iterate through each message
                for (Map<String, Object> message : messages) {
                    String encodedValue = (String) message.get("value");

                    if (encodedValue == null || encodedValue.isEmpty()) {
                        System.out.println("Missing 'value' in message: " + message);
                        continue;
                    }

                    // Decode the Base64 value
                    try {
                        byte[] decodedBytes = Base64.getDecoder().decode(encodedValue);
                        String decodedValue = new String(decodedBytes, StandardCharsets.UTF_8);

                        // Parse the decoded value as JSON
                        ObjectMapper objectMapper = new ObjectMapper();
                        Map<String, Object> jsonValue = objectMapper.readValue(decodedValue, new TypeReference<>() {
                        });
                        System.out.println("Decoded message: " + jsonValue);

                        String userId = (String) jsonValue.get("userId");
                        String orderStatus = (String) jsonValue.get("orderStatus");
                        String orderNumber = (String) jsonValue.get("orderNumber");
                        String notifyMessage = prepareOrderMessage(orderStatus, orderNumber);

                        // Send the message to WebSocket or any other output mechanism
                        List<String> connectionIds = getConnectionsByUserIds(userId);
                        sendOrderStatusUpdateToClients(connectionIds, NotificationResponse.builder().status(orderStatus).message(notifyMessage).build());

                    } catch (Exception e) {
                        log.warn("Failed to decode message: {} and error: {}", encodedValue, e.getMessage());
                        return "Failed to decode message";
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error processing event: {}" ,e.getMessage());
            return "Error occurred while processing event";
        }
        return "Data sent successfully to websocket connection";
    }

    private String prepareOrderMessage(String orderStatus, String orderNumber) {
        if(orderStatus.equalsIgnoreCase("OrderProcessed")){
            return "Your order: "+orderNumber+" has been processed successfully";
        }
        return "Your order: "+orderNumber+" has been failed, Please contact to our customer care or try to place order again after sometimes.";
    }

    private List<String> getConnectionsByUserIds(String userId) {
        log.info("getConnectionsByUserIds() invoked for user: {}", userId);
        List<UserConnectionId> userConnectionIds = repository.findByUserId(userId);
        log.info("Get connections from DB for user: {}", userConnectionIds);
        return userConnectionIds.stream().map(UserConnectionId::getConnectionId).toList();
    }

    private void sendOrderStatusUpdateToClients(List<String> connectionIds, NotificationResponse notificationResponse) {
        try {
            for (String connectionId : connectionIds) {
                sendDataToWebsocketConnection(connectionId, notificationResponse);
            }
        } catch (Exception ex) {
            log.error("An error occurred while processing connections: {}", ex.getMessage());
        }
    }

    private void sendDataToWebsocketConnection(String connectionId, NotificationResponse notificationResponse) {
        try {
            log.info("Sending data to websocket connection:{}", connectionId);


            ObjectMapper mapper=new ObjectMapper();
            ByteBuffer byteBuffer = ByteBuffer.wrap(mapper.writeValueAsString(notificationResponse).getBytes(StandardCharsets.UTF_8));
            SdkBytes sdkBytes = SdkBytes.fromByteBuffer(byteBuffer);

            PostToConnectionRequest postToConnectionRequest = PostToConnectionRequest.builder()
                    .connectionId(connectionId) // Set the connection ID
                    .data(sdkBytes) // Convert message to SdkBytes
                    .build();

            // Send the message to the connection
            PostToConnectionResponse response = apiGatewayManagementApiClient.postToConnection(postToConnectionRequest);

            log.info("Data sent successfully to websocket connection:{}", response);
        } catch (Exception e) {
            log.error("Failed to send message to connection: {} and error: {}", connectionId, e.getMessage());
        }
    }
}
