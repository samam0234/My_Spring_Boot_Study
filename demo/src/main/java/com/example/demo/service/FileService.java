package com.example.demo.service;

import com.example.demo.domain.UploadedFile;
import com.example.demo.domain.User;
import com.example.demo.dto.FileResponse;
import com.example.demo.exception.InvalidFileException;
import com.example.demo.exception.StoredFileNotFoundException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.repository.UploadedFileRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileService {

    private final UploadedFileRepository uploadedFileRepository;
    private final UserRepository userRepository;
    private final Path uploadDir;

    public FileService(UploadedFileRepository uploadedFileRepository,
                        UserRepository userRepository,
                        @Value("${file.upload-dir:uploads}") String uploadDir) {
        this.uploadedFileRepository = uploadedFileRepository;
        this.userRepository = userRepository;
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new UncheckedIOException("업로드 디렉터리를 생성할 수 없습니다.", e);
        }
    }

    public FileResponse upload(String uploaderUsername, MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidFileException("빈 파일은 업로드할 수 없습니다.");
        }

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String extension = getExtension(originalName);
        String storedName = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);

        try {
            Path target = uploadDir.resolve(storedName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("파일 저장에 실패했습니다.", e);
        }

        User uploader = userRepository.findByUsername(uploaderUsername)
                .orElseThrow(() -> new UserNotFoundException(uploaderUsername));

        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        UploadedFile saved = uploadedFileRepository.save(
                new UploadedFile(storedName, originalName, contentType, file.getSize(), uploader));

        return FileResponse.from(saved);
    }

    public UploadedFile getMetadata(String storedName) {
        return uploadedFileRepository.findByStoredName(storedName)
                .orElseThrow(() -> new StoredFileNotFoundException(storedName));
    }

    public Resource loadAsResource(String storedName) {
        Path filePath = uploadDir.resolve(storedName).normalize();

        // 경로 조작(path traversal) 공격 방지: 업로드 디렉터리 바깥을 가리키면 거부
        if (!filePath.startsWith(uploadDir) || !Files.exists(filePath)) {
            throw new StoredFileNotFoundException(storedName);
        }

        return new FileSystemResource(filePath);
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex == -1 ? "" : filename.substring(dotIndex + 1);
    }
}
