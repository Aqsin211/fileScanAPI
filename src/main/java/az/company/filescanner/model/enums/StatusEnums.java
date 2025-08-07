package az.company.filescanner.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StatusEnums {
    UNEXPECTED_ERROR("INTERNAL_SERVER_ERROR", "An unexpected error occurred."),
    SCAN_RESULT_NOT_FOUND("SCAN_RESULT_NOT_FOUND", "Scan result not found for scanId: %s"),
    UNRECOGNIZED_RESPONSE("UNRECOGNIZED_RESPONSE", "Unrecognized response"),
    PENDING("PENDING", "Pending"),
    UNKNOWN("UNKNOWN", "Unknown"),
    ERROR("ERROR", "Error"),
    INFECTED("INFECTED", "Infected"),
    CLEAN("CLEAN", "Clean");

    private final String code;
    private final String message;
}
