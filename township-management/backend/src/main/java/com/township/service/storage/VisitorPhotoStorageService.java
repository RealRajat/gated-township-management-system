package com.township.service.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class VisitorPhotoStorageService {

    private static final Logger log = LoggerFactory.getLogger(VisitorPhotoStorageService.class);
    private static final long MAX_PHOTO_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            ".jpg", ".jpeg", ".png", ".webp"
    );

    private final Path storageDirectory;

    public VisitorPhotoStorageService(@Value("${township.visitor.upload.dir:uploads/visitors}") String uploadDir) {
        this.storageDirectory = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageDirectory);
            log.info("Visitor photo storage directory initialized at: {}", this.storageDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create visitor photo storage directory", e);
        }
    }

    public record StoredVisitorPhotoInfo(
            String storagePath,
            String originalFilename,
            String contentType,
            long sizeBytes
    ) {}

    public StoredVisitorPhotoInfo storePhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        if (file.getSize() > MAX_PHOTO_SIZE_BYTES) {
            throw new IllegalArgumentException("Visitor photo size exceeds the maximum limit of 5MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Invalid file type: " + contentType + ". Only JPEG, PNG and WebP images are permitted.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            originalFilename = "visitor_capture.jpg";
        }

        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalFilename.substring(dotIndex).toLowerCase(Locale.ROOT);
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Invalid file extension: " + extension + ". Allowed: .jpg, .jpeg, .png, .webp");
        }

        String safeFileName = UUID.randomUUID().toString() + extension;
        Path targetPath = this.storageDirectory.resolve(safeFileName).normalize();

        if (!targetPath.startsWith(this.storageDirectory)) {
            throw new SecurityException("Potential path traversal attempt detected in filename.");
        }

        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored visitor photo: original='{}', stored='{}', size={} bytes", originalFilename, safeFileName, file.getSize());
            return new StoredVisitorPhotoInfo(safeFileName, originalFilename, contentType, file.getSize());
        } catch (IOException e) {
            log.error("Failed to write visitor photo to storage", e);
            throw new RuntimeException("Could not store visitor photo on disk", e);
        }
    }

    public Resource loadPhotoAsResource(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            throw new IllegalArgumentException("Photo storage path cannot be empty.");
        }

        try {
            Path filePath = this.storageDirectory.resolve(storagePath).normalize();
            if (!filePath.startsWith(this.storageDirectory)) {
                throw new SecurityException("Potential path traversal attempt.");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Visitor photo file not found or not readable: " + storagePath);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid file path", e);
        }
    }

    public boolean deletePhoto(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return false;
        }
        try {
            Path filePath = this.storageDirectory.resolve(storagePath).normalize();
            if (filePath.startsWith(this.storageDirectory) && Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Physically deleted visitor photo: {}", storagePath);
                return true;
            }
        } catch (IOException e) {
            log.warn("Failed to delete visitor photo file: {}", storagePath, e);
        }
        return false;
    }
}

