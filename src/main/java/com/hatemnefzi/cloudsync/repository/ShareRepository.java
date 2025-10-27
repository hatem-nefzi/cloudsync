package com.hatemnefzi.cloudsync.repository;
import com.hatemnefzi.cloudsync.entity.Share;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ShareRepository extends JpaRepository<Share, Long> {
    Optional<Share> findByShareToken(String shareToken);
}