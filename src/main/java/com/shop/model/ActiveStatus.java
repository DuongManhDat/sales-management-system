package com.shop.model;

public enum ActiveStatus {
    ACTIVE(1, "Hoạt động"),
    INACTIVE(0, "Ngừng hoạt động");

    private final int value;
    private final String displayName;

    ActiveStatus(int value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public int getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ActiveStatus fromInt(int value) {
        return value == 1 ? ACTIVE : INACTIVE;
    }

    public static ActiveStatus fromBoolean(boolean active) {
        return active ? ACTIVE : INACTIVE;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
