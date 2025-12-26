package com.spreadsheet.mapper;

import com.spreadsheet.domain.entity.Workbook;
import com.spreadsheet.dto.WorkbookDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WorkbookMapper {
    
    private final SheetMapper sheetMapper;
    
    public WorkbookDTO toDTO(Workbook workbook) {
        if (workbook == null) {
            return null;
        }
        
        return WorkbookDTO.builder()
                .id(workbook.getId())
                .name(workbook.getName())
                .sheets(workbook.getSheets().stream()
                        .map(sheetMapper::toDTO)
                        .collect(Collectors.toList()))
                .version(workbook.getVersion())
                .createdAt(workbook.getCreatedAt())
                .updatedAt(workbook.getUpdatedAt())
                .build();
    }
}

