package com.hatemnefzi.cloudsync.repository;
import com.hatemnefzi.cloudsync.entity.Activity;
import com.hatemnefzi.cloudsync.entity.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    Page<Activity> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}