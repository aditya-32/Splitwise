package com.spreadsheet.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchUpdateCellsRequest {
    @NotEmpty(message = "Cell updates list cannot be empty")
    @Valid
    private List<UpdateCellRequest> cells;
}

