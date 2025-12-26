package com.spreadsheet.service.formula;

import lombok.*;

/**
 * Represents a cell reference (e.g., A1, B5)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CellReference {
    private Integer rowIndex;
    private Integer columnIndex;
    
    public String toAddress() {
        return columnIndexToLetter(columnIndex) + rowIndex;
    }
    
    public static CellReference fromAddress(String address) {
        // Parse "A1", "B5", "AA10" etc.
        String columnPart = address.replaceAll("[0-9]", "");
        String rowPart = address.replaceAll("[A-Z]", "");
        
        return CellReference.builder()
                .columnIndex(columnLetterToIndex(columnPart))
                .rowIndex(Integer.parseInt(rowPart))
                .build();
    }
    
    private static String columnIndexToLetter(int index) {
        StringBuilder result = new StringBuilder();
        while (index >= 0) {
            result.insert(0, (char) ('A' + (index % 26)));
            index = index / 26 - 1;
        }
        return result.toString();
    }
    
    private static int columnLetterToIndex(String letter) {
        int result = 0;
        for (int i = 0; i < letter.length(); i++) {
            result = result * 26 + (letter.charAt(i) - 'A' + 1);
        }
        return result - 1;
    }
}

