package com.spreadsheet.service.formula;

import com.spreadsheet.domain.enums.ErrorType;
import com.spreadsheet.exception.FormulaEvaluationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses formulas and extracts cell references
 */
@Component
@Slf4j
public class FormulaParser {
    
    // Pattern to match cell references like A1, B5, AA10
    private static final Pattern CELL_REFERENCE_PATTERN = Pattern.compile("([A-Z]+)([0-9]+)");
    
    // Pattern to match range references like A1:A10
    private static final Pattern RANGE_PATTERN = Pattern.compile("([A-Z]+[0-9]+):([A-Z]+[0-9]+)");
    
    /**
     * Check if the value is a formula (starts with =)
     */
    public boolean isFormula(String value) {
        return value != null && value.trim().startsWith("=");
    }
    
    /**
     * Extract all cell references from a formula
     */
    public Set<CellReference> extractCellReferences(String formula) {
        Set<CellReference> references = new HashSet<>();
        
        if (!isFormula(formula)) {
            return references;
        }
        
        String formulaBody = formula.substring(1).trim(); // Remove '='
        
        // Handle ranges first (e.g., SUM(A1:A10))
        Matcher rangeMatcher = RANGE_PATTERN.matcher(formulaBody);
        while (rangeMatcher.find()) {
            String startCell = rangeMatcher.group(1);
            String endCell = rangeMatcher.group(2);
            references.addAll(expandRange(startCell, endCell));
        }
        
        // Handle individual cell references
        Matcher cellMatcher = CELL_REFERENCE_PATTERN.matcher(formulaBody);
        while (cellMatcher.find()) {
            String cellAddress = cellMatcher.group(0);
            try {
                references.add(CellReference.fromAddress(cellAddress));
            } catch (Exception e) {
                log.warn("Invalid cell reference: {}", cellAddress);
            }
        }
        
        return references;
    }
    
    /**
     * Expand a range like A1:A10 into individual cell references
     */
    private List<CellReference> expandRange(String startAddress, String endAddress) {
        List<CellReference> cells = new ArrayList<>();
        
        try {
            CellReference start = CellReference.fromAddress(startAddress);
            CellReference end = CellReference.fromAddress(endAddress);
            
            // Handle single column ranges (e.g., A1:A10)
            if (start.getColumnIndex().equals(end.getColumnIndex())) {
                int startRow = Math.min(start.getRowIndex(), end.getRowIndex());
                int endRow = Math.max(start.getRowIndex(), end.getRowIndex());
                
                for (int row = startRow; row <= endRow; row++) {
                    cells.add(CellReference.builder()
                            .rowIndex(row)
                            .columnIndex(start.getColumnIndex())
                            .build());
                }
            }
            // Handle single row ranges (e.g., A1:E1)
            else if (start.getRowIndex().equals(end.getRowIndex())) {
                int startCol = Math.min(start.getColumnIndex(), end.getColumnIndex());
                int endCol = Math.max(start.getColumnIndex(), end.getColumnIndex());
                
                for (int col = startCol; col <= endCol; col++) {
                    cells.add(CellReference.builder()
                            .rowIndex(start.getRowIndex())
                            .columnIndex(col)
                            .build());
                }
            }
            // Handle rectangular ranges (e.g., A1:C3)
            else {
                int startRow = Math.min(start.getRowIndex(), end.getRowIndex());
                int endRow = Math.max(start.getRowIndex(), end.getRowIndex());
                int startCol = Math.min(start.getColumnIndex(), end.getColumnIndex());
                int endCol = Math.max(start.getColumnIndex(), end.getColumnIndex());
                
                for (int row = startRow; row <= endRow; row++) {
                    for (int col = startCol; col <= endCol; col++) {
                        cells.add(CellReference.builder()
                                .rowIndex(row)
                                .columnIndex(col)
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error expanding range {}:{}", startAddress, endAddress, e);
            throw new FormulaEvaluationException(ErrorType.REF_ERROR, "Invalid range: " + startAddress + ":" + endAddress);
        }
        
        return cells;
    }
    
    /**
     * Validate formula syntax
     */
    public void validateFormula(String formula) {
        if (!isFormula(formula)) {
            throw new FormulaEvaluationException(ErrorType.PARSE_ERROR, "Formula must start with =");
        }
        
        String formulaBody = formula.substring(1).trim();
        
        if (formulaBody.isEmpty()) {
            throw new FormulaEvaluationException(ErrorType.PARSE_ERROR, "Empty formula");
        }
        
        // Check for balanced parentheses
        int balance = 0;
        for (char c : formulaBody.toCharArray()) {
            if (c == '(') balance++;
            if (c == ')') balance--;
            if (balance < 0) {
                throw new FormulaEvaluationException(ErrorType.PARSE_ERROR, "Unbalanced parentheses");
            }
        }
        
        if (balance != 0) {
            throw new FormulaEvaluationException(ErrorType.PARSE_ERROR, "Unbalanced parentheses");
        }
    }
}

