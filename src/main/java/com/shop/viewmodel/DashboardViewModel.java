package com.shop.viewmodel;

import com.shop.model.DashboardData;
import com.shop.model.LowStockRow;
import com.shop.model.TopProductRow;
import com.shop.service.DashboardService;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.chart.XYChart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

public class DashboardViewModel {
    private static final Logger log = LoggerFactory.getLogger(DashboardViewModel.class);
    
    private final DashboardService dashboardService = new DashboardService();
    
    // Ngày được chọn (bind tới DatePicker)
    private final ObjectProperty<LocalDate> selectedDate = new SimpleObjectProperty<>(LocalDate.now());

    // KPI
    private final LongProperty totalRevenue = new SimpleLongProperty(0);
    private final LongProperty grossProfit = new SimpleLongProperty(0);
    private final IntegerProperty totalOrders = new SimpleIntegerProperty(0);
    private final IntegerProperty lowStockCount = new SimpleIntegerProperty(0);

    // Biểu đồ
    private final ObservableList<XYChart.Data<String, Number>> revenueByHour = FXCollections.observableArrayList();
    private final ObservableList<XYChart.Data<String, Number>> revenueByDayOfWeek = FXCollections.observableArrayList();

    // Top 5 SP
    private final ObservableList<TopProductRow> topProducts = FXCollections.observableArrayList();

    // Hàng sắp hết
    private final ObservableList<LowStockRow> lowStockProducts = FXCollections.observableArrayList();
    private final IntegerProperty lowStockThreshold = new SimpleIntegerProperty(10); // 5 | 10 | 20

    // Trạng thái loading
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final StringProperty errorMessage = new SimpleStringProperty();

    public DashboardViewModel() {
        // Lắng nghe thay đổi của ngày và threshold để load lại dữ liệu
        selectedDate.addListener((obs, oldVal, newVal) -> loadData());
        lowStockThreshold.addListener((obs, oldVal, newVal) -> loadData());
    }

    public void loadData() {
        if (selectedDate.get() == null) return;
        
        loading.set(true);
        errorMessage.set(null);
        
        Task<DashboardData> task = new Task<>() {
            @Override
            protected DashboardData call() throws Exception {
                return dashboardService.loadAll(selectedDate.get(), lowStockThreshold.get());
            }
        };
        
        task.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                applyData(task.getValue());
                loading.set(false);
            });
        });
        
        task.setOnFailed(e -> {
            Platform.runLater(() -> {
                errorMessage.set("Không thể tải dữ liệu Dashboard.");
                loading.set(false);
                log.error("Dashboard load failed", task.getException());
            });
        });
        
        // Sử dụng executor hoặc Thread
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    private void applyData(DashboardData data) {
        totalRevenue.set(data.totalRevenue());
        grossProfit.set(data.grossProfit());
        totalOrders.set(data.totalOrders());
        lowStockCount.set(data.lowStockCount());
        
        revenueByHour.setAll(data.revenueByHour());
        revenueByDayOfWeek.setAll(data.revenueByDayOfWeek());
        topProducts.setAll(data.topProducts());
        lowStockProducts.setAll(data.lowStockProducts());
    }

    // Getters for properties
    public ObjectProperty<LocalDate> selectedDateProperty() { return selectedDate; }
    public LongProperty totalRevenueProperty() { return totalRevenue; }
    public LongProperty grossProfitProperty() { return grossProfit; }
    public IntegerProperty totalOrdersProperty() { return totalOrders; }
    public IntegerProperty lowStockCountProperty() { return lowStockCount; }
    
    public ObservableList<XYChart.Data<String, Number>> getRevenueByHour() { return revenueByHour; }
    public ObservableList<XYChart.Data<String, Number>> getRevenueByDayOfWeek() { return revenueByDayOfWeek; }
    public ObservableList<TopProductRow> getTopProducts() { return topProducts; }
    public ObservableList<LowStockRow> getLowStockProducts() { return lowStockProducts; }
    
    public IntegerProperty lowStockThresholdProperty() { return lowStockThreshold; }
    public BooleanProperty loadingProperty() { return loading; }
    public StringProperty errorMessageProperty() { return errorMessage; }
}
