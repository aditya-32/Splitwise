-- Collaborative Spreadsheet Database Schema
-- Compatible with PostgreSQL 12+

-- Drop existing tables (for clean setup)
DROP TABLE IF EXISTS cells CASCADE;
DROP TABLE IF EXISTS sheets CASCADE;
DROP TABLE IF EXISTS workbooks CASCADE;

-- Create workbooks table
CREATE TABLE workbooks (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create sheets table
CREATE TABLE sheets (
    id BIGSERIAL PRIMARY KEY,
    workbook_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    row_count INTEGER NOT NULL DEFAULT 1000,
    column_count INTEGER NOT NULL DEFAULT 26,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sheet_workbook FOREIGN KEY (workbook_id) 
        REFERENCES workbooks(id) ON DELETE CASCADE
);

-- Create cells table (sparse storage - only non-empty cells)
CREATE TABLE cells (
    id BIGSERIAL PRIMARY KEY,
    sheet_id BIGINT NOT NULL,
    row_index INTEGER NOT NULL,
    column_index INTEGER NOT NULL,
    cell_type VARCHAR(20) NOT NULL CHECK (cell_type IN ('TEXT', 'NUMBER', 'FORMULA', 'BOOLEAN', 'ERROR')),
    raw_value TEXT,
    computed_value TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cell_sheet FOREIGN KEY (sheet_id) 
        REFERENCES sheets(id) ON DELETE CASCADE,
    CONSTRAINT uk_cell_position UNIQUE (sheet_id, row_index, column_index)
);

-- Create indexes for performance
CREATE INDEX idx_sheet_workbook ON sheets(workbook_id);
CREATE INDEX idx_cell_sheet ON cells(sheet_id);
CREATE INDEX idx_cell_position ON cells(sheet_id, row_index, column_index);
CREATE INDEX idx_cell_type ON cells(cell_type);
CREATE INDEX idx_cell_updated ON cells(updated_at);

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create triggers for automatic updated_at
CREATE TRIGGER update_workbooks_updated_at
    BEFORE UPDATE ON workbooks
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_sheets_updated_at
    BEFORE UPDATE ON sheets
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_cells_updated_at
    BEFORE UPDATE ON cells
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Add comments for documentation
COMMENT ON TABLE workbooks IS 'Top-level container for spreadsheets';
COMMENT ON TABLE sheets IS 'Individual sheets within workbooks (like tabs in Excel)';
COMMENT ON TABLE cells IS 'Individual cells - sparse storage (only non-empty cells stored)';
COMMENT ON COLUMN cells.version IS 'Optimistic locking version for concurrent updates';
COMMENT ON COLUMN cells.raw_value IS 'Original value entered by user (e.g., "=A1+B1" for formulas)';
COMMENT ON COLUMN cells.computed_value IS 'Evaluated result (e.g., "30" for formula =A1+B1 where A1=10, B1=20)';

