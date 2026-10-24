package org.example.knockin.meta.service.impl;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.model.*;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.FileErrorCode;
import org.example.knockin.meta.dto.PresignedUrlDto;
import org.example.knockin.meta.service.FileUploadService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;


@Service
@RequiredArgsConstructor
@Profile("prod")
public class FileUploadServiceImpl implements FileUploadService {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private static final int EXPIRE_MINUTES = 15;
    private static final long MAX_IMAGE_BYTES = 50L * 1024 * 1024;
    private static final Map<String, String> IMAGE_EXTENSIONS = Map.of("image/jpeg", "jpg", "image/png", "png", "image/webp", "webp", "image/gif", "gif");

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.public-url}")
    private String publicUrl;

    @Override
    public String uploadImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(FileErrorCode.FILE_EMPTY);
        }
        String type = file.getContentType();

        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename,type);
        String key = "uploads/"+ UUID.randomUUID()+extension;

        PutObjectRequest req = PutObjectRequest.builder().bucket(bucket).key(key).contentType(type).build();

        s3Client.putObject(req, RequestBody.fromBytes(file.getBytes()));

        return publicUrl+"/"+key;
    }

    @Override
    public void deleteImage(String savedFileName) {
        if (savedFileName == null || savedFileName.isBlank()) {
            return;
        }

        String key = savedFileName;
        String prefix = publicUrl + "/";
        if (savedFileName.startsWith(prefix)) {
            key = savedFileName.substring(prefix.length());
        }

        DeleteObjectRequest req = DeleteObjectRequest.builder().bucket(bucket).key(key).build();
        s3Client.deleteObject(req);
    }

    @Override
    public PresignedUrlDto.Response presignedUploadUrl(PresignedUrlDto.Request request, Long memberId) {
        if (memberId == null || memberId <= 0 || request.getFileInfoList() == null || request.getFileInfoList().isEmpty() || request.getFileInfoList().size() > 10) {
            throw new BusinessException(FileErrorCode.FILE_UPLOAD_INVALID);
        }

        List<PresignedUrlDto.Response.FileUploadInfo> items = request.getFileInfoList().stream().map(file -> {
            String extension = file == null || file.getContentType() == null ? null : IMAGE_EXTENSIONS.get(file.getContentType());
            if (extension == null || file.getOriginalFilename() == null || file.getOriginalFilename().isBlank() || file.getOriginalFilename().length() > 500) {
                throw new BusinessException(FileErrorCode.FILE_UPLOAD_INVALID);
            }

            String fileKey = "uploads/" + memberId + "/" + UUID.randomUUID() + "." + extension;
            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(bucket).key(fileKey).contentType(file.getContentType()).build();
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(EXPIRE_MINUTES)).putObjectRequest(objectRequest).build();
            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
            return PresignedUrlDto.Response.FileUploadInfo.builder().filename(file.getOriginalFilename()).uploadUrl(presignedRequest.url().toString()).fileUrl(publicUrl + "/" + fileKey).fileKey(fileKey).build();
        }).toList();

        return PresignedUrlDto.Response.builder().fileUploadInfoList(items).build();
    }

    @Override
    public String getUploadedImageUrl(String fileKey, Long memberId) {
        String prefix = "uploads/" + memberId + "/";
        if (memberId == null || memberId <= 0 || fileKey == null || !fileKey.startsWith(prefix) || !fileKey.substring(prefix.length()).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp|gif)")) {
            throw new BusinessException(FileErrorCode.FILE_UPLOAD_INVALID);
        }

        try {
            HeadObjectResponse head = s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(fileKey).build());
            String extension = head.contentType() == null ? null : IMAGE_EXTENSIONS.get(head.contentType());

            if (head.contentLength() == null || head.contentLength() <= 0 || head.contentLength() > MAX_IMAGE_BYTES || extension == null || !fileKey.endsWith("." + extension)) {
                throw new BusinessException(FileErrorCode.FILE_UPLOAD_INVALID);
            }

            return publicUrl.replaceAll("/+$", "") + "/" + fileKey;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new BusinessException(FileErrorCode.FILE_NOT_FOUND);
            }

            throw new BusinessException(FileErrorCode.FILE_UPLOAD_FAILED);
        } catch (SdkException e) {
            throw new BusinessException(FileErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private String extractExtension(String originalFileName, String type){
        if (originalFileName != null && originalFileName.contains(".")) {
            return originalFileName.substring(originalFileName.lastIndexOf('.')).toLowerCase();
        }

        return switch (type) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> "";
        };
    }
}
