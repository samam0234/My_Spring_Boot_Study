package com.example.demo.controller;

import com.example.demo.domain.UploadedFile;
import com.example.demo.dto.FileResponse;
import com.example.demo.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping
    public ResponseEntity<FileResponse> upload(@AuthenticationPrincipal UserDetails me,
                                                @RequestParam("file") MultipartFile file) {
        FileResponse response = fileService.upload(me.getUsername(), file);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{storedName}")
    public ResponseEntity<Resource> download(@PathVariable String storedName) {
        UploadedFile metadata = fileService.getMetadata(storedName);
        Resource resource = fileService.loadAsResource(storedName);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + metadata.getOriginalName() + "\"")
                .body(resource);
    }
}
