package com.yourname.ssm.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.yourname.ssm.database.DatabaseContract;
import com.yourname.ssm.database.DatabaseHelper;
import com.yourname.ssm.model.Transaction;
import com.yourname.ssm.model.Budget;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class TransactionRepository {
    private static final String TAG = "TransactionRepository";
    private final AtomicReference<DatabaseHelper> dbHelperRef = new AtomicReference<>();
    private final Context appContext;
    private BudgetRepository budgetRepository;

    public TransactionRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.budgetRepository = new BudgetRepository(context);
    }
    
    // Lazy initialization của DatabaseHelper để tối ưu tài nguyên
    private DatabaseHelper getDbHelper() {
        DatabaseHelper dbHelper = dbHelperRef.get();
        if (dbHelper == null) {
            dbHelper = new DatabaseHelper(appContext);
            dbHelperRef.set(dbHelper);
        }
        return dbHelper;
    }

    public long addTransaction(Transaction transaction) {
        SQLiteDatabase db = null;
        long result = -1;
        
        try {
            db = getDbHelper().getWritableDatabase();
            
            ContentValues values = new ContentValues();
            values.put(DatabaseContract.TransactionsEntry.COLUMN_USER_ID, transaction.getUserId());
            values.put(DatabaseContract.TransactionsEntry.COLUMN_AMOUNT, transaction.getAmount());
            values.put(DatabaseContract.TransactionsEntry.COLUMN_TYPE, transaction.getType());
            values.put(DatabaseContract.TransactionsEntry.COLUMN_CATEGORY_ID, transaction.getCategoryId());
            values.put(DatabaseContract.TransactionsEntry.COLUMN_NOTE, transaction.getNote());
            values.put(DatabaseContract.TransactionsEntry.COLUMN_DATE, transaction.getDate());
            
            // Log thông tin giao dịch
            Log.d(TAG, "Adding transaction: " + transaction.getType() + ", amount: " + transaction.getAmount() +
                  ", category: " + transaction.getCategoryId() + ", date: " + transaction.getDate());
            
            // Insert giao dịch
            result = db.insert(DatabaseContract.TransactionsEntry.TABLE_NAME, null, values);
            Log.d(TAG, "Transaction insert result: " + result);
            
            // Update budget nếu giao dịch được thêm thành công
            if (result > 0) {
                updateBudgetForTransaction(transaction);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding transaction", e);
        }
        
        return result;
    }

    private void updateBudgetForTransaction(Transaction transaction) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        
        try {
            // Phân tách ngày từ giao dịch để lấy năm và tháng
            String[] dateParts = transaction.getDate().split("-");
            if (dateParts.length < 3) {
                Log.e(TAG, "Invalid date format in transaction: " + transaction.getDate());
                return;
            }
            
            int year = Integer.parseInt(dateParts[0]);
            int month = Integer.parseInt(dateParts[1]);
            int userId = transaction.getUserId();
            
            Log.d(TAG, "Updating budget for transaction date: " + year + "-" + month);
            
            // Lấy ngân sách hiện tại cho tháng này
            db = getDbHelper().getReadableDatabase();
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
                
                // Đóng cursor ngay sau khi lấy dữ liệu
                cursor.close();
                cursor = null;
                
                // Cập nhật số tiền cho ngân sách
                double newAmount = currentAmount;
                
                // Chỉ cập nhật ngân sách cho chi tiêu
                if ("expense".equals(transaction.getType())) {
                    newAmount = currentAmount + transaction.getAmount();
                    Log.d(TAG, "Updating budget amount from " + currentAmount + " to " + newAmount);
                    
                    // Cập nhật ngân sách
                    budgetRepository.updateCurrentAmount(budgetId, newAmount);
                }
            } else {
                // Đảm bảo đóng cursor nếu không có kết quả
                if (cursor != null) {
                    cursor.close();
                    cursor = null;
                }
                
                // Tạo ngân sách mới nếu chưa có
                if ("expense".equals(transaction.getType())) {
                    // Ngân sách mặc định 5 triệu
                    double defaultBudget = 5000000.0;
                    Budget newBudget = new Budget(0, userId, year, month, defaultBudget, transaction.getAmount());
                    
                    Log.d(TAG, "Creating new budget with limit " + defaultBudget + 
                          " and initial expense " + transaction.getAmount());
                    
                    // Thêm ngân sách mới
                    budgetRepository.createBudget(newBudget);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating budget for transaction", e);
        } finally {
            // Đảm bảo cursor luôn được đóng
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public List<Transaction> getTransactionsForCurrentMonth(int userId) {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1; // Calendar months are 0-based
        
        return getTransactionsForMonth(userId, year, month);
    }

    public List<Transaction> getTransactionsForMonth(int userId, int year, int month) {
        List<Transaction> transactions = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        
        try {
            db = getDbHelper().getReadableDatabase();
            
            // Format tháng với số 0 đằng trước nếu cần
            String monthStr = month < 10 ? "0" + month : String.valueOf(month);
            String startDate = year + "-" + monthStr + "-01";
            String endDate = year + "-" + monthStr + "-31"; // Dùng 31 để bao gồm tất cả ngày có thể
            
            Log.d(TAG, "Getting transactions from " + startDate + " to " + endDate);
            
            String[] projection = {
                    DatabaseContract.TransactionsEntry._ID,
                    DatabaseContract.TransactionsEntry.COLUMN_USER_ID,
                    DatabaseContract.TransactionsEntry.COLUMN_AMOUNT,
                    DatabaseContract.TransactionsEntry.COLUMN_TYPE,
                    DatabaseContract.TransactionsEntry.COLUMN_CATEGORY_ID,
                    DatabaseContract.TransactionsEntry.COLUMN_NOTE,
                    DatabaseContract.TransactionsEntry.COLUMN_DATE,
                    DatabaseContract.TransactionsEntry.COLUMN_CREATED_AT
            };
            
            String selection = DatabaseContract.TransactionsEntry.COLUMN_USER_ID + " = ? AND " +
                    DatabaseContract.TransactionsEntry.COLUMN_DATE + " BETWEEN ? AND ?";
            String[] selectionArgs = {
                    String.valueOf(userId),
                    startDate,
                    endDate
            };
            
            String sortOrder = DatabaseContract.TransactionsEntry.COLUMN_DATE + " DESC";
            
            cursor = db.query(
                    DatabaseContract.TransactionsEntry.TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder
            );
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    try {
                        int idColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.TransactionsEntry._ID);
                        int userIdColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.TransactionsEntry.COLUMN_USER_ID);
                        int amountColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.TransactionsEntry.COLUMN_AMOUNT);
                        int typeColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.TransactionsEntry.COLUMN_TYPE);
                        int categoryIdColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.TransactionsEntry.COLUMN_CATEGORY_ID);
                        int noteColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.TransactionsEntry.COLUMN_NOTE);
                        int dateColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.TransactionsEntry.COLUMN_DATE);
                        int createdAtColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.TransactionsEntry.COLUMN_CREATED_AT);
                        
                        Transaction transaction = new Transaction(
                                cursor.getInt(idColumnIndex),
                                cursor.getInt(userIdColumnIndex),
                                cursor.getDouble(amountColumnIndex),
                                cursor.getString(typeColumnIndex),
                                cursor.getInt(categoryIdColumnIndex),
                                cursor.getString(noteColumnIndex),
                                cursor.getString(dateColumnIndex),
                                cursor.getString(createdAtColumnIndex)
                        );
                        
                        // Tải thông tin chi tiết về danh mục
                        loadCategoryDetails(transaction);
                        transactions.add(transaction);
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing transaction row", e);
                    }
                } while (cursor.moveToNext());
            } else {
                Log.d(TAG, "No transactions found for the specified period");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting transactions for month", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return transactions;
    }

    private void loadCategoryDetails(Transaction transaction) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        
        try {
            db = getDbHelper().getReadableDatabase();
            
            String[] projection = {
                    DatabaseContract.CategoriesEntry.COLUMN_NAME,
                    DatabaseContract.CategoriesEntry.COLUMN_ICON
            };
            
            String selection = DatabaseContract.CategoriesEntry._ID + " = ?";
            String[] selectionArgs = { String.valueOf(transaction.getCategoryId()) };
            
            cursor = db.query(
                    DatabaseContract.CategoriesEntry.TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    null
            );
            
            if (cursor != null && cursor.moveToFirst()) {
                int nameColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.CategoriesEntry.COLUMN_NAME);
                int iconColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.CategoriesEntry.COLUMN_ICON);
                
                transaction.setCategoryName(cursor.getString(nameColumnIndex));
                
                // Lấy icon ID và kiểm tra tính hợp lệ
                int iconId = cursor.getInt(iconColumnIndex);
                if (iconId <= 0) {
                    // Sử dụng icon mặc định dựa vào loại giao dịch
                    iconId = transaction.isIncome() ? 
                             android.R.drawable.ic_menu_agenda : android.R.drawable.ic_menu_edit;
                }
                transaction.setCategoryIcon(iconId);
            } else {
                // Nếu không tìm thấy danh mục, đặt giá trị mặc định dựa trên loại giao dịch
                if (cursor != null) cursor.close();
                
                if (transaction.isIncome()) {
                    transaction.setCategoryName("Lương");
                    transaction.setCategoryIcon(android.R.drawable.ic_menu_agenda);
                } else {
                    transaction.setCategoryName("Chi tiêu khác");
                    transaction.setCategoryIcon(android.R.drawable.ic_menu_more);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading category details for transaction", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public double getTotalIncomeForMonth(int userId, int year, int month) {
        return getTotalForMonthByType(userId, year, month, "income");
    }

    public double getTotalExpenseForMonth(int userId, int year, int month) {
        return getTotalForMonthByType(userId, year, month, "expense");
    }

    private double getTotalForMonthByType(int userId, int year, int month, String type) {
        double total = 0;
        SQLiteDatabase db = null;
        Cursor cursor = null;
        
        try {
            db = getDbHelper().getReadableDatabase();
            
            // Format tháng với số 0 đằng trước nếu cần
            String monthStr = month < 10 ? "0" + month : String.valueOf(month);
            String startDate = year + "-" + monthStr + "-01";
            String endDate = year + "-" + monthStr + "-31";
            
            String[] columns = {"SUM(" + DatabaseContract.TransactionsEntry.COLUMN_AMOUNT + ") as total"};
            
            String selection = DatabaseContract.TransactionsEntry.COLUMN_USER_ID + " = ? AND " +
                     DatabaseContract.TransactionsEntry.COLUMN_TYPE + " = ? AND " +
                     DatabaseContract.TransactionsEntry.COLUMN_DATE + " BETWEEN ? AND ?";
            
            String[] selectionArgs = {
                    String.valueOf(userId),
                    type,
                    startDate,
                    endDate
            };
            
            cursor = db.query(
                    DatabaseContract.TransactionsEntry.TABLE_NAME,
                    columns,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    null
            );
            
            if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0)) {
                total = cursor.getDouble(0);
                Log.d(TAG, "Total " + type + " for month " + month + ": " + total);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating total for month by type", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return total;
    }
    
    /**
     * Đóng kết nối database để tránh rò rỉ tài nguyên
     */
    public void close() {
        try {
            DatabaseHelper dbHelper = dbHelperRef.getAndSet(null);
            if (dbHelper != null) {
                dbHelper.close();
                Log.d(TAG, "DatabaseHelper closed");
            }
            
            // Không cần đóng budgetRepository vì nó sẽ tự quản lý việc đóng kết nối DB của nó
            budgetRepository = null;
            
            Log.d(TAG, "TransactionRepository closed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error closing TransactionRepository", e);
        }
    }

    public List<Transaction> getTransactionsForDateRange(int userId, String fromDate, String toDate) {
        List<Transaction> transactions = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        
        try {
            db = getDbHelper().getReadableDatabase();
            
            Log.d(TAG, "Getting transactions from " + fromDate + " to " + toDate);
            
            String[] projection = {
                    DatabaseContract.TransactionsEntry._ID,
                    DatabaseContract.TransactionsEntry.COLUMN_USER_ID,
                    DatabaseContract.TransactionsEntry.COLUMN_AMOUNT,
                    DatabaseContract.TransactionsEntry.COLUMN_TYPE,
                    DatabaseContract.TransactionsEntry.COLUMN_CATEGORY_ID,
                    DatabaseContract.TransactionsEntry.COLUMN_NOTE,
                    DatabaseContract.TransactionsEntry.COLUMN_DATE,
                    DatabaseContract.TransactionsEntry.COLUMN_CREATED_AT
            };
            
            String selection = DatabaseContract.TransactionsEntry.COLUMN_USER_ID + " = ? AND " +
                    DatabaseContract.TransactionsEntry.COLUMN_DATE + " BETWEEN ? AND ?";
            String[] selectionArgs = {
                    String.valueOf(userId),
                    fromDate,
                    toDate
            };
            
            String sortOrder = DatabaseContract.TransactionsEntry.COLUMN_DATE + " DESC";
            
            cursor = db.query(
                    DatabaseContract.TransactionsEntry.TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder
            );
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    try {
                        int idColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.TransactionsEntry._ID);
                        int userIdColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.TransactionsEntry.COLUMN_USER_ID);
                        int amountColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.TransactionsEntry.COLUMN_AMOUNT);
                        int typeColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.TransactionsEntry.COLUMN_TYPE);
                        int categoryIdColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.TransactionsEntry.COLUMN_CATEGORY_ID);
                        int noteColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.TransactionsEntry.COLUMN_NOTE);
                        int dateColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.TransactionsEntry.COLUMN_DATE);
                        int createdAtColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.TransactionsEntry.COLUMN_CREATED_AT);
                        
                        Transaction transaction = new Transaction(
                                cursor.getInt(idColumnIndex),
                                cursor.getInt(userIdColumnIndex),
                                cursor.getDouble(amountColumnIndex),
                                cursor.getString(typeColumnIndex),
                                cursor.getInt(categoryIdColumnIndex),
                                cursor.getString(noteColumnIndex),
                                cursor.getString(dateColumnIndex),
                                cursor.getString(createdAtColumnIndex)
                        );
                        
                        // Tải thông tin chi tiết về danh mục
                        loadCategoryDetails(transaction);
                        transactions.add(transaction);
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing transaction", e);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting transactions for date range", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return transactions;
    }
    
    public boolean updateTransaction(Transaction transaction) {
        SQLiteDatabase db = null;
        boolean success = false;
        
        try {
            db = getDbHelper().getWritableDatabase();
            
            ContentValues values = new ContentValues();
            values.put(DatabaseContract.TransactionsEntry.COLUMN_AMOUNT, transaction.getAmount());
            values.put(DatabaseContract.TransactionsEntry.COLUMN_CATEGORY_ID, transaction.getCategoryId());
            values.put(DatabaseContract.TransactionsEntry.COLUMN_NOTE, transaction.getNote());
            values.put(DatabaseContract.TransactionsEntry.COLUMN_DATE, transaction.getDate());
            
            // Cập nhật giao dịch
            int rowsAffected = db.update(
                    DatabaseContract.TransactionsEntry.TABLE_NAME,
                    values,
                    DatabaseContract.TransactionsEntry._ID + " = ?",
                    new String[] { String.valueOf(transaction.getId()) }
            );
            
            success = rowsAffected > 0;
            
            if (success) {
                Log.d(TAG, "Transaction updated successfully: " + transaction.getId());
                
                // Cập nhật lại ngân sách (nếu cần)
                // Trường hợp phức tạp hơn nên triển khai sau
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating transaction", e);
        }
        
        return success;
    }
    
    public boolean deleteTransaction(int transactionId) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        boolean success = false;
        
        try {
            db = getDbHelper().getWritableDatabase();
            
            // Đầu tiên lấy thông tin giao dịch để cập nhật ngân sách (nếu cần)
            String[] projection = {
                    DatabaseContract.TransactionsEntry.COLUMN_USER_ID,
                    DatabaseContract.TransactionsEntry.COLUMN_AMOUNT,
                    DatabaseContract.TransactionsEntry.COLUMN_TYPE,
                    DatabaseContract.TransactionsEntry.COLUMN_DATE
            };
            
            String selection = DatabaseContract.TransactionsEntry._ID + " = ?";
            String[] selectionArgs = { String.valueOf(transactionId) };
            
            cursor = db.query(
                    DatabaseContract.TransactionsEntry.TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    null
            );
            
            if (cursor != null && cursor.moveToFirst()) {
                int typeColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.TransactionsEntry.COLUMN_TYPE);
                String type = cursor.getString(typeColumnIndex);
                
                // Nếu là chi tiêu, cập nhật lại ngân sách
                if ("expense".equals(type)) {
                    int userIdColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.TransactionsEntry.COLUMN_USER_ID);
                    int amountColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.TransactionsEntry.COLUMN_AMOUNT);
                    int dateColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.TransactionsEntry.COLUMN_DATE);
                    
                    int userId = cursor.getInt(userIdColumnIndex);
                    double amount = cursor.getDouble(amountColumnIndex);
                    String date = cursor.getString(dateColumnIndex);
                    
                    // Đóng cursor trước khi làm các thao tác khác
                    cursor.close();
                    cursor = null;
                    
                    // Cập nhật lại ngân sách (trừ đi khoản chi tiêu)
                    updateBudgetForDeletedTransaction(userId, amount, date);
                }
            }
            
            // Đóng cursor nếu còn mở
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
            
            // Xóa giao dịch
            int rowsDeleted = db.delete(
                    DatabaseContract.TransactionsEntry.TABLE_NAME,
                    DatabaseContract.TransactionsEntry._ID + " = ?",
                    new String[] { String.valueOf(transactionId) }
            );
            
            success = rowsDeleted > 0;
            
            if (success) {
                Log.d(TAG, "Transaction deleted successfully: " + transactionId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting transaction", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return success;
    }
    
    private void updateBudgetForDeletedTransaction(int userId, double amount, String date) {
        try {
            // Phân tách ngày từ giao dịch để lấy năm và tháng
            String[] dateParts = date.split("-");
            if (dateParts.length < 3) {
                Log.e(TAG, "Invalid date format in transaction: " + date);
                return;
            }
            
            int year = Integer.parseInt(dateParts[0]);
            int month = Integer.parseInt(dateParts[1]);
            
            // Cập nhật ngân sách (giảm số tiền chi tiêu)
            budgetRepository.reduceBudgetExpense(userId, year, month, amount);
        } catch (Exception e) {
            Log.e(TAG, "Error updating budget for deleted transaction", e);
        }
    }
} 