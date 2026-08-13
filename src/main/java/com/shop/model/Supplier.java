package com.shop.model;

public class Supplier {
    private int id;
    private String code;
    private String name;
    private String phone;
    private String address;
    private String note;
    private boolean isActive;
    private String createdAt;

    public Supplier() {}

    public Supplier(int id, String code, String name, String phone, String address, String note, boolean isActive, String createdAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.note = note;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
