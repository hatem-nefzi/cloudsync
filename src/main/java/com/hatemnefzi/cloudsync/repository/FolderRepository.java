package com.hatemnefzi.cloudsync.repository;
import com.hatemnefzi.cloudsync.entity.Folder;
import com.hatemnefzi.cloudsync.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long> {
    List<Folder> findByOwnerAndParentIsNull(User owner);
    List<Folder> findByOwnerAndParent(User owner, Folder parent);
    Optional<Folder> findByIdAndOwner(Long id, User owner);
}