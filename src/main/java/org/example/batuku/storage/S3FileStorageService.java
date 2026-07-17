package org.example.batuku.storage;

import org.example.batuku.config.StorageProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

@Service
@ConditionalOnProperty(name = "batuku.storage.type", havingValue = "s3")
public class S3FileStorageService implements FileStorageService {

    private final StorageProperties props;
    private final S3Client s3Client;
    private final S3Presigner presigner;

    public S3FileStorageService(StorageProperties props) {
        this.props = props;
        StorageProperties.S3 s3 = props.getS3();

        AwsBasicCredentials credentials = AwsBasicCredentials.create(s3.getAccessKey(), s3.getSecretKey());

        S3Configuration s3Config = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build();

        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(s3.getEndpoint()))
                .region(Region.of(s3.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(s3Config)
                .build();

        this.presigner = S3Presigner.builder()
                .endpointOverride(URI.create(s3.getEndpoint()))
                .region(Region.of(s3.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(s3Config)
                .build();
    }

    @Override
    public String store(MultipartFile file, FileCategory category) {
        String key = FileKeyGenerator.generate(category, file.getOriginalFilename());
        String bucket = bucket(category);
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();
            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
            throw new RuntimeException("Erro ao enviar ficheiro para S3.", e);
        }
        return key;
    }

    @Override
    public String resolveUrl(String key, FileCategory category) {
        if (category == FileCategory.AVATAR) {
            return props.getS3().getAvatarPublicBaseUrl() + "/" + key;
        }
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket(category))
                .key(key)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(props.getS3().getAudioPresignMinutes()))
                .getObjectRequest(getObjectRequest)
                .build();
        PresignedGetObjectRequest presigned = presigner.presignGetObject(presignRequest);
        return presigned.url().toString();
    }

    @Override
    public void delete(String key, FileCategory category) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucket(category))
                .key(key)
                .build();
        s3Client.deleteObject(deleteRequest);
    }

    private String bucket(FileCategory category) {
        return category == FileCategory.AVATAR
                ? props.getS3().getAvatarBucket()
                : props.getS3().getAudioBucket();
    }
}
