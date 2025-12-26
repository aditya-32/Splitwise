package com.spreadsheet.service;

import com.spreadsheet.domain.entity.Sheet;
import com.spreadsheet.domain.entity.Workbook;
import com.spreadsheet.dto.SheetDTO;
import com.spreadsheet.dto.request.CreateSheetRequest;
import com.spreadsheet.exception.ResourceNotFoundException;
import com.spreadsheet.mapper.SheetMapper;
import com.spreadsheet.repository.SheetRepository;
import com.spreadsheet.repository.WorkbookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SheetService {
    
    private final SheetRepository sheetRepository;
    private final WorkbookRepository workbookRepository;
    private final SheetMapper sheetMapper;
    
    @Transactional
    public SheetDTO createSheet(Long workbookId, CreateSheetRequest request) {
        log.info("Creating sheet '{}' in workbook: {}", request.getName(), workbookId);
        
        Workbook workbook = workbookRepository.findById(workbookId)
                .orElseThrow(() -> new ResourceNotFoundException("Workbook not found with ID: " + workbookId));
        
        Sheet sheet = Sheet.builder()
                .name(request.getName())
                .rowCount(request.getRowCount() != null ? request.getRowCount() : 1000)
                .columnCount(request.getColumnCount() != null ? request.getColumnCount() : 26)
                .build();
        
        workbook.addSheet(sheet);
        Sheet saved = sheetRepository.save(sheet);
        
        log.info("Created sheet with ID: {}", saved.getId());
        return sheetMapper.toDTO(saved);
    }
    
    @Transactional(readOnly = true)
    public SheetDTO getSheet(Long id) {
        log.info("Fetching sheet: {}", id);
        
        Sheet sheet = sheetRepository.findByIdWithCells(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sheet not found with ID: " + id));
        
        return sheetMapper.toDTO(sheet);
    }
    
    @Transactional(readOnly = true)
    public List<SheetDTO> getSheetsByWorkbook(Long workbookId) {
        log.info("Fetching sheets for workbook: {}", workbookId);
        
        return sheetRepository.findByWorkbookId(workbookId).stream()
                .map(sheetMapper::toDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void deleteSheet(Long id) {
        log.info("Deleting sheet: {}", id);
        
        if (!sheetRepository.existsById(id)) {
            throw new ResourceNotFoundException("Sheet not found with ID: " + id);
        }
        
        sheetRepository.deleteById(id);
        log.info("Deleted sheet: {}", id);
    }
}

