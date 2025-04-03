package com.yourname.ssm.model;

public class Category {
    private int id;
    private String name;
    private int iconResourceId;
    private int type; // 0: Chi tiêu, 1: Thu nhập

    public Category(int id, String name, int iconResourceId, int type) {
        this.id = id;
        this.name = name;
        this.iconResourceId = iconResourceId;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getIconResourceId() {
        return iconResourceId;
    }

    public void setIconResourceId(int iconResourceId) {
        this.iconResourceId = iconResourceId;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isExpense() {
        return type == 0;
    }

    public boolean isIncome() {
        return type == 1;
    }
} 