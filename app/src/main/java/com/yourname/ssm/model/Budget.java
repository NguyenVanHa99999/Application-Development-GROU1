package com.yourname.ssm.model;

public class Budget {
    private int id;
    private int userId;
    private int year;
    private int month;
    private double limit;
    private double currentAmount;

    public Budget() {
    }

    public Budget(int id, int userId, int year, int month, double limit, double currentAmount) {
        this.id = id;
        this.userId = userId;
        this.year = year;
        this.month = month;
        this.limit = limit;
        this.currentAmount = currentAmount;
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

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public double getLimit() {
        return limit;
    }

    public void setLimit(double limit) {
        this.limit = limit;
    }

    public double getCurrentAmount() {
        return currentAmount;
    }

    public void setCurrentAmount(double currentAmount) {
        this.currentAmount = currentAmount;
    }

    public double getRemainingAmount() {
        return limit - currentAmount;
    }

    public boolean isOverLimit() {
        return currentAmount >= limit;
    }

    public double getPercentageUsed() {
        if (limit == 0) return 0;
        return (currentAmount / limit) * 100;
    }
} 