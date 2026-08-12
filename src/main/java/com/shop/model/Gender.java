package com.shop.model;

public enum Gender {
    MALE("Nam"), 
    FEMALE("Nữ");

    private final String displayName;

    Gender(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
