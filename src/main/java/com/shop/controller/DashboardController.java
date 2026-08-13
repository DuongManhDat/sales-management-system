package com.shop.controller;

import com.shop.model.LowStockRow;
import com.shop.model.TopProductRow;
import com.shop.util.FormatterUtil;
import java.math.BigDecimal;
import com.shop.viewmodel.DashboardViewModel;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DashboardController {
    
    @FXML private DatePicker datePicker;
    @FXML private Button refreshButton;
    @FXML private Label lblTotalRevenue;
    @FXML private Label lblGrossProfit;
    @FXML private Label lblTotalOrders;
    @FXML private Label lblLowStockCount;
    @FXML private BarChart<String, Number> chartRevenueByHour;
    @FXML private BarChart<String, Number> chartRevenueByDay;
    @FXML private TableView<TopProductRow> tblTopProducts;
    @FXML private ComboBox<Integer> cboLowStockThreshold;
    @FXML private TableView<LowStockRow> tblLowStock;
    @FXML private ProgressIndicator progressIndicator;

    private final DashboardViewModel viewModel = new DashboardViewModel();

    @FXML
    public void initialize() {
        // Init threshold combo box
        cboLowStockThreshold.getItems().addAll(5, 10, 20);
        
        // DatePicker formatting
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        datePicker.setConverter(new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                if (date != null) {
                    return formatter.format(date);
                } else {
                    return "";
                }
            }

            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    return LocalDate.parse(string, formatter);
                } else {
                    return null;
                }
            }
        });

        // Bind loading state
        progressIndicator.visibleProperty().bind(viewModel.loadingProperty());
        progressIndicator.managedProperty().bind(viewModel.loadingProperty());
        refreshButton.disableProperty().bind(viewModel.loadingProperty());

        // Bind view model properties to UI controls
        datePicker.valueProperty().bindBidirectional(viewModel.selectedDateProperty());
        cboLowStockThreshold.valueProperty().bindBidirectional(viewModel.lowStockThresholdProperty().asObject());

        lblTotalRevenue.textProperty().bind(Bindings.createStringBinding(
            () -> FormatterUtil.formatCurrency(BigDecimal.valueOf(viewModel.totalRevenueProperty().get())),
            viewModel.totalRevenueProperty()
        ));
        
        lblGrossProfit.textProperty().bind(Bindings.createStringBinding(
            () -> FormatterUtil.formatCurrency(BigDecimal.valueOf(viewModel.grossProfitProperty().get())),
            viewModel.grossProfitProperty()
        ));

        lblTotalOrders.textProperty().bind(viewModel.totalOrdersProperty().asString());
        lblLowStockCount.textProperty().bind(viewModel.lowStockCountProperty().asString());

        // Setup Charts
        BarChart.Series<String, Number> hourSeries = new BarChart.Series<>();
        Bindings.bindContent(hourSeries.getData(), viewModel.getRevenueByHour());
        chartRevenueByHour.getData().add(hourSeries);

        BarChart.Series<String, Number> daySeries = new BarChart.Series<>();
        Bindings.bindContent(daySeries.getData(), viewModel.getRevenueByDayOfWeek());
        chartRevenueByDay.getData().add(daySeries);

        // Setup Tables
        tblTopProducts.setItems(viewModel.getTopProducts());
        tblLowStock.setItems(viewModel.getLowStockProducts());
        
        // Error handling
        viewModel.errorMessageProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Lỗi");
                alert.setHeaderText("Lỗi tải dữ liệu");
                alert.setContentText(newVal);
                alert.showAndWait();
            }
        });

        // Tải dữ liệu lần đầu
        viewModel.loadData();
    }

    @FXML
    private void handleRefresh() {
        viewModel.loadData();
    }
}
