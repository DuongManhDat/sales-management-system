-- V9: Bổ sung trạng thái và thời gian hủy phiếu kiểm kho
ALTER TABLE stock_adjustments ADD COLUMN status TEXT DEFAULT 'COMPLETED';
ALTER TABLE stock_adjustments ADD COLUMN canceled_at TEXT;
