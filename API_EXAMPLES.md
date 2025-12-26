# API Usage Examples

## Example 1: Basic Cell Operations

### Step 1: Create a Workbook
```bash
curl -X POST http://localhost:8080/api/workbooks \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My Spreadsheet",
    "sheetName": "Budget 2024"
  }'
```

**Response:**
```json
{
  "success": true,
  "message": "Workbook created successfully",
  "data": {
    "id": 1,
    "name": "My Spreadsheet",
    "sheets": [
      {
        "id": 1,
        "name": "Budget 2024",
        "rowCount": 1000,
        "columnCount": 26
      }
    ]
  }
}
```

### Step 2: Add Data to Cells
```bash
# Add value to A1
curl -X PUT http://localhost:8080/api/sheets/1/cells \
  -H "Content-Type: application/json" \
  -d '{
    "rowIndex": 1,
    "columnIndex": 0,
    "value": "100"
  }'

# Add value to A2
curl -X PUT http://localhost:8080/api/sheets/1/cells \
  -H "Content-Type: application/json" \
  -d '{
    "rowIndex": 2,
    "columnIndex": 0,
    "value": "200"
  }'

# Add formula to A3
curl -X PUT http://localhost:8080/api/sheets/1/cells \
  -H "Content-Type: application/json" \
  -d '{
    "rowIndex": 3,
    "columnIndex": 0,
    "value": "=A1+A2"
  }'
```

**Response for A3:**
```json
{
  "success": true,
  "message": "Cell updated successfully",
  "data": {
    "id": 3,
    "rowIndex": 3,
    "columnIndex": 0,
    "address": "A3",
    "cellType": "FORMULA",
    "rawValue": "=A1+A2",
    "computedValue": "300"
  }
}
```

---

## Example 2: Batch Update

```bash
curl -X PUT http://localhost:8080/api/sheets/1/cells/batch \
  -H "Content-Type: application/json" \
  -d '{
    "cells": [
      {"rowIndex": 1, "columnIndex": 0, "value": "10"},
      {"rowIndex": 2, "columnIndex": 0, "value": "20"},
      {"rowIndex": 3, "columnIndex": 0, "value": "30"},
      {"rowIndex": 4, "columnIndex": 0, "value": "40"},
      {"rowIndex": 5, "columnIndex": 0, "value": "=SUM(A1:A4)"}
    ]
  }'
```

**Response:**
```json
{
  "success": true,
  "message": "Successfully updated 5 cells",
  "data": [
    {
      "address": "A1",
      "computedValue": "10"
    },
    {
      "address": "A5",
      "computedValue": "100"
    }
  ]
}
```

---

## Example 3: Complex Formulas

### Monthly Budget Calculation
```bash
# Income cells
curl -X PUT http://localhost:8080/api/sheets/1/cells/batch \
  -H "Content-Type: application/json" \
  -d '{
    "cells": [
      {"rowIndex": 1, "columnIndex": 0, "value": "5000"},
      {"rowIndex": 2, "columnIndex": 0, "value": "2000"},
      {"rowIndex": 3, "columnIndex": 0, "value": "=A1+A2"}
    ]
  }'

# Expense cells
curl -X PUT http://localhost:8080/api/sheets/1/cells/batch \
  -H "Content-Type: application/json" \
  -d '{
    "cells": [
      {"rowIndex": 5, "columnIndex": 0, "value": "1500"},
      {"rowIndex": 6, "columnIndex": 0, "value": "800"},
      {"rowIndex": 7, "columnIndex": 0, "value": "400"},
      {"rowIndex": 8, "columnIndex": 0, "value": "=SUM(A5:A7)"}
    ]
  }'

# Net savings
curl -X PUT http://localhost:8080/api/sheets/1/cells \
  -H "Content-Type: application/json" \
  -d '{
    "rowIndex": 10,
    "columnIndex": 0,
    "value": "=A3-A8"
  }'
```

**Result:**
- A3 (Total Income): 7000
- A8 (Total Expenses): 2700
- A10 (Net Savings): 4300

---

## Example 4: Using AVERAGE Function

```bash
# Student grades
curl -X PUT http://localhost:8080/api/sheets/1/cells/batch \
  -H "Content-Type: application/json" \
  -d '{
    "cells": [
      {"rowIndex": 1, "columnIndex": 1, "value": "85"},
      {"rowIndex": 2, "columnIndex": 1, "value": "92"},
      {"rowIndex": 3, "columnIndex": 1, "value": "78"},
      {"rowIndex": 4, "columnIndex": 1, "value": "88"},
      {"rowIndex": 5, "columnIndex": 1, "value": "=AVERAGE(B1:B4)"}
    ]
  }'
```

