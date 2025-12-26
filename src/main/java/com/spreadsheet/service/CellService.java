package com.spreadsheet.service;

import com.spreadsheet.domain.entity.Cell;
import com.spreadsheet.domain.entity.Sheet;
import com.spreadsheet.domain.enums.CellType;
import com.spreadsheet.dto.CellDTO;
import com.spreadsheet.dto.request.BatchUpdateCellsRequest;
import com.spreadsheet.dto.request.UpdateCellRequest;
import com.spreadsheet.exception.CyclicDependencyException;
import com.spreadsheet.exception.ResourceNotFoundException;
import com.spreadsheet.mapper.CellMapper;
import com.spreadsheet.repository.CellRepository;
import com.spreadsheet.repository.SheetRepository;
import com.spreadsheet.service.formula.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CellService {
    
    private final CellRepository cellRepository;
    private final SheetRepository sheetRepository;
    private final FormulaParser formulaParser;
    private final FormulaEvaluator formulaEvaluator;
    private final DependencyGraph dependencyGraph;
    private final CellMapper cellMapper;
    private final ApplicationEventPublisher eventPublisher;
    
    @Transactional
    @Retryable(
        retryFor = {ObjectOptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public CellDTO updateCell(Long sheetId, UpdateCellRequest request) {
        log.info("Updating cell at ({}, {}) in sheet {}", request.getRowIndex(), request.getColumnIndex(), sheetId);
        
        // Validate sheet exists
        Sheet sheet = sheetRepository.findById(sheetId)
                .orElseThrow(() -> new ResourceNotFoundException("Sheet not found with ID: " + sheetId));
        
        // Validate coordinates
        validateCoordinates(sheet, request.getRowIndex(), request.getColumnIndex());
        
        // Find or create cell
        Cell cell = cellRepository.findBySheetIdAndRowIndexAndColumnIndex(
                sheetId, request.getRowIndex(), request.getColumnIndex()
        ).orElse(null);
        
        if (cell == null && (request.getValue() == null || request.getValue().trim().isEmpty())) {
            // No cell exists and trying to set empty value - nothing to do
            return null;
        }
        
        if (cell == null) {
            // Create new cell
            cell = Cell.builder()
                    .sheet(sheet)
                    .rowIndex(request.getRowIndex())
                    .columnIndex(request.getColumnIndex())
                    .build();
        }
        
        // Check for empty value (clear cell)
        if (request.getValue() == null || request.getValue().trim().isEmpty()) {
            cellRepository.delete(cell);
            log.info("Deleted empty cell at {}", cell.getAddress());
            
            // Re-evaluate dependent formulas
            reEvaluateDependentCells(sheetId, cell.getAddress());
            
            return null;
        }
        
        // Determine cell type and set values
        String value = request.getValue().trim();
        CellType cellType = determineCellType(value);
        
        cell.setCellType(cellType);
        cell.setRawValue(value);
        
        if (cellType == CellType.FORMULA) {
            // Validate formula syntax
            formulaParser.validateFormula(value);
            
            // Check for circular dependencies
            checkCircularDependency(sheetId, cell);
            
            // Evaluate formula
            String computedValue = evaluateFormula(sheetId, cell);
            cell.setComputedValue(computedValue);
        } else {
            // For non-formula cells, computed value is same as raw value
            cell.setComputedValue(value);
        }
        
        Cell saved = cellRepository.save(cell);
        log.info("Updated cell {} with type {}", saved.getAddress(), saved.getCellType());
        
        // Re-evaluate dependent formulas
        reEvaluateDependentCells(sheetId, saved.getAddress());
        
        // Publish event for auto-save
        eventPublisher.publishEvent(new CellUpdatedEvent(this, saved));
        
        return cellMapper.toDTO(saved);
    }
    
    @Transactional
    public List<CellDTO> batchUpdateCells(Long sheetId, BatchUpdateCellsRequest request) {
        log.info("Batch updating {} cells in sheet {}", request.getCells().size(), sheetId);
        
        List<CellDTO> results = new ArrayList<>();
        
        for (UpdateCellRequest cellRequest : request.getCells()) {
            try {
                CellDTO result = updateCell(sheetId, cellRequest);
                if (result != null) {
                    results.add(result);
                }
            } catch (Exception e) {
                log.error("Error updating cell at ({}, {}): {}", 
                        cellRequest.getRowIndex(), cellRequest.getColumnIndex(), e.getMessage());
                // Continue with other cells
            }
        }
        
        return results;
    }
    
    @Transactional(readOnly = true)
    public CellDTO getCell(Long sheetId, Integer rowIndex, Integer columnIndex) {
        log.info("Fetching cell at ({}, {}) in sheet {}", rowIndex, columnIndex, sheetId);
        
        Cell cell = cellRepository.findBySheetIdAndRowIndexAndColumnIndex(sheetId, rowIndex, columnIndex)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Cell not found at (%d, %d) in sheet %d", rowIndex, columnIndex, sheetId)));
        
        return cellMapper.toDTO(cell);
    }
    
    @Transactional(readOnly = true)
    public List<CellDTO> getAllCells(Long sheetId) {
        log.info("Fetching all cells for sheet: {}", sheetId);
        
        return cellRepository.findBySheetId(sheetId).stream()
                .map(cellMapper::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Determine cell type from value
     */
    private CellType determineCellType(String value) {
        if (value.startsWith("=")) {
            return CellType.FORMULA;
        } else if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return CellType.BOOLEAN;
        } else {
            try {
                Double.parseDouble(value);
                return CellType.NUMBER;
            } catch (NumberFormatException e) {
                return CellType.TEXT;
            }
        }
    }
    
    /**
     * Check for circular dependencies before updating a formula cell
     */
    private void checkCircularDependency(Long sheetId, Cell cell) {
        if (!formulaParser.isFormula(cell.getRawValue())) {
            return;
        }
        
        // Get all cells in the sheet
        List<Cell> allCells = cellRepository.findBySheetId(sheetId);
        
        // Build dependency graph
        Map<String, Set<String>> graph = dependencyGraph.buildDependencyGraph(allCells, formulaParser);
        
        // Extract dependencies for the current cell
        Set<CellReference> references = formulaParser.extractCellReferences(cell.getRawValue());
        Set<String> dependencies = references.stream()
                .map(CellReference::toAddress)
                .collect(Collectors.toSet());
        
        // Check if adding this dependency would create a cycle
        if (dependencyGraph.wouldCreateCycle(graph, cell.getAddress(), dependencies)) {
            throw new CyclicDependencyException(
                    com.spreadsheet.domain.enums.ErrorType.CYCLE_ERROR,
                    "Circular dependency detected for cell " + cell.getAddress()
            );
        }
    }
    
    /**
     * Evaluate a formula cell
     */
    private String evaluateFormula(Long sheetId, Cell cell) {
        // Get all cells in the sheet
        List<Cell> allCells = cellRepository.findBySheetId(sheetId);
        
        // Build map of cell addresses to computed values
        Map<String, String> cellValues = allCells.stream()
                .collect(Collectors.toMap(
                        Cell::getAddress,
                        c -> c.getComputedValue() != null ? c.getComputedValue() : "0"
                ));
        
        // Evaluate the formula
        return formulaEvaluator.evaluate(cell.getRawValue(), cellValues);
    }
    
    /**
     * Re-evaluate all formulas that depend on the changed cell
     */
    private void reEvaluateDependentCells(Long sheetId, String changedCellAddress) {
        log.info("Re-evaluating cells dependent on {}", changedCellAddress);
        
        // Get all formula cells
        List<Cell> allCells = cellRepository.findBySheetId(sheetId);
        List<Cell> formulaCells = cellRepository.findAllFormulaCellsBySheetId(sheetId);
        
        if (formulaCells.isEmpty()) {
            return;
        }
        
        // Build dependency graph
        Map<String, Set<String>> graph = dependencyGraph.buildDependencyGraph(allCells, formulaParser);
        
        // Find cells that depend on the changed cell
        Set<String> dependentCells = dependencyGraph.findDependentCells(graph, changedCellAddress);
        
        if (dependentCells.isEmpty()) {
            return;
        }
        
        log.info("Found {} dependent cells to re-evaluate", dependentCells.size());
        
        // Get evaluation order using topological sort
        List<String> evaluationOrder;
        try {
            evaluationOrder = dependencyGraph.topologicalSort(graph);
        } catch (CyclicDependencyException e) {
            log.error("Cycle detected during re-evaluation: {}", e.getMessage());
            return;
        }
        
        // Build current cell values map
        Map<String, String> cellValues = allCells.stream()
                .collect(Collectors.toMap(
                        Cell::getAddress,
                        c -> c.getComputedValue() != null ? c.getComputedValue() : "0"
                ));
        
        // Re-evaluate in order
        for (String cellAddress : evaluationOrder) {
            if (dependentCells.contains(cellAddress)) {
                Cell cell = allCells.stream()
                        .filter(c -> c.getAddress().equals(cellAddress))
                        .findFirst()
                        .orElse(null);
                
                if (cell != null && cell.getCellType() == CellType.FORMULA) {
                    String newValue = formulaEvaluator.evaluate(cell.getRawValue(), cellValues);
                    cell.setComputedValue(newValue);
                    cellRepository.save(cell);
                    
                    // Update the map for next iterations
                    cellValues.put(cellAddress, newValue);
                    
                    log.debug("Re-evaluated cell {} = {}", cellAddress, newValue);
                }
            }
        }
    }
    
    /**
     * Validate cell coordinates
     */
    private void validateCoordinates(Sheet sheet, Integer rowIndex, Integer columnIndex) {
        if (rowIndex < 1 || rowIndex > sheet.getRowCount()) {
            throw new IllegalArgumentException(
                    String.format("Row index %d out of bounds (1-%d)", rowIndex, sheet.getRowCount()));
        }
        
        if (columnIndex < 0 || columnIndex >= sheet.getColumnCount()) {
            throw new IllegalArgumentException(
                    String.format("Column index %d out of bounds (0-%d)", columnIndex, sheet.getColumnCount() - 1));
        }
    }
    
    /**
     * Event published when a cell is updated (for auto-save)
     */
    public static class CellUpdatedEvent {
        private final Object source;
        private final Cell cell;
        
        public CellUpdatedEvent(Object source, Cell cell) {
            this.source = source;
            this.cell = cell;
        }
        
        public Cell getCell() {
            return cell;
        }
    }
}

