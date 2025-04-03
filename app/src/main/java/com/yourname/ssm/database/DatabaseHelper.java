package com.yourname.ssm.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.yourname.ssm.R;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ssm_database.db";
    private static final int DATABASE_VERSION = 8; // Nâng cấp lên version 8 để thêm bảng chat_messages

    // Tạo bảng Roles
    private static final String CREATE_TABLE_ROLES =
            "CREATE TABLE " + DatabaseContract.RolesEntry.TABLE_NAME + " (" +
                    DatabaseContract.RolesEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DatabaseContract.RolesEntry.COLUMN_ROLE_NAME + " TEXT NOT NULL);";

    // Tạo bảng Users
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

    // Tạo bảng Categories
    private static final String CREATE_TABLE_CATEGORIES =
            "CREATE TABLE " + DatabaseContract.CategoriesEntry.TABLE_NAME + " (" +
                    DatabaseContract.CategoriesEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DatabaseContract.CategoriesEntry.COLUMN_NAME + " TEXT NOT NULL, " +
                    DatabaseContract.CategoriesEntry.COLUMN_TYPE + " TEXT NOT NULL, " +
                    DatabaseContract.CategoriesEntry.COLUMN_ICON + " INTEGER NOT NULL);";

    // Tạo bảng Transactions
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

    // Tạo bảng Budget
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

    // Tạo bảng Settings
    private static final String CREATE_TABLE_SETTINGS =
            "CREATE TABLE " + DatabaseContract.SettingsEntry.TABLE_NAME + " (" +
                    DatabaseContract.SettingsEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DatabaseContract.SettingsEntry.COLUMN_USER_ID + " INTEGER NOT NULL UNIQUE, " +
                    DatabaseContract.SettingsEntry.COLUMN_THEME + " TEXT DEFAULT 'light', " +
                    DatabaseContract.SettingsEntry.COLUMN_CURRENCY + " TEXT DEFAULT 'VND', " +
                    "FOREIGN KEY(" + DatabaseContract.SettingsEntry.COLUMN_USER_ID + ") REFERENCES " +
                    DatabaseContract.UsersEntry.TABLE_NAME + "(" + DatabaseContract.UsersEntry._ID + "));";

    // Tạo bảng Logs
    private static final String CREATE_TABLE_LOGS =
            "CREATE TABLE " + DatabaseContract.LogsEntry.TABLE_NAME + " (" +
                    DatabaseContract.LogsEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DatabaseContract.LogsEntry.COLUMN_USER_ID + " INTEGER NOT NULL, " +
                    DatabaseContract.LogsEntry.COLUMN_ACTION + " TEXT NOT NULL, " +
                    DatabaseContract.LogsEntry.COLUMN_TIMESTAMP + " TEXT DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY(" + DatabaseContract.LogsEntry.COLUMN_USER_ID + ") REFERENCES " +
                    DatabaseContract.UsersEntry.TABLE_NAME + "(" + DatabaseContract.UsersEntry._ID + "));";

    // Tạo bảng Chat Messages
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
        // Tạo các bảng
        db.execSQL(CREATE_TABLE_ROLES);
        db.execSQL(CREATE_TABLE_USERS);
        db.execSQL(CREATE_TABLE_CATEGORIES);
        db.execSQL(CREATE_TABLE_TRANSACTIONS);
        db.execSQL(CREATE_TABLE_BUDGET);
        db.execSQL(CREATE_TABLE_SETTINGS);
        db.execSQL(CREATE_TABLE_LOGS);
        db.execSQL(CREATE_TABLE_CHAT_MESSAGES);

        // Thêm dữ liệu mẫu
        initSampleData(db);
    }

    private void initSampleData(SQLiteDatabase db) {
        // Thêm roles
        db.execSQL("INSERT INTO " + DatabaseContract.RolesEntry.TABLE_NAME + " (" +
                DatabaseContract.RolesEntry.COLUMN_ROLE_NAME + ") VALUES " +
                "('admin'), ('student');");

        // Thêm users
        db.execSQL("INSERT INTO " + DatabaseContract.UsersEntry.TABLE_NAME + " (" +
                DatabaseContract.UsersEntry.COLUMN_NAME + ", " +
                DatabaseContract.UsersEntry.COLUMN_EMAIL + ", " +
                DatabaseContract.UsersEntry.COLUMN_PASSWORD + ", " +
                DatabaseContract.UsersEntry.COLUMN_ROLE_ID + ") VALUES " +
                "('Admin User', 'admin@example.com', 'hash_password', 1), " +
                "('Student User', 'student@example.com', 'hash_password', 2);");

        // Thêm categories với drawable resources chính xác
        db.execSQL("INSERT INTO " + DatabaseContract.CategoriesEntry.TABLE_NAME + " (" +
                DatabaseContract.CategoriesEntry.COLUMN_NAME + ", " +
                DatabaseContract.CategoriesEntry.COLUMN_TYPE + ", " +
                DatabaseContract.CategoriesEntry.COLUMN_ICON + ") VALUES " +
                "('Ăn uống', 'expense', " + R.drawable.ic_category_food + "), " +
                "('Di chuyển', 'expense', " + R.drawable.ic_category_transport + "), " +
                "('Lương', 'income', " + R.drawable.ic_category_salary + "), " +
                "('Đầu tư', 'income', " + R.drawable.ic_category_investment + ");");

        // Thêm transactions
        db.execSQL("INSERT INTO " + DatabaseContract.TransactionsEntry.TABLE_NAME + " (" +
                DatabaseContract.TransactionsEntry.COLUMN_USER_ID + ", " +
                DatabaseContract.TransactionsEntry.COLUMN_AMOUNT + ", " +
                DatabaseContract.TransactionsEntry.COLUMN_TYPE + ", " +
                DatabaseContract.TransactionsEntry.COLUMN_CATEGORY_ID + ", " +
                DatabaseContract.TransactionsEntry.COLUMN_NOTE + ", " +
                DatabaseContract.TransactionsEntry.COLUMN_DATE + ") VALUES " +
                "(2, 100000, 'expense', 1, 'Ăn trưa', '2023-01-01'), " +
                "(2, 50000, 'expense', 2, 'Xe ôm', '2023-01-02'), " +
                "(2, 5000000, 'income', 3, 'Lương tháng 1', '2023-01-03'), " +
                "(2, 1000000, 'income', 4, 'Cổ phiếu', '2023-01-04');");

        // Thêm budget
        db.execSQL("INSERT INTO " + DatabaseContract.BudgetEntry.TABLE_NAME + " (" +
                DatabaseContract.BudgetEntry.COLUMN_USER_ID + ", " +
                DatabaseContract.BudgetEntry.COLUMN_YEAR + ", " +
                DatabaseContract.BudgetEntry.COLUMN_MONTH + ", " +
                DatabaseContract.BudgetEntry.COLUMN_LIMIT + ") VALUES " +
                "(2, 2023, 1, 3000000);");

        // Thêm settings
        db.execSQL("INSERT INTO " + DatabaseContract.SettingsEntry.TABLE_NAME + " (" +
                DatabaseContract.SettingsEntry.COLUMN_USER_ID + ", " +
                DatabaseContract.SettingsEntry.COLUMN_THEME + ", " +
                DatabaseContract.SettingsEntry.COLUMN_CURRENCY + ") VALUES " +
                "(2, 'light', 'VND');");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Nâng cấp database
        if (oldVersion < 8) {
            // Thêm bảng chat_messages nếu chưa tồn tại
            db.execSQL("DROP TABLE IF EXISTS " + DatabaseContract.ChatMessagesEntry.TABLE_NAME);
            db.execSQL(CREATE_TABLE_CHAT_MESSAGES);
        }
    }
}
