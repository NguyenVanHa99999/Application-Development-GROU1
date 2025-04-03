package com.yourname.ssm.database;

import android.provider.BaseColumns;

public final class DatabaseContract {

    private DatabaseContract() {}

    // User Table
    public static class UsersEntry implements BaseColumns {
        public static final String TABLE_NAME = "users";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_EMAIL = "email";
        public static final String COLUMN_PASSWORD = "password";
        public static final String COLUMN_GENDER = "gender";
        public static final String COLUMN_DOB = "dob";
        public static final String COLUMN_PHONE = "phone";
        public static final String COLUMN_ADDRESS = "address";
        public static final String COLUMN_CREATED_AT = "created_at";
        public static final String COLUMN_UPDATED_AT = "updated_at";
        public static final String COLUMN_IS_ACTIVE = "is_active";
        public static final String COLUMN_ROLE_ID = "role_id";
        public static final String COLUMN_RESET_TOKEN = "reset_token";
        public static final String COLUMN_PROFILE_IMAGE = "profile_image";
    }

    // Roles Table
    public static class RolesEntry implements BaseColumns {
        public static final String TABLE_NAME = "roles";
        public static final String COLUMN_ROLE_NAME = "role_name";
    }

    // Transactions Table (Thay thế SpendingEntry)
    public static class TransactionsEntry implements BaseColumns {
        public static final String TABLE_NAME = "transactions";
        public static final String COLUMN_USER_ID = "user_id";
        public static final String COLUMN_AMOUNT = "amount";
        public static final String COLUMN_TYPE = "type"; // income/expense
        public static final String COLUMN_CATEGORY_ID = "category_id";
        public static final String COLUMN_NOTE = "note";
        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_CREATED_AT = "created_at";
    }

    // Categories Table
    public static class CategoriesEntry implements BaseColumns {
        public static final String TABLE_NAME = "categories";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_TYPE = "type"; // income/expense
        public static final String COLUMN_ICON = "icon";
    }

    // Budget Table
    public static class BudgetEntry implements BaseColumns {
        public static final String TABLE_NAME = "budget";
        public static final String COLUMN_USER_ID = "user_id";
        public static final String COLUMN_YEAR = "year";
        public static final String COLUMN_MONTH = "month";
        public static final String COLUMN_LIMIT = "budget_limit";
        public static final String COLUMN_CURRENT_AMOUNT = "current_amount";
    }

    // Settings Table
    public static class SettingsEntry implements BaseColumns {
        public static final String TABLE_NAME = "settings";
        public static final String COLUMN_USER_ID = "user_id";
        public static final String COLUMN_THEME = "theme"; // light/dark
        public static final String COLUMN_CURRENCY = "currency";
    }

    // Logs Table
    public static class LogsEntry implements BaseColumns {
        public static final String TABLE_NAME = "logs";
        public static final String COLUMN_USER_ID = "user_id";
        public static final String COLUMN_ACTION = "activity";
        public static final String COLUMN_TIMESTAMP = "timestamp";
    }
    
    /**
     * Định nghĩa bảng chat_messages
     */
    public static class ChatMessagesEntry implements BaseColumns {
        public static final String TABLE_NAME = "chat_messages";
        public static final String COLUMN_USER_ID = "user_id";
        public static final String COLUMN_MESSAGE = "message";
        public static final String COLUMN_TYPE = "type";
        public static final String COLUMN_TIMESTAMP = "timestamp";
        public static final String COLUMN_CREATED_AT = "created_at";
        public static final String COLUMN_IMAGE_URL = "image_url";
        public static final String COLUMN_CONTENT_TYPE = "content_type";
    }
}

