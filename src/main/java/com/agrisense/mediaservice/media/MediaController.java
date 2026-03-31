package com.agrisense.mediaservice.media;

import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/media")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaResponse> upload(@RequestParam("file") @NotNull MultipartFile file) {
        MediaResponse response = mediaService.upload(file);
        return ResponseEntity.created(URI.create("/media/" + response.id())).body(response);
    }

    @GetMapping
    public List<MediaResponse> listMedia() {
        return mediaService.listMedia();
    }

    @GetMapping("/{id}")
    public MediaResponse getMedia(@PathVariable("id") Long id) {
        return mediaService.getMedia(id);
    }

    @GetMapping("/files/{id}")
    public ResponseEntity<Resource> getMediaFile(@PathVariable("id") Long id) {
        MediaResponse media = mediaService.getMedia(id);
        Resource resource = mediaService.getMediaFile(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + media.fileName() + "\"")
                .contentType(MediaType.parseMediaType(media.contentType()))
                .body(resource);
    }
}
