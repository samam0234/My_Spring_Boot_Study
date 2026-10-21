package com.example.demo.repository;

import com.example.demo.domain.StorageItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StorageItemRepository extends JpaRepository<StorageItem, Long> {

    Optional<StorageItem> findByOwner_UsernameAndItemKey(String username, String itemKey);

    List<StorageItem> findByOwner_UsernameOrderByUpdatedAtDesc(String username);

    void deleteByOwner_UsernameAndItemKey(String username, String itemKey);
}
