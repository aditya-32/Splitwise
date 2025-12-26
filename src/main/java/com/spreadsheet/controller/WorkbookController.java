package com.spreadsheet.controller;

import com.spreadsheet.dto.WorkbookDTO;
import com.spreadsheet.dto.request.CreateWorkbookRequest;
import com.spreadsheet.dto.response.ApiResponse;
import com.spreadsheet.service.WorkbookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workbooks")
@RequiredArgsConstructor
@Slf4j
public class WorkbookController {
    
    private final WorkbookService workbookService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<WorkbookDTO>> createWorkbook(
            @Valid @RequestBody CreateWorkbookRequest request) {
        log.info("POST /api/workbooks - Creating workbook: {}", request.getName());
        
        WorkbookDTO workbook = workbookService.createWorkbook(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Workbook created successfully", workbook));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkbookDTO>> getWorkbook(@PathVariable Long id) {
        log.info("GET /api/workbooks/{} - Fetching workbook", id);
        
        WorkbookDTO workbook = workbookService.getWorkbook(id);
        return ResponseEntity.ok(ApiResponse.success(workbook));
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkbookDTO>>> getAllWorkbooks() {
        log.info("GET /api/workbooks - Fetching all workbooks");
        
        List<WorkbookDTO> workbooks = workbookService.getAllWorkbooks();
        return ResponseEntity.ok(ApiResponse.success(workbooks));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteWorkbook(@PathVariable Long id) {
        log.info("DELETE /api/workbooks/{} - Deleting workbook", id);
        
        workbookService.deleteWorkbook(id);
        return ResponseEntity.ok(ApiResponse.success("Workbook deleted successfully", null));
    }
}

