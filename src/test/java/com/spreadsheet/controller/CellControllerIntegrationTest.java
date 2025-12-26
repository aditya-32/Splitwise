package com.spreadsheet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spreadsheet.dto.request.CreateWorkbookRequest;
import com.spreadsheet.dto.request.UpdateCellRequest;
import com.spreadsheet.dto.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CellControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void testFullWorkflowCreateAndUpdateCells() throws Exception {
        // 1. Create workbook
        CreateWorkbookRequest workbookRequest = CreateWorkbookRequest.builder()
                .name("Test Workbook")
                .sheetName("Sheet1")
                .build();
        
        MvcResult workbookResult = mockMvc.perform(post("/api/workbooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(workbookRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        
        // Extract sheet ID from response
        String responseBody = workbookResult.getResponse().getContentAsString();
        Long sheetId = 1L; // For simplicity, assuming first sheet
        
        // 2. Update cell A1 with value 10
        UpdateCellRequest cellRequest1 = UpdateCellRequest.builder()
                .rowIndex(1)
                .columnIndex(0)
                .value("10")
                .build();
        
        mockMvc.perform(put("/api/sheets/" + sheetId + "/cells")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cellRequest1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.computedValue").value("10"));
        
        // 3. Update cell A2 with value 20
        UpdateCellRequest cellRequest2 = UpdateCellRequest.builder()
                .rowIndex(2)
                .columnIndex(0)
                .value("20")
                .build();
        
        mockMvc.perform(put("/api/sheets/" + sheetId + "/cells")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cellRequest2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        
        // 4. Update cell A3 with formula =A1+A2
        UpdateCellRequest cellRequest3 = UpdateCellRequest.builder()
                .rowIndex(3)
                .columnIndex(0)
                .value("=A1+A2")
                .build();
        
        mockMvc.perform(put("/api/sheets/" + sheetId + "/cells")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cellRequest3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.cellType").value("FORMULA"))
                .andExpect(jsonPath("$.data.computedValue").value("30"));
        
        // 5. Get all cells
        mockMvc.perform(get("/api/sheets/" + sheetId + "/cells/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }
    
    @Test
    void testCyclicDependencyDetection() throws Exception {
        // Create workbook and sheet
        CreateWorkbookRequest workbookRequest = CreateWorkbookRequest.builder()
                .name("Test Workbook")
                .build();
        
        mockMvc.perform(post("/api/workbooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(workbookRequest)))
                .andExpect(status().isCreated());
        
        Long sheetId = 1L;
        
        // Create A1 = A2
        UpdateCellRequest cellRequest1 = UpdateCellRequest.builder()
                .rowIndex(1)
                .columnIndex(0)
                .value("=A2")
                .build();
        
        mockMvc.perform(put("/api/sheets/" + sheetId + "/cells")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cellRequest1)))
                .andExpect(status().isOk());
        
        // Try to create A2 = A1 (should fail with cycle)
        UpdateCellRequest cellRequest2 = UpdateCellRequest.builder()
                .rowIndex(2)
                .columnIndex(0)
                .value("=A1")
                .build();
        
        mockMvc.perform(put("/api/sheets/" + sheetId + "/cells")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cellRequest2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("CYCLE")));
    }
}

