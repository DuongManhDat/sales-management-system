package com.shop.service;

import com.shop.dao.PurchaseDao;
import com.shop.model.Purchase;
import com.shop.model.PurchaseItem;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PurchaseService {

    private final PurchaseDao purchaseDao = new PurchaseDao();

    public List<Purchase> getAllPurchases(String search, String status) {
        return purchaseDao.getAllPurchases(search, status);
    }

    public void createPurchase(Purchase purchase, List<PurchaseItem> items) throws SQLException, IllegalArgumentException {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Phiếu nhập phải có ít nhất 1 sản phẩm.");
        }

        long totalCost = 0;
        for (PurchaseItem item : items) {
            if (item.getQty() <= 0) {
                throw new IllegalArgumentException("Số lượng sản phẩm phải > 0.");
            }
            if (item.getCostPrice() < 0) {
                throw new IllegalArgumentException("Đơn giá nhập phải >= 0.");
            }
            long amount = item.getQty() * item.getCostPrice();
            item.setAmount(amount);
            totalCost += amount;
        }

        purchase.setTotalCost(totalCost);

        if (purchase.getPaid() < 0) {
            throw new IllegalArgumentException("Số tiền đã trả không hợp lệ.");
        }

        long debt = totalCost - purchase.getPaid();
        if (debt < 0) debt = 0; // If they overpaid? Just cap it or let debt be negative (advance payment). Cap at 0 for now.
        purchase.setDebt(debt);

        if (purchase.getPaid() >= totalCost) {
            purchase.setStatus("Đã thanh toán");
        } else {
            purchase.setStatus("Còn nợ");
        }

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String currentTime = now.format(dtf);

        if (purchase.getPurchaseDate() == null || purchase.getPurchaseDate().isEmpty()) {
            purchase.setPurchaseDate(currentTime);
        }
        purchase.setCreatedAt(currentTime);

        if (purchase.getCode() == null || purchase.getCode().isEmpty()) {
            purchase.setCode("PO-" + System.currentTimeMillis());
        }

        purchaseDao.createPurchaseTransaction(purchase, items);
    }

    public List<PurchaseItem> getItemsByPurchaseId(int purchaseId) {
        return purchaseDao.getItemsByPurchaseId(purchaseId);
    }

    public Purchase getPurchaseById(int id) {
        return purchaseDao.getPurchaseById(id);
    }

    public Purchase payDebt(int purchaseId, long paymentAmount, String note) throws SQLException, IllegalArgumentException {
        if (paymentAmount <= 0) {
            throw new IllegalArgumentException("Số tiền thanh toán phải lớn hơn 0.");
        }

        Purchase current = purchaseDao.getPurchaseById(purchaseId);
        if (current == null) {
            throw new IllegalArgumentException("Không tìm thấy phiếu nhập có ID: " + purchaseId);
        }

        if (current.getDebt() <= 0) {
            throw new IllegalArgumentException("Phiếu nhập này đã thanh toán đủ, không còn nợ.");
        }

        if (paymentAmount > current.getDebt()) {
            throw new IllegalArgumentException("Số tiền thanh toán (" + paymentAmount + " đ) không được vượt quá số tiền còn nợ (" + current.getDebt() + " đ).");
        }

        long newPaid = current.getPaid() + paymentAmount;
        long newDebt = current.getTotalCost() - newPaid;
        if (newDebt < 0) {
            newDebt = 0;
        }

        String newStatus = (newDebt == 0) ? "Đã thanh toán" : "Còn nợ";
        String paymentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String paymentNote = (note != null && !note.trim().isEmpty()) ? note.trim() : "Trả nợ NCC cho phiếu " + current.getCode();

        purchaseDao.recordPayment(purchaseId, paymentAmount, newPaid, newDebt, newStatus, paymentDate, paymentNote);

        return purchaseDao.getPurchaseById(purchaseId);
    }
}
