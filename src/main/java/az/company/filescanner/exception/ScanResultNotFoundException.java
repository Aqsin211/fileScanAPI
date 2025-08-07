package az.company.filescanner.exception;

public class ScanResultNotFoundException extends RuntimeException {
    public ScanResultNotFoundException(String message) {
        super(message);
    }
}
