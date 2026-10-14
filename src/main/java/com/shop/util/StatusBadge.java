package com.shop.util;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

/**
 * Component hiển thị Badge trạng thái chuẩn hóa theo phong cách Flat Dashboard của màn Hàng Hóa.
 */
public class StatusBadge extends Label {

    public StatusBadge() {
        getStyleClass().add("badge-status");
    }

    public StatusBadge(String text, String colorType) {
        this();
        setStatus(text, colorType);
    }

    /**
     * Cập nhật text và kiểu màu sắc cho Badge.
     * 
     * @param text Nội dung hiển thị (ví dụ: "Hoạt động", "Đã thanh toán", "Đang xử lý")
     * @param colorType Loại màu: "success" (xanh lá), "warning" (cam/vàng), "danger" (đỏ), "info" (xanh dương), "neutral" (xám)
     */
    public void setStatus(String text, String colorType) {
        setText(text);
        getStyleClass().removeIf(cls -> cls.startsWith("badge-status-"));
        if (colorType != null && !colorType.trim().isEmpty()) {
            getStyleClass().add("badge-status-" + colorType.toLowerCase());
        }
    }

    /**
     * Tự động xác định màu sắc dựa trên nội dung text trạng thái tiếng Việt thông dụng
     */
    public void setAutoStatus(String text) {
        if (text == null || text.trim().isEmpty()) {
            setText("");
            return;
        }
        String colorType = mapColorType(text);
        setStatus(text, colorType);
    }

    /**
     * Tạo TableCell Factory tiện dụng cho các TableColumn hiển thị trạng thái
     */
    public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> createCellFactory() {
        return col -> new TableCell<>() {
            private final StatusBadge badge = new StatusBadge();
            {
                setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.trim().isEmpty()) {
                    setGraphic(null);
                    setText(null);
                } else {
                    badge.setAutoStatus(item);
                    setGraphic(badge);
                    setText(null);
                }
            }
        };
    }

    private static String mapColorType(String text) {
        String t = text.trim().toLowerCase();
        if (t.contains("hoạt động") || t.contains("thanh toán") || t.contains("hoàn thành") || t.contains("hoàn tất") || t.equals("paid") || t.equals("active")) {
            return "success";
        }
        if (t.contains("tạm ngưng") || t.contains("chờ") || t.contains("ngừng hđ") || t.contains("inactive")) {
            return "warning";
        }
        if (t.contains("còn nợ") || t.contains("nợ") || t.contains("hủy") || t.contains("thất bại") || t.equals("canceled") || t.equals("cancelled")) {
            return "danger";
        }
        if (t.contains("đang xử lý") || t.contains("đang giao")) {
            return "info";
        }
        return "neutral";
    }
}
