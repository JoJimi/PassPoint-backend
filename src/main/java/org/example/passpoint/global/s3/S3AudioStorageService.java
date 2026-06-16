package org.example.passpoint.global.s3;

import lombok.RequiredArgsConstructor;
import org.example.passpoint.domain.answer.dto.response.AudioPresignedUrlResponse;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3AudioStorageService {

    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

    public AudioPresignedUrlResponse generateUploadPresignedUrl(String fileExtension) {
        String key = "answers/audio/" + UUID.randomUUID() + "." + fileExtension;

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(s3Properties.bucket())
                .key(key)
                .build();

        var presigned = s3Presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(s3Properties.presignedUrlExpiryMinutes()))
                        .putObjectRequest(putRequest)
                        .build()
        );

        return new AudioPresignedUrlResponse(presigned.url().toString(), key);
    }
}
