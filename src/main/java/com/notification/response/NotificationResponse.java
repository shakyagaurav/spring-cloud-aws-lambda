package com.notification.response;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class NotificationResponse implements Serializable {
    private String status;
    private String message;
}
