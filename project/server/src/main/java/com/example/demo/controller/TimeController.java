package com.example.demo.controller;

import com.example.demo.dto.TimeStatusResponse;
import com.example.demo.service.TimeService;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/time")
public class TimeController {

    private final TimeService timeService;
    private final Environment environment;

    public TimeController(TimeService timeService, Environment environment) {
        this.timeService = timeService;
        this.environment = environment;
    }

    @GetMapping("/status")
    public TimeStatusResponse status() {
        return timeService.getStatus();
    }

    @GetMapping("/profile")
    public Map<String, Object> profile() {
        String[] active = environment.getActiveProfiles();
        List<String> names = active.length == 0 ? List.of("default") : List.of(active);
        return Map.of(
                "activeProfiles", names,
                "datasource", environment.getProperty("spring.datasource.url", ""));
    }
}
