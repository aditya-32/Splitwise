package com.spreadsheet.service.formula;

import com.spreadsheet.domain.enums.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FormulaEvaluatorTest {
    
    private FormulaEvaluator evaluator;
    private FormulaParser parser;
    
    @BeforeEach
    void setUp() {
        parser = new FormulaParser();
        evaluator = new FormulaEvaluator(parser);
    }
    
    @Test
    void testEvaluateSimpleArithmetic() {
        Map<String, String> cellValues = new HashMap<>();
        
        String result = evaluator.evaluate("=5+3", cellValues);
        assertEquals("8", result);
        
        result = evaluator.evaluate("=10-4", cellValues);
        assertEquals("6", result);
        
        result = evaluator.evaluate("=6*7", cellValues);
        assertEquals("42", result);
        
        result = evaluator.evaluate("=20/5", cellValues);
        assertEquals("4", result);
    }
    
    @Test
    void testEvaluateWithCellReferences() {
        Map<String, String> cellValues = new HashMap<>();
        cellValues.put("A1", "10");
        cellValues.put("A2", "20");
        
        String result = evaluator.evaluate("=A1+A2", cellValues);
        assertEquals("30", result);
        
        result = evaluator.evaluate("=A2-A1", cellValues);
        assertEquals("10", result);
        
        result = evaluator.evaluate("=A1*A2", cellValues);
        assertEquals("200", result);
    }
    
    @Test
    void testEvaluateComplexExpression() {
        Map<String, String> cellValues = new HashMap<>();
        cellValues.put("A1", "5");
        cellValues.put("A2", "3");
        cellValues.put("A3", "2");
        
        String result = evaluator.evaluate("=(A1+A2)*A3", cellValues);
        assertEquals("16", result);
        
        result = evaluator.evaluate("=A1+(A2*A3)", cellValues);
        assertEquals("11", result);
    }
    
    @Test
    void testEvaluateSumFunction() {
        Map<String, String> cellValues = new HashMap<>();
        cellValues.put("A1", "10");
        cellValues.put("A2", "20");
        cellValues.put("A3", "30");
        
        String result = evaluator.evaluate("=SUM(A1:A3)", cellValues);
        assertEquals("60", result);
    }
    
    @Test
    void testEvaluateSumFunctionWithFormula() {
        Map<String, String> cellValues = new HashMap<>();
        cellValues.put("A1", "10");
        cellValues.put("A2", "20");
        cellValues.put("A3", "30");
        
        String result = evaluator.evaluate("=SUM(A1:A3)+5", cellValues);
        assertEquals("65", result);
    }
    
    @Test
    void testEvaluateAverageFunction() {
        Map<String, String> cellValues = new HashMap<>();
        cellValues.put("A1", "10");
        cellValues.put("A2", "20");
        cellValues.put("A3", "30");
        
        String result = evaluator.evaluate("=AVERAGE(A1:A3)", cellValues);
        assertEquals("20", result); // Should return 20 not 20.0 as it's a whole number
    }
    
    @Test
    void testEvaluateCountFunction() {
        Map<String, String> cellValues = new HashMap<>();
        cellValues.put("A1", "10");
        cellValues.put("A2", "20");
        cellValues.put("A3", "30");
        
        String result = evaluator.evaluate("=COUNT(A1:A3)", cellValues);
        assertEquals("3", result);
    }
    
    @Test
    void testEvaluateDivisionByZero() {
        Map<String, String> cellValues = new HashMap<>();
        
        String result = evaluator.evaluate("=10/0", cellValues);
        // exp4j may return #ERROR! or #DIV/0! depending on how it handles division by zero
        assertTrue(result.startsWith("#"), "Result should be an error symbol");
    }
    
    @Test
    void testEvaluateWithMissingCell() {
        Map<String, String> cellValues = new HashMap<>();
        cellValues.put("A1", "10");
        
        // Missing cell should be treated as 0
        String result = evaluator.evaluate("=A1+B1", cellValues);
        assertEquals("10", result);
    }
    
    @Test
    void testEvaluateWithTextCell() {
        Map<String, String> cellValues = new HashMap<>();
        cellValues.put("A1", "10");
        cellValues.put("A2", "hello");
        
        // Text cell should be treated as 0
        String result = evaluator.evaluate("=A1+A2", cellValues);
        assertEquals("10", result);
    }
    
    @Test
    void testEvaluateWithErrorCell() {
        Map<String, String> cellValues = new HashMap<>();
        cellValues.put("A1", "10");
        cellValues.put("A2", ErrorType.DIV_ZERO.getSymbol());
        
        // Error should propagate
        String result = evaluator.evaluate("=A1+A2", cellValues);
        assertTrue(result.startsWith("#"));
    }
    
    @Test
    void testEvaluateNonFormula() {
        Map<String, String> cellValues = new HashMap<>();
        
        String result = evaluator.evaluate("100", cellValues);
        assertEquals("100", result);
        
        result = evaluator.evaluate("hello", cellValues);
        assertEquals("hello", result);
    }
    
    @Test
    void testEvaluateDecimalResult() {
        Map<String, String> cellValues = new HashMap<>();
        
        String result = evaluator.evaluate("=10/3", cellValues);
        assertNotNull(result);
        assertTrue(result.contains("."));
    }
    
    @Test
    void testEvaluateWholeNumberResult() {
        Map<String, String> cellValues = new HashMap<>();
        
        String result = evaluator.evaluate("=10/2", cellValues);
        assertEquals("5", result); // Should not have .0
    }
}

