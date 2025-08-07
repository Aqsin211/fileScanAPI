package az.company.filescanner.model.mapper;

import az.company.filescanner.dao.entity.ScanResult;
import az.company.filescanner.model.response.ScanResultDTO;

public class ResultMapper {

    public static ScanResultDTO mapEntityToResponse(ScanResult scanResult) {
        return ScanResultDTO.builder()
                .filename(scanResult.getFilename())
                .scanId(scanResult.getScanId())
                .virusName(scanResult.getVirusName())
                .scannedAt(scanResult.getScannedAt())
                .status(scanResult.getStatus())
                .build();
    }
}
