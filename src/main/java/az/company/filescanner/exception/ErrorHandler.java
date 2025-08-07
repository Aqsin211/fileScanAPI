package az.company.filescanner.exception;

import az.company.filescanner.model.enums.StatusEnums;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(ScanResultNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleScanResultNotFound(ScanResultNotFoundException exception) {
        return ErrorResponse.builder()
                .code(StatusEnums.SCAN_RESULT_NOT_FOUND.getCode())
                .message(exception.getMessage())
                .status(HttpStatus.NOT_FOUND.value())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGenericException(Exception exception) {
        log.error("Unhandled exception caught", exception);
        return ErrorResponse.builder()
                .code(StatusEnums.UNEXPECTED_ERROR.getCode())
                .message(StatusEnums.UNEXPECTED_ERROR.getMessage())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timestamp(LocalDateTime.now())
                .build();
    }

}
