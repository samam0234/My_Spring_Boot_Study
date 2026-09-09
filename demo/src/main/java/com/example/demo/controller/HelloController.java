package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    // GET /hello
    @GetMapping("/hello")
    public String hello() {
        return "Hello, Spring Boot!";
    }

    // GET /hello/name?name=철수  (쿼리 파라미터)
    @GetMapping("/hello/name")
    public String helloByQueryParam(@RequestParam(defaultValue = "World") String name) {
        return "Hello, " + name + "! (query param)";
    }

    // GET /hello/철수  (경로 변수)
    @GetMapping("/hello/{name}")
    public String helloByPathVariable(@PathVariable String name) {
        return "Hello, " + name + "! (path variable)";
    }
}
