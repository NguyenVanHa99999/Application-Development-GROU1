package com.yourname.ssm.model;

public class User {
    public String name, email, password, gender, dob, phone, address, resetToken;
    public int isActive, roleId;
    public int id;

    public User(String name, String email, String password, String gender,
                String dob, String phone, String address, int roleId) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.gender = gender;
        this.dob = dob;
        this.phone = phone;
        this.address = address;
        this.roleId = roleId;
        this.isActive = 1;
        this.resetToken = "";
        this.id = 0;
    }
    
    public String getName() {
        return name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public String getGender() {
        return gender;
    }
    
    public String getDob() {
        return dob;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public String getAddress() {
        return address;
    }
    
    public String getResetToken() {
        return resetToken;
    }
    
    public int getIsActive() {
        return isActive;
    }
    
    public int getRoleId() {
        return roleId;
    }
    
    public int getId() {
        return id;
    }
}
