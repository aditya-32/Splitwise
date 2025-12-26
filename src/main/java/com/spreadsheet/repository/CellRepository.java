package com.spreadsheet.repository;

import com.spreadsheet.domain.entity.Cell;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CellRepository extends JpaRepository<Cell, Long> {
    
    Optional<Cell> findBySheetIdAndRowIndexAndColumnIndex(Long sheetId, Integer rowIndex, Integer columnIndex);
    
    List<Cell> findBySheetId(Long sheetId);
    
    @Query("SELECT c FROM Cell c WHERE c.sheet.id = :sheetId AND c.cellType = 'FORMULA'")
    List<Cell> findAllFormulaCellsBySheetId(@Param("sheetId") Long sheetId);
    
    void deleteBySheetIdAndRowIndexAndColumnIndex(Long sheetId, Integer rowIndex, Integer columnIndex);
}

