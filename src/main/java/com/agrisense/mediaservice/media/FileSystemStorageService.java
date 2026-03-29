package com.agrisense.mediaservice.media;

import com.agrisense.mediaservice.shared.StorageException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(name = "media.storage.mode", havingValue = "filesystem", matchIfMissing = true)
public class FileSystemStorageService implements FileStorageService {

    private final Path uploadPath;
    private final StorageProperties storageProperties;

    public FileSystemStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
        this.uploadPath = Paths.get(storageProperties.getUploadDir()).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.uploadPath);
        } catch (IOException exception) {
            throw new StorageException("Failed to initialize upload directory", exception);
        }
    }

    @Override
    public StoredFile store(MultipartFile file) {
        String extension = extractExtension(file.getOriginalFilename());
        String storageFileName = UUID.randomUUID() + extension;

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, uploadPath.resolve(storageFileName), StandardCopyOption.REPLACE_EXISTING);
            String publicUrl = storageProperties.getPublicBaseUrl() + "/" + storageFileName;
            return new StoredFile(storageFileName, publicUrl);
        } catch (IOException exception) {
            throw new StorageException("Failed to store file", exception);
        }
    }

    @Override
    public Resource loadAsResource(String storageFileName) {
        try {
            Path filePath = uploadPath.resolve(storageFileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new StorageException("Stored file is not accessible: " + storageFileName);
            }
            return resource;
        } catch (MalformedURLException exception) {
            throw new StorageException("Failed to read stored file", exception);
        }
    }

    private String extractExtension(String originalFileName) {
        String safeName = Objects.toString(originalFileName, "");
        int extensionIndex = safeName.lastIndexOf('.');
        return extensionIndex >= 0 ? safeName.substring(extensionIndex) : "";
    }
}
