package com.example.demo.exception;

public class StorageItemNotFoundException extends RuntimeException {

    public StorageItemNotFoundException(String key) {
        super("저장된 항목을 찾을 수 없습니다: " + key);
    }
}
