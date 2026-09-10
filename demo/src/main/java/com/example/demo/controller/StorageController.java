package com.example.demo.controller;

import com.example.demo.dto.StorageRequest;
import com.example.demo.dto.StorageResponse;
import com.example.demo.service.StorageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/storage")
public class StorageController {

    private final StorageService storageService;

    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PutMapping("/{key}")
    public StorageResponse save(@AuthenticationPrincipal UserDetails me,
                                 @PathVariable String key,
                                 @Valid @RequestBody StorageRequest request) {
        return storageService.save(me.getUsername(), key, request.value());
    }

    @GetMapping
    public List<StorageResponse> findAll(@AuthenticationPrincipal UserDetails me) {
        return storageService.findAll(me.getUsername());
    }

    @GetMapping("/{key}")
    public StorageResponse findOne(@AuthenticationPrincipal UserDetails me, @PathVariable String key) {
        return storageService.findOne(me.getUsername(), key);
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserDetails me, @PathVariable String key) {
        storageService.delete(me.getUsername(), key);
        return ResponseEntity.noContent().build();
    }
}
