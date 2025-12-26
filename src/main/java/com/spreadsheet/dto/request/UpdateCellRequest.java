package com.spreadsheet.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCellRequest {
    @NotNull(message = "Row index is required")
    @Min(value = 1, message = "Row index must be at least 1")
    private Integer rowIndex;
    
    @NotNull(message = "Column index is required")
    @Min(value = 0, message = "Column index must be at least 0")
    private Integer columnIndex;
    
    @NotNull(message = "Value is required")
    private String value; // Can be empty string to clear cell
    
    private Long version; // For optimistic locking
}

