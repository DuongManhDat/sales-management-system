package com.shop.model;

import java.util.List;
import javafx.scene.chart.XYChart;

public record DashboardData(
    long totalRevenue,
    long grossProfit,
    int totalOrders,
    int lowStockCount,
    List<XYChart.Data<String, Number>> revenueByHour,
    List<XYChart.Data<String, Number>> revenueByDayOfWeek,
    List<TopProductRow> topProducts,
    List<LowStockRow> lowStockProducts
) {}
