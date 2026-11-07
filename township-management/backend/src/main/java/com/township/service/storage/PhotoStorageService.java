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
public class PhotoStorageService {

    private static final Logger log = LoggerFactory.getLogger(PhotoStorageService.class);
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

    public PhotoStorageService(@Value("${township.upload.dir:uploads/complaints}") String uploadDir) {
        this.storageDirectory = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageDirectory);
            log.info("Complaint photo storage directory initialized at: {}", this.storageDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create complaint photo storage directory", e);
        }
    }

    public record StoredPhotoInfo(
            String storagePath,
            String originalFilename,
            String contentType,
            long sizeBytes
    ) {}

    public StoredPhotoInfo storePhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        if (file.getSize() > MAX_PHOTO_SIZE_BYTES) {
            throw new IllegalArgumentException("Photo size exceeds the maximum limit of 5MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase(Locale.ENGLISH))) {
            throw new IllegalArgumentException("Invalid photo type. Only JPEG, PNG, and WebP images are permitted.");
        }

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "photo.jpg";
        String ext = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(ext.toLowerCase(Locale.ENGLISH))) {
            throw new IllegalArgumentException("Invalid file extension: " + ext);
        }

        String safeStoredName = "CMP_IMG_" + UUID.randomUUID() + ext;
        Path targetPath = this.storageDirectory.resolve(safeStoredName).normalize();

        // Path Traversal Security Check
        if (!targetPath.startsWith(this.storageDirectory)) {
            throw new SecurityException("Cannot store file outside target upload directory.");
        }

        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return new StoredPhotoInfo(
                    targetPath.toString(),
                    originalFilename,
                    contentType,
                    file.getSize()
            );
        } catch (IOException e) {
            log.error("Failed to store complaint photo: ", e);
            throw new IllegalStateException("Failed to save photo file to storage.", e);
        }
    }

    public Resource loadPhotoAsResource(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            throw new IllegalArgumentException("Storage path cannot be empty.");
        }

        try {
            Path file = Paths.get(storagePath).normalize();
            if (!file.startsWith(this.storageDirectory)) {
                throw new SecurityException("Path traversal attempt detected.");
            }
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new IllegalArgumentException("Photo file not found or not readable: " + file.getFileName());
            }
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid storage path URL", e);
        }
    }

    private String getFileExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot > 0 && dot < filename.length() - 1) {
            return filename.substring(dot);
        }
        return ".jpg";
    }
}

