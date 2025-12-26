package com.spreadsheet.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SheetDTO {
    private Long id;
    private Long workbookId;
    private String name;
    private Integer rowCount;
    private Integer columnCount;
    private List<CellDTO> cells; // Only non-empty cells
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

