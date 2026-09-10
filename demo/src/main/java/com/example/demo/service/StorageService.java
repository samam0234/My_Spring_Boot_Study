package com.example.demo.service;

import com.example.demo.domain.StorageItem;
import com.example.demo.domain.User;
import com.example.demo.dto.StorageResponse;
import com.example.demo.exception.StorageItemNotFoundException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.repository.StorageItemRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class StorageService {

    private final StorageItemRepository storageItemRepository;
    private final UserRepository userRepository;

    public StorageService(StorageItemRepository storageItemRepository, UserRepository userRepository) {
        this.storageItemRepository = storageItemRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public StorageResponse save(String username, String key, String value) {
        Optional<StorageItem> existing = storageItemRepository.findByOwner_UsernameAndItemKey(username, key);

        StorageItem item;
        if (existing.isPresent()) {
            item = existing.get();
            item.updateValue(value);
        } else {
            User owner = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UserNotFoundException(username));
            item = new StorageItem(owner, key, value);
        }

        return StorageResponse.from(storageItemRepository.save(item));
    }

    public List<StorageResponse> findAll(String username) {
        return storageItemRepository.findByOwner_UsernameOrderByUpdatedAtDesc(username).stream()
                .map(StorageResponse::from)
                .toList();
    }

    public StorageResponse findOne(String username, String key) {
        return storageItemRepository.findByOwner_UsernameAndItemKey(username, key)
                .map(StorageResponse::from)
                .orElseThrow(() -> new StorageItemNotFoundException(key));
    }

    @Transactional
    public void delete(String username, String key) {
        if (storageItemRepository.findByOwner_UsernameAndItemKey(username, key).isEmpty()) {
            throw new StorageItemNotFoundException(key);
        }
        storageItemRepository.deleteByOwner_UsernameAndItemKey(username, key);
    }
}
