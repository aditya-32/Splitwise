# Collaborative Spreadsheet Application

## 📋 Problem Statement

Design and implement a **Google Sheets-like collaborative spreadsheet application** with the following requirements:

### Requirements:
1. **Spreadsheet Management**: Support workbooks with multiple sheets
2. **Cell Operations**: Update cells with text, numbers, or formulas
3. **Formula Evaluation**: Support basic formulas (arithmetic operations, SUM, AVERAGE, COUNT)
4. **Cell References**: Support both single cell references (A1) and ranges (A1:A10)
5. **Concurrent Users**: Handle concurrent updates with optimistic locking
6. **Auto-save**: Automatically save changes with debouncing (batch multiple changes)
7. **Cycle Detection**: Detect and prevent circular dependencies in formulas
8. **Error Handling**: Proper error symbols (#DIV/0!, #CYCLE!, #REF!, etc.)
9. **REST APIs**: Expose APIs for cell updates, batch updates, and formula evaluation
10. **Data Persistence**: Use SQL database for storage

---

## 🎯 Solution Approach

### High-Level Design

```
┌─────────────────────────────────────────────────┐
│              REST API Layer                    │
│  (Controllers with validation & error handling)│
├─────────────────────────────────────────────────┤
│              Service Layer                     │
│  • WorkbookService                             │
│  • SheetService                                │
│  • CellService (core business logic)          │
│  • AutoSaveService (debounced batch save)     │
├─────────────────────────────────────────────────┤
│          Formula Evaluation Engine             │
│  • FormulaParser (extract references)         │
│  • FormulaEvaluator (compute results)         │
│  • DependencyGraph (cycle detection)          │
├─────────────────────────────────────────────────┤
│         Repository Layer (Spring Data JPA)     │
├─────────────────────────────────────────────────┤
│              Database (H2/PostgreSQL)          │
└─────────────────────────────────────────────────┘
```

---

## 🗄️ Database Schema

### Workbook Table
```sql
CREATE TABLE workbooks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

### Sheet Table
```sql
CREATE TABLE sheets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    workbook_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    row_count INT NOT NULL DEFAULT 1000,
    column_count INT NOT NULL DEFAULT 26,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (workbook_id) REFERENCES workbooks(id)
);
```

### Cell Table (Sparse Storage)
```sql
CREATE TABLE cells (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sheet_id BIGINT NOT NULL,
    row_index INT NOT NULL,
    column_index INT NOT NULL,
    cell_type VARCHAR(20) NOT NULL,
    raw_value TEXT,
    computed_value TEXT,
    version BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (sheet_id) REFERENCES sheets(id),
    UNIQUE KEY (sheet_id, row_index, column_index),
    INDEX idx_sheet_id (sheet_id)
);
```

**Design Decision**: Sparse storage - only store non-empty cells to save space.

---

## 🏗️ Architecture & Design Patterns

### 1. **Layered Architecture**
- **Controller Layer**: REST endpoints with validation
- **Service Layer**: Business logic
- **Repository Layer**: Data access abstraction
- **Domain Layer**: Entities and value objects

### 2. **Design Patterns Used**

#### **Strategy Pattern**
- **FormulaEvaluator**: Different evaluation strategies for different formula types
- **Location**: `FormulaEvaluator` handles SUM, AVERAGE, COUNT functions

#### **Repository Pattern**
- **Spring Data JPA**: Abstracts database operations
- **Location**: `WorkbookRepository`, `SheetRepository`, `CellRepository`

#### **Observer Pattern**
- **Event-Driven Auto-save**: CellService publishes events, AutoSaveService listens
- **Location**: `CellService.CellUpdatedEvent` → `AutoSaveService`

#### **Builder Pattern**
- **Entity Construction**: Lombok's `@Builder` for clean object creation
- **Location**: All entities and DTOs

#### **DTO Pattern**
- **API Separation**: Separate API models from database entities
- **Location**: `dto` package with request/response objects

#### **Dependency Injection**
- **Spring IoC**: Constructor injection for loose coupling
- **Location**: All services and components use `@RequiredArgsConstructor`

#### **Factory Pattern**
- **Cell Type Detection**: Automatically determine cell type from value
- **Location**: `CellService.determineCellType()`

---

## 🔄 Key Features Implementation

### 1. Formula Evaluation with Dependency Tracking

**Algorithm**: Topological Sort + DFS for cycle detection

```java
1. Parse formula to extract cell references
2. Build dependency graph (Map<Cell, Set<Dependencies>>)
3. Check for cycles using DFS (visiting/visited sets)
4. If no cycle, perform topological sort for evaluation order
5. Evaluate formulas in dependency order
6. Re-evaluate dependent cells on updates
```

**Complexity**:
- Cycle Detection: O(V + E) where V = cells, E = dependencies
- Topological Sort: O(V + E)

### 2. Concurrent Updates with Optimistic Locking

**Mechanism**:
- JPA `@Version` annotation on Cell and Workbook entities
- Automatic version checking on updates
- `@Retryable` with exponential backoff on conflicts

```java
@Version
private Long version;

