package com.yourname.ssm.model;

public class User {
    public String name, email, password, gender, dob, phone, address, resetToken;
    public int isActive, roleId;
    public int id;
    private String role; // For storing role as string (admin/student)

    // Default constructor for registration
    public User() {
        this.isActive = 1;
        this.resetToken = "";
        this.id = 0;
    }

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
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getGender() {
        return gender;
    }
    
    public void setGender(String gender) {
        this.gender = gender;
    }
    
    public String getDob() {
        return dob;
    }
    
    public void setDob(String dob) {
        this.dob = dob;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getResetToken() {
        return resetToken;
    }
    
    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }
    
    public int getIsActive() {
        return isActive;
    }
    
    public void setIsActive(int isActive) {
        this.isActive = isActive;
    }
    
    public int getRoleId() {
        return roleId;
    }
    
    public void setRoleId(int roleId) {
        this.roleId = roleId;
    }
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
        // Also set the roleId based on the role string
        this.roleId = "admin".equalsIgnoreCase(role) ? 1 : 2;
    }
}
