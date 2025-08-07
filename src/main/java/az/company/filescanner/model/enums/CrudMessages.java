package az.company.filescanner.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CrudMessages {
    OPERATION_DELETED("Deleted");
    private final String message;
}

