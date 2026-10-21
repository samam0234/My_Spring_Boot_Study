package com.example.demo.exception;

public class StoredFileNotFoundException extends RuntimeException {

    public StoredFileNotFoundException(String filename) {
        super("파일을 찾을 수 없습니다: " + filename);
    }
}
