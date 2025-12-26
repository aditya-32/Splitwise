package com.spreadsheet.dto;

import com.spreadsheet.domain.enums.CellType;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CellDTO {
    private Long id;
    private Integer rowIndex;
    private Integer columnIndex;
    private String address; // e.g., "A1", "B5"
    private CellType cellType;
    private String rawValue;
    private String computedValue;
    private Long version;
    private LocalDateTime updatedAt;
}

