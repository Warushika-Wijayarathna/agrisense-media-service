package com.agrisense.mediaservice.media;

import com.agrisense.mediaservice.shared.ResourceNotFoundException;
import com.agrisense.mediaservice.shared.StorageException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(name = "media.storage.mode", havingValue = "gcs")
public class GcsStorageService implements FileStorageService {

    private final Storage storage;
    private final StorageProperties storageProperties;

    public GcsStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
        this.storage = buildStorageClient(storageProperties);

        if (!StringUtils.hasText(storageProperties.getBucket())) {
            throw new StorageException("media.storage.bucket must be configured when media.storage.mode=gcs");
        }
    }

    @Override
    public StoredFile store(MultipartFile file) {
        String objectName = buildObjectName(file.getOriginalFilename());
        BlobInfo blobInfo = BlobInfo.newBuilder(storageProperties.getBucket(), objectName)
                .setContentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                .build();

        try (InputStream inputStream = file.getInputStream()) {
            storage.createFrom(blobInfo, inputStream);
            return new StoredFile(objectName, buildPublicUrl(objectName));
        } catch (IOException exception) {
            throw new StorageException("Failed to store file in Google Cloud Storage", exception);
        }
    }

    @Override
    public Resource loadAsResource(String storageFileName) {
        Blob blob = storage.get(BlobId.of(storageProperties.getBucket(), storageFileName));
        if (blob == null || !blob.exists()) {
            throw new ResourceNotFoundException("Stored file not found in Google Cloud Storage: " + storageFileName);
        }

        return new ByteArrayResource(blob.getContent()) {
            @Override
            public String getFilename() {
                int separatorIndex = storageFileName.lastIndexOf("/");
                return separatorIndex >= 0 ? storageFileName.substring(separatorIndex + 1) : storageFileName;
            }
        };
    }

    private Storage buildStorageClient(StorageProperties properties) {
        try {
            StorageOptions.Builder builder = StorageOptions.newBuilder();
            if (StringUtils.hasText(properties.getProjectId())) {
                builder.setProjectId(properties.getProjectId());
            }
            if (StringUtils.hasText(properties.getCredentialsPath())) {
                Path credentialsPath = Paths.get(properties.getCredentialsPath()).toAbsolutePath().normalize();
                try (InputStream inputStream = Files.newInputStream(credentialsPath)) {
                    builder.setCredentials(GoogleCredentials.fromStream(inputStream));
                }
            }
            return builder.build().getService();
        } catch (IOException exception) {
            throw new StorageException("Failed to initialize Google Cloud Storage client", exception);
        }
    }

    private String buildObjectName(String originalFileName) {
        String extension = extractExtension(originalFileName);
        String prefix = normalizePrefix(storageProperties.getObjectPrefix());
        return prefix + UUID.randomUUID() + extension;
    }

    private String buildPublicUrl(String objectName) {
        if (StringUtils.hasText(storageProperties.getPublicBaseUrl())) {
            return storageProperties.getPublicBaseUrl() + "/" + objectName;
        }
        return "https://storage.googleapis.com/" + storageProperties.getBucket() + "/" + objectName;
    }

    private String normalizePrefix(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return "";
        }
        String trimmed = prefix.trim();
        if (trimmed.endsWith("/")) {
            return trimmed;
        }
        return trimmed + "/";
    }

    private String extractExtension(String originalFileName) {
        String safeName = Objects.toString(originalFileName, "");
        int extensionIndex = safeName.lastIndexOf('.');
        return extensionIndex >= 0 ? safeName.substring(extensionIndex) : "";
    }
}


