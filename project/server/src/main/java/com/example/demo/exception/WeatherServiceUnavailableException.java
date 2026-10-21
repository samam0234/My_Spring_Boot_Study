package com.example.demo.exception;

public class WeatherServiceUnavailableException extends RuntimeException {

    public WeatherServiceUnavailableException() {
        super("날씨 정보를 가져오는 데 실패했습니다. 잠시 후 다시 시도해주세요.");
    }
}
