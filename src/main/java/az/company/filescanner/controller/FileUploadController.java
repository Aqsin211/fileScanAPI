package az.company.filescanner.controller;

import az.company.filescanner.model.response.ScanIdResponse;
import az.company.filescanner.model.response.ScanResultDTO;
import az.company.filescanner.service.FileScanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static az.company.filescanner.model.enums.CrudMessages.OPERATION_DELETED;

@RestController
@RequestMapping("/filescan")
public class FileUploadController {

    private final FileScanService fileScanService;

    public FileUploadController(FileScanService fileScanService) {
        this.fileScanService = fileScanService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ScanIdResponse> upload(@RequestParam("file") MultipartFile file) {
        String scanId = fileScanService.startScan(file);
        return ResponseEntity.accepted().body(new ScanIdResponse(scanId));
    }


    @GetMapping("/status/{scanId}")
    public ResponseEntity<ScanResultDTO> checkStatus(@PathVariable String scanId) {
        return ResponseEntity.ok(fileScanService.getScanResult(scanId));
    }

    @DeleteMapping("/delete/{scanId}")
    public ResponseEntity<String> deleteFile(@PathVariable String scanId) {
        fileScanService.deleteFile(scanId);
        return ResponseEntity.ok(OPERATION_DELETED.getMessage());
    }

}
