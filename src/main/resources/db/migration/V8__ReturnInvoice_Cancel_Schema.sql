-- V8: Bổ sung hủy phiếu trả hàng và lưu vết thời gian hủy
ALTER TABLE return_invoices ADD COLUMN canceled_at TEXT;
