package com.spreadsheet.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSheetRequest {
    @NotBlank(message = "Sheet name is required")
    private String name;
    
    @Min(value = 1, message = "Row count must be at least 1")
    @Builder.Default
    private Integer rowCount = 1000;
    
    @Min(value = 1, message = "Column count must be at least 1")
    @Builder.Default
    private Integer columnCount = 26;
}

