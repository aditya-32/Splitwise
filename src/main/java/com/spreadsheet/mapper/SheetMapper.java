package com.spreadsheet.mapper;

import com.spreadsheet.domain.entity.Sheet;
import com.spreadsheet.dto.SheetDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SheetMapper {
    
    private final CellMapper cellMapper;
    
    public SheetDTO toDTO(Sheet sheet) {
        if (sheet == null) {
            return null;
        }
        
        return SheetDTO.builder()
                .id(sheet.getId())
                .workbookId(sheet.getWorkbook() != null ? sheet.getWorkbook().getId() : null)
                .name(sheet.getName())
                .rowCount(sheet.getRowCount())
                .columnCount(sheet.getColumnCount())
                .cells(sheet.getCells().stream()
                        .map(cellMapper::toDTO)
                        .collect(Collectors.toList()))
                .createdAt(sheet.getCreatedAt())
                .updatedAt(sheet.getUpdatedAt())
                .build();
    }
}

