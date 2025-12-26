package com.spreadsheet.concurrency;

import com.spreadsheet.domain.entity.Cell;
import com.spreadsheet.domain.entity.Sheet;
import com.spreadsheet.domain.entity.Workbook;
import com.spreadsheet.domain.enums.CellType;
import com.spreadsheet.dto.request.UpdateCellRequest;
import com.spreadsheet.repository.CellRepository;
import com.spreadsheet.repository.SheetRepository;
import com.spreadsheet.repository.WorkbookRepository;
import com.spreadsheet.service.CellService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify concurrency handling with optimistic locking and retry mechanism.
 * 
 * This test demonstrates that the application correctly handles concurrent updates to the same cell
 * using JPA's @Version annotation (optimistic locking) and Spring Retry's @Retryable.
 */
@SpringBootTest
class ConcurrencyTest {
    
    @Autowired
    private CellService cellService;
    
    @Autowired
    private WorkbookRepository workbookRepository;
    
    @Autowired
    private SheetRepository sheetRepository;
    
    @Autowired
    private CellRepository cellRepository;
    
    private Long testSheetId;
    
    @BeforeEach
    void setUp() {
        // Clean up
        cellRepository.deleteAll();
        sheetRepository.deleteAll();
        workbookRepository.deleteAll();
        
        // Create test data
        Workbook workbook = Workbook.builder()
                .name("Concurrency Test Workbook")
                .build();
        
        Sheet sheet = Sheet.builder()
                .name("Test Sheet")
                .rowCount(1000)
                .columnCount(26)
                .build();
        
        workbook.addSheet(sheet);
        workbook = workbookRepository.save(workbook);
        testSheetId = workbook.getSheets().get(0).getId();
        
        // Create initial cell
        Cell cell = Cell.builder()
                .sheet(sheet)
                .rowIndex(1)
                .columnIndex(0)
                .cellType(CellType.NUMBER)
                .rawValue("0")
                .computedValue("0")
                .build();
        cellRepository.save(cell);
    }
    
    @Test
    void testConcurrentUpdatesWithOptimisticLocking() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger retryCount = new AtomicInteger(0);
        
        // Launch multiple threads trying to update the same cell simultaneously
        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    UpdateCellRequest request = UpdateCellRequest.builder()
                            .rowIndex(1)
                            .columnIndex(0)
                            .value(String.valueOf(threadNum * 100))
                            .build();
                    
                    // The @Retryable annotation in CellService will automatically retry
                    // if an OptimisticLockingFailureException occurs
                    cellService.updateCell(testSheetId, request);
                    successCount.incrementAndGet();
                    
                } catch (ObjectOptimisticLockingFailureException e) {
                    retryCount.incrementAndGet();
                    System.out.println("Thread " + threadNum + " failed after retries: " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("Thread " + threadNum + " error: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for all threads to complete (with timeout)
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        
        assertTrue(completed, "All threads should complete within timeout");
        
        // Verify results
        System.out.println("Success count: " + successCount.get());
        System.out.println("Retry count: " + retryCount.get());
        
        // At least some updates should succeed (with retries)
        assertTrue(successCount.get() > 0, "At least some updates should succeed");
        
        // The final value should be from one of the threads
        Cell finalCell = cellRepository.findBySheetIdAndRowIndexAndColumnIndex(testSheetId, 1, 0)
                .orElseThrow();
        
        assertNotNull(finalCell.getRawValue());
        System.out.println("Final cell value: " + finalCell.getRawValue());
        System.out.println("Final cell version: " + finalCell.getVersion());
        
        // Version should be >= successCount (each successful update increments version)
        assertTrue(finalCell.getVersion() >= successCount.get() - 1, 
                "Version should reflect number of successful updates");
    }
    
    @Test
    void testOptimisticLockingVersionIncrement() {
        // Get initial cell
        Cell cell = cellRepository.findBySheetIdAndRowIndexAndColumnIndex(testSheetId, 1, 0)
                .orElseThrow();
        Long initialVersion = cell.getVersion();
        
        // Update cell multiple times
        for (int i = 1; i <= 5; i++) {
            UpdateCellRequest request = UpdateCellRequest.builder()
                    .rowIndex(1)
                    .columnIndex(0)
                    .value(String.valueOf(i * 10))
                    .build();
            
            cellService.updateCell(testSheetId, request);
        }
        
        // Get updated cell
        Cell updatedCell = cellRepository.findBySheetIdAndRowIndexAndColumnIndex(testSheetId, 1, 0)
                .orElseThrow();
        
        // Version should have incremented by 5
        assertEquals(initialVersion + 5, updatedCell.getVersion(), 
                "Version should increment with each update");
        
        assertEquals("50", updatedCell.getRawValue(), 
                "Final value should be from last update");
    }
    
    @Test
    void testRetryMechanismWithSimulatedConflict() throws InterruptedException {
        // This test demonstrates that the @Retryable annotation works
        // by updating the same cell from two threads with a slight delay
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        
        AtomicInteger successCount = new AtomicInteger(0);
        
        // Thread 1
        Thread t1 = new Thread(() -> {
            try {
                startLatch.await(); // Wait for signal
                UpdateCellRequest request = UpdateCellRequest.builder()
                        .rowIndex(1)
                        .columnIndex(0)
                        .value("100")
                        .build();
                cellService.updateCell(testSheetId, request);
                successCount.incrementAndGet();
            } catch (Exception e) {
                System.err.println("Thread 1 error: " + e.getMessage());
            } finally {
                doneLatch.countDown();
            }
        });
        
        // Thread 2
        Thread t2 = new Thread(() -> {
            try {
                startLatch.await(); // Wait for signal
                UpdateCellRequest request = UpdateCellRequest.builder()
                        .rowIndex(1)
                        .columnIndex(0)
                        .value("200")
                        .build();
                cellService.updateCell(testSheetId, request);
                successCount.incrementAndGet();
            } catch (Exception e) {
                System.err.println("Thread 2 error: " + e.getMessage());
            } finally {
                doneLatch.countDown();
            }
        });
        
        t1.start();
        t2.start();
        
        // Release both threads simultaneously
        startLatch.countDown();
        
        // Wait for completion
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        assertTrue(completed, "Both threads should complete");
        
        // Both should succeed thanks to retry mechanism
        assertEquals(2, successCount.get(), 
                "Both threads should succeed with retry mechanism");
        
        // Final cell should have one of the values
        Cell finalCell = cellRepository.findBySheetIdAndRowIndexAndColumnIndex(testSheetId, 1, 0)
                .orElseThrow();
        assertTrue(finalCell.getRawValue().equals("100") || finalCell.getRawValue().equals("200"),
                "Final value should be from one of the threads");
    }
}

