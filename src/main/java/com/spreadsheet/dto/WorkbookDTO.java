package com.spreadsheet.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkbookDTO {
    private Long id;
    private String name;
    private List<SheetDTO> sheets;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

