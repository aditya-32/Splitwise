package com.spreadsheet.controller;

import com.spreadsheet.dto.CellDTO;
import com.spreadsheet.dto.request.BatchUpdateCellsRequest;
import com.spreadsheet.dto.request.UpdateCellRequest;
import com.spreadsheet.dto.response.ApiResponse;
import com.spreadsheet.service.CellService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sheets/{sheetId}/cells")
@RequiredArgsConstructor
@Slf4j
public class CellController {
    
    private final CellService cellService;
    
    /**
     * Update a single cell
     */
    @PutMapping
    public ResponseEntity<ApiResponse<CellDTO>> updateCell(
            @PathVariable Long sheetId,
            @Valid @RequestBody UpdateCellRequest request) {
        log.info("PUT /api/sheets/{}/cells - Updating cell at ({}, {})", 
                sheetId, request.getRowIndex(), request.getColumnIndex());
        
        CellDTO cell = cellService.updateCell(sheetId, request);
        
        if (cell == null) {
            return ResponseEntity.ok(ApiResponse.success("Cell cleared successfully", null));
        }
        
        return ResponseEntity.ok(ApiResponse.success("Cell updated successfully", cell));
    }
    
    /**
     * Batch update multiple cells
     */
    @PutMapping("/batch")
    public ResponseEntity<ApiResponse<List<CellDTO>>> batchUpdateCells(
            @PathVariable Long sheetId,
            @Valid @RequestBody BatchUpdateCellsRequest request) {
        log.info("PUT /api/sheets/{}/cells/batch - Batch updating {} cells", 
                sheetId, request.getCells().size());
        
        List<CellDTO> cells = cellService.batchUpdateCells(sheetId, request);
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Successfully updated %d cells", cells.size()), cells));
    }
    
    /**
     * Get a specific cell
     */
    @GetMapping
    public ResponseEntity<ApiResponse<CellDTO>> getCell(
            @PathVariable Long sheetId,
            @RequestParam Integer rowIndex,
            @RequestParam Integer columnIndex) {
        log.info("GET /api/sheets/{}/cells?rowIndex={}&columnIndex={}", 
                sheetId, rowIndex, columnIndex);
        
        CellDTO cell = cellService.getCell(sheetId, rowIndex, columnIndex);
        return ResponseEntity.ok(ApiResponse.success(cell));
    }
    
    /**
     * Get all non-empty cells in a sheet
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<CellDTO>>> getAllCells(@PathVariable Long sheetId) {
        log.info("GET /api/sheets/{}/cells/all - Fetching all cells", sheetId);
        
        List<CellDTO> cells = cellService.getAllCells(sheetId);
        return ResponseEntity.ok(ApiResponse.success(cells));
    }
}

