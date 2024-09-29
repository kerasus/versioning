package com.ada.versioning.storage.impl;

import com.ada.versioning.storage.UserConfigStorage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.PutObjectArgs;
import io.minio.ObjectWriteResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream; // Import added
import java.io.ByteArrayOutputStream; // Import added
import java.io.InputStream;
import java.util.Optional;

@Component
public class MinIOUserConfigStorage implements UserConfigStorage {

    private final MinioClient minioClient;
    private final String bucketName;
    private final ObjectMapper objectMapper;

    public MinIOUserConfigStorage(MinioClient minioClient,
                                  @Value("${minio.bucket.name}") String bucketName,
                                  ObjectMapper objectMapper) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
        this.objectMapper = objectMapper;

        // Ensure the bucket exists
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to ensure MinIO bucket exists", e);
        }
    }

    @Override
    public Optional<JsonNode> getUserConfig(String userId, Optional<String> version) {
        try {
            String objectName = userId + "/config.json";

            // Retrieve the config file as InputStream
            InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );

            return Optional.of(objectMapper.readTree(inputStream));
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public JsonNode saveUserConfig(String userId, JsonNode config) {
        try {
            String objectName = userId + "/config.json";

            // Save the config
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            objectMapper.writeValue(byteArrayOutputStream, config);

            // Convert ByteArrayOutputStream to InputStream
            ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

            ObjectWriteResponse response = minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, byteArrayOutputStream.size(), -1) // Use inputStream directly
                            .build()
            );

            return config;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to save user config", e);
        }
    }
}