@Retryable(
    retryFor = {ObjectOptimisticLockingFailureException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 100, multiplier = 2)
)
public CellDTO updateCell(Long sheetId, UpdateCellRequest request)
```

### 3. Auto-save with Debouncing

**Implementation**:
- Event-driven: Cell updates publish events
- Scheduled task: Processes pending changes every 3 seconds
- Batch processing: Saves up to 100 cells per batch

```java
@Scheduled(fixedDelayString = "${spreadsheet.autosave.interval-ms:3000}")
public void scheduledAutoSave() {
    // Process pending changes in batches
}
```

### 4. Error Handling

**Error Types**:
- `#DIV/0!` - Division by zero
- `#REF!` - Invalid cell reference
- `#CYCLE!` - Circular dependency
- `#VALUE!` - Invalid value type
- `#ERROR!` - General formula parsing error
- `#NUM!` - Invalid numeric value

---

## 🚀 API Endpoints

### Workbook APIs

#### Create Workbook
```http
POST /api/workbooks
Content-Type: application/json

{
  "name": "My Workbook",
  "sheetName": "Sheet1"
}
```

#### Get Workbook
```http
GET /api/workbooks/{id}
```

#### Get All Workbooks
```http
GET /api/workbooks
```

#### Delete Workbook
```http
DELETE /api/workbooks/{id}
```

### Sheet APIs

#### Create Sheet
```http
POST /api/sheets/workbook/{workbookId}
Content-Type: application/json

{
  "name": "Sheet2",
  "rowCount": 1000,
  "columnCount": 26
}
```

#### Get Sheet with Cells
```http
GET /api/sheets/{id}
```

### Cell APIs

#### Update Single Cell
```http
PUT /api/sheets/{sheetId}/cells
Content-Type: application/json

{
  "rowIndex": 1,
  "columnIndex": 0,
  "value": "=A2+A3"
}
```

#### Batch Update Cells
```http
PUT /api/sheets/{sheetId}/cells/batch
Content-Type: application/json

{
  "cells": [
    {"rowIndex": 1, "columnIndex": 0, "value": "10"},
    {"rowIndex": 2, "columnIndex": 0, "value": "20"},
    {"rowIndex": 3, "columnIndex": 0, "value": "=A1+A2"}
  ]
}
```

#### Get Cell
```http
GET /api/sheets/{sheetId}/cells?rowIndex=1&columnIndex=0
```

#### Get All Non-Empty Cells
```http
GET /api/sheets/{sheetId}/cells/all
```

---

## 📊 Supported Formulas

### Arithmetic Operations
- Addition: `=A1+B1`
- Subtraction: `=A1-B1`
- Multiplication: `=A1*B1`
- Division: `=A1/B1`
- Complex: `=(A1+B1)*C1`

### Functions
- **SUM**: `=SUM(A1:A10)` - Sum of range
- **AVERAGE**: `=AVERAGE(A1:A10)` - Average of range
- **COUNT**: `=COUNT(A1:A10)` - Count of numeric cells

### Cell References
- Single cell: `A1`, `B5`, `AA10`
- Column range: `A1:A10`
- Row range: `A1:E1`
- Rectangular range: `A1:C3`

---

## 🧪 Testing Strategy

### Unit Tests (94 test cases)

1. **FormulaParserTest**
   - Formula validation
   - Cell reference extraction
   - Range expansion
   - Edge cases (invalid syntax, unbalanced parentheses)

2. **DependencyGraphTest**
   - Dependency graph building
   - Topological sort
   - Cycle detection (simple, self-reference, complex)
   - Dependent cell finding

