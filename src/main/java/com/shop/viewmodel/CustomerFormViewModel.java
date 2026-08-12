package com.shop.viewmodel;

import com.shop.model.Customer;
import com.shop.model.Gender;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;

import java.time.LocalDate;

public class CustomerFormViewModel {
    private final Customer customer;
    private final boolean isEditMode;

    private final StringProperty name = new SimpleStringProperty("");
    private final StringProperty phone = new SimpleStringProperty("");
    private final StringProperty email = new SimpleStringProperty("");
    private final ObjectProperty<LocalDate> dateOfBirth = new SimpleObjectProperty<>();
    private final ObjectProperty<Gender> gender = new SimpleObjectProperty<>();
    private final StringProperty address = new SimpleStringProperty("");
    private final StringProperty note = new SimpleStringProperty("");
    private final BooleanProperty isActive = new SimpleBooleanProperty(true);

    private final BooleanProperty isValid = new SimpleBooleanProperty(false);

    public CustomerFormViewModel(Customer customer) {
        if (customer == null) {
            this.customer = new Customer();
            this.isEditMode = false;
        } else {
            this.customer = customer;
            this.isEditMode = true;
            
            this.name.set(customer.getName());
            this.phone.set(customer.getPhone());
            this.email.set(customer.getEmail());
            this.dateOfBirth.set(customer.getDateOfBirth());
            this.gender.set(customer.getGender());
            this.address.set(customer.getAddress());
            this.note.set(customer.getNote());
            this.isActive.set(customer.isActive());
        }

        isValid.bind(
                Bindings.createBooleanBinding(() -> {
                    String n = name.get();
                    String p = phone.get();
                    return n != null && !n.trim().isEmpty() &&
                           p != null && p.matches("\\d{10}");
                }, name, phone)
        );
    }

    public Customer getCustomer() {
        customer.setName(name.get().trim());
        customer.setPhone(phone.get().trim());
        customer.setEmail(email.get() != null ? email.get().trim() : null);
        customer.setDateOfBirth(dateOfBirth.get());
        customer.setGender(gender.get());
        customer.setAddress(address.get() != null ? address.get().trim() : null);
        customer.setNote(note.get() != null ? note.get().trim() : null);
        customer.setActive(isActive.get());
        return customer;
    }

    public boolean isEditMode() {
        return isEditMode;
    }

    public StringProperty nameProperty() { return name; }
    public StringProperty phoneProperty() { return phone; }
    public StringProperty emailProperty() { return email; }
    public ObjectProperty<LocalDate> dateOfBirthProperty() { return dateOfBirth; }
    public ObjectProperty<Gender> genderProperty() { return gender; }
    public StringProperty addressProperty() { return address; }
    public StringProperty noteProperty() { return note; }
    public BooleanProperty isActiveProperty() { return isActive; }
    public BooleanProperty isValidProperty() { return isValid; }
}
