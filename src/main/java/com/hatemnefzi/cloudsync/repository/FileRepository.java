package com.hatemnefzi.cloudsync.repository;
import com.hatemnefzi.cloudsync.entity.File;
import com.hatemnefzi.cloudsync.entity.Folder;
import com.hatemnefzi.cloudsync.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<File, Long> {
    List<File> findByOwnerAndFolderAndDeletedAtIsNull(User owner, Folder folder);
    List<File> findByOwnerAndDeletedAtIsNull(User owner);
    Optional<File> findByIdAndDeletedAtIsNull(Long id);
    List<File> findByChecksum(String checksum);
    List<File> findByNameContainingIgnoreCaseAndOwnerAndDeletedAtIsNull(String name, User owner);
    Optional<File> findFirstByChecksumAndDeletedAtIsNull(String checksum);
}