package org.solace.scholar_ai.project_service.service.note;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.note.ImageUploadDto;
import org.solace.scholar_ai.project_service.mapping.note.NoteImageMapper;
import org.solace.scholar_ai.project_service.model.note.NoteImage;
import org.solace.scholar_ai.project_service.repository.note.NoteImageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "transactionManager")
public class NoteImageService {

    @Value("${scholarai.notes.images.upload-path:/tmp/note-images}")
    private String uploadPath;

    @Value("${scholarai.notes.images.max-size:10485760}") // 10MB default
    private long maxFileSize;

    private final NoteImageRepository noteImageRepository;
    private final NoteImageMapper noteImageMapper;

    /**
     * Upload an image for a project
     */
    public ImageUploadDto uploadImage(UUID projectId, MultipartFile file) {
        log.info("Uploading image for project: {}", projectId);

        // Validate file
        validateImageFile(file);

        try {
            // Create upload directory if it doesn't exist
            Path uploadDir = Paths.get(uploadPath, projectId.toString());
            Files.createDirectories(uploadDir);

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String storedFilename = "img_" + UUID.randomUUID() + fileExtension;
            Path filePath = uploadDir.resolve(storedFilename);

            // Save file to disk
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Create database record
            NoteImage noteImage = new NoteImage();
            noteImage.setProjectId(projectId);
            noteImage.setOriginalFilename(originalFilename);
            noteImage.setStoredFilename(storedFilename);
            noteImage.setFileSize(file.getSize());
            noteImage.setMimeType(file.getContentType());
            noteImage.setFilePath(filePath.toString());
            noteImage.setUploadedAt(Instant.now());

            NoteImage savedImage = noteImageRepository.save(noteImage);

            // Generate public URL
            String imageUrl = String.format("/api/v1/projects/%s/notes/images/%s", projectId, savedImage.getId());

            log.info("Image uploaded successfully: {} (ID: {})", storedFilename, savedImage.getId());
            return noteImageMapper.toDto(savedImage, imageUrl);

        } catch (IOException e) {
            log.error("Failed to upload image for project {}: {}", projectId, e.getMessage());
            throw new RuntimeException("Failed to upload image: " + e.getMessage(), e);
        }
    }

    /**
     * Get image by ID
     */
    @Transactional(readOnly = true, transactionManager = "transactionManager")
    public NoteImage getImageById(UUID imageId) {
        return noteImageRepository.findById(imageId).orElseThrow(() -> new RuntimeException("Image not found"));
    }

    /**
     * Get all images for a project
     */
    @Transactional(readOnly = true, transactionManager = "transactionManager")
    public List<ImageUploadDto> getImagesByProjectId(UUID projectId) {
        List<NoteImage> images = noteImageRepository.findByProjectIdOrderByUploadedAtDesc(projectId);
        return images.stream()
                .map(image -> {
                    String imageUrl = String.format("/api/v1/projects/%s/notes/images/%s", projectId, image.getId());
                    return noteImageMapper.toDto(image, imageUrl);
                })
                .toList();
    }

    /**
     * Associate image with a note
     */
    public void associateImageWithNote(UUID imageId, UUID noteId) {
        NoteImage image = getImageById(imageId);
        image.setNoteId(noteId);
        noteImageRepository.save(image);
        log.info("Associated image {} with note {}", imageId, noteId);
    }

    /**
     * Associate all orphaned images (images with null noteId) for a project with a
     * specific note
     */
    public void associateOrphanedImagesWithNote(UUID projectId, UUID noteId) {
        List<NoteImage> orphanedImages = noteImageRepository.findByProjectIdOrderByUploadedAtDesc(projectId).stream()
                .filter(image -> image.getNoteId() == null)
                .toList();

        for (NoteImage image : orphanedImages) {
            image.setNoteId(noteId);
            noteImageRepository.save(image);
            log.info("Associated orphaned image {} with note {}", image.getId(), noteId);
        }

        log.info("Associated {} orphaned images with note {}", orphanedImages.size(), noteId);
    }

