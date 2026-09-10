package com.example.demo.dto;

import com.example.demo.domain.UploadedFile;

public record FileResponse(
        String filename,
        String originalName,
        String url,
        String contentType,
        long size
) {

    public static FileResponse from(UploadedFile file) {
        return new FileResponse(
                file.getStoredName(),
                file.getOriginalName(),
                "/files/" + file.getStoredName(),
                file.getContentType(),
                file.getSize()
        );
    }
}
