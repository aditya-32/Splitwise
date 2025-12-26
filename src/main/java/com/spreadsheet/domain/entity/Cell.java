package com.spreadsheet.domain.entity;

import com.spreadsheet.domain.enums.CellType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "cells",
    uniqueConstraints = @UniqueConstraint(columnNames = {"sheet_id", "row_index", "column_index"}),
    indexes = {
        @Index(name = "idx_sheet_id", columnList = "sheet_id"),
        @Index(name = "idx_sheet_row_col", columnList = "sheet_id, row_index, column_index")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cell {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sheet_id", nullable = false)
    private Sheet sheet;
    
    @Column(name = "row_index", nullable = false)
    private Integer rowIndex; // 1-based: 1, 2, 3...
    
    @Column(name = "column_index", nullable = false)
    private Integer columnIndex; // 0-based: 0=A, 1=B, 2=C...
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CellType cellType;
    
    @Column(columnDefinition = "TEXT")
    private String rawValue; // Stores "=A1+B1" for formulas, "42" for numbers, "text" for text
    
    @Column(columnDefinition = "TEXT")
    private String computedValue; // Evaluated result for formulas, same as rawValue for others
    
    @Version
    private Long version;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * Get the cell address in A1 notation (e.g., "A1", "B5")
     */
    public String getAddress() {
        return columnIndexToLetter(columnIndex) + rowIndex;
    }
    
    /**
     * Convert column index to letter (0 -> A, 1 -> B, 25 -> Z)
     */
    public static String columnIndexToLetter(int index) {
        StringBuilder result = new StringBuilder();
        while (index >= 0) {
            result.insert(0, (char) ('A' + (index % 26)));
            index = index / 26 - 1;
        }
        return result.toString();
    }
    
    /**
     * Convert column letter to index (A -> 0, B -> 1, Z -> 25)
     */
    public static int columnLetterToIndex(String letter) {
        int result = 0;
        for (int i = 0; i < letter.length(); i++) {
            result = result * 26 + (letter.charAt(i) - 'A' + 1);
        }
        return result - 1;
    }
}

