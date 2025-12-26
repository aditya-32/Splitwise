-- Sample data for Collaborative Spreadsheet Application
-- This file contains example data for testing and demonstration

-- Insert sample workbooks
INSERT INTO workbooks (id, name, version, created_at, updated_at) VALUES
(1, 'Personal Budget 2024', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'Sales Report Q1', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'Project Timeline', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert sample sheets
INSERT INTO sheets (id, workbook_id, name, row_count, column_count, created_at, updated_at) VALUES
(1, 1, 'January', 1000, 26, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 1, 'February', 1000, 26, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 2, 'Sales Data', 1000, 26, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 3, 'Tasks', 1000, 26, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert sample cells for Budget (Sheet 1)
-- Headers
INSERT INTO cells (sheet_id, row_index, column_index, cell_type, raw_value, computed_value, version, updated_at) VALUES
(1, 1, 0, 'TEXT', 'Category', 'Category', 0, CURRENT_TIMESTAMP),
(1, 1, 1, 'TEXT', 'Amount', 'Amount', 0, CURRENT_TIMESTAMP),
(1, 1, 2, 'TEXT', 'Notes', 'Notes', 0, CURRENT_TIMESTAMP);

-- Income section
INSERT INTO cells (sheet_id, row_index, column_index, cell_type, raw_value, computed_value, version, updated_at) VALUES
(1, 3, 0, 'TEXT', 'INCOME', 'INCOME', 0, CURRENT_TIMESTAMP),
(1, 4, 0, 'TEXT', 'Salary', 'Salary', 0, CURRENT_TIMESTAMP),
(1, 4, 1, 'NUMBER', '5000', '5000', 0, CURRENT_TIMESTAMP),
(1, 5, 0, 'TEXT', 'Freelance', 'Freelance', 0, CURRENT_TIMESTAMP),
(1, 5, 1, 'NUMBER', '2000', '2000', 0, CURRENT_TIMESTAMP),
(1, 6, 0, 'TEXT', 'Total Income', 'Total Income', 0, CURRENT_TIMESTAMP),
(1, 6, 1, 'FORMULA', '=SUM(B4:B5)', '7000', 0, CURRENT_TIMESTAMP);

-- Expense section
INSERT INTO cells (sheet_id, row_index, column_index, cell_type, raw_value, computed_value, version, updated_at) VALUES
(1, 8, 0, 'TEXT', 'EXPENSES', 'EXPENSES', 0, CURRENT_TIMESTAMP),
(1, 9, 0, 'TEXT', 'Rent', 'Rent', 0, CURRENT_TIMESTAMP),
(1, 9, 1, 'NUMBER', '1500', '1500', 0, CURRENT_TIMESTAMP),
(1, 10, 0, 'TEXT', 'Groceries', 'Groceries', 0, CURRENT_TIMESTAMP),
(1, 10, 1, 'NUMBER', '800', '800', 0, CURRENT_TIMESTAMP),
(1, 11, 0, 'TEXT', 'Utilities', 'Utilities', 0, CURRENT_TIMESTAMP),
(1, 11, 1, 'NUMBER', '400', '400', 0, CURRENT_TIMESTAMP),
(1, 12, 0, 'TEXT', 'Total Expenses', 'Total Expenses', 0, CURRENT_TIMESTAMP),
(1, 12, 1, 'FORMULA', '=SUM(B9:B11)', '2700', 0, CURRENT_TIMESTAMP);

-- Net savings
INSERT INTO cells (sheet_id, row_index, column_index, cell_type, raw_value, computed_value, version, updated_at) VALUES
(1, 14, 0, 'TEXT', 'Net Savings', 'Net Savings', 0, CURRENT_TIMESTAMP),
(1, 14, 1, 'FORMULA', '=B6-B12', '4300', 0, CURRENT_TIMESTAMP);

-- Insert sample cells for Sales Data (Sheet 3)
INSERT INTO cells (sheet_id, row_index, column_index, cell_type, raw_value, computed_value, version, updated_at) VALUES
(3, 1, 0, 'TEXT', 'Product', 'Product', 0, CURRENT_TIMESTAMP),
(3, 1, 1, 'TEXT', 'Quantity', 'Quantity', 0, CURRENT_TIMESTAMP),
(3, 1, 2, 'TEXT', 'Price', 'Price', 0, CURRENT_TIMESTAMP),
(3, 1, 3, 'TEXT', 'Total', 'Total', 0, CURRENT_TIMESTAMP),
(3, 2, 0, 'TEXT', 'Widget A', 'Widget A', 0, CURRENT_TIMESTAMP),
(3, 2, 1, 'NUMBER', '100', '100', 0, CURRENT_TIMESTAMP),
(3, 2, 2, 'NUMBER', '25', '25', 0, CURRENT_TIMESTAMP),
(3, 2, 3, 'FORMULA', '=B2*C2', '2500', 0, CURRENT_TIMESTAMP),
(3, 3, 0, 'TEXT', 'Widget B', 'Widget B', 0, CURRENT_TIMESTAMP),
(3, 3, 1, 'NUMBER', '50', '50', 0, CURRENT_TIMESTAMP),
(3, 3, 2, 'NUMBER', '40', '40', 0, CURRENT_TIMESTAMP),
(3, 3, 3, 'FORMULA', '=B3*C3', '2000', 0, CURRENT_TIMESTAMP),
(3, 4, 0, 'TEXT', 'Grand Total', 'Grand Total', 0, CURRENT_TIMESTAMP),
(3, 4, 3, 'FORMULA', '=SUM(D2:D3)', '4500', 0, CURRENT_TIMESTAMP);

-- Reset sequences to avoid conflicts
SELECT setval('workbooks_id_seq', (SELECT MAX(id) FROM workbooks));
SELECT setval('sheets_id_seq', (SELECT MAX(id) FROM sheets));
SELECT setval('cells_id_seq', (SELECT MAX(id) FROM cells));

