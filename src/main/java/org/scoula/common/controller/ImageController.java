package org.scoula.common.controller;

import lombok.RequiredArgsConstructor;
import org.scoula.common.dto.ImageUploadResponse;
import org.scoula.common.service.S3ImageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {
    private final S3ImageService s3ImageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ImageUploadResponse> upload(
            @RequestParam(value = "file", required = false) MultipartFile file) {
        String key = s3ImageService.upload(file);
        String url = s3ImageService.createPresignedGetUrl(key);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ImageUploadResponse(key, url));
    }
}
