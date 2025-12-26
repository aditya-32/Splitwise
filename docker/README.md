# Docker Setup for Collaborative Spreadsheet

## 🐳 Quick Start

### 1. Start PostgreSQL Database
```bash
cd docker
docker-compose up -d
```

This will:
- ✅ Start PostgreSQL 15 on port `5432`
- ✅ Create database `spreadsheet`
- ✅ Execute `schema.sql` (create tables)
- ✅ Execute `data.sql` (load sample data)
- ✅ Start Adminer (web-based DB management) on port `8081`

### 2. Verify Database is Running
```bash
docker-compose ps
```

Expected output:
```
NAME                    STATUS              PORTS
spreadsheet-postgres    Up (healthy)        0.0.0.0:5432->5432/tcp
spreadsheet-adminer     Up                  0.0.0.0:8081->8080/tcp
```

### 3. Access Database

#### Option A: Using Adminer (Web UI)
1. Open browser: http://localhost:8081
2. Login with:
   - **System**: PostgreSQL
   - **Server**: postgres
   - **Username**: spreadsheet_user
   - **Password**: spreadsheet_pass
   - **Database**: spreadsheet

#### Option B: Using psql CLI
```bash
docker exec -it spreadsheet-postgres psql -U spreadsheet_user -d spreadsheet
```

#### Option C: Using DBeaver/DataGrip
- **Host**: localhost
- **Port**: 5432
- **Database**: spreadsheet
- **Username**: spreadsheet_user
- **Password**: spreadsheet_pass

---

## 📝 Configuration for Spring Boot Application

### Update application.yml

Create `application-docker.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/spreadsheet
    username: spreadsheet_user
    password: spreadsheet_pass
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: validate  # Don't auto-create, use existing schema
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

spreadsheet:
  autosave:
    enabled: true
    interval-ms: 3000
    batch-size: 100

logging:
  level:
    com.spreadsheet: DEBUG
    org.hibernate.SQL: DEBUG
```

### Run Application with Docker Profile
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=docker
```

Or set environment variable:
```bash
export SPRING_PROFILES_ACTIVE=docker
mvn spring-boot:run
```

---

## 🗃️ Database Schema Overview

### Tables Created

1. **workbooks** - Top-level containers
   - id (BIGSERIAL)
   - name (VARCHAR)
   - version (BIGINT) - for optimistic locking
   - created_at, updated_at (TIMESTAMP)

2. **sheets** - Individual sheets within workbooks
   - id (BIGSERIAL)
   - workbook_id (FK)
   - name (VARCHAR)
   - row_count, column_count (INTEGER)
   - created_at, updated_at (TIMESTAMP)

3. **cells** - Sparse storage for non-empty cells
   - id (BIGSERIAL)
   - sheet_id (FK)
   - row_index, column_index (INTEGER)
   - cell_type (VARCHAR) - TEXT, NUMBER, FORMULA, etc.
   - raw_value (TEXT) - user input
   - computed_value (TEXT) - evaluated result
   - version (BIGINT) - for optimistic locking
   - updated_at (TIMESTAMP)

### Indexes Created

- `idx_sheet_workbook` - Fast sheet lookups by workbook
- `idx_cell_sheet` - Fast cell lookups by sheet
- `idx_cell_position` - Fast cell lookups by position
- `idx_cell_type` - Filter cells by type
- `idx_cell_updated` - Sort by update time

### Sample Data Loaded

After running the setup, you'll have:
- 3 sample workbooks
- 4 sample sheets
- ~35 sample cells with formulas

Example data includes:
- **Personal Budget** - Income/Expenses with SUM formulas
- **Sales Report** - Product sales with calculations
- **Project Timeline** - Task tracking

---

## 🧪 Verify Concurrency Handling

### Test Optimistic Locking

```sql
-- Terminal 1: Start a transaction
BEGIN;
SELECT * FROM cells WHERE id = 1 FOR UPDATE;
UPDATE cells SET raw_value = '100' WHERE id = 1;
-- Don't COMMIT yet

