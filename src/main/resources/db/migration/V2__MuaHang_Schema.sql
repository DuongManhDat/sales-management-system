-- src/main/resources/db/migration/V2__MuaHang_Schema.sql
CREATE TABLE IF NOT EXISTS suppliers (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    code        TEXT    NOT NULL UNIQUE,
    name        TEXT    NOT NULL,
    phone       TEXT,
    address     TEXT,
    note        TEXT,
    is_active   INTEGER NOT NULL DEFAULT 1,
    created_at  TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS purchases (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    code            TEXT    NOT NULL UNIQUE,
    supplier_id     INTEGER REFERENCES suppliers(id),
    purchase_date   TEXT    NOT NULL,
    total_cost      INTEGER NOT NULL,
    paid            INTEGER NOT NULL DEFAULT 0,
    debt            INTEGER NOT NULL DEFAULT 0,
    status          TEXT    NOT NULL,
    note            TEXT,
    created_at      TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS inventory_batches (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    product_id       INTEGER NOT NULL REFERENCES products(id),
    purchase_item_id INTEGER REFERENCES purchase_items(id),
    cost_price       INTEGER NOT NULL,
    qty_initial      INTEGER NOT NULL,
    qty_remaining    INTEGER NOT NULL,
    received_date    TEXT    NOT NULL,
    note             TEXT,
    created_at       TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS purchase_items (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    purchase_id INTEGER NOT NULL REFERENCES purchases(id),
    product_id  INTEGER NOT NULL REFERENCES products(id),
    batch_id    INTEGER NOT NULL REFERENCES inventory_batches(id),
    qty         INTEGER NOT NULL,
    cost_price  INTEGER NOT NULL,
    amount      INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS supplier_payments (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    purchase_id  INTEGER NOT NULL REFERENCES purchases(id),
    amount       INTEGER NOT NULL,
    payment_date TEXT    NOT NULL,
    note         TEXT
);

CREATE TABLE IF NOT EXISTS invoice_item_batches (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    invoice_item_id INTEGER NOT NULL REFERENCES invoice_items(id),
    batch_id        INTEGER NOT NULL REFERENCES inventory_batches(id),
    qty             INTEGER NOT NULL,
    cost_price      INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_inventory_batches_product ON inventory_batches(product_id, received_date);
CREATE INDEX IF NOT EXISTS idx_purchases_status ON purchases(status);
CREATE INDEX IF NOT EXISTS idx_purchases_supplier ON purchases(supplier_id);
CREATE INDEX IF NOT EXISTS idx_invoice_item_batches_item ON invoice_item_batches(invoice_item_id);
