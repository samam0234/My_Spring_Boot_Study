package com.example.demo.exception;

public class DuplicateWatchlistItemException extends RuntimeException {

    public DuplicateWatchlistItemException(String symbol) {
        super("이미 관심 종목에 등록되어 있습니다: " + symbol);
    }
}
