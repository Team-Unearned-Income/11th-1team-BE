package org.example.knockin.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import jakarta.validation.Validation;
import org.example.knockin.board.dto.BoardPreSignedDto;
import org.example.knockin.global.exception.BusinessException;
import org.example.knockin.global.exception.FileErrorCode;
import org.example.knockin.meta.dto.PresignedUrlDto;
import org.example.knockin.meta.service.impl.FileUploadServiceImpl;
import org.example.knockin.meta.service.impl.LocalFileUploadServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

class PresignedFileUploadTest {
    private static final String KEY = "uploads/42/12345678-1234-1234-1234-123456789abc.jpg";
    private final S3Client s3 = mock(S3Client.class);
    private S3Presigner signer;
    private FileUploadServiceImpl service;

    @BeforeEach
    void setUp() {
        signer = S3Presigner.builder().region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test-key", "test-secret")))
                .endpointOverride(URI.create("https://example.r2.cloudflarestorage.com")).build();
        service = new FileUploadServiceImpl(s3, signer);
        ReflectionTestUtils.setField(service, "bucket", "test-bucket");
        ReflectionTestUtils.setField(service, "publicUrl", "https://images.example.com");
    }

    @AfterEach
    void tearDown() { signer.close(); }

    @Test
    void returnsSignedPutUrlPublicUrlAndOwnedKeyInCorrectFields() {
        var request = new PresignedUrlDto.Request(List.of(new PresignedUrlDto.Request.FileInfo("방.jpeg", "image/jpeg")));
        var file = service.presignedUploadUrl(request, 42L).getFileUploadInfoList().getFirst();
        assertThat(file.getFileKey()).startsWith("uploads/42/").endsWith(".jpg");
        assertThat(file.getFileUrl()).isEqualTo("https://images.example.com/" + file.getFileKey());
        assertThat(file.getFilename()).isEqualTo("방.jpeg");
        assertThat(URLDecoder.decode(file.getUploadUrl(), StandardCharsets.UTF_8))
                .contains("r2.cloudflarestorage.com", "X-Amz-Signature=", "X-Amz-Expires=900", "content-type;host");
        verifyNoInteractions(s3);
    }

    @Test
    void acceptsUploadedImageWithoutUploadingBytesAgain() {
        when(s3.headObject(any(HeadObjectRequest.class))).thenReturn(
                HeadObjectResponse.builder().contentType("image/jpeg").contentLength(100L).build());
        assertThat(service.getUploadedImageUrl(KEY, 42L)).isEqualTo("https://images.example.com/" + KEY);
        verify(s3).headObject(HeadObjectRequest.builder().bucket("test-bucket").key(KEY).build());
        verifyNoMoreInteractions(s3);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "uploads/presigned/43/12345678-1234-1234-1234-123456789abc.jpg",
            "uploads/presigned/42/../43/12345678-1234-1234-1234-123456789abc.jpg",
            "https://images.example.com/image.jpg", "uploads/legacy.jpg"})
    void rejectsOtherMembersAndArbitraryKeysBeforeCallingR2(String key) {
        assertThatThrownBy(() -> service.getUploadedImageUrl(key, 42L)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(s3);
    }

    @Test
    void missingUploadIsNotFound() {
        when(s3.headObject(any(HeadObjectRequest.class))).thenThrow(S3Exception.builder().statusCode(404).build());
        assertThatThrownBy(() -> service.getUploadedImageUrl(KEY, 42L))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(FileErrorCode.FILE_NOT_FOUND));
    }

    @Test
    void rejectsEmptyOversizeAndMismatchedContentType() {
        for (var head : List.of(
                HeadObjectResponse.builder().contentLength(0L).contentType("image/jpeg").build(),
                HeadObjectResponse.builder().contentLength(52428801L).contentType("image/jpeg").build(),
                HeadObjectResponse.builder().contentLength(100L).contentType("image/png").build())) {
            when(s3.headObject(any(HeadObjectRequest.class))).thenReturn(head);
            assertThatThrownBy(() -> service.getUploadedImageUrl(KEY, 42L))
                    .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(FileErrorCode.FILE_UPLOAD_INVALID));
        }
    }

    @Test
    void localImplementationReportsUnavailableInsteadOfEmptySuccess() {
        var local = new LocalFileUploadServiceImpl();
        assertThatThrownBy(() -> local.presignedUploadUrl(new PresignedUrlDto.Request(), 42L))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(FileErrorCode.FILE_UPLOAD_UNAVAILABLE));
    }

    @ParameterizedTest
    @CsvSource({"pepe.jpeg,image/jpeg,jpg", "pepe.JPG,image/jpeg,jpg", "room.PNG,image/png,png", "room.WEBP,image/webp,webp"})
    void issuedKeyCanBeRegisteredRegardlessOfOriginalExtension(String name, String type, String extension) {
        var request = new PresignedUrlDto.Request(List.of(new PresignedUrlDto.Request.FileInfo(name, type)));
        var issued = service.presignedUploadUrl(request, 42L).getFileUploadInfoList().getFirst();
        when(s3.headObject(any(HeadObjectRequest.class))).thenReturn(
                HeadObjectResponse.builder().contentType(type).contentLength(100L).build());
        assertThat(issued.getFileKey()).endsWith("." + extension);
        assertThat(issued.getFilename()).isEqualTo(name);
        assertThat(service.getUploadedImageUrl(issued.getFileKey(), 42L)).isEqualTo(issued.getFileUrl());
    }

    @Test
    void issuedKeyCannotBeRegisteredByAnotherMember() {
        var request = new PresignedUrlDto.Request(List.of(new PresignedUrlDto.Request.FileInfo("room.jpg", "image/jpeg")));
        var key = service.presignedUploadUrl(request, 42L).getFileUploadInfoList().getFirst().getFileKey();
        assertThatThrownBy(() -> service.getUploadedImageUrl(key, 43L))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(FileErrorCode.FILE_UPLOAD_INVALID));
        verifyNoInteractions(s3);
    }

    @Test
    void maximumLengthOriginalFilenameDoesNotMakeFileKeyTooLong() {
        String name = "a".repeat(496) + ".png";
        var request = new PresignedUrlDto.Request(List.of(new PresignedUrlDto.Request.FileInfo(name, "image/png")));
        var issued = service.presignedUploadUrl(request, 42L).getFileUploadInfoList().getFirst();
        assertThat(issued.getFilename()).hasSize(500);
        assertThat(issued.getFileKey().length()).isLessThan(300);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(request)).isEmpty();
            assertThat(factory.getValidator().validate(
                    new BoardPreSignedDto.Request.FileDto(issued.getFileKey(), name, true))).isEmpty();
        }
        when(s3.headObject(any(HeadObjectRequest.class))).thenReturn(
                HeadObjectResponse.builder().contentType("image/png").contentLength(100L).build());
        assertThat(service.getUploadedImageUrl(issued.getFileKey(), 42L)).isEqualTo(issued.getFileUrl());
    }
}
