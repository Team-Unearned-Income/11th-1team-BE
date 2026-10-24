package org.example.knockin.meta.service.impl;

import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.knockin.meta.dto.PresignedUrlDto;
import org.example.knockin.meta.entity.File;
import org.example.knockin.meta.entity.FileType;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.FileErrorCode;
import org.example.knockin.meta.repository.FileRepository;
import org.example.knockin.meta.service.FileService;
import org.example.knockin.meta.service.FileUploadService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileUploadService fileUploadService;
    private final FileRepository fileRepository;

    @Override
    public File upload(MultipartFile multipartFile, FileType type) throws IOException {
        String extension = getExtension(multipartFile);

        String savedFileName = fileUploadService.uploadImage(multipartFile);

        return File.builder()
                .type(type)
                .originalFileName(multipartFile.getOriginalFilename())
                .savedFileName(savedFileName)
                .fileExt(extension)
                .build();
    }

    @Override
    public File save(MultipartFile multipartFile, FileType type) throws IOException {
        return fileRepository.save(upload(multipartFile, type));
    }

    @Override
    public File findBySavedFileNameAndType(String savedFileName, FileType type) {
        return fileRepository.findBySavedFileNameAndType(savedFileName, type)
                .orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));
    }

    @Override
    public void deleteAll(List<File> files) {
        for (File file : files) {
            try {
                fileUploadService.deleteImage(file.getSavedFileName());
            } catch (IOException | RuntimeException ignored) {
                // 보상 삭제 실패는 원래 예외를 가리지 않도록 삼킨다.
            }
        }
    }

    @Override
    @Transactional
    public PresignedUrlDto.Response presignedUploadUrl(PresignedUrlDto.Request request, Long memberId) {
        return fileUploadService.presignedUploadUrl(request, memberId);
    }

    @Override
    public File savePresigned(String fileKey, String originalFilename, Long memberId, FileType type) {
        if (originalFilename == null || originalFilename.isBlank() || originalFilename.length() > 500) {
            throw new BusinessException(FileErrorCode.FILE_UPLOAD_INVALID);
        }
        String fileUrl = fileUploadService.getUploadedImageUrl(fileKey, memberId);
        return fileRepository.save(File.builder().type(type).originalFileName(originalFilename)
                .savedFileName(fileUrl).fileExt(fileKey.substring(fileKey.lastIndexOf('.') + 1)).build());
    }

    private String getExtension(MultipartFile multipartFile) {
        String fileName = multipartFile.getOriginalFilename();

        if (fileName == null || !fileName.contains(".")) {
            throw new BusinessException(FileErrorCode.FILE_EXTENSION_MISSING);
        }

        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
}
