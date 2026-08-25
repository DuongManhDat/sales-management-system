-- src/main/resources/db/migration/V4__QuanLyHangHoa_Schema.sql
PRAGMA foreign_keys=off;

-- 1. Create the new table
CREATE TABLE IF NOT EXISTS products_new (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    code        TEXT    NOT NULL UNIQUE,     
    name        TEXT    NOT NULL,
    unit_id     INTEGER NOT NULL REFERENCES units(id)      ON DELETE RESTRICT,
    category_id INTEGER          REFERENCES categories(id) ON DELETE SET NULL,
    sale_price  INTEGER NOT NULL DEFAULT 0,  
    stock_qty   REAL    NOT NULL DEFAULT 0,  
    note        TEXT,
    deleted_at  TEXT,                        
    created_at  TEXT    NOT NULL,
    updated_at  TEXT    NOT NULL,
    CHECK (sale_price >= 0)
);

-- 2. Copy data from the old table
INSERT INTO products_new (id, code, name, unit_id, category_id, sale_price, stock_qty, created_at, updated_at, deleted_at)
SELECT 
    id, code, name, unit_id, category_id, sale_price, CAST(stock_qty AS REAL),
    datetime('now', 'localtime'), datetime('now', 'localtime'),
    CASE WHEN status = 0 THEN datetime('now', 'localtime') ELSE NULL END
FROM products;

-- 3. Drop the old table
DROP TABLE products;

-- 4. Rename the new table
ALTER TABLE products_new RENAME TO products;

-- 5. Recreate indexes for the new table
CREATE INDEX IF NOT EXISTS idx_products_name     ON products(name);
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_products_deleted  ON products(deleted_at);

PRAGMA foreign_keys=on;

-- New tables
CREATE TABLE IF NOT EXISTS price_history (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    product_id  INTEGER NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    old_price   INTEGER NOT NULL,
    new_price   INTEGER NOT NULL,
    changed_at  TEXT    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_price_history_product ON price_history(product_id);

CREATE TABLE IF NOT EXISTS stock_adjustments (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    code            TEXT    NOT NULL UNIQUE,
    adjustment_date TEXT    NOT NULL,
    note            TEXT,
    created_at      TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS stock_adjustment_items (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    adjustment_id   INTEGER NOT NULL REFERENCES stock_adjustments(id) ON DELETE CASCADE,
    product_id      INTEGER NOT NULL REFERENCES products(id)          ON DELETE RESTRICT,
    current_qty     REAL    NOT NULL,
    actual_qty      REAL    NOT NULL,
    variance        REAL    NOT NULL,
    cost_price      INTEGER,
    reason          TEXT    NOT NULL,
    CHECK (actual_qty >= 0)
);
CREATE INDEX IF NOT EXISTS idx_adj_items_adj     ON stock_adjustment_items(adjustment_id);
CREATE INDEX IF NOT EXISTS idx_adj_items_product ON stock_adjustment_items(product_id);

-- Alter inventory_batches to support adjustment
ALTER TABLE inventory_batches ADD COLUMN source TEXT DEFAULT 'PURCHASE';
ALTER TABLE inventory_batches ADD COLUMN stock_adjustment_item_id INTEGER REFERENCES stock_adjustment_items(id);
