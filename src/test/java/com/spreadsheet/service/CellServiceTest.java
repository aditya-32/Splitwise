package com.spreadsheet.service;

import com.spreadsheet.domain.entity.Cell;
import com.spreadsheet.domain.entity.Sheet;
import com.spreadsheet.domain.enums.CellType;
import com.spreadsheet.dto.CellDTO;
import com.spreadsheet.dto.request.UpdateCellRequest;
import com.spreadsheet.exception.CyclicDependencyException;
import com.spreadsheet.exception.FormulaEvaluationException;
import com.spreadsheet.exception.ResourceNotFoundException;
import com.spreadsheet.mapper.CellMapper;
import com.spreadsheet.repository.CellRepository;
import com.spreadsheet.repository.SheetRepository;
import com.spreadsheet.service.formula.DependencyGraph;
import com.spreadsheet.service.formula.FormulaEvaluator;
import com.spreadsheet.service.formula.FormulaParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CellServiceTest {
    
    @Mock
    private CellRepository cellRepository;
    
    @Mock
    private SheetRepository sheetRepository;
    
    @Mock
    private FormulaParser formulaParser;
    
    @Mock
    private FormulaEvaluator formulaEvaluator;
    
    @Mock
    private DependencyGraph dependencyGraph;
    
    @Mock
    private CellMapper cellMapper;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;
    
    @InjectMocks
    private CellService cellService;
    
    private Sheet testSheet;
    private Cell testCell;
    
    @BeforeEach
    void setUp() {
        testSheet = Sheet.builder()
                .id(1L)
                .name("Sheet1")
                .rowCount(1000)
                .columnCount(26)
                .build();
        
        testCell = Cell.builder()
                .id(1L)
                .sheet(testSheet)
                .rowIndex(1)
                .columnIndex(0)
                .cellType(CellType.NUMBER)
                .rawValue("42")
                .computedValue("42")
                .build();
    }
    
    @Test
    void testUpdateCellNumber() {
        UpdateCellRequest request = UpdateCellRequest.builder()
                .rowIndex(1)
                .columnIndex(0)
                .value("42")
                .build();
        
        when(sheetRepository.findById(1L)).thenReturn(Optional.of(testSheet));
        when(cellRepository.findBySheetIdAndRowIndexAndColumnIndex(1L, 1, 0))
                .thenReturn(Optional.empty());
        when(cellRepository.save(any(Cell.class))).thenReturn(testCell);
        when(cellRepository.findAllFormulaCellsBySheetId(1L)).thenReturn(Collections.emptyList());
        when(cellMapper.toDTO(any(Cell.class))).thenReturn(new CellDTO());
        
        CellDTO result = cellService.updateCell(1L, request);
        
        assertNotNull(result);
        verify(cellRepository).save(any(Cell.class));
        verify(eventPublisher).publishEvent(any(CellService.CellUpdatedEvent.class));
    }
    
    @Test
    void testUpdateCellFormula() {
        UpdateCellRequest request = UpdateCellRequest.builder()
                .rowIndex(1)
                .columnIndex(0)
                .value("=A2+A3")
                .build();
        
        Cell formulaCell = Cell.builder()
                .sheet(testSheet)
                .rowIndex(1)
                .columnIndex(0)
                .cellType(CellType.FORMULA)
                .rawValue("=A2+A3")
                .computedValue("100")
                .build();
        
        when(sheetRepository.findById(1L)).thenReturn(Optional.of(testSheet));
        when(cellRepository.findBySheetIdAndRowIndexAndColumnIndex(1L, 1, 0))
                .thenReturn(Optional.empty());
        when(formulaParser.isFormula("=A2+A3")).thenReturn(true);
        when(cellRepository.findBySheetId(1L)).thenReturn(Collections.singletonList(formulaCell));
        when(dependencyGraph.buildDependencyGraph(any(), any())).thenReturn(new HashMap<>());
        when(dependencyGraph.wouldCreateCycle(any(), any(), any())).thenReturn(false);
        when(formulaEvaluator.evaluate(any(), any())).thenReturn("100");
        when(cellRepository.save(any(Cell.class))).thenReturn(formulaCell);
        when(cellRepository.findAllFormulaCellsBySheetId(1L)).thenReturn(Collections.emptyList());
        when(cellMapper.toDTO(any(Cell.class))).thenReturn(new CellDTO());
        
        CellDTO result = cellService.updateCell(1L, request);
        
        assertNotNull(result);
        verify(formulaParser).validateFormula("=A2+A3");
        verify(formulaEvaluator).evaluate(any(), any());
    }
    
    @Test
    void testUpdateCellDetectsCycle() {
        UpdateCellRequest request = UpdateCellRequest.builder()
                .rowIndex(1)
                .columnIndex(0)
                .value("=A2")
                .build();
        
        when(sheetRepository.findById(1L)).thenReturn(Optional.of(testSheet));
        when(cellRepository.findBySheetIdAndRowIndexAndColumnIndex(1L, 1, 0))
                .thenReturn(Optional.empty());
        when(formulaParser.isFormula("=A2")).thenReturn(true);
        when(cellRepository.findBySheetId(1L)).thenReturn(Collections.emptyList());
        when(dependencyGraph.buildDependencyGraph(any(), any())).thenReturn(new HashMap<>());
        when(dependencyGraph.wouldCreateCycle(any(), any(), any())).thenReturn(true);
        
        assertThrows(CyclicDependencyException.class, () -> cellService.updateCell(1L, request));
    }
    
    @Test
    void testUpdateCellInvalidSheet() {
        UpdateCellRequest request = UpdateCellRequest.builder()
                .rowIndex(1)
                .columnIndex(0)
                .value("42")
                .build();
        
        when(sheetRepository.findById(1L)).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, () -> cellService.updateCell(1L, request));
    }
    
    @Test
    void testUpdateCellInvalidCoordinates() {
        UpdateCellRequest request = UpdateCellRequest.builder()
                .rowIndex(2000) // Beyond sheet bounds
                .columnIndex(0)
                .value("42")
                .build();
        
        when(sheetRepository.findById(1L)).thenReturn(Optional.of(testSheet));
        
        assertThrows(IllegalArgumentException.class, () -> cellService.updateCell(1L, request));
    }
    
    @Test
    void testUpdateCellClearValue() {
        UpdateCellRequest request = UpdateCellRequest.builder()
                .rowIndex(1)
                .columnIndex(0)
                .value("")
                .build();
        
        when(sheetRepository.findById(1L)).thenReturn(Optional.of(testSheet));
        when(cellRepository.findBySheetIdAndRowIndexAndColumnIndex(1L, 1, 0))
                .thenReturn(Optional.of(testCell));
        when(cellRepository.findAllFormulaCellsBySheetId(1L)).thenReturn(Collections.emptyList());
        
        CellDTO result = cellService.updateCell(1L, request);
        
        assertNull(result);
        verify(cellRepository).delete(testCell);
    }
    
    @Test
    void testGetCell() {
        when(cellRepository.findBySheetIdAndRowIndexAndColumnIndex(1L, 1, 0))
                .thenReturn(Optional.of(testCell));
        when(cellMapper.toDTO(testCell)).thenReturn(new CellDTO());
        
        CellDTO result = cellService.getCell(1L, 1, 0);
        
        assertNotNull(result);
        verify(cellRepository).findBySheetIdAndRowIndexAndColumnIndex(1L, 1, 0);
    }
    
    @Test
    void testGetCellNotFound() {
        when(cellRepository.findBySheetIdAndRowIndexAndColumnIndex(1L, 1, 0))
                .thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, () -> cellService.getCell(1L, 1, 0));
    }
    
    @Test
    void testGetAllCells() {
        List<Cell> cells = Arrays.asList(testCell);
        when(cellRepository.findBySheetId(1L)).thenReturn(cells);
        when(cellMapper.toDTO(any(Cell.class))).thenReturn(new CellDTO());
        
        List<CellDTO> result = cellService.getAllCells(1L);
        
        assertNotNull(result);
        assertEquals(1, result.size());
    }
}

