package com.yourname.ssm.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.yourname.ssm.R;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ssm_database.db";
    private static final int DATABASE_VERSION = 12; // Increase version to force category language update

    // Create Roles table
    private static final String CREATE_TABLE_ROLES =
            "CREATE TABLE " + DatabaseContract.RolesEntry.TABLE_NAME + " (" +
                    DatabaseContract.RolesEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DatabaseContract.RolesEntry.COLUMN_ROLE_NAME + " TEXT NOT NULL);";

    // Create Users table
    private static final String CREATE_TABLE_USERS =
            "CREATE TABLE " + DatabaseContract.UsersEntry.TABLE_NAME + " (" +
                    DatabaseContract.UsersEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DatabaseContract.UsersEntry.COLUMN_NAME + " TEXT NOT NULL, " +
                    DatabaseContract.UsersEntry.COLUMN_EMAIL + " TEXT NOT NULL UNIQUE, " +
                    DatabaseContract.UsersEntry.COLUMN_PASSWORD + " TEXT NOT NULL, " +
                    DatabaseContract.UsersEntry.COLUMN_GENDER + " TEXT, " +
                    DatabaseContract.UsersEntry.COLUMN_DOB + " TEXT, " +
                    DatabaseContract.UsersEntry.COLUMN_PHONE + " TEXT, " +
                    DatabaseContract.UsersEntry.COLUMN_ADDRESS + " TEXT, " +
                    DatabaseContract.UsersEntry.COLUMN_CREATED_AT + " TEXT DEFAULT CURRENT_TIMESTAMP, " +
                    DatabaseContract.UsersEntry.COLUMN_UPDATED_AT + " TEXT, " +
                    DatabaseContract.UsersEntry.COLUMN_IS_ACTIVE + " INTEGER DEFAULT 1, " +
                    DatabaseContract.UsersEntry.COLUMN_ROLE_ID + " INTEGER DEFAULT 2, " +
                    DatabaseContract.UsersEntry.COLUMN_RESET_TOKEN + " TEXT, " +
                    DatabaseContract.UsersEntry.COLUMN_PROFILE_IMAGE + " TEXT, " +
                    "FOREIGN KEY(" + DatabaseContract.UsersEntry.COLUMN_ROLE_ID + ") REFERENCES " +
                    DatabaseContract.RolesEntry.TABLE_NAME + "(" + DatabaseContract.RolesEntry._ID + "));";

    // Create Categories table
    private static final String CREATE_TABLE_CATEGORIES =
            "CREATE TABLE " + DatabaseContract.CategoriesEntry.TABLE_NAME + " (" +
                    DatabaseContract.CategoriesEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DatabaseContract.CategoriesEntry.COLUMN_NAME + " TEXT NOT NULL, " +
                    DatabaseContract.CategoriesEntry.COLUMN_TYPE + " TEXT NOT NULL, " +
                    DatabaseContract.CategoriesEntry.COLUMN_ICON + " INTEGER NOT NULL);";

    // Create Transactions table
    private static final String CREATE_TABLE_TRANSACTIONS =
            "CREATE TABLE " + DatabaseContract.TransactionsEntry.TABLE_NAME + " (" +
                    DatabaseContract.TransactionsEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DatabaseContract.TransactionsEntry.COLUMN_USER_ID + " INTEGER NOT NULL, " +
                    DatabaseContract.TransactionsEntry.COLUMN_AMOUNT + " REAL NOT NULL, " +
                    DatabaseContract.TransactionsEntry.COLUMN_TYPE + " TEXT NOT NULL, " +
                    DatabaseContract.TransactionsEntry.COLUMN_CATEGORY_ID + " INTEGER NOT NULL, " +
                    DatabaseContract.TransactionsEntry.COLUMN_NOTE + " TEXT, " +
                    DatabaseContract.TransactionsEntry.COLUMN_DATE + " TEXT NOT NULL, " +
                    DatabaseContract.TransactionsEntry.COLUMN_CREATED_AT + " TEXT DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY(" + DatabaseContract.TransactionsEntry.COLUMN_USER_ID + ") REFERENCES " +
                    DatabaseContract.UsersEntry.TABLE_NAME + "(" + DatabaseContract.UsersEntry._ID + "), " +
                    "FOREIGN KEY(" + DatabaseContract.TransactionsEntry.COLUMN_CATEGORY_ID + ") REFERENCES " +
                    DatabaseContract.CategoriesEntry.TABLE_NAME + "(" + DatabaseContract.CategoriesEntry._ID + "));";

    // Create Budget table
    private static final String CREATE_TABLE_BUDGET =
            "CREATE TABLE " + DatabaseContract.BudgetEntry.TABLE_NAME + " (" +
                    DatabaseContract.BudgetEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DatabaseContract.BudgetEntry.COLUMN_USER_ID + " INTEGER NOT NULL, " +
                    DatabaseContract.BudgetEntry.COLUMN_YEAR + " INTEGER NOT NULL, " +
                    DatabaseContract.BudgetEntry.COLUMN_MONTH + " INTEGER NOT NULL, " +
                    DatabaseContract.BudgetEntry.COLUMN_LIMIT + " REAL NOT NULL, " +
                    DatabaseContract.BudgetEntry.COLUMN_CURRENT_AMOUNT + " REAL DEFAULT 0, " +
                    "FOREIGN KEY(" + DatabaseContract.BudgetEntry.COLUMN_USER_ID + ") REFERENCES " +
                    DatabaseContract.UsersEntry.TABLE_NAME + "(" + DatabaseContract.UsersEntry._ID + "));";

    // Create Settings table
    private static final String CREATE_TABLE_SETTINGS =
            "CREATE TABLE " + DatabaseContract.SettingsEntry.TABLE_NAME + " (" +
                    DatabaseContract.SettingsEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DatabaseContract.SettingsEntry.COLUMN_USER_ID + " INTEGER NOT NULL UNIQUE, " +
                    DatabaseContract.SettingsEntry.COLUMN_THEME + " TEXT DEFAULT 'light', " +
                    DatabaseContract.SettingsEntry.COLUMN_CURRENCY + " TEXT DEFAULT 'VND', " +
                    "FOREIGN KEY(" + DatabaseContract.SettingsEntry.COLUMN_USER_ID + ") REFERENCES " +
                    DatabaseContract.UsersEntry.TABLE_NAME + "(" + DatabaseContract.UsersEntry._ID + "));";

    // Create Logs table
    private static final String CREATE_TABLE_LOGS =
            "CREATE TABLE " + DatabaseContract.LogsEntry.TABLE_NAME + " (" +
                    DatabaseContract.LogsEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DatabaseContract.LogsEntry.COLUMN_USER_ID + " INTEGER NOT NULL, " +
                    DatabaseContract.LogsEntry.COLUMN_ACTION + " TEXT NOT NULL, " +
                    DatabaseContract.LogsEntry.COLUMN_TIMESTAMP + " TEXT DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY(" + DatabaseContract.LogsEntry.COLUMN_USER_ID + ") REFERENCES " +
                    DatabaseContract.UsersEntry.TABLE_NAME + "(" + DatabaseContract.UsersEntry._ID + "));";

    // Create Chat Messages table
    private static final String CREATE_TABLE_CHAT_MESSAGES =
            "CREATE TABLE " + DatabaseContract.ChatMessagesEntry.TABLE_NAME + " (" +
                    DatabaseContract.ChatMessagesEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DatabaseContract.ChatMessagesEntry.COLUMN_USER_ID + " INTEGER NOT NULL, " +
                    DatabaseContract.ChatMessagesEntry.COLUMN_MESSAGE + " TEXT, " +
                    DatabaseContract.ChatMessagesEntry.COLUMN_TYPE + " INTEGER NOT NULL, " +
                    DatabaseContract.ChatMessagesEntry.COLUMN_TIMESTAMP + " INTEGER NOT NULL, " +
                    DatabaseContract.ChatMessagesEntry.COLUMN_IMAGE_URL + " TEXT, " +
                    DatabaseContract.ChatMessagesEntry.COLUMN_CONTENT_TYPE + " INTEGER DEFAULT 0, " +
                    DatabaseContract.ChatMessagesEntry.COLUMN_CREATED_AT + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create tables
        db.execSQL(CREATE_TABLE_ROLES);
        db.execSQL(CREATE_TABLE_USERS);
        db.execSQL(CREATE_TABLE_CATEGORIES);
        db.execSQL(CREATE_TABLE_TRANSACTIONS);
        db.execSQL(CREATE_TABLE_BUDGET);
        db.execSQL(CREATE_TABLE_SETTINGS);
        db.execSQL(CREATE_TABLE_LOGS);
        db.execSQL(CREATE_TABLE_CHAT_MESSAGES);

        // Add sample data
        initSampleData(db);
    }

    private void initSampleData(SQLiteDatabase db) {
        // Add roles
        db.execSQL("INSERT INTO " + DatabaseContract.RolesEntry.TABLE_NAME + " (" +
                DatabaseContract.RolesEntry.COLUMN_ROLE_NAME + ") VALUES " +
                "('admin'), ('student');");

        // Add users
        db.execSQL("INSERT INTO " + DatabaseContract.UsersEntry.TABLE_NAME + " (" +
                DatabaseContract.UsersEntry.COLUMN_NAME + ", " +
                DatabaseContract.UsersEntry.COLUMN_EMAIL + ", " +
                DatabaseContract.UsersEntry.COLUMN_PASSWORD + ", " +
                DatabaseContract.UsersEntry.COLUMN_ROLE_ID + ") VALUES " +
                "('Admin User', 'admin@example.com', 'hash_password', 1), " +
                "('Student User', 'student@example.com', 'hash_password', 2);");

        // Add categories with correct drawable resources
        db.execSQL("INSERT INTO " + DatabaseContract.CategoriesEntry.TABLE_NAME + " (" +
                DatabaseContract.CategoriesEntry.COLUMN_NAME + ", " +
                DatabaseContract.CategoriesEntry.COLUMN_TYPE + ", " +
                DatabaseContract.CategoriesEntry.COLUMN_ICON + ") VALUES " +
                "('Food', 'expense', " + R.drawable.ic_category_food + "), " +
                "('Transport', 'expense', " + R.drawable.ic_category_transport + "), " +
                "('Salary', 'income', " + R.drawable.ic_category_salary + "), " +
                "('Investment', 'income', " + R.drawable.ic_category_investment + ");");

        // Add transactions
        db.execSQL("INSERT INTO " + DatabaseContract.TransactionsEntry.TABLE_NAME + " (" +
                DatabaseContract.TransactionsEntry.COLUMN_USER_ID + ", " +
                DatabaseContract.TransactionsEntry.COLUMN_AMOUNT + ", " +
                DatabaseContract.TransactionsEntry.COLUMN_TYPE + ", " +
                DatabaseContract.TransactionsEntry.COLUMN_CATEGORY_ID + ", " +
                DatabaseContract.TransactionsEntry.COLUMN_NOTE + ", " +
                DatabaseContract.TransactionsEntry.COLUMN_DATE + ") VALUES " +
                "(2, 100000, 'expense', 1, 'Lunch', '2023-01-01'), " +
                "(2, 50000, 'expense', 2, 'Taxi', '2023-01-02'), " +
                "(2, 5000000, 'income', 3, 'January salary', '2023-01-03'), " +
                "(2, 1000000, 'income', 4, 'Stock returns', '2023-01-04');");

        // Add budget
        db.execSQL("INSERT INTO " + DatabaseContract.BudgetEntry.TABLE_NAME + " (" +
                DatabaseContract.BudgetEntry.COLUMN_USER_ID + ", " +
                DatabaseContract.BudgetEntry.COLUMN_YEAR + ", " +
                DatabaseContract.BudgetEntry.COLUMN_MONTH + ", " +
                DatabaseContract.BudgetEntry.COLUMN_LIMIT + ") VALUES " +
                "(2, 2023, 1, 3000000);");

        // Add settings
        db.execSQL("INSERT INTO " + DatabaseContract.SettingsEntry.TABLE_NAME + " (" +
                DatabaseContract.SettingsEntry.COLUMN_USER_ID + ", " +
                DatabaseContract.SettingsEntry.COLUMN_THEME + ", " +
                DatabaseContract.SettingsEntry.COLUMN_CURRENCY + ") VALUES " +
                "(2, 'light', 'VND');");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Upgrade database
        if (oldVersion < 8) {
            // Add chat_messages table if not exists
            db.execSQL("DROP TABLE IF EXISTS " + DatabaseContract.ChatMessagesEntry.TABLE_NAME);
            db.execSQL(CREATE_TABLE_CHAT_MESSAGES);
        }
        
        // Version 9 upgrades - fix any potential issues with tables
        if (oldVersion < 9) {
            try {
                // Temporarily disable foreign key constraints
                db.execSQL("PRAGMA foreign_keys=OFF");
                
                // Save data from current transaction table
                db.execSQL("CREATE TABLE IF NOT EXISTS temp_transactions AS SELECT * FROM " + 
                           DatabaseContract.TransactionsEntry.TABLE_NAME);
                
                // Save data from current budget table
                db.execSQL("CREATE TABLE IF NOT EXISTS temp_budgets AS SELECT * FROM " + 
                           DatabaseContract.BudgetEntry.TABLE_NAME);
                
                // Save data from current category table
                db.execSQL("CREATE TABLE IF NOT EXISTS temp_categories AS SELECT * FROM " + 
                           DatabaseContract.CategoriesEntry.TABLE_NAME);
                
                // Drop old tables
                db.execSQL("DROP TABLE IF EXISTS " + DatabaseContract.TransactionsEntry.TABLE_NAME);
                db.execSQL("DROP TABLE IF EXISTS " + DatabaseContract.BudgetEntry.TABLE_NAME);
                db.execSQL("DROP TABLE IF EXISTS " + DatabaseContract.CategoriesEntry.TABLE_NAME);
                
                // Recreate tables with new structure
                db.execSQL(CREATE_TABLE_CATEGORIES);
                db.execSQL(CREATE_TABLE_BUDGET);
                db.execSQL(CREATE_TABLE_TRANSACTIONS);
                
                // Add back data from temp tables
                try {
                    db.execSQL("INSERT OR IGNORE INTO " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                              " SELECT * FROM temp_categories");
                } catch (Exception e) {
                    // If error, create new categories
                    initSampleData(db);
                }
                
                try {
                    db.execSQL("INSERT OR IGNORE INTO " + DatabaseContract.BudgetEntry.TABLE_NAME + 
                              " SELECT * FROM temp_budgets");
                } catch (Exception e) {
                    Log.e("DatabaseHelper", "Error restoring budget data", e);
                }
                
                try {
                    db.execSQL("INSERT OR IGNORE INTO " + DatabaseContract.TransactionsEntry.TABLE_NAME + 
                              " SELECT * FROM temp_transactions");
                } catch (Exception e) {
                    Log.e("DatabaseHelper", "Error restoring transaction data", e);
                }
                
                // Drop temp tables
                db.execSQL("DROP TABLE IF EXISTS temp_transactions");
                db.execSQL("DROP TABLE IF EXISTS temp_budgets");
                db.execSQL("DROP TABLE IF EXISTS temp_categories");
                
                // Re-enable foreign key constraints
                db.execSQL("PRAGMA foreign_keys=ON");
                
                Log.d("DatabaseHelper", "Database migration to version 9 completed successfully");
            } catch (Exception e) {
                Log.e("DatabaseHelper", "Error upgrading database to version 9", e);
                
                // If error, try recreating from scratch
                try {
                    db.execSQL("PRAGMA foreign_keys=OFF");
                    
                    // Drop potentially conflicting tables
                    db.execSQL("DROP TABLE IF EXISTS " + DatabaseContract.TransactionsEntry.TABLE_NAME);
                    db.execSQL("DROP TABLE IF EXISTS " + DatabaseContract.BudgetEntry.TABLE_NAME);
                    db.execSQL("DROP TABLE IF EXISTS " + DatabaseContract.CategoriesEntry.TABLE_NAME);
                    db.execSQL("DROP TABLE IF EXISTS temp_transactions");
                    db.execSQL("DROP TABLE IF EXISTS temp_budgets");
                    db.execSQL("DROP TABLE IF EXISTS temp_categories");
                    
                    // Recreate tables
                    db.execSQL(CREATE_TABLE_CATEGORIES);
                    db.execSQL(CREATE_TABLE_BUDGET);
                    db.execSQL(CREATE_TABLE_TRANSACTIONS);
                    
                    // Add sample data
                    initSampleData(db);
                    
                    db.execSQL("PRAGMA foreign_keys=ON");
                    
                    Log.d("DatabaseHelper", "Database recreated from scratch due to migration errors");
                } catch (Exception e2) {
                    Log.e("DatabaseHelper", "Fatal error during database recovery", e2);
                }
            }
        }
        
        // Version 11 upgrades - Only add new categories if needed, don't delete data
        if (oldVersion < 11) {
            try {
                Log.d("DatabaseHelper", "====== STARTING UPGRADE TO VERSION 11 ======");
                db.execSQL("PRAGMA foreign_keys=OFF");
                
                // Count existing categories
                Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseContract.CategoriesEntry.TABLE_NAME, null);
                cursor.moveToFirst();
                int categoryCount = cursor.getInt(0);
                cursor.close();
                
                // If fewer than 20 categories, add missing ones
                if (categoryCount < 20) {
                    Log.d("DatabaseHelper", "Only " + categoryCount + " categories, adding missing ones");
                    
                    // Check and add missing expense categories
                    for (int i = 1; i <= 10; i++) {
                        cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                                            " WHERE " + DatabaseContract.CategoriesEntry._ID + " = ?", 
                                            new String[]{String.valueOf(i)});
                        cursor.moveToFirst();
                        int count = cursor.getInt(0);
                        cursor.close();
                        
                        if (count == 0) {
                            switch (i) {
                                case 1:
                                    db.execSQL("INSERT INTO " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                                              " VALUES (1, 'Food', 'expense', " + R.drawable.ic_category_food + ")");
                                    break;
                                case 2:
                                    db.execSQL("INSERT INTO " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                                              " VALUES (2, 'Transport', 'expense', " + R.drawable.ic_category_transport + ")");
                                    break;
                                case 3:
                                    db.execSQL("INSERT INTO " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                                              " VALUES (3, 'Shopping', 'expense', " + R.drawable.ic_category_shopping + ")");
                                    break;
                                case 4:
                                    db.execSQL("INSERT INTO " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                                              " VALUES (4, 'Entertainment', 'expense', " + R.drawable.ic_category_entertainment + ")");
                                    break;
                                case 5:
                                    db.execSQL("INSERT INTO " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                                              " VALUES (5, 'Healthcare', 'expense', " + R.drawable.ic_category_health + ")");
                                    break;
                                case 6:
                                    db.execSQL("INSERT INTO " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                                              " VALUES (6, 'Education', 'expense', " + R.drawable.ic_category_education + ")");
                                    break;
                                case 7:
                                    db.execSQL("INSERT INTO " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                                              " VALUES (7, 'Housing', 'expense', " + R.drawable.ic_category_housing + ")");
                                    break;
                                case 8:
                                    db.execSQL("INSERT INTO " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                                              " VALUES (8, 'Utilities', 'expense', " + R.drawable.ic_category_utilities + ")");
                                    break;
                                case 9:
                                    db.execSQL("INSERT INTO " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                                              " VALUES (9, 'Travel', 'expense', " + R.drawable.ic_category_travel + ")");
                                    break;
                                case 10:
                                    db.execSQL("INSERT INTO " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                                              " VALUES (10, 'Other', 'expense', " + R.drawable.ic_category_other + ")");
                                    break;
                            }
                        }
                    }
                    
                    // Check and add missing income categories
                    for (int i = 11; i <= 20; i++) {
                        cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                                            " WHERE " + DatabaseContract.CategoriesEntry._ID + " = ?", 
                                            new String[]{String.valueOf(i)});
                        cursor.moveToFirst();
                        int count = cursor.getInt(0);
                        cursor.close();
                        
                        if (count == 0) {
                            switch (i) {
                                case 11:
                                    db.execSQL("INSERT INTO " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                                              " VALUES (11, 'Salary', 'income', " + R.drawable.ic_category_salary + ")");
                                    break;
                                case 12:
                                    db.execSQL("INSERT INTO " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                                              " VALUES (12, 'Investment', 'income', " + R.drawable.ic_category_investment + ")");
                                    break;
                                case 13:
                                    db.execSQL("INSERT INTO " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                                              " VALUES (13, 'Gift', 'income', " + R.drawable.ic_category_gift + ")");
                                    break;
                                case 14:
                                    db.execSQL("INSERT INTO " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                                              " VALUES (14, 'Scholarship', 'income', " + R.drawable.ic_category_education + ")");
                                    break;
                                case 15:
                                    db.execSQL("INSERT INTO " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                                              " VALUES (15, 'Sales', 'income', " + R.drawable.ic_category_shopping + ")");
                                    break;
                                case 16:
                                    db.execSQL("INSERT INTO " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                                              " VALUES (16, 'Bonus', 'income', " + R.drawable.ic_category_gift + ")");
                                    break;
                                case 17:
                                    db.execSQL("INSERT INTO " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                                              " VALUES (17, 'Loan', 'income', " + R.drawable.ic_category_loan + ")");
                                    break;
                                case 18:
                                    db.execSQL("INSERT INTO " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                                              " VALUES (18, 'Refund', 'income', " + R.drawable.ic_category_tech + ")");
                                    break;
                                case 19:
                                    db.execSQL("INSERT INTO " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                                              " VALUES (19, 'Part-time', 'income', " + R.drawable.ic_category_utilities + ")");
                                    break;
                                case 20:
                                    db.execSQL("INSERT INTO " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                                              " VALUES (20, 'Services', 'income', " + R.drawable.ic_category_tech + ")");
                                    break;
                            }
                        }
                    }
                }
                
                // Update all existing categories to use English names
                db.execSQL("UPDATE " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                          " SET " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Food' " +
                          " WHERE " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Ăn uống' AND " +
                          DatabaseContract.CategoriesEntry.COLUMN_TYPE + " = 'expense'");
                          
                db.execSQL("UPDATE " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                          " SET " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Transport' " +
                          " WHERE " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Di chuyển' AND " +
                          DatabaseContract.CategoriesEntry.COLUMN_TYPE + " = 'expense'");
                          
                db.execSQL("UPDATE " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                          " SET " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Shopping' " +
                          " WHERE " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Mua sắm' AND " +
                          DatabaseContract.CategoriesEntry.COLUMN_TYPE + " = 'expense'");
                          
                db.execSQL("UPDATE " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                          " SET " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Entertainment' " +
                          " WHERE " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Giải trí' AND " +
                          DatabaseContract.CategoriesEntry.COLUMN_TYPE + " = 'expense'");
                          
                db.execSQL("UPDATE " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                          " SET " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Healthcare' " +
                          " WHERE " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Y tế' AND " +
                          DatabaseContract.CategoriesEntry.COLUMN_TYPE + " = 'expense'");
                          
                db.execSQL("UPDATE " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                          " SET " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Education' " +
                          " WHERE " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Giáo dục' AND " +
                          DatabaseContract.CategoriesEntry.COLUMN_TYPE + " = 'expense'");
                          
                db.execSQL("UPDATE " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                          " SET " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Housing' " +
                          " WHERE " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Nhà ở' AND " +
                          DatabaseContract.CategoriesEntry.COLUMN_TYPE + " = 'expense'");
                          
                db.execSQL("UPDATE " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                          " SET " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Utilities' " +
                          " WHERE " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Hóa đơn & Tiện ích' AND " +
                          DatabaseContract.CategoriesEntry.COLUMN_TYPE + " = 'expense'");
                          
                db.execSQL("UPDATE " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                          " SET " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Travel' " +
                          " WHERE " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Du lịch' AND " +
                          DatabaseContract.CategoriesEntry.COLUMN_TYPE + " = 'expense'");
                          
                // Update income categories
                db.execSQL("UPDATE " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                          " SET " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Salary' " +
                          " WHERE " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Lương' AND " +
                          DatabaseContract.CategoriesEntry.COLUMN_TYPE + " = 'income'");
                          
                db.execSQL("UPDATE " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                          " SET " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Investment' " +
                          " WHERE " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Đầu tư' AND " +
                          DatabaseContract.CategoriesEntry.COLUMN_TYPE + " = 'income'");
                          
                db.execSQL("UPDATE " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                          " SET " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Gift' " +
                          " WHERE " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Quà tặng' AND " +
                          DatabaseContract.CategoriesEntry.COLUMN_TYPE + " = 'income'");
                          
                db.execSQL("UPDATE " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                          " SET " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Scholarship' " +
                          " WHERE " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Học bổng' AND " +
                          DatabaseContract.CategoriesEntry.COLUMN_TYPE + " = 'income'");
                          
                db.execSQL("UPDATE " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                          " SET " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Sales' " +
                          " WHERE " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Bán hàng' AND " +
                          DatabaseContract.CategoriesEntry.COLUMN_TYPE + " = 'income'");
                          
                db.execSQL("UPDATE " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                          " SET " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Bonus' " +
                          " WHERE " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Thưởng' AND " +
                          DatabaseContract.CategoriesEntry.COLUMN_TYPE + " = 'income'");
                          
                db.execSQL("UPDATE " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                          " SET " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Loan' " +
                          " WHERE " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Cho vay' AND " +
                          DatabaseContract.CategoriesEntry.COLUMN_TYPE + " = 'income'");
                          
                db.execSQL("UPDATE " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                          " SET " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Refund' " +
                          " WHERE " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Hoàn tiền' AND " +
                          DatabaseContract.CategoriesEntry.COLUMN_TYPE + " = 'income'");
                          
                db.execSQL("UPDATE " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                          " SET " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Part-time' " +
                          " WHERE " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Thu nhập phụ' AND " +
                          DatabaseContract.CategoriesEntry.COLUMN_TYPE + " = 'income'");
                          
                db.execSQL("UPDATE " + DatabaseContract.CategoriesEntry.TABLE_NAME + 
                          " SET " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Services' " +
                          " WHERE " + DatabaseContract.CategoriesEntry.COLUMN_NAME + " = 'Dịch vụ' AND " +
                          DatabaseContract.CategoriesEntry.COLUMN_TYPE + " = 'income'");
                
                // Check categories after update
                cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseContract.CategoriesEntry.TABLE_NAME, null);
                cursor.moveToFirst();
                int newCount = cursor.getInt(0);
                cursor.close();
                
                Log.d("DatabaseHelper", "Categories after update: " + newCount);
                
                db.execSQL("PRAGMA foreign_keys=ON");
                Log.d("DatabaseHelper", "====== FINISHED UPGRADE TO VERSION 11 ======");
            } catch (Exception e) {
                Log.e("DatabaseHelper", "Error upgrading to version 11", e);
            }
        }
    }
}
