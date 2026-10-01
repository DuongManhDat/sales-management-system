package com.shop.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class FormatterUtil {
    
    private static final Locale VI_LOCALE = new Locale("vi", "VN");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(VI_LOCALE);
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DATE_FORMAT_DASH = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public static String formatCurrency(BigDecimal amount) {
        if (amount == null) return CURRENCY_FORMAT.format(BigDecimal.ZERO);
        return CURRENCY_FORMAT.format(amount);
    }
    
    public static BigDecimal parseCurrency(String text) {
        if (text == null || text.trim().isEmpty()) return BigDecimal.ZERO;
        try {
            Number number = CURRENCY_FORMAT.parse(text);
            return new BigDecimal(number.toString());
        } catch (ParseException e) {
            try {
                String cleanText = text.replaceAll("[^0-9]", "");
                if (cleanText.isEmpty()) return BigDecimal.ZERO;
                return new BigDecimal(cleanText);
            } catch (Exception ex) {
                return BigDecimal.ZERO;
            }
        }
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DATE_TIME_FORMAT);
    }
    
    public static String formatDate(LocalDateTime date) {
        if (date == null) return "";
        return date.format(DATE_FORMAT);
    }

    public static String formatDateToDdMmYyyy(LocalDate date) {
        if (date == null) return "";
        return date.format(DATE_FORMAT_DASH);
    }

    public static String formatDateToDdMmYyyy(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DATE_FORMAT_DASH);
    }

    public static String formatDateToDdMmYyyy(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) return "";
        String s = rawDate.trim();
        if (s.contains(" ")) {
            s = s.substring(0, s.indexOf(" "));
        } else if (s.contains("T")) {
            s = s.substring(0, s.indexOf("T"));
        }
        
        if (s.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            String[] parts = s.split("-");
            return parts[2] + "-" + parts[1] + "-" + parts[0];
        }
        if (s.matches("^\\d{2}/\\d{2}/\\d{4}$")) {
            return s.replace('/', '-');
        }
        if (s.matches("^\\d{2}-\\d{2}-\\d{4}$")) {
            return s;
        }
        
        String[] patterns = {"dd-MM-yyyy", "dd/MM/yyyy", "yyyy-MM-dd", "yyyy/MM/dd", "d-M-yyyy", "d/M/yyyy"};
        for (String pattern : patterns) {
            try {
                LocalDate d = LocalDate.parse(s, DateTimeFormatter.ofPattern(pattern));
                return d.format(DATE_FORMAT_DASH);
            } catch (Exception ignored) {}
        }
        return s;
    }

    public static String formatPurchaseStatus(String status) {
        if (status == null || status.trim().isEmpty()) return "Đã thanh toán";
        String s = status.trim().toUpperCase();
        if (s.equals("PAID") || s.equals("ĐÃ THANH TOÁN") || s.equals("DA THANH TOAN") || s.equals("COMPLETED")) {
            return "Đã thanh toán";
        }
        if (s.equals("DEBT") || s.equals("CÒN NỢ") || s.equals("CON NO") || s.equals("UNPAID") || s.equals("PARTIAL")) {
            return "Còn nợ";
        }
        return status;
    }

    public static String formatInvoiceStatus(String status) {
        if (status == null || status.trim().isEmpty()) return "Đã thanh toán";
        String s = status.trim().toUpperCase();
        switch (s) {
            case "PAID":
            case "ĐÃ THANH TOÁN":
            case "DA THANH TOAN":
                return "Đã thanh toán";
            case "DEBT":
            case "UNPAID":
            case "PARTIAL":
            case "CÒN NỢ":
            case "CON NO":
                return "Còn nợ";
            case "PENDING":
            case "ĐANG XỬ LÝ":
            case "DANG XU LY":
                return "Đang xử lý";
            case "CANCELLED":
            case "CANCELED":
            case "ĐÃ HỦY":
            case "DA HUY":
                return "Đã hủy";
            case "COMPLETED":
            case "ĐÃ HOÀN THÀNH":
            case "DA HOAN THANH":
                return "Đã hoàn thành";
            default:
                return status;
        }
    }

    public static String formatInvoiceStatus(com.shop.model.Invoice invoice) {
        if (invoice == null) return "";
        if (invoice.getStatus() != null && !invoice.getStatus().trim().isEmpty()) {
            return formatInvoiceStatus(invoice.getStatus());
        }
        return invoice.getDebt() > 0 ? "Còn nợ" : "Đã thanh toán";
    }

    public static String formatNumberWithDots(long amount) {
        NumberFormat nf = NumberFormat.getInstance(Locale.GERMANY);
        return nf.format(amount);
    }

    public static long parseNumberFromText(String text) {
        if (text == null || text.trim().isEmpty()) return 0L;
        String digits = text.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return 0L;
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    public static void attachCurrencyFormatter(javafx.scene.control.TextField textField, Runnable onValueChange) {
        textField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            String cleanOld = oldVal != null ? oldVal.replaceAll("[^0-9]", "") : "";
            String cleanNew = newVal.replaceAll("[^0-9]", "");

            if (cleanOld.equals(cleanNew) && newVal.contains(".")) {
                return;
            }

            if (cleanNew.isEmpty()) {
                if (!newVal.isEmpty()) {
                    textField.setText("");
                }
                if (onValueChange != null) {
                    onValueChange.run();
                }
                return;
            }

            try {
                long val = Long.parseLong(cleanNew);
                String formatted = formatNumberWithDots(val);

                int originalCaret = textField.getCaretPosition();
                int digitsBeforeCaret = 0;
                for (int i = 0; i < Math.min(originalCaret, newVal.length()); i++) {
                    if (Character.isDigit(newVal.charAt(i))) {
                        digitsBeforeCaret++;
                    }
                }

                textField.setText(formatted);

                int newCaret = 0;
                int countDigits = 0;
                for (int i = 0; i < formatted.length(); i++) {
                    if (Character.isDigit(formatted.charAt(i))) {
                        countDigits++;
                    }
                    if (countDigits == digitsBeforeCaret) {
                        newCaret = i + 1;
                        break;
                    }
                }
                if (digitsBeforeCaret == 0) {
                    newCaret = 0;
                } else if (countDigits < digitsBeforeCaret) {
                    newCaret = formatted.length();
                }

                textField.positionCaret(Math.min(newCaret, formatted.length()));
            } catch (NumberFormatException ignored) {}

            if (onValueChange != null) {
                onValueChange.run();
            }
        });
    }
}
