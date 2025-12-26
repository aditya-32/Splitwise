package com.spreadsheet.exception;

import com.spreadsheet.domain.enums.ErrorType;

public class CyclicDependencyException extends FormulaEvaluationException {
    public CyclicDependencyException(ErrorType errorType, String message) {
        super(errorType, message);
    }
}

