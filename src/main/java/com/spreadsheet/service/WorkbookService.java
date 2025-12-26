package com.spreadsheet.service;

import com.spreadsheet.domain.entity.Sheet;
import com.spreadsheet.domain.entity.Workbook;
import com.spreadsheet.dto.WorkbookDTO;
import com.spreadsheet.dto.request.CreateWorkbookRequest;
import com.spreadsheet.exception.ResourceNotFoundException;
import com.spreadsheet.mapper.WorkbookMapper;
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
public class WorkbookService {
    
    private final WorkbookRepository workbookRepository;
    private final WorkbookMapper workbookMapper;
    
    @Transactional
    public WorkbookDTO createWorkbook(CreateWorkbookRequest request) {
        log.info("Creating workbook: {}", request.getName());
        
        Workbook workbook = Workbook.builder()
                .name(request.getName())
                .build();
        
        // Create default sheet
        String sheetName = request.getSheetName() != null ? request.getSheetName() : "Sheet1";
        Sheet sheet = Sheet.builder()
                .name(sheetName)
                .rowCount(1000)
                .columnCount(26)
                .build();
        
        workbook.addSheet(sheet);
        
        Workbook saved = workbookRepository.save(workbook);
        log.info("Created workbook with ID: {}", saved.getId());
        
        return workbookMapper.toDTO(saved);
    }
    
    @Transactional(readOnly = true)
    public WorkbookDTO getWorkbook(Long id) {
        log.info("Fetching workbook: {}", id);
        
        Workbook workbook = workbookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workbook not found with ID: " + id));
        
        return workbookMapper.toDTO(workbook);
    }
    
    @Transactional(readOnly = true)
    public List<WorkbookDTO> getAllWorkbooks() {
        log.info("Fetching all workbooks");
        
        return workbookRepository.findAll().stream()
                .map(workbookMapper::toDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void deleteWorkbook(Long id) {
        log.info("Deleting workbook: {}", id);
        
        if (!workbookRepository.existsById(id)) {
            throw new ResourceNotFoundException("Workbook not found with ID: " + id);
        }
        
        workbookRepository.deleteById(id);
        log.info("Deleted workbook: {}", id);
    }
}

