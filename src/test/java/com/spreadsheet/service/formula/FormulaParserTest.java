package com.spreadsheet.service.formula;

import com.spreadsheet.domain.enums.ErrorType;
import com.spreadsheet.exception.FormulaEvaluationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FormulaParserTest {
    
    private FormulaParser parser;
    
    @BeforeEach
    void setUp() {
        parser = new FormulaParser();
    }
    
    @Test
    void testIsFormula() {
        assertTrue(parser.isFormula("=A1+B1"));
        assertTrue(parser.isFormula("= A1 + B1"));
        assertFalse(parser.isFormula("A1+B1"));
        assertFalse(parser.isFormula("100"));
        assertFalse(parser.isFormula(null));
    }
    
    @Test
    void testExtractCellReferencesSimple() {
        Set<CellReference> refs = parser.extractCellReferences("=A1+B1");
        assertEquals(2, refs.size());
        
        boolean hasA1 = refs.stream().anyMatch(r -> r.getRowIndex() == 1 && r.getColumnIndex() == 0);
        boolean hasB1 = refs.stream().anyMatch(r -> r.getRowIndex() == 1 && r.getColumnIndex() == 1);
        
        assertTrue(hasA1);
        assertTrue(hasB1);
    }
    
    @Test
    void testExtractCellReferencesRange() {
        Set<CellReference> refs = parser.extractCellReferences("=SUM(A1:A3)");
        assertEquals(3, refs.size());
        
        assertTrue(refs.stream().anyMatch(r -> r.getRowIndex() == 1 && r.getColumnIndex() == 0));
        assertTrue(refs.stream().anyMatch(r -> r.getRowIndex() == 2 && r.getColumnIndex() == 0));
        assertTrue(refs.stream().anyMatch(r -> r.getRowIndex() == 3 && r.getColumnIndex() == 0));
    }
    
    @Test
    void testExtractCellReferencesMultipleColumns() {
        Set<CellReference> refs = parser.extractCellReferences("=A1+B2+C3");
        assertEquals(3, refs.size());
    }
    
    @Test
    void testExtractCellReferencesDoubleDigit() {
        Set<CellReference> refs = parser.extractCellReferences("=A10+B20");
        assertEquals(2, refs.size());
        
        assertTrue(refs.stream().anyMatch(r -> r.getRowIndex() == 10 && r.getColumnIndex() == 0));
        assertTrue(refs.stream().anyMatch(r -> r.getRowIndex() == 20 && r.getColumnIndex() == 1));
    }
    
    @Test
    void testValidateFormulaValid() {
        assertDoesNotThrow(() -> parser.validateFormula("=A1+B1"));
        assertDoesNotThrow(() -> parser.validateFormula("=SUM(A1:A10)"));
        assertDoesNotThrow(() -> parser.validateFormula("=(A1+B1)*C1"));
    }
    
    @Test
    void testValidateFormulaInvalidNoEquals() {
        FormulaEvaluationException ex = assertThrows(
                FormulaEvaluationException.class,
                () -> parser.validateFormula("A1+B1")
        );
        assertEquals(ErrorType.PARSE_ERROR, ex.getErrorType());
    }
    
    @Test
    void testValidateFormulaEmpty() {
        FormulaEvaluationException ex = assertThrows(
                FormulaEvaluationException.class,
                () -> parser.validateFormula("=")
        );
        assertEquals(ErrorType.PARSE_ERROR, ex.getErrorType());
    }
    
    @Test
    void testValidateFormulaUnbalancedParentheses() {
        assertThrows(FormulaEvaluationException.class, () -> parser.validateFormula("=SUM(A1:A10"));
        assertThrows(FormulaEvaluationException.class, () -> parser.validateFormula("=SUM A1:A10)"));
        assertThrows(FormulaEvaluationException.class, () -> parser.validateFormula("=(A1+B1))"));
    }
    
    @Test
    void testExtractCellReferencesNoReferences() {
        Set<CellReference> refs = parser.extractCellReferences("=100+200");
        assertEquals(0, refs.size());
    }
    
    @Test
    void testExtractCellReferencesRectangularRange() {
        Set<CellReference> refs = parser.extractCellReferences("=SUM(A1:B2)");
        assertEquals(4, refs.size());
        
        // Should include A1, A2, B1, B2
        assertTrue(refs.stream().anyMatch(r -> r.getRowIndex() == 1 && r.getColumnIndex() == 0));
        assertTrue(refs.stream().anyMatch(r -> r.getRowIndex() == 2 && r.getColumnIndex() == 0));
        assertTrue(refs.stream().anyMatch(r -> r.getRowIndex() == 1 && r.getColumnIndex() == 1));
        assertTrue(refs.stream().anyMatch(r -> r.getRowIndex() == 2 && r.getColumnIndex() == 1));
    }
}

