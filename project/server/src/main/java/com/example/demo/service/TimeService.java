package com.example.demo.service;

import com.example.demo.dto.TimeStatusResponse;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class TimeService {

    private static final int NIGHT_START_HOUR = 22; // 오후 10시부터
    private static final int NIGHT_END_HOUR = 7;    // 오전 7시 전까지
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Clock clock;

    public TimeService(Clock clock) {
        this.clock = clock;
    }

    public TimeStatusResponse getStatus() {
        ZonedDateTime now = ZonedDateTime.now(clock);
        boolean night = isNight(now.getHour());
        String period = night ? "야간" : "주간";
        String message = night ? "지금은 야간입니다." : "지금은 주간입니다.";

        return new TimeStatusResponse(now.format(FORMATTER), period, night, message);
    }

    private boolean isNight(int hour) {
        return hour >= NIGHT_START_HOUR || hour < NIGHT_END_HOUR;
    }
}
