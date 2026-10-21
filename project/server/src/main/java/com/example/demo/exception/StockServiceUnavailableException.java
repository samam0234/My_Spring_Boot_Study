package com.example.demo.exception;

public class StockServiceUnavailableException extends RuntimeException {

    public StockServiceUnavailableException() {
        super("주식 시세를 가져오는 데 실패했습니다. 잠시 후 다시 시도해주세요.");
    }
}
