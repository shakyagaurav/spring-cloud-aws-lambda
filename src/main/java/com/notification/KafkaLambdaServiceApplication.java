package com.notification;

import com.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Map;
import java.util.function.Function;

@SpringBootApplication
@Slf4j
public class KafkaLambdaServiceApplication {

    public KafkaLambdaServiceApplication(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public static void main(String[] args) {
        SpringApplication.run(KafkaLambdaServiceApplication.class, args);
    }

    private final NotificationService notificationService;

    @Bean
    public Function<Map<String, Object>, String> consumeMessageFromTopicAndSendToWebsocket() {
        log.info("consumeMessageFromTopicAndSendToWebsocket() invoked");
        return notificationService::handleRequest;
    }
}
