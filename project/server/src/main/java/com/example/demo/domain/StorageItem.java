package com.example.demo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "storage_items", uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "item_key"}))
public class StorageItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "item_key", nullable = false)
    private String itemKey;

    @Column(nullable = false, length = 4000)
    private String itemValue;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected StorageItem() {
    }

    public StorageItem(User owner, String itemKey, String itemValue) {
        this.owner = owner;
        this.itemKey = itemKey;
        this.itemValue = itemValue;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateValue(String itemValue) {
        this.itemValue = itemValue;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public String getItemKey() {
        return itemKey;
    }

    public String getItemValue() {
        return itemValue;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
