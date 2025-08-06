package az.company.filescanner.exception;

public class VirusFoundException extends RuntimeException {
    public VirusFoundException(String message) {
        super(message);
    }
}
