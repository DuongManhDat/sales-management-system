-- Migration V6: Bổ sung audit log cho lịch sử thanh toán hóa đơn
ALTER TABLE payments ADD COLUMN status TEXT NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE payments ADD COLUMN cancelled_at TEXT;
ALTER TABLE payments ADD COLUMN cancel_reason TEXT;
