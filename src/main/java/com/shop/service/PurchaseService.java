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
}
