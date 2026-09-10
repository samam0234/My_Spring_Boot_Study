package com.example.demo.service;

import com.example.demo.domain.User;
import com.example.demo.domain.WatchlistItem;
import com.example.demo.dto.StockResponse;
import com.example.demo.exception.DuplicateWatchlistItemException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.WatchlistItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WatchlistService {

    private final WatchlistItemRepository watchlistItemRepository;
    private final UserRepository userRepository;
    private final StockService stockService;

    public WatchlistService(WatchlistItemRepository watchlistItemRepository,
                             UserRepository userRepository,
                             StockService stockService) {
        this.watchlistItemRepository = watchlistItemRepository;
        this.userRepository = userRepository;
        this.stockService = stockService;
    }

    public void add(String username, String symbol) {
        String normalized = symbol.toUpperCase();

        if (watchlistItemRepository.existsByOwner_UsernameAndSymbol(username, normalized)) {
            throw new DuplicateWatchlistItemException(normalized);
        }

        // 존재하지 않는 종목이면 여기서 StockNotFoundException이 그대로 던져짐
        stockService.getQuote(normalized);

        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
        watchlistItemRepository.save(new WatchlistItem(owner, normalized));
    }

    public List<StockResponse> list(String username) {
        return watchlistItemRepository.findByOwner_UsernameOrderByAddedAtAsc(username).stream()
                .map(item -> stockService.getQuote(item.getSymbol()))
                .toList();
    }

    @Transactional
    public void remove(String username, String symbol) {
        watchlistItemRepository.deleteByOwner_UsernameAndSymbol(username, symbol.toUpperCase());
    }
}
