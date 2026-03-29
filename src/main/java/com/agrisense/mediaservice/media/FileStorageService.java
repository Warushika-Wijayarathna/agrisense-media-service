package com.agrisense.mediaservice.media;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    StoredFile store(MultipartFile file);

    Resource loadAsResource(String storageFileName);
}
