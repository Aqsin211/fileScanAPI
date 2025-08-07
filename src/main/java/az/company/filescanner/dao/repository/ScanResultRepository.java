package az.company.filescanner.dao.repository;

import az.company.filescanner.dao.entity.ScanResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScanResultRepository extends JpaRepository<ScanResult, Long> {
    Optional<ScanResult> findByScanId(String scanId);
}
