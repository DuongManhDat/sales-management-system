CREATE TABLE IF NOT EXISTS units (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    status INTEGER DEFAULT 1
);

CREATE TABLE IF NOT EXISTS categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    status INTEGER DEFAULT 1
);

CREATE TABLE IF NOT EXISTS products (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    unit_id INTEGER,
    category_id INTEGER,
    cost_price INTEGER DEFAULT 0,
    sale_price INTEGER DEFAULT 0,
    stock_qty INTEGER DEFAULT 0,
    status INTEGER DEFAULT 1,
    FOREIGN KEY(unit_id) REFERENCES units(id),
    FOREIGN KEY(category_id) REFERENCES categories(id)
);

CREATE TABLE IF NOT EXISTS customers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    phone TEXT,
    address TEXT,
    status INTEGER DEFAULT 1
);

CREATE TABLE IF NOT EXISTS invoices (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    code          TEXT    NOT NULL UNIQUE,
    customer_id   INTEGER NOT NULL REFERENCES customers(id),
    invoice_date  TEXT    NOT NULL,
    subtotal      INTEGER NOT NULL,
    discount_pct  REAL    NOT NULL DEFAULT 0,
    discount_amt  INTEGER NOT NULL DEFAULT 0,
    total         INTEGER NOT NULL,
    paid          INTEGER NOT NULL DEFAULT 0,
    debt          INTEGER NOT NULL DEFAULT 0,
    status        TEXT    NOT NULL DEFAULT 'PAID'
);

CREATE TABLE IF NOT EXISTS invoice_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    invoice_id INTEGER NOT NULL,
    product_id INTEGER NOT NULL,
    qty INTEGER NOT NULL,
    sale_price INTEGER NOT NULL,
    amount INTEGER NOT NULL,
    FOREIGN KEY(invoice_id) REFERENCES invoices(id),
    FOREIGN KEY(product_id) REFERENCES products(id)
);

CREATE TABLE IF NOT EXISTS stock_movements (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    product_id    INTEGER NOT NULL REFERENCES products(id),
    type          TEXT    NOT NULL,
    qty_change    INTEGER NOT NULL,
    stock_after   INTEGER NOT NULL,
    ref_type      TEXT    NOT NULL,
    ref_id        INTEGER NOT NULL,
    created_at    TEXT    NOT NULL,
    note          TEXT
);

CREATE TABLE IF NOT EXISTS payments (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    invoice_id    INTEGER NOT NULL REFERENCES invoices(id),
    amount        INTEGER NOT NULL,
    payment_date  TEXT    NOT NULL,
    note          TEXT
);

CREATE TABLE IF NOT EXISTS app_user (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT NOT NULL DEFAULT 'owner',
    password_hash TEXT NOT NULL,
    created_at    TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS settings (
    key TEXT PRIMARY KEY,
    value TEXT
);
