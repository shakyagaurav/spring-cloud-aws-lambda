package com.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "user_websocket_connections")
public class UserConnectionId {

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Id
    @Column(name = "connection_id", nullable = false)
    private String connectionId;
}

