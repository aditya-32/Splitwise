package com.spreadsheet.exception;

import com.spreadsheet.domain.enums.ErrorType;
import lombok.Getter;

@Getter
public class FormulaEvaluationException extends RuntimeException {
    private final ErrorType errorType;
    
    public FormulaEvaluationException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }
    
    public FormulaEvaluationException(ErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }
}