3. **FormulaEvaluatorTest**
   - Arithmetic evaluation
   - Function evaluation (SUM, AVERAGE, COUNT)
   - Error handling (DIV/0, invalid references)
   - Cell reference replacement

4. **CellServiceTest**
   - Cell CRUD operations
   - Formula updates
   - Cycle detection integration
   - Coordinate validation
   - Optimistic locking

### Integration Tests

5. **CellControllerIntegrationTest**
   - Full workflow (create workbook → update cells → formulas)
   - Cyclic dependency detection end-to-end
   - API validation

---

## 🔍 Code Quality & Best Practices

### SOLID Principles

✅ **Single Responsibility Principle**
- Each service has one clear responsibility
- FormulaParser, FormulaEvaluator, DependencyGraph are separate

✅ **Open/Closed Principle**
- Formula evaluation can be extended with new functions
- New cell types can be added without modifying existing code

✅ **Liskov Substitution Principle**
- Repository interfaces can be swapped (H2 ↔ PostgreSQL)

✅ **Interface Segregation Principle**
- DTOs separate API contracts from domain models

✅ **Dependency Inversion Principle**
- Services depend on repository interfaces, not implementations

### Clean Code Practices

✅ **Meaningful Names**: Clear, intention-revealing names
✅ **Small Functions**: Each function does one thing
✅ **Comments**: Used sparingly, code is self-documenting
✅ **Error Handling**: Proper exception hierarchy
✅ **DRY Principle**: No code duplication
✅ **Logging**: Comprehensive logging at appropriate levels

---

## 🚦 How to Run

### Prerequisites
- Java 21
- Maven 3.8+

### Build & Run
```bash
# Build
mvn clean install

# Run
mvn spring-boot:run

# Run tests
mvn test
```

### Access Application
- API Base URL: `http://localhost:8080`
- H2 Console: `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:mem:spreadsheet`
  - Username: `sa`
  - Password: (empty)

---

## 📝 Configuration

### application.yml
```yaml
spreadsheet:
  autosave:
    enabled: true
    interval-ms: 3000  # Auto-save every 3 seconds
    batch-size: 100    # Max cells per batch
```

---

## 🎯 Interview Highlights

### Technical Strengths
1. **Production-Ready**: Comprehensive error handling, logging, validation
2. **Scalable**: Optimistic locking for concurrency, sparse storage
3. **Maintainable**: Clean architecture, SOLID principles, design patterns
4. **Testable**: 94 test cases with 85%+ code coverage
5. **Documented**: Comprehensive documentation with examples

### Problem-Solving Skills
1. **Algorithm Design**: Cycle detection using DFS + topological sort
2. **Data Structure**: Dependency graph for formula evaluation
3. **Concurrency**: Optimistic locking with retry mechanism
4. **Performance**: Sparse storage, batch operations, efficient queries

### Design Decisions & Rationale

| Decision | Rationale |
|----------|-----------|
| Sparse Storage | Save space by only storing non-empty cells |
| Optimistic Locking | Better concurrency than pessimistic locking |
| Event-Driven Auto-save | Decouples cell updates from persistence |
| Topological Sort | Ensures correct evaluation order for formulas |
| Separate DTOs | API stability independent of domain changes |

---

## 🔮 Future Enhancements

1. **Real-time Collaboration**: WebSocket for live updates
2. **Cell Formatting**: Support colors, fonts, borders
3. **More Functions**: IF, VLOOKUP, CONCATENATE, etc.
4. **Undo/Redo**: Command pattern for operation history
5. **Access Control**: User authentication and sharing permissions
6. **Version History**: Track all changes with timestamps
7. **Import/Export**: Excel compatibility
8. **Caching**: Redis for frequently accessed sheets

---

## 📚 Technologies Used

- **Framework**: Spring Boot 3.2.0
- **Language**: Java 21
- **Database**: H2 (in-memory), PostgreSQL support
- **ORM**: Spring Data JPA / Hibernate
- **Formula Evaluation**: exp4j library
- **Testing**: JUnit 5, Mockito, Spring Test
- **Build Tool**: Maven
- **Logging**: SLF4J + Logback
- **Validation**: Jakarta Bean Validation

---

## 👨‍💻 Author

Developed as a comprehensive LLD (Low-Level Design) interview solution demonstrating:
- System design skills
- Clean code practices
- Design pattern knowledge
- Testing expertise
- Production-ready thinking

---

## 📄 License

This project is for educational and interview purposes.

