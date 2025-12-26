package com.spreadsheet.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWorkbookRequest {
    @NotBlank(message = "Workbook name is required")
    private String name;
    
    private String sheetName; // Optional: default sheet name
}

