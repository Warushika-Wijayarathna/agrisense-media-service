package com.agrisense.mediaservice.media;

import java.time.Instant;

public record MediaResponse(
        Long id,
        String fileName,
        String contentType,
        long size,
        String url,
        Instant uploadedAt
) {

    public static MediaResponse from(MediaAsset mediaAsset) {
        return new MediaResponse(
                mediaAsset.getId(),
                mediaAsset.getOriginalFileName(),
                mediaAsset.getContentType(),
                mediaAsset.getSize(),
                mediaAsset.getUrl(),
                mediaAsset.getUploadedAt()
        );
    }
}
