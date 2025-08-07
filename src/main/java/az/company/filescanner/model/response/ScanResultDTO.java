package az.company.filescanner.model.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScanResultDTO {
    String scanId;
    String filename;
    String status;
    String virusName;
    LocalDateTime scannedAt;
}
