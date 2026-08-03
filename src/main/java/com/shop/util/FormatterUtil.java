package com.shop.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class FormatterUtil {
    
    private static final Locale VI_LOCALE = new Locale("vi", "VN");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(VI_LOCALE);
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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
}
