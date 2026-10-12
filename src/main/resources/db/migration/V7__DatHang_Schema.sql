-- V7: Hoàn thiện nghiệp vụ đặt hàng
-- Thêm reserved_qty vào products: số lượng đang được giữ chỗ bởi đơn PENDING
ALTER TABLE products ADD COLUMN reserved_qty INTEGER NOT NULL DEFAULT 0;

-- Thêm delivery_date vào invoices: ngày hẹn giao hàng (chỉ áp dụng cho đơn PENDING)
ALTER TABLE invoices ADD COLUMN delivery_date TEXT;

-- Thêm payment_type vào payments: phân biệt cọc đặt hàng và thanh toán hóa đơn
-- Giá trị: 'PAYMENT' (thanh toán hóa đơn), 'DEPOSIT' (cọc đặt hàng)
ALTER TABLE payments ADD COLUMN payment_type TEXT NOT NULL DEFAULT 'PAYMENT';
