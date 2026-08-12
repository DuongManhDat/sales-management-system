package com.shop.model;

public class Customer {
    private int id;
    private String name;
    private String phone;
    private String address;
    private int status;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    
    @Override
    public String toString() {
        return name + (phone != null && !phone.isEmpty() ? " - " + phone : "");
    }
}
