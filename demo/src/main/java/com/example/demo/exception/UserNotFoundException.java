package com.example.demo.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String username) {
        super("사용자를 찾을 수 없습니다: " + username);
    }
}
