package com.agrisense.mediaservice.media;

import com.agrisense.mediaservice.shared.ResourceNotFoundException;
import com.agrisense.mediaservice.shared.StorageException;
import java.time.Instant;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class MediaService {

    private final MediaAssetRepository mediaAssetRepository;
    private final FileStorageService fileStorageService;

    public MediaService(MediaAssetRepository mediaAssetRepository, FileStorageService fileStorageService) {
        this.mediaAssetRepository = mediaAssetRepository;
        this.fileStorageService = fileStorageService;
    }

    public MediaResponse upload(MultipartFile file) {
        if (file.isEmpty()) {
            throw new StorageException("Uploaded file must not be empty");
        }

        StoredFile storedFile = fileStorageService.store(file);

        MediaAsset mediaAsset = new MediaAsset();
        mediaAsset.setOriginalFileName(file.getOriginalFilename() == null ? "uploaded-file" : file.getOriginalFilename());
        mediaAsset.setStorageFileName(storedFile.storageFileName());
        mediaAsset.setContentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
        mediaAsset.setSize(file.getSize());
        mediaAsset.setUrl(storedFile.url());
        mediaAsset.setUploadedAt(Instant.now());

        return MediaResponse.from(mediaAssetRepository.save(mediaAsset));
    }

    @Transactional(readOnly = true)
    public List<MediaResponse> listMedia() {
        return mediaAssetRepository.findAllByOrderByUploadedAtDesc()
                .stream()
                .map(MediaResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MediaResponse getMedia(Long id) {
        return mediaAssetRepository.findById(id)
                .map(MediaResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Media asset not found for id: " + id));
    }

    @Transactional(readOnly = true)
    public Resource getMediaFile(Long id) {
        MediaAsset mediaAsset = mediaAssetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Media asset not found for id: " + id));
        return fileStorageService.loadAsResource(mediaAsset.getStorageFileName());
    }
}
