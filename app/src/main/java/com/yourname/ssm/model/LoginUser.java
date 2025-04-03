package com.yourname.ssm.model;

public class LoginUser {
    private String email;
    private String password;
    private String role; // "1" for Admin, "2" for Student
    private int userId;

    public LoginUser(String email, String password, String role) {
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public LoginUser(String email, String password, String role, int userId) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }

    public int getUserId() {
        return userId;
    }
}
