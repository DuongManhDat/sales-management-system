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
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
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
}
