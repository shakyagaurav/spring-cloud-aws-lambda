package com.notification.repository;

import com.notification.entity.UserConnectionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserConnectionsRepository extends JpaRepository<UserConnectionId, String> {
     List<UserConnectionId> findByUserId(String userId);
}
