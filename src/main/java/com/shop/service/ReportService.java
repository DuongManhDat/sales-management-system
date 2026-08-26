package com.shop.service;

import com.shop.dao.ReportDAO;
import com.shop.model.InventoryReportRow;
import com.shop.model.ProductReportRow;
import com.shop.model.RevenueReportRow;
import com.shop.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {
    private ReportDAO reportDAO = new ReportDAO();

    public List<RevenueReportRow> getRevenueReport(LocalDate startDate, LocalDate endDate) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String start = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String end = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            return reportDAO.getRevenueReport(conn, start, end);
        }
    }

    public List<ProductReportRow> getProductReport(LocalDate startDate, LocalDate endDate) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String start = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String end = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            return reportDAO.getProductReport(conn, start, end);
        }
    }

    public List<InventoryReportRow> getInventoryReport(LocalDate startDate, LocalDate endDate) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String start = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String end = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            return reportDAO.getInventoryReport(conn, start, end);
        }
    }
}
