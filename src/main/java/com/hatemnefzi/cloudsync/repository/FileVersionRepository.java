package com.hatemnefzi.cloudsync.repository;
import com.hatemnefzi.cloudsync.entity.File;
import com.hatemnefzi.cloudsync.entity.FileVersion;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {
    List<FileVersion> findByFileOrderByVersionNumberDesc(File file);
}