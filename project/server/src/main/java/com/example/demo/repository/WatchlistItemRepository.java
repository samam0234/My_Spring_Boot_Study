package com.example.demo.repository;

import com.example.demo.domain.WatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, Long> {

    List<WatchlistItem> findByOwner_UsernameOrderByAddedAtAsc(String username);

    boolean existsByOwner_UsernameAndSymbol(String username, String symbol);

    void deleteByOwner_UsernameAndSymbol(String username, String symbol);
}
