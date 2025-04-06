package com.yourname.ssm.model;

public class Transaction {
    private int id;
    private int userId;
    private double amount;
    private String type; // "income" or "expense"
    private int categoryId;
    private String categoryName; // For display purposes
    private int categoryIcon; // For display purposes
    private String note;
    private String date;
    private String createdAt;

    public Transaction() {
    }

    public Transaction(int id, int userId, double amount, String type, int categoryId, String note, String date, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.categoryId = categoryId;
        this.note = note;
        this.date = date;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public int getCategoryIcon() {
        return categoryIcon;
    }

    public void setCategoryIcon(int categoryIcon) {
        this.categoryIcon = categoryIcon;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isIncome() {
        return "income".equals(type);
    }

    public boolean isExpense() {
        return "expense".equals(type);
    }

    public void setIncome(boolean isIncome) {
        this.type = isIncome ? "income" : "expense";
    }
} 