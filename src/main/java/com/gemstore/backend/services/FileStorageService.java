package com.gemstore.backend.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file. Paths;
import java.nio. file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload. dir: uploads}")
    private String uploadDir;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public String uploadFile(MultipartFile file, String folder) {
        try {
            // Trim any whitespace from config
            String cleanUploadDir = uploadDir.trim();
            String cleanBaseUrl = baseUrl.trim();

            // Create directory if not exists
            Path uploadPath = Paths.get(cleanUploadDir, folder);
            Files.createDirectories(uploadPath);

            // Generate unique filename with extension
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename, file.getContentType());
            String filename = UUID.randomUUID().toString() + extension;

            // Save file
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption. REPLACE_EXISTING);

            // Return URL
            return cleanBaseUrl + "/" + cleanUploadDir + "/" + folder + "/" + filename;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }

    /**
     * Extract file extension safely.
     */
    private String getFileExtension(String filename, String contentType) {
        // Try to get extension from filename
        if (filename != null && filename.contains(".")) {
            int lastDot = filename.lastIndexOf(".");
            if (lastDot > 0 && lastDot < filename.length() - 1) {
                return filename.substring(lastDot); // includes the dot, e.g., ". jpg"
            }
        }

        // Fallback:  get extension from content type
        if (contentType != null) {
            switch (contentType.toLowerCase()) {
                case "image/jpeg":
                    return ".jpg";
                case "image/png":
                    return ".png";
                case "image/gif":
                    return ".gif";
                case "image/webp":
                    return ".webp";
                case "image/svg+xml":
                    return ". svg";
                case "image/bmp":
                    return ".bmp";
                default:
                    break;
            }
        }

        // Default extension for images
        return ".jpg";
    }

    /**
     * Delete file from storage.
     */
    public void deleteFile(String fileUrl) {
        try {
            String cleanBaseUrl = baseUrl.trim();
            String relativePath = fileUrl.replace(cleanBaseUrl + "/", "");
            Path filePath = Paths.get(relativePath);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + e.getMessage(), e);
        }
    }
}