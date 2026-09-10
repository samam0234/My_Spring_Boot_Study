package com.example.demo.repository;

import com.example.demo.domain.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {

    Optional<UploadedFile> findByStoredName(String storedName);
}
