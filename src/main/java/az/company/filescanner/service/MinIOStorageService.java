package az.company.filescanner.service;

import io.minio.*;
import io.minio.errors.MinioException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Slf4j
@Service
public class MinIOStorageService {

    private final MinioClient minioClient;

    public MinIOStorageService(
            @Value("${minio.url:}") String url,
            @Value("${minio.access-key:}") String accessKey,
            @Value("${minio.secret-key:}") String secretKey
    ) {
        this.minioClient = MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();
    }

    public void uploadFile(String bucketName, String objectName, MultipartFile file) {
        try {
            ensureBucketExists(bucketName);

            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );
                log.info("File [{}] uploaded to bucket [{}]", objectName, bucketName);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to MinIO: " + objectName, e);
        }
    }

    public MultipartFile downloadFile(String bucketName, String objectName) {
        try {
            GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build();

            try (InputStream stream = minioClient.getObject(getObjectArgs);
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = stream.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                byte[] content = baos.toByteArray();

                return new InMemoryMultipartFile(objectName, content);

            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to download file from MinIO: " + objectName, e);
        }
    }

    public void deleteFile(String bucketName, String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            log.info("File [{}] deleted from bucket [{}]", objectName, bucketName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file from MinIO: " + objectName, e);
        }
    }

    private void ensureBucketExists(String bucketName) {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );

            if (!exists) {
                try {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                    log.info("Bucket [{}] created", bucketName);
                } catch (MinioException e) {
                    if (e.getMessage().contains("Bucket already exists")) {
                        log.warn("Bucket [{}] already exists (race condition)", bucketName);
                    } else {
                        throw e;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Bucket check/creation failed for: " + bucketName, e);
        }
    }

    // In-memory MultipartFile implementation for downloaded bytes
    private static class InMemoryMultipartFile implements MultipartFile {

        private final String name;
        private final byte[] content;

        public InMemoryMultipartFile(String name, byte[] content) {
            this.name = name;
            this.content = content;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return name;
        }

        @Override
        public String getContentType() {
            String type = java.net.URLConnection.guessContentTypeFromName(name);
            return type != null ? type : "application/octet-stream";
        }

        @Override
        public boolean isEmpty() {
            return content == null || content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content;
        }

        @Override
        public InputStream getInputStream() {
            return new java.io.ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(java.io.File dest) {
            throw new UnsupportedOperationException("transferTo not supported");
        }
    }
}
