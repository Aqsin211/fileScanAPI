package az.company.filescanner.service;

import az.company.filescanner.dao.entity.ScanResult;
import az.company.filescanner.dao.repository.ScanResultRepository;
import az.company.filescanner.exception.ScanResultNotFoundException;
import az.company.filescanner.model.mapper.ResultMapper;
import az.company.filescanner.model.response.ScanResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

import static az.company.filescanner.model.enums.StatusEnums.CLEAN;
import static az.company.filescanner.model.enums.StatusEnums.ERROR;
import static az.company.filescanner.model.enums.StatusEnums.PENDING;
import static az.company.filescanner.model.enums.StatusEnums.SCAN_RESULT_NOT_FOUND;

@Slf4j
@Service
public class FileScanService {

    private final ClamAVClientService clamAVClientService;
    private final ScanResultRepository scanResultRepository;
    private final MinIOStorageService minIOStorageService;
    private final EmailNotificationService emailService;

    @Value("${minio.bucket.temp}")
    private String tempBucket;

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

    /**
     * Starts the file scan asynchronously.
     * Saves initial ScanResult with status PENDING.
     * Uploads file to temp bucket.
     * Launches async scan.
     * Returns the generated scanId for client polling.
     */
    public String startScan(MultipartFile file) {
        String scanId = UUID.randomUUID().toString();
        String originalFilename = file.getOriginalFilename();

        saveInitialPendingScanResult(scanId, originalFilename);
        uploadToTempBucket(scanId, file);
        scanAsync(scanId, originalFilename);

        return scanId;
    }

    private void saveInitialPendingScanResult(String scanId, String filename) {
        ScanResult pending = ScanResult.builder()
                .scanId(scanId)
                .filename(filename)
                .status(PENDING.getMessage())
                .scannedAt(null)
                .build();
        scanResultRepository.save(pending);
    }

    private void uploadToTempBucket(String scanId, MultipartFile file) {
        try {
            minIOStorageService.uploadFile(tempBucket, scanId, file);
            log.info("File [{}] uploaded to temp bucket [{}]", scanId, tempBucket);
        } catch (Exception e) {
            log.error("Failed to upload file to temp bucket for scanId {}", scanId, e);
            updateScanResultWithError(scanId, "Failed to upload file: " + e.getMessage());
            throw new RuntimeException("Failed to upload file to temp storage", e);
        }
    }

    /**
     * Async scanning logic.
     * Downloads file from temp bucket,
     * scans it with ClamAV,
     * moves to clean or quarantine bucket,
     * updates DB ScanResult accordingly,
     * deletes from temp bucket.
     */
    @Async("scanExecutor")
    public void scanAsync(String scanId, String originalFilename) {
        try {
            MultipartFile file = minIOStorageService.downloadFile(tempBucket, scanId);

            ScanResult scannedResult = clamAVClientService.scanFile(file.getInputStream(), originalFilename);

            if (CLEAN.getMessage().equals(scannedResult.getStatus())) {
                minIOStorageService.uploadFile(cleanBucket, scanId, file);
                log.info("File [{}] moved to clean bucket [{}]", scanId, cleanBucket);
            } else {
                minIOStorageService.uploadFile(quarantineBucket, scanId, file);
                log.info("File [{}] moved to quarantine bucket [{}]", scanId, quarantineBucket);
                emailService.sendVirusAlert(scanId, scannedResult.getVirusName());
            }

            updateScanResult(scanId, scannedResult.getStatus(), scannedResult.getVirusName());

            minIOStorageService.deleteFile(tempBucket, scanId);
            log.info("File [{}] deleted from temp bucket [{}]", scanId, tempBucket);

        } catch (Exception e) {
            log.error("Scan failed for scanId {}", scanId, e);
            updateScanResultWithError(scanId, e.getMessage());
        }
    }

    private void updateScanResult(String scanId, String status, String virusName) {
        ScanResult existingResult = scanResultRepository.findByScanId(scanId)
                .orElseThrow(() -> new RuntimeException("ScanResult not found for scanId " + scanId));

        existingResult.setStatus(status);
        existingResult.setVirusName(virusName);
        existingResult.setScannedAt(LocalDateTime.now());

        scanResultRepository.save(existingResult);
        log.info("ScanResult updated for scanId {} with status {}", scanId, status);
    }

    private void updateScanResultWithError(String scanId, String errorMsg) {
        ScanResult existing = scanResultRepository.findByScanId(scanId)
                .orElseGet(() -> ScanResult.builder().scanId(scanId).build());

        existing.setStatus(ERROR.getMessage());
        existing.setVirusName(errorMsg);
        existing.setScannedAt(LocalDateTime.now());

        scanResultRepository.save(existing);
        log.info("ScanResult updated with error for scanId {}: {}", scanId, errorMsg);
    }

    // For client to check scan result
    public ScanResultDTO getScanResult(String scanId) {
        return scanResultRepository.findByScanId(scanId)
                .map(ResultMapper::mapEntityToResponse)
                .orElseThrow(() -> new ScanResultNotFoundException(
                        String.format(SCAN_RESULT_NOT_FOUND.getMessage(), scanId)
                ));
    }

    public void deleteFile(String scanId) {
        ScanResult scanResult = scanResultRepository.findByScanId(scanId)
                .orElseThrow(() -> new ScanResultNotFoundException("ScanResult not found: " + scanId));

        String bucketName = getBucketNameByStatus(scanResult.getStatus());
        minIOStorageService.deleteFile(bucketName, scanId);
        scanResultRepository.delete(scanResult);
        log.info("ScanResult and file deleted for scanId {}", scanId);
    }

    private String getBucketNameByStatus(String status) {
        switch (status.toUpperCase()) {
            case "PENDING":
            case "UNKNOWN":
            case "ERROR":
                return tempBucket;
            case "CLEAN":
                return cleanBucket;
            case "INFECTED":
                return quarantineBucket;
            default:
                throw new IllegalStateException("Unknown scan status: " + status);
        }
    }
}
