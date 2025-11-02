package com.hatemnefzi.cloudsync.repository;
import com.hatemnefzi.cloudsync.entity.Share;
import com.hatemnefzi.cloudsync.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShareRepository extends JpaRepository<Share, Long> {
    Optional<Share> findByShareToken(String shareToken);
    List<Share> findBySharedByOrderByCreatedAtDesc(User sharedBy);
    List<Share> findBySharedWithOrderByCreatedAtDesc(User sharedWith);
}