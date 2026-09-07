package com.shop.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CustomerImportItem {
    private int rowNumber;
    private String code = "";
    private String name = "";
    private String phone = "";
    private String email = "";
    private String dateOfBirthStr = "";
    private LocalDate dateOfBirth;
    private String genderStr = "";
    private Gender gender;
    private String address = "";
    private String note = "";

    private boolean codeError = false;
    private boolean nameError = false;
    private boolean phoneError = false;
    private boolean emailError = false;
    private boolean dobError = false;
    private boolean genderError = false;

    private final List<String> errorMessages = new ArrayList<>();

    public int getRowNumber() { return rowNumber; }
    public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code != null ? code.trim() : ""; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name != null ? name.trim() : ""; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone != null ? phone.trim() : ""; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email != null ? email.trim() : ""; }

    public String getDateOfBirthStr() { return dateOfBirthStr; }
    public void setDateOfBirthStr(String dateOfBirthStr) { this.dateOfBirthStr = dateOfBirthStr != null ? dateOfBirthStr.trim() : ""; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getGenderStr() { return genderStr; }
    public void setGenderStr(String genderStr) { this.genderStr = genderStr != null ? genderStr.trim() : ""; }

    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address != null ? address.trim() : ""; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note != null ? note.trim() : ""; }

    public boolean isCodeError() { return codeError; }
    public void setCodeError(boolean codeError) { this.codeError = codeError; }

    public boolean isNameError() { return nameError; }
    public void setNameError(boolean nameError) { this.nameError = nameError; }

    public boolean isPhoneError() { return phoneError; }
    public void setPhoneError(boolean phoneError) { this.phoneError = phoneError; }

    public boolean isEmailError() { return emailError; }
    public void setEmailError(boolean emailError) { this.emailError = emailError; }

    public boolean isDobError() { return dobError; }
    public void setDobError(boolean dobError) { this.dobError = dobError; }

    public boolean isGenderError() { return genderError; }
    public void setGenderError(boolean genderError) { this.genderError = genderError; }

    public void addErrorMessage(String msg) {
        if (msg != null && !msg.trim().isEmpty()) {
            this.errorMessages.add(msg.trim());
        }
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public String getErrorMessage() {
        return String.join("; ", errorMessages);
    }

    public boolean hasError() {
        return codeError || nameError || phoneError || emailError || dobError || genderError || !errorMessages.isEmpty();
    }
}
