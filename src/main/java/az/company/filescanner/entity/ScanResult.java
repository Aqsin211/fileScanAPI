package az.company.filescanner.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class ScanResult {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;
    String filename;
    String status; // CLEAN, INFECTED, ERROR
    String virusName; // If there will be available virus name
    LocalDateTime scannedAt;
}
