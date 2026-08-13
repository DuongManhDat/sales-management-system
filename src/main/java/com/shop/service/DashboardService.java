package com.shop.service;

import com.shop.dao.DashboardDao;
import com.shop.model.DashboardData;
import com.shop.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DashboardService {
    private DashboardDao dao = new DashboardDao();
    
    public DashboardData loadAll(LocalDate date, int lowStockThreshold) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String startDateStr = date.minusDays(6).format(DateTimeFormatter.ISO_LOCAL_DATE);
            
            return new DashboardData(
                dao.getTotalRevenue(conn, dateStr),
                dao.getGrossProfit(conn, dateStr),
                dao.getTotalOrders(conn, dateStr),
                dao.getLowStockProducts(conn, lowStockThreshold).size(),
                dao.getRevenueByHour(conn, dateStr),
                dao.getRevenueByDayOfWeek(conn, startDateStr, dateStr),
                dao.getTopProducts(conn, dateStr, 5),
                dao.getLowStockProducts(conn, lowStockThreshold)
            );
        }
    }
}
