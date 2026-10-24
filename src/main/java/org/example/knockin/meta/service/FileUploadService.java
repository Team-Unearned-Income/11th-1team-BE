package org.example.knockin.meta.service;

import java.io.IOException;

import org.example.knockin.meta.dto.PresignedUrlDto;
import org.springframework.web.multipart.MultipartFile;


public interface FileUploadService {
    String uploadImage(MultipartFile file) throws IOException;

    void deleteImage(String savedFileName) throws IOException;

    String getUploadedImageUrl(String fileKey, Long memberId);

    PresignedUrlDto.Response presignedUploadUrl(PresignedUrlDto.Request request, Long memberId);
}
