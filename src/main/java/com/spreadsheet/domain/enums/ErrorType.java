package com.spreadsheet.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorType {
    DIV_ZERO("#DIV/0!", "Division by zero"),
    REF_ERROR("#REF!", "Invalid cell reference"),
    CYCLE_ERROR("#CYCLE!", "Circular dependency detected"),
    VALUE_ERROR("#VALUE!", "Invalid value type"),
    NAME_ERROR("#NAME?", "Unrecognized formula or function"),
    PARSE_ERROR("#ERROR!", "Formula parsing error"),
    NUM_ERROR("#NUM!", "Invalid numeric value");

    private final String symbol;
    private final String description;
}

