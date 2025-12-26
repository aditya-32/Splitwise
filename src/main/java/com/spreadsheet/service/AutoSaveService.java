package com.spreadsheet.service;

import com.spreadsheet.domain.entity.Cell;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Handles auto-save functionality with debouncing
 */
@Service
@Slf4j
public class AutoSaveService {
    
    @Value("${spreadsheet.autosave.enabled:true}")
    private boolean autoSaveEnabled;
    
    @Value("${spreadsheet.autosave.batch-size:100}")
    private int batchSize;
    
    private final Queue<Cell> pendingChanges = new ConcurrentLinkedQueue<>();
    
    /**
     * Listen for cell update events
     */
    @Async
    @EventListener
    public void handleCellUpdate(CellService.CellUpdatedEvent event) {
        if (!autoSaveEnabled) {
            return;
        }
        
        Cell cell = event.getCell();
        log.debug("Queuing cell {} for auto-save", cell.getAddress());
        
        // Add to pending changes queue
        pendingChanges.offer(cell);
        
        // If queue is too large, trigger immediate save
        if (pendingChanges.size() >= batchSize) {
            log.info("Queue size exceeded batch size, triggering immediate save");
            processPendingChanges();
        }
    }
    
    /**
     * Scheduled task to process pending changes
     * Runs every 3 seconds (configurable via application.yml)
     */
    @Scheduled(fixedDelayString = "${spreadsheet.autosave.interval-ms:3000}")
    public void scheduledAutoSave() {
        if (!autoSaveEnabled || pendingChanges.isEmpty()) {
            return;
        }
        
        log.info("Auto-save triggered with {} pending changes", pendingChanges.size());
        processPendingChanges();
    }
    
    /**
     * Process all pending changes
     */
    private void processPendingChanges() {
        int count = 0;
        
        while (!pendingChanges.isEmpty() && count < batchSize) {
            Cell cell = pendingChanges.poll();
            if (cell != null) {
                log.debug("Auto-saved cell: {}", cell.getAddress());
                count++;
            }
        }
        
        if (count > 0) {
            log.info("Auto-saved {} cells", count);
        }
    }
    
    /**
     * Get pending changes count (for monitoring)
     */
    public int getPendingChangesCount() {
        return pendingChanges.size();
    }
}

