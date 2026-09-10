package com.example.demo.service;

import com.example.demo.dto.TimeStatusResponse;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

// Spring 컨텍스트 없이 순수 자바 객체만으로 동작하는 단위 테스트 (study5의 통합 테스트와 대비됨)
class TimeServiceTest {

    private TimeService serviceAt(int hour, int minute) {
        ZoneId seoul = ZoneId.of("Asia/Seoul");
        ZonedDateTime fixedNow = ZonedDateTime.of(2026, 9, 10, hour, minute, 0, 0, seoul);
        Clock clock = Clock.fixed(fixedNow.toInstant(), seoul);
        return new TimeService(clock);
    }

    @Test
    void hour22IsNight() {
        TimeStatusResponse status = serviceAt(22, 0).getStatus();
        assertThat(status.night()).isTrue();
        assertThat(status.period()).isEqualTo("야간");
    }

    @Test
    void hour7IsDay() {
        TimeStatusResponse status = serviceAt(7, 0).getStatus();
        assertThat(status.night()).isFalse();
        assertThat(status.period()).isEqualTo("주간");
    }

    @Test
    void hour6_59IsStillNight() {
        TimeStatusResponse status = serviceAt(6, 59).getStatus();
        assertThat(status.night()).isTrue();
    }

    @Test
    void hour21_59IsStillDay() {
        TimeStatusResponse status = serviceAt(21, 59).getStatus();
        assertThat(status.night()).isFalse();
    }

    @Test
    void midnightIsNight() {
        TimeStatusResponse status = serviceAt(0, 0).getStatus();
        assertThat(status.night()).isTrue();
    }

    @Test
    void noonIsDay() {
        TimeStatusResponse status = serviceAt(12, 0).getStatus();
        assertThat(status.night()).isFalse();
    }
}