**Result:**
- B5: 85.75

---

## Example 5: Error Scenarios

### Division by Zero
```bash
curl -X PUT http://localhost:8080/api/sheets/1/cells \
  -H "Content-Type: application/json" \
  -d '{
    "rowIndex": 1,
    "columnIndex": 0,
    "value": "=10/0"
  }'
```

**Response:**
```json
{
  "success": true,
  "data": {
    "computedValue": "#DIV/0!"
  }
}
```

### Circular Dependency
```bash
# Create A1 = A2
curl -X PUT http://localhost:8080/api/sheets/1/cells \
  -H "Content-Type: application/json" \
  -d '{
    "rowIndex": 1,
    "columnIndex": 0,
    "value": "=A2"
  }'

# Try to create A2 = A1 (will fail)
curl -X PUT http://localhost:8080/api/sheets/1/cells \
  -H "Content-Type: application/json" \
  -d '{
    "rowIndex": 2,
    "columnIndex": 0,
    "value": "=A1"
  }'
```

**Response:**
```json
{
  "success": false,
  "message": "#CYCLE! Circular dependency detected for cell A2"
}
```

---

## Example 6: Get Sheet Data

```bash
curl -X GET http://localhost:8080/api/sheets/1
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "Budget 2024",
    "rowCount": 1000,
    "columnCount": 26,
    "cells": [
      {
        "address": "A1",
        "cellType": "NUMBER",
        "rawValue": "100",
        "computedValue": "100"
      },
      {
        "address": "A3",
        "cellType": "FORMULA",
        "rawValue": "=A1+A2",
        "computedValue": "300"
      }
    ]
  }
}
```

---

## Example 7: Clear Cell

```bash
curl -X PUT http://localhost:8080/api/sheets/1/cells \
  -H "Content-Type: application/json" \
  -d '{
    "rowIndex": 1,
    "columnIndex": 0,
    "value": ""
  }'
```

**Response:**
```json
{
  "success": true,
  "message": "Cell cleared successfully",
  "data": null
}
```

---

## Example 8: Using COUNT Function

```bash
curl -X PUT http://localhost:8080/api/sheets/1/cells/batch \
  -H "Content-Type: application/json" \
  -d '{
    "cells": [
      {"rowIndex": 1, "columnIndex": 0, "value": "10"},
      {"rowIndex": 2, "columnIndex": 0, "value": "20"},
      {"rowIndex": 3, "columnIndex": 0, "value": "hello"},
      {"rowIndex": 4, "columnIndex": 0, "value": "30"},
      {"rowIndex": 5, "columnIndex": 0, "value": "=COUNT(A1:A4)"}
    ]
  }'
```

**Result:**
- A5: 3 (counts only numeric cells)

---

## Example 9: Complex Nested Formula

```bash
curl -X PUT http://localhost:8080/api/sheets/1/cells \
  -H "Content-Type: application/json" \
  -d '{
    "rowIndex": 10,
    "columnIndex": 0,
    "value": "=(SUM(A1:A5)+10)*2"
  }'
```

---

## Example 10: Multi-Column Operations

```bash
# Sales data
curl -X PUT http://localhost:8080/api/sheets/1/cells/batch \
  -H "Content-Type: application/json" \
  -d '{
    "cells": [
      {"rowIndex": 1, "columnIndex": 0, "value": "100"},
      {"rowIndex": 1, "columnIndex": 1, "value": "5"},
      {"rowIndex": 1, "columnIndex": 2, "value": "=A1*B1"},
      {"rowIndex": 2, "columnIndex": 0, "value": "200"},
      {"rowIndex": 2, "columnIndex": 1, "value": "3"},
      {"rowIndex": 2, "columnIndex": 2, "value": "=A2*B2"},
      {"rowIndex": 3, "columnIndex": 2, "value": "=SUM(C1:C2)"}
    ]
  }'
```

**Result:**
- C1: 500 (100 × 5)
- C2: 600 (200 × 3)
- C3: 1100 (Total)

---

## Testing Checklist

- [x] Create workbook
- [x] Update single cell
- [x] Batch update cells
- [x] Formula evaluation
- [x] SUM function
- [x] AVERAGE function
- [x] COUNT function
- [x] Error handling (#DIV/0!)
- [x] Cycle detection (#CYCLE!)
- [x] Cell references
- [x] Range operations
- [x] Clear cells
- [x] Get cell data
- [x] Get all cells

