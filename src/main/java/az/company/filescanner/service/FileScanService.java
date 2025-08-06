package az.company.filescanner.service;

import az.company.filescanner.entity.ScanResult;
import az.company.filescanner.repository.ScanResultRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class FileScanService {

    private final ClamAVClientService clamAVClientService;
    private final ScanResultRepository scanResultRepository;
    private final MinIOStorageService minIOStorageService;
    private final EmailNotificationService emailService;

    @Value("${minio.bucket.clean}")
    private String cleanBucket;

    @Value("${minio.bucket.quarantine}")
    private String quarantineBucket;

    public FileScanService(
            ClamAVClientService clamAVClientService,
            ScanResultRepository scanResultRepository,
            MinIOStorageService minIOStorageService,
            EmailNotificationService emailService
    ) {
        this.clamAVClientService = clamAVClientService;
        this.scanResultRepository = scanResultRepository;
        this.minIOStorageService = minIOStorageService;
        this.emailService = emailService;
    }

    @Async("scanExecutor")
    public void scanAndStore(MultipartFile file) {
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        boolean isClean;
        String virusName = null;

        try (InputStream inputStream = file.getInputStream()) {
            isClean = clamAVClientService.isFileClean(inputStream);
        } catch (Exception e) {
            logResult(fileName, "ERROR", null);
            throw new RuntimeException("Failed to scan file: " + fileName, e);
        }

        try {
            if (isClean) {
                minIOStorageService.uploadFile(cleanBucket, fileName, file);
                logResult(fileName, "CLEAN", null);
            } else {
                minIOStorageService.uploadFile(quarantineBucket, fileName, file);
                virusName = "UNKNOWN"; // ClamAV basic response doesn't return virus name unless parsed manually
                logResult(fileName, "INFECTED", virusName);
                emailService.sendVirusAlert(fileName, virusName);
            }
        } catch (Exception e) {
            throw new RuntimeException("File storage failed: " + fileName, e);
        }
    }

    private void logResult(String fileName, String status, String virusName) {
        ScanResult result = new ScanResult();
        result.setFilename(fileName);
        result.setStatus(status);
        result.setVirusName(virusName);
        result.setScannedAt(LocalDateTime.now());
        scanResultRepository.save(result);
    }
}