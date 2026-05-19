package com.bsuir.book_store.shared.storage;

import com.bsuir.book_store.shared.exception.DomainException;
import io.minio.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@org.springframework.context.annotation.Profile("!test")
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket.name}")
    private String bucketName;

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.public-url}")
    private String minioPublicUrl;

    @PostConstruct
    public void init() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                
                String policy = """
                        {
                          "Statement": [
                            {
                              "Action": "s3:GetObject",
                              "Effect": "Allow",
                              "Principal": "*",
                              "Resource": "arn:aws:s3:::%s/*"
                            }
                          ],
                          "Version": "2012-10-17"
                        }
                        """.formatted(bucketName);
                
                minioClient.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucketName).config(policy).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error initializing MinIO", e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        try {
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            return store(file.getInputStream(), filename, file.getContentType(), file.getSize());
        } catch (Exception e) {
            throw new DomainException("Ошибка при сохранении файла", e);
        }
    }

    @Override
    public String store(InputStream inputStream, String filename, String contentType, long size) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(filename)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build());
            return minioPublicUrl + "/" + bucketName + "/" + filename;
        } catch (Exception e) {
            throw new DomainException("Ошибка при загрузке потока в MinIO", e);
        }
    }

    @Override
    public void delete(String url) {
        if (url == null || url.isBlank()) return;
        try {
            String prefix = minioPublicUrl + "/" + bucketName + "/";
            String altPrefix = minioUrl + "/" + bucketName + "/";
            String objectName;
            if (url.startsWith(prefix)) {
                objectName = url.substring(prefix.length());
            } else if (url.startsWith(altPrefix)) {
                objectName = url.substring(altPrefix.length());
            } else {
                return;
            }
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
        } catch (Exception e) {
            throw new DomainException("Ошибка при удалении файла из MinIO", e);
        }
    }
}