    /**
     * Delete image
     */
    public void deleteImage(UUID imageId) {
        NoteImage image = getImageById(imageId);

        try {
            // Delete file from disk
            Files.deleteIfExists(Paths.get(image.getFilePath()));

            // Delete database record
            noteImageRepository.delete(image);

            log.info("Image deleted successfully: {}", imageId);
        } catch (IOException e) {
            log.error("Failed to delete image file {}: {}", imageId, e.getMessage());
            // Still delete from database even if file deletion fails
            noteImageRepository.delete(image);
        }
    }

    /**
     * Delete all images associated with a note
     */
    public void deleteImagesByNoteId(UUID noteId) {
        List<NoteImage> images = noteImageRepository.findByNoteIdOrderByUploadedAtDesc(noteId);

        for (NoteImage image : images) {
            try {
                // Delete file from disk
                Files.deleteIfExists(Paths.get(image.getFilePath()));

                // Delete database record
                noteImageRepository.delete(image);

                log.info("Deleted image {} for note {}", image.getId(), noteId);
            } catch (IOException e) {
                log.error("Failed to delete image file {} for note {}: {}", image.getId(), noteId, e.getMessage());
                // Still delete from database even if file deletion fails
                noteImageRepository.delete(image);
            }
        }

        log.info("Deleted {} images for note {}", images.size(), noteId);
    }

    /**
     * Delete all images for a project (used when project is deleted)
     */
    public void deleteImagesByProjectId(UUID projectId) {
        List<NoteImage> images = noteImageRepository.findByProjectIdOrderByUploadedAtDesc(projectId);

        for (NoteImage image : images) {
            try {
                // Delete file from disk
                Files.deleteIfExists(Paths.get(image.getFilePath()));

                // Delete database record
                noteImageRepository.delete(image);

                log.info("Deleted image {} for project {}", image.getId(), projectId);
            } catch (IOException e) {
                log.error(
                        "Failed to delete image file {} for project {}: {}", image.getId(), projectId, e.getMessage());
                // Still delete from database even if file deletion fails
                noteImageRepository.delete(image);
            }
        }

        log.info("Deleted {} images for project {}", images.size(), projectId);
    }

    /**
     * Clean up orphaned images (images not associated with any note)
     */
    public void cleanupOrphanedImages(UUID projectId) {
        List<NoteImage> orphanedImages = noteImageRepository.findByProjectIdOrderByUploadedAtDesc(projectId).stream()
                .filter(image -> image.getNoteId() == null)
                .toList();

        for (NoteImage image : orphanedImages) {
            deleteImage(image.getId());
        }

        log.info("Cleaned up {} orphaned images for project {}", orphanedImages.size(), projectId);
    }

    /**
     * Clean up old orphaned images (images not associated with any note and older
     * than specified hours)
     */
    public void cleanupOldOrphanedImages(UUID projectId, int hoursOld) {
        Instant cutoffTime = Instant.now().minus(hoursOld, java.time.temporal.ChronoUnit.HOURS);

        List<NoteImage> oldOrphanedImages = noteImageRepository.findByProjectIdOrderByUploadedAtDesc(projectId).stream()
                .filter(image ->
                        image.getNoteId() == null && image.getUploadedAt().isBefore(cutoffTime))
                .toList();

        for (NoteImage image : oldOrphanedImages) {
            deleteImage(image.getId());
        }

        log.info(
                "Cleaned up {} old orphaned images (older than {} hours) for project {}",
                oldOrphanedImages.size(),
                hoursOld,
                projectId);
    }

    /**
     * Validate image file
     */
    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("Image file is empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("Image file size exceeds maximum allowed size of " + maxFileSize + " bytes");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("File must be an image");
        }

        // Check for common image types
        String[] allowedTypes = {"image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"};
        boolean isValidType = false;
        for (String allowedType : allowedTypes) {
            if (contentType.equals(allowedType)) {
                isValidType = true;
                break;
            }
        }

        if (!isValidType) {
            throw new RuntimeException("Unsupported image type. Allowed types: JPEG, PNG, GIF, WebP");
        }
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
