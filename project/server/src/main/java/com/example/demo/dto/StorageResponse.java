package com.example.demo.dto;

import com.example.demo.domain.StorageItem;

import java.time.LocalDateTime;

public record StorageResponse(
        String key,
        String value,
        LocalDateTime updatedAt
) {

    public static StorageResponse from(StorageItem item) {
        return new StorageResponse(item.getItemKey(), item.getItemValue(), item.getUpdatedAt());
    }
}
