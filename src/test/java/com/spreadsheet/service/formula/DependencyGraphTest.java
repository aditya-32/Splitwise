package com.spreadsheet.service.formula;

import com.spreadsheet.domain.entity.Cell;
import com.spreadsheet.domain.entity.Sheet;
import com.spreadsheet.domain.enums.CellType;
import com.spreadsheet.exception.CyclicDependencyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DependencyGraphTest {
    
    private DependencyGraph dependencyGraph;
    private FormulaParser parser;
    
    @BeforeEach
    void setUp() {
        dependencyGraph = new DependencyGraph();
        parser = new FormulaParser();
    }
    
    @Test
    void testBuildDependencyGraphSimple() {
        List<Cell> cells = createCells(
                createCell("A1", "10"),
                createCell("A2", "20"),
                createCell("A3", "=A1+A2")
        );
        
        Map<String, Set<String>> graph = dependencyGraph.buildDependencyGraph(cells, parser);
        
        assertEquals(1, graph.size());
        assertTrue(graph.containsKey("A3"));
        assertEquals(2, graph.get("A3").size());
        assertTrue(graph.get("A3").contains("A1"));
        assertTrue(graph.get("A3").contains("A2"));
    }
    
    @Test
    void testBuildDependencyGraphChain() {
        List<Cell> cells = createCells(
                createCell("A1", "10"),
                createCell("A2", "=A1*2"),
                createCell("A3", "=A2+5")
        );
        
        Map<String, Set<String>> graph = dependencyGraph.buildDependencyGraph(cells, parser);
        
        assertEquals(2, graph.size());
        assertTrue(graph.get("A2").contains("A1"));
        assertTrue(graph.get("A3").contains("A2"));
    }
    
    @Test
    void testTopologicalSortSimple() {
        Map<String, Set<String>> graph = new HashMap<>();
        graph.put("A3", new HashSet<>(Arrays.asList("A1", "A2")));
        
        List<String> order = dependencyGraph.topologicalSort(graph);
        
        // The result should contain all cells (A1, A2, A3)
        assertEquals(3, order.size(), "Should have 3 cells");
        assertTrue(order.contains("A3"), "Result should contain A3");
        assertTrue(order.contains("A1"), "Result should contain A1");
        assertTrue(order.contains("A2"), "Result should contain A2");
        
        // A1 and A2 should come before A3
        int indexA3 = order.indexOf("A3");
        int indexA1 = order.indexOf("A1");
        int indexA2 = order.indexOf("A2");
        
        assertTrue(indexA1 < indexA3, "A1 should come before A3");
        assertTrue(indexA2 < indexA3, "A2 should come before A3");
    }
    
    @Test
    void testTopologicalSortChain() {
        Map<String, Set<String>> graph = new HashMap<>();
        graph.put("A2", new HashSet<>(Collections.singletonList("A1")));
        graph.put("A3", new HashSet<>(Collections.singletonList("A2")));
        
        List<String> order = dependencyGraph.topologicalSort(graph);
        
        // The result should contain all three cells
        assertEquals(3, order.size(), "Should have 3 cells");
        assertTrue(order.contains("A1"), "Result should contain A1");
        assertTrue(order.contains("A2"), "Result should contain A2");
        assertTrue(order.contains("A3"), "Result should contain A3");
        
        int indexA1 = order.indexOf("A1");
        int indexA2 = order.indexOf("A2");
        int indexA3 = order.indexOf("A3");
        
        // Check correct ordering: A1 -> A2 -> A3
        assertTrue(indexA1 < indexA2, "A1 should come before A2");
        assertTrue(indexA2 < indexA3, "A2 should come before A3");
    }
    
    @Test
    void testDetectCycleSimple() {
        Map<String, Set<String>> graph = new HashMap<>();
        graph.put("A1", new HashSet<>(Collections.singletonList("A2")));
        graph.put("A2", new HashSet<>(Collections.singletonList("A1")));
        
        assertThrows(CyclicDependencyException.class, () -> dependencyGraph.topologicalSort(graph));
    }
    
    @Test
    void testDetectCycleSelfReference() {
        Map<String, Set<String>> graph = new HashMap<>();
        graph.put("A1", new HashSet<>(Collections.singletonList("A1")));
        
        assertThrows(CyclicDependencyException.class, () -> dependencyGraph.topologicalSort(graph));
    }
    
    @Test
    void testDetectCycleComplex() {
        Map<String, Set<String>> graph = new HashMap<>();
        graph.put("A1", new HashSet<>(Collections.singletonList("A2")));
        graph.put("A2", new HashSet<>(Collections.singletonList("A3")));
        graph.put("A3", new HashSet<>(Collections.singletonList("A1")));
        
        assertThrows(CyclicDependencyException.class, () -> dependencyGraph.topologicalSort(graph));
    }
    
    @Test
    void testWouldCreateCycle() {
        Map<String, Set<String>> graph = new HashMap<>();
        graph.put("A2", new HashSet<>(Collections.singletonList("A1")));
        
        // Adding A1 -> A2 would create a cycle
        boolean wouldCycle = dependencyGraph.wouldCreateCycle(
                graph, "A1", new HashSet<>(Collections.singletonList("A2"))
        );
        assertTrue(wouldCycle);
        
        // Adding A3 -> A1 would not create a cycle
        wouldCycle = dependencyGraph.wouldCreateCycle(
                graph, "A3", new HashSet<>(Collections.singletonList("A1"))
        );
        assertFalse(wouldCycle);
    }
    
    @Test
    void testFindDependentCells() {
        Map<String, Set<String>> graph = new HashMap<>();
        graph.put("A2", new HashSet<>(Collections.singletonList("A1")));
        graph.put("A3", new HashSet<>(Collections.singletonList("A2")));
        graph.put("A4", new HashSet<>(Collections.singletonList("A3")));
        
        Set<String> dependents = dependencyGraph.findDependentCells(graph, "A1");
        
        assertEquals(3, dependents.size());
        assertTrue(dependents.contains("A2"));
        assertTrue(dependents.contains("A3"));
        assertTrue(dependents.contains("A4"));
    }
    
    @Test
    void testFindDependentCellsMultiplePaths() {
        Map<String, Set<String>> graph = new HashMap<>();
        graph.put("A2", new HashSet<>(Collections.singletonList("A1")));
        graph.put("A3", new HashSet<>(Collections.singletonList("A1")));
        graph.put("A4", new HashSet<>(Arrays.asList("A2", "A3")));
        
        Set<String> dependents = dependencyGraph.findDependentCells(graph, "A1");
        
        assertEquals(3, dependents.size());
        assertTrue(dependents.contains("A2"));
        assertTrue(dependents.contains("A3"));
        assertTrue(dependents.contains("A4"));
    }
    
    // Helper methods
    private Cell createCell(String address, String rawValue) {
        CellReference ref = CellReference.fromAddress(address);
        CellType type = rawValue.startsWith("=") ? CellType.FORMULA : CellType.NUMBER;
        
        return Cell.builder()
                .rowIndex(ref.getRowIndex())
                .columnIndex(ref.getColumnIndex())
                .cellType(type)
                .rawValue(rawValue)
                .computedValue(type == CellType.FORMULA ? null : rawValue)
                .sheet(new Sheet())
                .build();
    }
    
    private List<Cell> createCells(Cell... cells) {
        return Arrays.asList(cells);
    }
}

