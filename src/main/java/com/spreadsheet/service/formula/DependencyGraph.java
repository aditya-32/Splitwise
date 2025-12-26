package com.spreadsheet.service.formula;

import com.spreadsheet.domain.entity.Cell;
import com.spreadsheet.domain.enums.ErrorType;
import com.spreadsheet.exception.CyclicDependencyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Manages cell dependencies and detects cycles
 */
@Component
@Slf4j
public class DependencyGraph {
    
    /**
     * Build dependency graph for all formula cells in a sheet
     * Returns map: cell -> set of cells it depends on
     */
    public Map<String, Set<String>> buildDependencyGraph(List<Cell> cells, FormulaParser parser) {
        Map<String, Set<String>> graph = new HashMap<>();
        
        for (Cell cell : cells) {
            String cellAddress = cell.getAddress();
            
            if (cell.getRawValue() != null && parser.isFormula(cell.getRawValue())) {
                Set<CellReference> references = parser.extractCellReferences(cell.getRawValue());
                Set<String> dependencies = new HashSet<>();
                
                for (CellReference ref : references) {
                    dependencies.add(ref.toAddress());
                }
                
                graph.put(cellAddress, dependencies);
            }
        }
        
        return graph;
    }
    
    /**
     * Detect if adding a dependency would create a cycle
     * Returns true if cycle would be created
     */
    public boolean wouldCreateCycle(Map<String, Set<String>> graph, String fromCell, Set<String> toCells) {
        // Create a temporary graph with the new dependency
        Map<String, Set<String>> tempGraph = new HashMap<>(graph);
        tempGraph.put(fromCell, new HashSet<>(toCells));
        
        try {
            topologicalSort(tempGraph);
            return false; // No cycle
        } catch (CyclicDependencyException e) {
            return true; // Cycle detected
        }
    }
    
    /**
     * Perform topological sort to get evaluation order
     * Throws CyclicDependencyException if cycle detected
     */
    public List<String> topologicalSort(Map<String, Set<String>> graph) {
        List<String> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        
        // Get all cells (nodes in the graph)
        Set<String> allCells = new HashSet<>(graph.keySet());
        for (Set<String> deps : graph.values()) {
            allCells.addAll(deps);
        }
        
        for (String cell : allCells) {
            if (!visited.contains(cell)) {
                dfs(cell, graph, visited, visiting, result);
            }
        }
        
        // No need to reverse - DFS adds nodes in post-order (dependencies before dependents)
        return result;
    }
    
    /**
     * DFS for cycle detection and topological sort
     */
    private void dfs(String cell, Map<String, Set<String>> graph, 
                    Set<String> visited, Set<String> visiting, List<String> result) {
        
        if (visiting.contains(cell)) {
            // Cycle detected
            throw new CyclicDependencyException(ErrorType.CYCLE_ERROR, 
                    "Circular dependency detected involving cell: " + cell);
        }
        
        if (visited.contains(cell)) {
            return; // Already processed
        }
        
        visiting.add(cell);
        
        // Visit dependencies
        Set<String> dependencies = graph.getOrDefault(cell, Collections.emptySet());
        for (String dep : dependencies) {
            dfs(dep, graph, visited, visiting, result);
        }
        
        visiting.remove(cell);
        visited.add(cell);
        result.add(cell);
    }
    
    /**
     * Find all cells that depend on a given cell (directly or indirectly)
     */
    public Set<String> findDependentCells(Map<String, Set<String>> graph, String targetCell) {
        Set<String> dependents = new HashSet<>();
        
        for (Map.Entry<String, Set<String>> entry : graph.entrySet()) {
            String cell = entry.getKey();
            Set<String> dependencies = entry.getValue();
            
            if (dependencies.contains(targetCell)) {
                dependents.add(cell);
                // Recursively find cells that depend on this cell
                dependents.addAll(findDependentCells(graph, cell));
            }
        }
        
        return dependents;
    }
}

