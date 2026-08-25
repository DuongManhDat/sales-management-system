CREATE TABLE IF NOT EXISTS return_invoices (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code TEXT NOT NULL,
    invoice_id INTEGER NOT NULL,
    customer_id INTEGER,
    return_date TEXT NOT NULL,
    total_refund INTEGER DEFAULT 0,
    return_fee INTEGER DEFAULT 0,
    status TEXT DEFAULT 'COMPLETED',
    FOREIGN KEY(invoice_id) REFERENCES invoices(id),
    FOREIGN KEY(customer_id) REFERENCES customers(id)
);

CREATE TABLE IF NOT EXISTS return_invoice_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    return_invoice_id INTEGER NOT NULL,
    invoice_item_id INTEGER NOT NULL,
    product_id INTEGER NOT NULL,
    return_qty INTEGER NOT NULL,
    refund_price INTEGER NOT NULL,
    amount INTEGER NOT NULL,
    FOREIGN KEY(return_invoice_id) REFERENCES return_invoices(id),
    FOREIGN KEY(invoice_item_id) REFERENCES invoice_items(id),
    FOREIGN KEY(product_id) REFERENCES products(id)
);
