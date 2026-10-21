package com.example.demo.exception;

public class CityNotFoundException extends RuntimeException {

    public CityNotFoundException(String city) {
        super("도시를 찾을 수 없습니다: " + city);
    }
}
