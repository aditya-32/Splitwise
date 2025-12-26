package com.spreadsheet.repository;

import com.spreadsheet.domain.entity.Sheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SheetRepository extends JpaRepository<Sheet, Long> {
    
    List<Sheet> findByWorkbookId(Long workbookId);
    
    @Query("SELECT s FROM Sheet s LEFT JOIN FETCH s.cells WHERE s.id = :id")
    Optional<Sheet> findByIdWithCells(@Param("id") Long id);
}

