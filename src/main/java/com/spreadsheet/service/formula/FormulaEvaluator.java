package com.spreadsheet.service.formula;

import com.spreadsheet.domain.entity.Cell;
import com.spreadsheet.domain.enums.ErrorType;
import com.spreadsheet.exception.FormulaEvaluationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluates formulas and computes cell values
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FormulaEvaluator {
    
    private final FormulaParser parser;
    
    private static final Pattern SUM_PATTERN = Pattern.compile("SUM\\s*\\(([^)]+)\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern AVERAGE_PATTERN = Pattern.compile("AVERAGE\\s*\\(([^)]+)\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern COUNT_PATTERN = Pattern.compile("COUNT\\s*\\(([^)]+)\\)", Pattern.CASE_INSENSITIVE);
    
    /**
     * Evaluate a formula given a map of cell values
     * @param formula The formula to evaluate (e.g., "=A1+B1")
     * @param cellValues Map of cell addresses to their computed values
     * @return The computed result as a string
     */
    public String evaluate(String formula, Map<String, String> cellValues) {
        try {
            if (!parser.isFormula(formula)) {
                return formula;
            }
            
            String formulaBody = formula.substring(1).trim(); // Remove '='
            
            // Handle built-in functions first
            formulaBody = handleSumFunction(formulaBody, cellValues);
            formulaBody = handleAverageFunction(formulaBody, cellValues);
            formulaBody = handleCountFunction(formulaBody, cellValues);
            
            // Replace cell references with their values
            formulaBody = replaceCellReferences(formulaBody, cellValues);
            
            // Evaluate the arithmetic expression
            Expression expression = new ExpressionBuilder(formulaBody).build();
            double result = expression.evaluate();
            
            // Handle division by zero
            if (Double.isInfinite(result)) {
                return ErrorType.DIV_ZERO.getSymbol();
            }
            
            if (Double.isNaN(result)) {
                return ErrorType.NUM_ERROR.getSymbol();
            }
            
            // Format result (remove .0 for whole numbers)
            if (result == Math.floor(result)) {
                return String.valueOf((long) result);
            }
            
            return String.valueOf(result);
            
        } catch (FormulaEvaluationException e) {
            log.error("Formula evaluation error: {}", e.getMessage());
            return e.getErrorType().getSymbol();
        } catch (Exception e) {
            log.error("Unexpected error evaluating formula: {}", formula, e);
            return ErrorType.PARSE_ERROR.getSymbol();
        }
    }
    
    /**
     * Replace cell references in formula with their values
     */
    private String replaceCellReferences(String formula, Map<String, String> cellValues) {
        Pattern cellPattern = Pattern.compile("([A-Z]+[0-9]+)");
        Matcher matcher = cellPattern.matcher(formula);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String cellAddress = matcher.group(1);
            String value = cellValues.getOrDefault(cellAddress, "0");
            
            // Check if value is a number
            try {
                Double.parseDouble(value);
                matcher.appendReplacement(result, value);
            } catch (NumberFormatException e) {
                // If not a number, treat as 0 or throw error
                if (value.startsWith("#")) {
                    // Propagate error
                    throw new FormulaEvaluationException(ErrorType.VALUE_ERROR, "Referenced cell contains error: " + cellAddress);
                }
                matcher.appendReplacement(result, "0");
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Handle SUM function: SUM(A1:A10) or SUM(A1,A2,A3)
     */
    private String handleSumFunction(String formula, Map<String, String> cellValues) {
        Matcher matcher = SUM_PATTERN.matcher(formula);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String args = matcher.group(1);
            double sum = calculateSum(args, cellValues);
            matcher.appendReplacement(result, String.valueOf(sum));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Handle AVERAGE function
     */
    private String handleAverageFunction(String formula, Map<String, String> cellValues) {
        Matcher matcher = AVERAGE_PATTERN.matcher(formula);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String args = matcher.group(1);
            double[] sumAndCount = calculateSumAndCount(args, cellValues);
            double average = sumAndCount[1] > 0 ? sumAndCount[0] / sumAndCount[1] : 0;
            matcher.appendReplacement(result, String.valueOf(average));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Handle COUNT function
     */
    private String handleCountFunction(String formula, Map<String, String> cellValues) {
        Matcher matcher = COUNT_PATTERN.matcher(formula);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String args = matcher.group(1);
            double[] sumAndCount = calculateSumAndCount(args, cellValues);
            matcher.appendReplacement(result, String.valueOf((long) sumAndCount[1]));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Calculate sum of cells
     */
    private double calculateSum(String args, Map<String, String> cellValues) {
        Set<CellReference> references = parser.extractCellReferences("=" + args);
        double sum = 0;
        
        for (CellReference ref : references) {
            String address = ref.toAddress();
            String value = cellValues.getOrDefault(address, "0");
            
            try {
                sum += Double.parseDouble(value);
            } catch (NumberFormatException e) {
                // Skip non-numeric values
            }
        }
        
        return sum;
    }
    
    /**
     * Calculate sum and count for average/count functions
     * Returns [sum, count]
     */
    private double[] calculateSumAndCount(String args, Map<String, String> cellValues) {
        Set<CellReference> references = parser.extractCellReferences("=" + args);
        double sum = 0;
        int count = 0;
        
        for (CellReference ref : references) {
            String address = ref.toAddress();
            String value = cellValues.getOrDefault(address, "0");
            
            try {
                sum += Double.parseDouble(value);
                count++;
            } catch (NumberFormatException e) {
                // Skip non-numeric values
            }
        }
        
        return new double[]{sum, count};
    }
}

