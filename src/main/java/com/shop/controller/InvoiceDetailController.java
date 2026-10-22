package com.shop.controller;

import com.shop.dao.CustomerDao;
import com.shop.dao.InvoiceItemDao;
import com.shop.model.Customer;
import com.shop.model.Invoice;
import com.shop.model.InvoiceItem;
import com.shop.util.FormatterUtil;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

public class InvoiceDetailController {
    private static final Logger log = LoggerFactory.getLogger(InvoiceDetailController.class);
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,###");

    @FXML private Label lblCode;
    @FXML private Label lblDate;
    @FXML private Label lblCustomer;
    @FXML private Label lblPhone;
    @FXML private Label lblStatus;

    @FXML private TableView<InvoiceItem> tableItems;
    @FXML private TableColumn<InvoiceItem, Number> colIndex;
    @FXML private TableColumn<InvoiceItem, String> colProductCode;
    @FXML private TableColumn<InvoiceItem, String> colProductName;
    @FXML private TableColumn<InvoiceItem, String> colUnitName;
    @FXML private TableColumn<InvoiceItem, Integer> colQty;
    @FXML private TableColumn<InvoiceItem, Integer> colReturnedQty;
    @FXML private TableColumn<InvoiceItem, Integer> colNetQty;
    @FXML private TableColumn<InvoiceItem, Long> colSalePrice;
    @FXML private TableColumn<InvoiceItem, Long> colAmount;

    @FXML private Label lblSubtotal;
    @FXML private Label lblDiscount;
    @FXML private Label lblTotal;
    @FXML private Label lblPaid;
    @FXML private Label lblDebt;

    private final InvoiceItemDao invoiceItemDao = new InvoiceItemDao();
    private final com.shop.dao.ReturnInvoiceItemDao returnInvoiceItemDao = new com.shop.dao.ReturnInvoiceItemDao();
    private final CustomerDao customerDao = new CustomerDao();
    private final java.util.Map<Integer, Integer> alreadyReturnedMap = new java.util.HashMap<>();

    @FXML
    public void initialize() {
        setupTable();
    }

    private void setupTable() {
        colIndex.setCellValueFactory(column -> new ReadOnlyObjectWrapper<>(tableItems.getItems().indexOf(column.getValue()) + 1));
        
        colProductCode.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getProductCode() != null && !data.getValue().getProductCode().trim().isEmpty()
                        ? data.getValue().getProductCode()
                        : "SP#" + data.getValue().getProductId()
        ));
        
        colProductName.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getProductName() != null && !data.getValue().getProductName().trim().isEmpty()
                        ? data.getValue().getProductName()
                        : "Sản phẩm #" + data.getValue().getProductId()
        ));
        
        colUnitName.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getUnitName() != null && !data.getValue().getUnitName().trim().isEmpty()
                        ? data.getValue().getUnitName()
                        : "—"
        ));
        
        colQty.setCellValueFactory(new PropertyValueFactory<>("qty"));

        colReturnedQty.setCellValueFactory(data -> {
            int returned = alreadyReturnedMap.getOrDefault(data.getValue().getId(), 0);
            return new javafx.beans.property.SimpleObjectProperty<>(returned);
        });
        colReturnedQty.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item == 0) {
                    setText("-");
                    setStyle("-fx-alignment: CENTER; -fx-text-fill: #9CA3AF;");
                } else {
                    setText(String.valueOf(item));
                    setStyle("-fx-alignment: CENTER; -fx-text-fill: #DC2626; -fx-font-weight: bold;");
                }
            }
        });

        colNetQty.setCellValueFactory(data -> {
            int returned = alreadyReturnedMap.getOrDefault(data.getValue().getId(), 0);
            int net = Math.max(0, data.getValue().getQty() - returned);
            return new javafx.beans.property.SimpleObjectProperty<>(net);
        });
        colNetQty.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.valueOf(item));
                    setStyle("-fx-alignment: CENTER; -fx-text-fill: #047857; -fx-font-weight: bold;");
                }
            }
        });

        colSalePrice.setCellValueFactory(new PropertyValueFactory<>("salePrice"));
        colSalePrice.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(CURRENCY_FORMAT.format(item) + " đ");
                }
                setStyle("-fx-alignment: CENTER_RIGHT;");
            }
        });

        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colAmount.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(CURRENCY_FORMAT.format(item) + " đ");
                }
                setStyle("-fx-alignment: CENTER_RIGHT; -fx-font-weight: bold;");
            }
        });
    }

    public void setInvoice(Invoice invoice) {
        if (invoice == null) return;

        lblCode.setText(invoice.getCode());
        lblDate.setText(FormatterUtil.formatDateToDdMmYyyy(invoice.getInvoiceDate()));

        // Thông tin khách hàng
        String customerName = invoice.getCustomerName();
        if (customerName == null || customerName.trim().isEmpty()) {
            customerName = "KH #" + invoice.getCustomerId();
        }
        lblCustomer.setText(customerName);

        try {
            Customer c = customerDao.findById(invoice.getCustomerId());
            if (c != null && c.getPhone() != null && !c.getPhone().trim().isEmpty()) {
                lblPhone.setText(c.getPhone());
            } else {
                lblPhone.setText("—");
            }
        } catch (SQLException e) {
            log.warn("Không thể tìm số điện thoại của khách hàng ID={}: {}", invoice.getCustomerId(), e.getMessage());
            lblPhone.setText("—");
        }

        // Trạng thái hóa đơn
        lblStatus.setText(FormatterUtil.formatInvoiceStatus(invoice));
        if (invoice.getDebt() <= 0) {
            lblStatus.setStyle("-fx-background-color: #DEF7EC; -fx-text-fill: #03543F; -fx-padding: 4 12; -fx-background-radius: 4; -fx-font-weight: bold;");
        } else {
            lblStatus.setStyle("-fx-background-color: #FDE8E8; -fx-text-fill: #9B1C1C; -fx-padding: 4 12; -fx-background-radius: 4; -fx-font-weight: bold;");
        }

        // Tóm tắt tài chính
        lblSubtotal.setText(CURRENCY_FORMAT.format(invoice.getSubtotal()) + " đ");
        lblDiscount.setText(CURRENCY_FORMAT.format(invoice.getDiscountAmt()) + " đ");
        lblTotal.setText(CURRENCY_FORMAT.format(invoice.getTotal()) + " đ");
        lblPaid.setText(CURRENCY_FORMAT.format(invoice.getPaid()) + " đ");
        lblDebt.setText(CURRENCY_FORMAT.format(invoice.getDebt()) + " đ");

        // Tải danh sách chi tiết các mặt hàng
        loadInvoiceItems(invoice.getId());
    }

    private void loadInvoiceItems(int invoiceId) {
        try {
            alreadyReturnedMap.clear();
            Map<Integer, Integer> retMap = returnInvoiceItemDao.getReturnedQtyMapByInvoiceId(invoiceId);
            alreadyReturnedMap.putAll(retMap);

            List<InvoiceItem> items = invoiceItemDao.findByInvoiceId(invoiceId);
            tableItems.setItems(FXCollections.observableArrayList(items));
        } catch (SQLException e) {
            log.error("Lỗi khi tải chi tiết các mặt hàng của hóa đơn ID={}: {}", invoiceId, e.getMessage(), e);
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) lblCode.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }
}
