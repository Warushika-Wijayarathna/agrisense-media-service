package com.agrisense.mediaservice.media;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {

    Optional<MediaAsset> findByStorageFileName(String storageFileName);

    List<MediaAsset> findAllByOrderByUploadedAtDesc();
}
