package org.example.knockin.meta.service.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.FileErrorCode;
import org.example.knockin.meta.dto.PresignedUrlDto;
import org.example.knockin.meta.service.FileUploadService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


@Service
@RequiredArgsConstructor
@Profile("!prod")
public class LocalFileUploadServiceImpl implements FileUploadService {
    private final String uploadDir = System.getProperty("user.dir") + "/local_uploads/";

    public String uploadImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(FileErrorCode.FILE_EMPTY);
        }

        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
        }

        String fileName = UUID.randomUUID() + extension;
        Path filePath = Paths.get(uploadDir, fileName);
        file.transferTo(filePath.toFile());

        return fileName;
    }

    @Override
    public void deleteImage(String savedFileName) throws IOException {
        if (savedFileName == null || savedFileName.isBlank()) {
            return;
        }

        Path filePath = Paths.get(uploadDir, savedFileName).normalize();
        Path uploadPath = Paths.get(uploadDir).normalize();
        if (!filePath.startsWith(uploadPath)) {
            throw new IllegalArgumentException("Invalid file path");
        }

        Files.deleteIfExists(filePath);
    }

    @Override
    public PresignedUrlDto.Response presignedUploadUrl(PresignedUrlDto.Request request, Long memberId) {
        throw new BusinessException(FileErrorCode.FILE_UPLOAD_UNAVAILABLE);
    }
    @Override
    public String getUploadedImageUrl(String fileKey, Long memberId) {
        throw new BusinessException(FileErrorCode.FILE_UPLOAD_UNAVAILABLE);
    }
}
