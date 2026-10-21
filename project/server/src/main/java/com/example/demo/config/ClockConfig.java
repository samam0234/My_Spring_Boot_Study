package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        // 서버가 어떤 시간대에서 돌아가든, 이 프로젝트는 항상 한국 시간(KST) 기준으로 동작
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}
