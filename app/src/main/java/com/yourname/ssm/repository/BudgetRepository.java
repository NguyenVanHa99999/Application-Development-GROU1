package com.yourname.ssm.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.content.SharedPreferences;

import com.yourname.ssm.database.DatabaseContract;
import com.yourname.ssm.database.DatabaseHelper;
import com.yourname.ssm.model.Budget;
import com.yourname.ssm.utils.EmailService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BudgetRepository {
    private DatabaseHelper dbHelper;
    private static final String TAG = "BudgetRepository";
    private Context context;

    public BudgetRepository(Context context) {
        this.context = context;
        dbHelper = new DatabaseHelper(context);
    }

    public Budget getBudgetForCurrentMonth(int userId) {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1; // Calendar months are 0-based
        
        return getBudgetForMonth(userId, year, month);
    }
    
    public Budget getBudgetForMonth(int userId, int year, int month) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        String[] projection = {
                DatabaseContract.BudgetEntry._ID,
                DatabaseContract.BudgetEntry.COLUMN_USER_ID,
                DatabaseContract.BudgetEntry.COLUMN_YEAR,
                DatabaseContract.BudgetEntry.COLUMN_MONTH,
                DatabaseContract.BudgetEntry.COLUMN_LIMIT,
                DatabaseContract.BudgetEntry.COLUMN_CURRENT_AMOUNT
        };
        
        String selection = DatabaseContract.BudgetEntry.COLUMN_USER_ID + " = ? AND " +
                DatabaseContract.BudgetEntry.COLUMN_YEAR + " = ? AND " +
                DatabaseContract.BudgetEntry.COLUMN_MONTH + " = ?";
        String[] selectionArgs = {
                String.valueOf(userId),
                String.valueOf(year),
                String.valueOf(month)
        };
        
        Cursor cursor = db.query(
                DatabaseContract.BudgetEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );
        
        Budget budget = null;
        
        if (cursor != null && cursor.moveToFirst()) {
            int idColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.BudgetEntry._ID);
            int userIdColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.BudgetEntry.COLUMN_USER_ID);
            int yearColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.BudgetEntry.COLUMN_YEAR);
            int monthColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.BudgetEntry.COLUMN_MONTH);
            int limitColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.BudgetEntry.COLUMN_LIMIT);
            int currentAmountColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.BudgetEntry.COLUMN_CURRENT_AMOUNT);
            
            budget = new Budget(
                    cursor.getInt(idColumnIndex),
                    cursor.getInt(userIdColumnIndex),
                    cursor.getInt(yearColumnIndex),
                    cursor.getInt(monthColumnIndex),
                    cursor.getDouble(limitColumnIndex),
                    cursor.getDouble(currentAmountColumnIndex)
            );
            
            cursor.close();
        } else {
            // If no budget exists for the current month, create a default one
            budget = new Budget(0, userId, year, month, 0, 0);
            long id = createBudget(budget);
            budget.setId((int) id);
        }
        
        return budget;
    }
    
    public long createBudget(Budget budget) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.BudgetEntry.COLUMN_USER_ID, budget.getUserId());
        values.put(DatabaseContract.BudgetEntry.COLUMN_YEAR, budget.getYear());
        values.put(DatabaseContract.BudgetEntry.COLUMN_MONTH, budget.getMonth());
        values.put(DatabaseContract.BudgetEntry.COLUMN_LIMIT, budget.getLimit());
        values.put(DatabaseContract.BudgetEntry.COLUMN_CURRENT_AMOUNT, budget.getCurrentAmount());
        
        return db.insert(DatabaseContract.BudgetEntry.TABLE_NAME, null, values);
    }
    
    public int updateBudget(Budget budget) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.BudgetEntry.COLUMN_LIMIT, budget.getLimit());
        values.put(DatabaseContract.BudgetEntry.COLUMN_CURRENT_AMOUNT, budget.getCurrentAmount());
        
        String selection = DatabaseContract.BudgetEntry._ID + " = ?";
        String[] selectionArgs = { String.valueOf(budget.getId()) };
        
        return db.update(
                DatabaseContract.BudgetEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs
        );
    }
    
    public int updateCurrentAmount(int budgetId, double newAmount) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        // Lấy thông tin ngân sách để kiểm tra
        String[] projection = {
                DatabaseContract.BudgetEntry.COLUMN_USER_ID,
                DatabaseContract.BudgetEntry.COLUMN_LIMIT
        };
        
        String selection = DatabaseContract.BudgetEntry._ID + " = ?";
        String[] selectionArgs = { String.valueOf(budgetId) };
        
        Cursor cursor = db.query(
                DatabaseContract.BudgetEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );
        
        int result = 0;
        
        if (cursor != null && cursor.moveToFirst()) {
            int userIdColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.BudgetEntry.COLUMN_USER_ID);
            int limitColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.BudgetEntry.COLUMN_LIMIT);
            
            int userId = cursor.getInt(userIdColumnIndex);
            double budgetLimit = cursor.getDouble(limitColumnIndex);
            
            cursor.close();
            
            // Kiểm tra xem chi tiêu mới có vượt quá ngân sách không
            boolean wasOverLimit = false;
            
            // Lấy giá trị currentAmount hiện tại
            Cursor currentCursor = db.query(
                    DatabaseContract.BudgetEntry.TABLE_NAME,
                    new String[] { DatabaseContract.BudgetEntry.COLUMN_CURRENT_AMOUNT },
                    selection,
                    selectionArgs,
                    null,
                    null,
                    null
            );
            
            if (currentCursor != null && currentCursor.moveToFirst()) {
                int currentAmountColumnIndex = currentCursor.getColumnIndexOrThrow(DatabaseContract.BudgetEntry.COLUMN_CURRENT_AMOUNT);
                double currentAmount = currentCursor.getDouble(currentAmountColumnIndex);
                
                wasOverLimit = currentAmount >= budgetLimit;
                currentCursor.close();
            }
            
            // Cập nhật số tiền mới
            ContentValues values = new ContentValues();
            values.put(DatabaseContract.BudgetEntry.COLUMN_CURRENT_AMOUNT, newAmount);
            
            result = db.update(
                    DatabaseContract.BudgetEntry.TABLE_NAME,
                    values,
                    selection,
                    selectionArgs
            );
            
            // Kiểm tra xem có vượt quá ngân sách không và nếu chưa vượt quá trước đó
            boolean isOverLimit = newAmount >= budgetLimit;
            
            if (isOverLimit && !wasOverLimit) {
                sendBudgetOverLimitNotification(userId);
            }
        }
        
        return result;
    }
    
    private void sendBudgetOverLimitNotification(int userId) {
        // Lấy email người dùng từ SharedPreferences
        SharedPreferences sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String userEmail = sharedPref.getString("user_email", "");
        
        // Nếu có email, gửi thông báo
        if (!userEmail.isEmpty()) {
            EmailService.sendBudgetWarningEmail(userEmail);
            Log.d(TAG, "Đã gửi thông báo vượt quá ngân sách qua email: " + userEmail);
        } else {
            Log.d(TAG, "Không tìm thấy email người dùng trong SharedPreferences");
        }
    }
    
    public boolean isOverBudget(int userId) {
        Budget budget = getBudgetForCurrentMonth(userId);
        return budget != null && budget.isOverLimit();
    }
    
    public double getBudgetPercentage(int userId) {
        Budget budget = getBudgetForCurrentMonth(userId);
        return budget != null ? budget.getPercentageUsed() : 0;
    }

    public void reduceBudgetExpense(int userId, int year, int month, double amount) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        
        try {
            db = dbHelper.getWritableDatabase();
            
            // Tìm ngân sách của tháng này
            String[] projection = {
                    DatabaseContract.BudgetEntry._ID,
                    DatabaseContract.BudgetEntry.COLUMN_CURRENT_AMOUNT
            };
            
            String selection = DatabaseContract.BudgetEntry.COLUMN_USER_ID + " = ? AND " +
                    DatabaseContract.BudgetEntry.COLUMN_YEAR + " = ? AND " +
                    DatabaseContract.BudgetEntry.COLUMN_MONTH + " = ?";
            String[] selectionArgs = {
                    String.valueOf(userId),
                    String.valueOf(year),
                    String.valueOf(month)
            };
            
            cursor = db.query(
                    DatabaseContract.BudgetEntry.TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    null
            );
            
            if (cursor != null && cursor.moveToFirst()) {
                int idColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.BudgetEntry._ID);
                int currentAmountColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.BudgetEntry.COLUMN_CURRENT_AMOUNT);
                
                int budgetId = cursor.getInt(idColumnIndex);
                double currentAmount = cursor.getDouble(currentAmountColumnIndex);
                
                // Tính toán số tiền mới (giảm xuống)
                double newAmount = Math.max(0, currentAmount - amount);
                
                // Cập nhật ngân sách
                ContentValues values = new ContentValues();
                values.put(DatabaseContract.BudgetEntry.COLUMN_CURRENT_AMOUNT, newAmount);
                
                int rowsUpdated = db.update(
                        DatabaseContract.BudgetEntry.TABLE_NAME,
                        values,
                        DatabaseContract.BudgetEntry._ID + " = ?",
                        new String[] { String.valueOf(budgetId) }
                );
                
                if (rowsUpdated > 0) {
                    Log.d(TAG, "Budget updated successfully after transaction deletion. New amount: " + newAmount);
                } else {
                    Log.e(TAG, "Failed to update budget after transaction deletion");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reducing budget for deleted transaction", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Đóng kết nối database để tránh rò rỉ tài nguyên
     */
    public void close() {
        try {
            if (dbHelper != null) {
                dbHelper.close();
                dbHelper = null;
            }
            Log.d(TAG, "BudgetRepository closed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error closing BudgetRepository", e);
        }
    }
} 