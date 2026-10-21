package com.example.demo.controller;

import com.example.demo.dto.StockResponse;
import com.example.demo.dto.WatchlistRequest;
import com.example.demo.service.WatchlistService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/watchlist")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @PostMapping
    public ResponseEntity<Void> add(@AuthenticationPrincipal UserDetails me, @Valid @RequestBody WatchlistRequest request) {
        watchlistService.add(me.getUsername(), request.symbol());
        return ResponseEntity.status(201).build();
    }

    @GetMapping
    public List<StockResponse> list(@AuthenticationPrincipal UserDetails me) {
        return watchlistService.list(me.getUsername());
    }

    @DeleteMapping("/{symbol}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal UserDetails me, @PathVariable String symbol) {
        watchlistService.remove(me.getUsername(), symbol);
        return ResponseEntity.noContent().build();
    }
}
