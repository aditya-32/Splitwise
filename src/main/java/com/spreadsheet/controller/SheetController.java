package com.spreadsheet.controller;

import com.spreadsheet.dto.SheetDTO;
import com.spreadsheet.dto.request.CreateSheetRequest;
import com.spreadsheet.dto.response.ApiResponse;
import com.spreadsheet.service.SheetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sheets")
@RequiredArgsConstructor
@Slf4j
public class SheetController {
    
    private final SheetService sheetService;
    
    @PostMapping("/workbook/{workbookId}")
    public ResponseEntity<ApiResponse<SheetDTO>> createSheet(
            @PathVariable Long workbookId,
            @Valid @RequestBody CreateSheetRequest request) {
        log.info("POST /api/sheets/workbook/{} - Creating sheet: {}", workbookId, request.getName());
        
        SheetDTO sheet = sheetService.createSheet(workbookId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Sheet created successfully", sheet));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SheetDTO>> getSheet(@PathVariable Long id) {
        log.info("GET /api/sheets/{} - Fetching sheet with cells", id);
        
        SheetDTO sheet = sheetService.getSheet(id);
        return ResponseEntity.ok(ApiResponse.success(sheet));
    }
    
    @GetMapping("/workbook/{workbookId}")
    public ResponseEntity<ApiResponse<List<SheetDTO>>> getSheetsByWorkbook(@PathVariable Long workbookId) {
        log.info("GET /api/sheets/workbook/{} - Fetching sheets", workbookId);
        
        List<SheetDTO> sheets = sheetService.getSheetsByWorkbook(workbookId);
        return ResponseEntity.ok(ApiResponse.success(sheets));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSheet(@PathVariable Long id) {
        log.info("DELETE /api/sheets/{} - Deleting sheet", id);
        
        sheetService.deleteSheet(id);
        return ResponseEntity.ok(ApiResponse.success("Sheet deleted successfully", null));
    }
}

