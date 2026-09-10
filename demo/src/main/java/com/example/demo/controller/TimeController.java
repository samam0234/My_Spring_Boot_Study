package com.example.demo.controller;

import com.example.demo.dto.TimeStatusResponse;
import com.example.demo.service.TimeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/time")
public class TimeController {

    private final TimeService timeService;

    public TimeController(TimeService timeService) {
        this.timeService = timeService;
    }

    @GetMapping("/status")
    public TimeStatusResponse status() {
        return timeService.getStatus();
    }
}
