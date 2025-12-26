package com.spreadsheet.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sheets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sheet {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workbook_id", nullable = false)
    private Workbook workbook;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer rowCount = 1000;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer columnCount = 26; // A-Z
    
    @OneToMany(mappedBy = "sheet", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Cell> cells = new ArrayList<>();
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    public void addCell(Cell cell) {
        cells.add(cell);
        cell.setSheet(this);
    }
    
    public void removeCell(Cell cell) {
        cells.remove(cell);
        cell.setSheet(null);
    }
}

