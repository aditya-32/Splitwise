package com.spreadsheet.mapper;

import com.spreadsheet.domain.entity.Cell;
import com.spreadsheet.dto.CellDTO;
import org.springframework.stereotype.Component;

@Component
public class CellMapper {
    
    public CellDTO toDTO(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        return CellDTO.builder()
                .id(cell.getId())
                .rowIndex(cell.getRowIndex())
                .columnIndex(cell.getColumnIndex())
                .address(cell.getAddress())
                .cellType(cell.getCellType())
                .rawValue(cell.getRawValue())
                .computedValue(cell.getComputedValue())
                .version(cell.getVersion())
                .updatedAt(cell.getUpdatedAt())
                .build();
    }
}