-- Terminal 2: Try to update same cell (will wait/fail)
UPDATE cells SET raw_value = '200' WHERE id = 1;

-- Terminal 1: COMMIT
COMMIT;
```

The Spring Boot app handles this with:
- `@Version` field (automatic version checking)
- `@Retryable` (automatic retry with backoff)

---

## 🔧 Useful Commands

### Stop Services
```bash
docker-compose down
```

### Stop and Remove Data
```bash
docker-compose down -v
```

### View Logs
```bash
docker-compose logs -f postgres
```

### Reset Database (reload schema + data)
```bash
docker-compose down -v
docker-compose up -d
```

### Backup Database
```bash
docker exec spreadsheet-postgres pg_dump -U spreadsheet_user spreadsheet > backup.sql
```

### Restore Database
```bash
docker exec -i spreadsheet-postgres psql -U spreadsheet_user spreadsheet < backup.sql
```

### Check Database Size
```bash
docker exec spreadsheet-postgres psql -U spreadsheet_user -d spreadsheet -c "
  SELECT 
    pg_size_pretty(pg_database_size('spreadsheet')) as database_size,
    (SELECT count(*) FROM cells) as cell_count;
"
```

---

## 📊 Sample Queries

### Get All Formulas
```sql
SELECT 
    c.id,
    s.name as sheet_name,
    c.row_index,
    c.column_index,
    c.raw_value as formula,
    c.computed_value as result
FROM cells c
JOIN sheets s ON c.sheet_id = s.id
WHERE c.cell_type = 'FORMULA'
ORDER BY s.name, c.row_index, c.column_index;
```

### Get Sheet Data
```sql
SELECT 
    row_index,
    column_index,
    cell_type,
    CASE 
        WHEN cell_type = 'FORMULA' THEN raw_value || ' = ' || computed_value
        ELSE computed_value
    END as value
FROM cells
WHERE sheet_id = 1
ORDER BY row_index, column_index;
```

### Check Concurrent Updates (Version History)
```sql
SELECT 
    id,
    row_index,
    column_index,
    raw_value,
    version,
    updated_at
FROM cells
WHERE sheet_id = 1
ORDER BY updated_at DESC
LIMIT 10;
```

---

## 🚨 Troubleshooting

### Port Already in Use
If port 5432 is already in use:

```yaml
# Edit docker-compose.yml
ports:
  - "5433:5432"  # Use different host port

# Update application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/spreadsheet
```

### Cannot Connect from Spring Boot
- Check Docker network: `docker network inspect docker_spreadsheet-network`
- Verify PostgreSQL is healthy: `docker-compose ps`
- Check firewall settings

### Schema Changes
If you modify `schema.sql`:
```bash
docker-compose down -v
docker-compose up -d
```

---

## 🎯 Production Considerations

For production deployment:

1. **Change Credentials**
   ```yaml
   environment:
     POSTGRES_PASSWORD: ${DB_PASSWORD}  # Use secrets
   ```

2. **Persistent Volumes**
   - Already configured with named volume `postgres_data`
   - Survives `docker-compose down`

3. **Connection Pooling**
   ```yaml
   spring:
     datasource:
       hikari:
         maximum-pool-size: 20
         minimum-idle: 5
   ```

4. **SSL/TLS**
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/spreadsheet?sslmode=require
   ```

5. **Monitoring**
   - Add pgAdmin or Grafana
   - Enable PostgreSQL metrics
   - Monitor connection pool

---

## ✅ Checklist

- [ ] Docker and Docker Compose installed
- [ ] Run `docker-compose up -d`
- [ ] Verify with `docker-compose ps`
- [ ] Access Adminer at http://localhost:8081
- [ ] Update Spring Boot `application.yml`
- [ ] Run application with `docker` profile
- [ ] Test API endpoints
- [ ] Verify data persists after restart

