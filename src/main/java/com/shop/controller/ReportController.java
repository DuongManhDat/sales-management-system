package com.shop.controller;

import com.shop.model.InventoryReportRow;
import com.shop.model.ProductReportRow;
import com.shop.model.RevenueReportRow;
import com.shop.service.ReportService;
import com.shop.util.DialogHelper;
import com.shop.util.FormatterUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportController {

    // Tab Doanh thu
    @FXML private DatePicker dpRevStart;
    @FXML private DatePicker dpRevEnd;
    @FXML private BarChart<String, Number> revenueChart;
    @FXML private TableView<RevenueReportRow> tvRevenue;
    @FXML private TableColumn<RevenueReportRow, String> colRevDate;
    @FXML private TableColumn<RevenueReportRow, String> colRevRevenue;
    @FXML private TableColumn<RevenueReportRow, String> colRevCost;
    @FXML private TableColumn<RevenueReportRow, String> colRevProfit;

    // Tab Sản phẩm
    @FXML private DatePicker dpProdStart;
    @FXML private DatePicker dpProdEnd;
    @FXML private PieChart productChart;
    @FXML private TableView<ProductReportRow> tvProduct;
    @FXML private TableColumn<ProductReportRow, Integer> colProdId;
    @FXML private TableColumn<ProductReportRow, String> colProdName;
    @FXML private TableColumn<ProductReportRow, Integer> colProdQty;
    @FXML private TableColumn<ProductReportRow, String> colProdRevenue;
    @FXML private TableColumn<ProductReportRow, String> colProdProfit;

    // Tab Tồn kho
    @FXML private DatePicker dpInvStart;
    @FXML private DatePicker dpInvEnd;
    @FXML private TableView<InventoryReportRow> tvInventory;
    @FXML private TableColumn<InventoryReportRow, Integer> colInvId;
    @FXML private TableColumn<InventoryReportRow, String> colInvName;
    @FXML private TableColumn<InventoryReportRow, Integer> colInvStart;
    @FXML private TableColumn<InventoryReportRow, Integer> colInvIn;
    @FXML private TableColumn<InventoryReportRow, Integer> colInvOut;
    @FXML private TableColumn<InventoryReportRow, Integer> colInvEnd;

    private ReportService reportService = new ReportService();

    @FXML
    public void initialize() {
        setupDatePickers();
        setupTables();
        
        // Load default data
        handleViewRevenueReport();
        handleViewProductReport();
        handleViewInventoryReport();
    }

    private void setupDatePickers() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        
        dpRevStart.setValue(startOfMonth);
        dpRevEnd.setValue(today);
        
        dpProdStart.setValue(startOfMonth);
        dpProdEnd.setValue(today);
        
        dpInvStart.setValue(startOfMonth);
        dpInvEnd.setValue(today);
    }

    private void setupTables() {
        // Revenue Table
        colRevDate.setCellValueFactory(cellData -> {
            LocalDate d = cellData.getValue().getDate();
            return new SimpleStringProperty(d != null ? d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
        });
        colRevRevenue.setCellValueFactory(cellData -> 
            new SimpleStringProperty(FormatterUtil.formatCurrency(new BigDecimal(cellData.getValue().getRevenue()))));
        colRevCost.setCellValueFactory(cellData -> 
            new SimpleStringProperty(FormatterUtil.formatCurrency(new BigDecimal(cellData.getValue().getCost()))));
        colRevProfit.setCellValueFactory(cellData -> 
            new SimpleStringProperty(FormatterUtil.formatCurrency(new BigDecimal(cellData.getValue().getProfit()))));

        // Product Table
        colProdId.setCellValueFactory(new PropertyValueFactory<>("productId"));
        colProdName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colProdQty.setCellValueFactory(new PropertyValueFactory<>("qtySold"));
        colProdRevenue.setCellValueFactory(cellData -> 
            new SimpleStringProperty(FormatterUtil.formatCurrency(new BigDecimal(cellData.getValue().getRevenue()))));
        colProdProfit.setCellValueFactory(cellData -> 
            new SimpleStringProperty(FormatterUtil.formatCurrency(new BigDecimal(cellData.getValue().getProfit()))));

        // Inventory Table
        colInvId.setCellValueFactory(new PropertyValueFactory<>("productId"));
        colInvName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colInvStart.setCellValueFactory(new PropertyValueFactory<>("startQty"));
        colInvIn.setCellValueFactory(new PropertyValueFactory<>("inQty"));
        colInvOut.setCellValueFactory(new PropertyValueFactory<>("outQty"));
        colInvEnd.setCellValueFactory(new PropertyValueFactory<>("endQty"));
    }

    @FXML
    private void handleViewRevenueReport() {
        LocalDate start = dpRevStart.getValue();
        LocalDate end = dpRevEnd.getValue();
        
        if (start == null || end == null || start.isAfter(end)) {
            DialogHelper.showError("Lỗi", "Ngày bắt đầu và kết thúc không hợp lệ");
            return;
        }

        try {
            List<RevenueReportRow> data = reportService.getRevenueReport(start, end);
            tvRevenue.setItems(FXCollections.observableArrayList(data));
            
            // Update Chart
            revenueChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Doanh thu");
            
            for (RevenueReportRow row : data) {
                String dateStr = row.getDate().format(DateTimeFormatter.ofPattern("dd/MM"));
                series.getData().add(new XYChart.Data<>(dateStr, row.getRevenue()));
            }
            revenueChart.getData().add(series);
            
        } catch (Exception e) {
            e.printStackTrace();
            DialogHelper.showError("Lỗi", "Không thể tải báo cáo doanh thu: " + e.getMessage());
        }
    }

    @FXML
    private void handleViewProductReport() {
        LocalDate start = dpProdStart.getValue();
        LocalDate end = dpProdEnd.getValue();
        
        if (start == null || end == null || start.isAfter(end)) {
            DialogHelper.showError("Lỗi", "Ngày bắt đầu và kết thúc không hợp lệ");
            return;
        }

        try {
            List<ProductReportRow> data = reportService.getProductReport(start, end);
            tvProduct.setItems(FXCollections.observableArrayList(data));
            
            // Update PieChart
            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
            int limit = Math.min(data.size(), 10); // Top 10
            for (int i = 0; i < limit; i++) {
                ProductReportRow row = data.get(i);
                if (row.getRevenue() > 0) {
                    pieChartData.add(new PieChart.Data(row.getProductName(), row.getRevenue()));
                }
            }
            productChart.setData(pieChartData);
            
        } catch (Exception e) {
            e.printStackTrace();
            DialogHelper.showError("Lỗi", "Không thể tải báo cáo sản phẩm: " + e.getMessage());
        }
    }

    @FXML
    private void handleViewInventoryReport() {
        LocalDate start = dpInvStart.getValue();
        LocalDate end = dpInvEnd.getValue();
        
        if (start == null || end == null || start.isAfter(end)) {
            DialogHelper.showError("Lỗi", "Ngày bắt đầu và kết thúc không hợp lệ");
            return;
        }

        try {
            List<InventoryReportRow> data = reportService.getInventoryReport(start, end);
            tvInventory.setItems(FXCollections.observableArrayList(data));
        } catch (Exception e) {
            e.printStackTrace();
            DialogHelper.showError("Lỗi", "Không thể tải báo cáo tồn kho: " + e.getMessage());
        }
    }
}
