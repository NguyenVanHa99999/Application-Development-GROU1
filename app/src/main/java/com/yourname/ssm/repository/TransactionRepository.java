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
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class TransactionRepository {
    private static final String TAG = "TransactionRepository";
    private final AtomicReference<DatabaseHelper> dbHelperRef = new AtomicReference<>();
    private final Context appContext;
    private BudgetRepository budgetRepository;

    public TransactionRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.budgetRepository = new BudgetRepository(context);
    }
    
    // Lazy initialization of DatabaseHelper to optimize resources
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
            
            // Log transaction info
            Log.d(TAG, "Adding transaction: " + transaction.getType() + ", amount: " + transaction.getAmount() +
                  ", category: " + transaction.getCategoryId() + ", date: " + transaction.getDate());
            
            // Insert transaction
            result = db.insert(DatabaseContract.TransactionsEntry.TABLE_NAME, null, values);
            Log.d(TAG, "Transaction insert result: " + result);
            
            // Update budget if transaction was added successfully
            if (result > 0) {
                updateBudgetForTransaction(transaction);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding transaction", e);
        }
        
        return result;
    }

    private void updateBudgetForTransaction(Transaction transaction) {
        if (transaction == null) return;
        
        int userId = transaction.getUserId();
        
        // Get month/year from transaction date
        Calendar cal = Calendar.getInstance();
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date transactionDate = dateFormat.parse(transaction.getDate());
            cal.setTime(transactionDate);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing transaction date", e);
            // Use current date if error
        }
        
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1; // Calendar months are 0-based
        
        Log.d(TAG, "Updating budget for transaction (" + transaction.getAmount() + " " + 
              transaction.getType() + ") for " + month + "/" + year);
        
        // Initialize BudgetRepository if not already initialized
        if (budgetRepository == null) {
            budgetRepository = new BudgetRepository(appContext);
        }
        
        SQLiteDatabase db = getDbHelper().getReadableDatabase();
        Cursor cursor = null;
        
        try {
            // Find budget for current month
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
                int budgetIdColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.BudgetEntry._ID);
                int currentAmountColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.BudgetEntry.COLUMN_CURRENT_AMOUNT);
                
                int budgetId = cursor.getInt(budgetIdColumnIndex);
                double currentAmount = cursor.getDouble(currentAmountColumnIndex);
                
                cursor.close();
                cursor = null;
                
                double newAmount = currentAmount;
                
                // Only update budget for expenses
                if ("expense".equals(transaction.getType())) {
                    newAmount = currentAmount + transaction.getAmount();
                    Log.d(TAG, "Updating budget amount from " + currentAmount + " to " + newAmount);
                    
                    // Update budget
                    budgetRepository.updateCurrentAmount(budgetId, newAmount);
                }
            } else {
                // Ensure cursor is closed if no results
                if (cursor != null) {
                    cursor.close();
                    cursor = null;
                }
                
                // Create new budget if none exists
                if ("expense".equals(transaction.getType())) {
                    // Default budget of 5 million
                    double defaultBudget = 5000000.0;
                    Budget newBudget = new Budget(0, userId, year, month, defaultBudget, transaction.getAmount());
                    
                    Log.d(TAG, "Creating new budget with limit " + defaultBudget + 
                          " and initial expense " + transaction.getAmount());
                    
                    // Add new budget
                    long budgetId = budgetRepository.createBudget(newBudget);
                    
                    // Check if budget creation failed
                    if (budgetId == -1) {
                        Log.e(TAG, "Failed to create budget, trying alternative method");
                        
                        // Create budget using alternative method
                        Budget monthBudget = budgetRepository.getBudgetForMonth(userId, year, month);
                        
                        // If budget is retrieved, update expense amount
                        if (monthBudget != null && monthBudget.getId() > 0) {
                            monthBudget.setCurrentAmount(transaction.getAmount());
                            budgetRepository.updateBudget(monthBudget);
                            Log.d(TAG, "Updated existing budget using alternative method. ID: " + monthBudget.getId());
                        }
                    } else {
                        Log.d(TAG, "Successfully created new budget with ID: " + budgetId);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating budget for transaction", e);
        } finally {
            // Ensure cursor is closed
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
            
            // Format month with leading zero if needed
            String monthStr = month < 10 ? "0" + month : String.valueOf(month);
            String startDate = year + "-" + monthStr + "-01";
            String endDate = year + "-" + monthStr + "-31"; // Use 31 to include all possible days
            
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
                        
                        // Load category details
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
                
                // Get icon ID and check for validity
                int iconId = cursor.getInt(iconColumnIndex);
                if (iconId <= 0) {
                    // Use default icon based on transaction type
                    iconId = transaction.isIncome() ? 
                             android.R.drawable.ic_menu_agenda : android.R.drawable.ic_menu_edit;
                }
                transaction.setCategoryIcon(iconId);
            } else {
                // If category not found, set default value based on transaction type
                if (cursor != null) cursor.close();
                
                if (transaction.isIncome()) {
                    transaction.setCategoryName("Salary");
                    transaction.setCategoryIcon(android.R.drawable.ic_menu_agenda);
                } else {
                    transaction.setCategoryName("Other Expense");
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
            
            // Format month with leading zero if needed
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
     * Close database connection to prevent resource leaks
     */
    public void close() {
        try {
            DatabaseHelper dbHelper = dbHelperRef.getAndSet(null);
            if (dbHelper != null) {
                dbHelper.close();
                Log.d(TAG, "DatabaseHelper closed");
            }
            
            // No need to close budgetRepository as it will manage its own database connection
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
                        
                        // Load category details
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
            
            // Update transaction
            int rowsAffected = db.update(
                    DatabaseContract.TransactionsEntry.TABLE_NAME,
                    values,
                    DatabaseContract.TransactionsEntry._ID + " = ?",
                    new String[] { String.valueOf(transaction.getId()) }
            );
            
            success = rowsAffected > 0;
            
            if (success) {
                Log.d(TAG, "Transaction updated successfully: " + transaction.getId());
                
                // Update budget (if needed)
                // Implementation will be more complex
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
            
            // First, get transaction info to update budget (if needed)
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
                
                // If expense, update budget
                if ("expense".equals(type)) {
                    int userIdColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.TransactionsEntry.COLUMN_USER_ID);
                    int amountColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.TransactionsEntry.COLUMN_AMOUNT);
                    int dateColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.TransactionsEntry.COLUMN_DATE);
                    
                    int userId = cursor.getInt(userIdColumnIndex);
                    double amount = cursor.getDouble(amountColumnIndex);
                    String date = cursor.getString(dateColumnIndex);
                    
                    // Close cursor before making other operations
                    cursor.close();
                    cursor = null;
                    
                    // Update budget (subtract expense amount)
                    updateBudgetForDeletedTransaction(userId, amount, date);
                }
            }
            
            // Close cursor if still open
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
            
            // Delete transaction
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
            // Split date to get year and month
            String[] dateParts = date.split("-");
            if (dateParts.length < 3) {
                Log.e(TAG, "Invalid date format in transaction: " + date);
                return;
            }
            
            int year = Integer.parseInt(dateParts[0]);
            int month = Integer.parseInt(dateParts[1]);
            
            // Update budget (reduce expense amount)
            budgetRepository.reduceBudgetExpense(userId, year, month, amount);
        } catch (Exception e) {
            Log.e(TAG, "Error updating budget for deleted transaction", e);
        }
    }

    /**
     * Get total spending amount for a specific category
     * @param userId The user ID
     * @param categoryId The category ID
     * @return The total amount spent on this category
     */
    public double getTotalAmountByCategory(int userId, int categoryId) {
        double total = 0;
        SQLiteDatabase db = null;
        Cursor cursor = null;
        
        try {
            db = getDbHelper().getReadableDatabase();
            
            String[] columns = {"SUM(" + DatabaseContract.TransactionsEntry.COLUMN_AMOUNT + ") as total"};
            
            String selection = DatabaseContract.TransactionsEntry.COLUMN_USER_ID + " = ? AND " +
                     DatabaseContract.TransactionsEntry.COLUMN_CATEGORY_ID + " = ?";
            
            String[] selectionArgs = {
                    String.valueOf(userId),
                    String.valueOf(categoryId)
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
                Log.d(TAG, "Total spent for category " + categoryId + ": " + total);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating total for category", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return total;
    }

    /**
     * Get total spending amount for a specific category in the current month
     * @param userId The user ID
     * @param categoryId The category ID
     * @return The total amount spent on this category in the current month
     */
    public double getTotalAmountByCategoryForCurrentMonth(int userId, int categoryId) {
        // Get current year and month
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1; // Calendar months are 0-based
        
        return getTotalAmountByCategoryForMonth(userId, categoryId, year, month);
    }

    /**
     * Get total spending amount for a specific category in a specific month
     * @param userId The user ID
     * @param categoryId The category ID
     * @param year The year
     * @param month The month (1-12)
     * @return The total amount spent on this category in the specified month
     */
    public double getTotalAmountByCategoryForMonth(int userId, int categoryId, int year, int month) {
        double total = 0;
        SQLiteDatabase db = null;
        Cursor cursor = null;
        
        try {
            db = getDbHelper().getReadableDatabase();
            
            // Format month with leading zero if needed
            String monthStr = month < 10 ? "0" + month : String.valueOf(month);
            String startDate = year + "-" + monthStr + "-01";
            String endDate = year + "-" + monthStr + "-31";
            
            String[] columns = {"SUM(" + DatabaseContract.TransactionsEntry.COLUMN_AMOUNT + ") as total"};
            
            String selection = DatabaseContract.TransactionsEntry.COLUMN_USER_ID + " = ? AND " +
                     DatabaseContract.TransactionsEntry.COLUMN_CATEGORY_ID + " = ? AND " +
                     DatabaseContract.TransactionsEntry.COLUMN_DATE + " BETWEEN ? AND ?";
            
            String[] selectionArgs = {
                    String.valueOf(userId),
                    String.valueOf(categoryId),
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
                Log.d(TAG, "Total spent for category " + categoryId + " in month " + month + ": " + total);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating total for category in month", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return total;
    }
} 