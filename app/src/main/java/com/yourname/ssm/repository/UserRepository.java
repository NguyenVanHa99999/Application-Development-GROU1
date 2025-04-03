package com.yourname.ssm.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.yourname.ssm.database.DatabaseHelper;
import com.yourname.ssm.database.DatabaseContract;
import com.yourname.ssm.model.User;

import java.util.ArrayList;
import java.util.List;

public class UserRepository {
    private static final String TAG = "UserRepository";
    private final DatabaseHelper dbHelper;

    public UserRepository(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public boolean isEmailExists(String email) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseContract.UsersEntry.TABLE_NAME,
                new String[]{DatabaseContract.UsersEntry._ID},
                DatabaseContract.UsersEntry.COLUMN_EMAIL + "=?",
                new String[]{email}, null, null, null);
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    public long insertUser(User user) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(DatabaseContract.UsersEntry.COLUMN_NAME, user.name);
        values.put(DatabaseContract.UsersEntry.COLUMN_EMAIL, user.email);
        values.put(DatabaseContract.UsersEntry.COLUMN_PASSWORD, user.password);
        values.put(DatabaseContract.UsersEntry.COLUMN_GENDER, user.gender);
        values.put(DatabaseContract.UsersEntry.COLUMN_DOB, user.dob);
        values.put(DatabaseContract.UsersEntry.COLUMN_PHONE, user.phone);
        values.put(DatabaseContract.UsersEntry.COLUMN_ADDRESS, user.address);
        values.put(DatabaseContract.UsersEntry.COLUMN_ROLE_ID, user.roleId);
        values.put(DatabaseContract.UsersEntry.COLUMN_IS_ACTIVE, user.isActive);
        values.put(DatabaseContract.UsersEntry.COLUMN_RESET_TOKEN, user.resetToken);
        values.put(DatabaseContract.UsersEntry.COLUMN_CREATED_AT, String.valueOf(System.currentTimeMillis()));
        values.put(DatabaseContract.UsersEntry.COLUMN_UPDATED_AT, String.valueOf(System.currentTimeMillis()));

        return db.insert(DatabaseContract.UsersEntry.TABLE_NAME, null, values);
    }
    
    /**
     * Update the user's avatar in the database
     * @param userId User ID whose avatar needs to be updated
     * @param base64Image Base64 encoded string of the image
     * @return true if update was successful, false otherwise
     */
    public boolean updateUserAvatar(int userId, String base64Image) {
        if (userId <= 0 || base64Image == null) {
            Log.e(TAG, "Invalid parameters: userId=" + userId + ", base64Image is " + (base64Image == null ? "null" : "not null"));
            return false;
        }
        
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            
            // Add the profile image to the database
            values.put(DatabaseContract.UsersEntry.COLUMN_PROFILE_IMAGE, base64Image);
            // Update the last modified timestamp
            values.put(DatabaseContract.UsersEntry.COLUMN_UPDATED_AT, String.valueOf(System.currentTimeMillis()));
            
            // Update the user record
            String selection = DatabaseContract.UsersEntry._ID + " = ?";
            String[] selectionArgs = { String.valueOf(userId) };
            
            int count = db.update(
                    DatabaseContract.UsersEntry.TABLE_NAME,
                    values,
                    selection,
                    selectionArgs
            );
            
            Log.d(TAG, "Avatar update result: Updated " + count + " rows for user ID " + userId);
            return count > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error updating avatar in database", e);
            return false;
        }
    }
    
    /**
     * Get user's avatar from the database
     * @param userId User ID to retrieve the avatar for
     * @return Base64 encoded string of the image, or null if not found
     */
    public String getUserAvatar(int userId) {
        if (userId <= 0) {
            Log.e(TAG, "Invalid userId: " + userId);
            return null;
        }
        
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = { DatabaseContract.UsersEntry.COLUMN_PROFILE_IMAGE };
        String selection = DatabaseContract.UsersEntry._ID + " = ?";
        String[] selectionArgs = { String.valueOf(userId) };
        
        try (Cursor cursor = db.query(
                DatabaseContract.UsersEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.UsersEntry.COLUMN_PROFILE_IMAGE);
                if (!cursor.isNull(columnIndex)) {
                    return cursor.getString(columnIndex);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving avatar from database", e);
        }
        
        return null;
    }
    
    /**
     * Get user by ID from the database
     * @param userId User ID to retrieve
     * @return User object, or null if not found
     */
    public User getUserById(int userId) {
        if (userId <= 0) {
            Log.e(TAG, "Invalid userId: " + userId);
            return null;
        }
        
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = { 
            DatabaseContract.UsersEntry._ID,
            DatabaseContract.UsersEntry.COLUMN_NAME,
            DatabaseContract.UsersEntry.COLUMN_EMAIL,
            DatabaseContract.UsersEntry.COLUMN_PASSWORD,
            DatabaseContract.UsersEntry.COLUMN_GENDER,
            DatabaseContract.UsersEntry.COLUMN_DOB,
            DatabaseContract.UsersEntry.COLUMN_PHONE,
            DatabaseContract.UsersEntry.COLUMN_ADDRESS,
            DatabaseContract.UsersEntry.COLUMN_ROLE_ID,
            DatabaseContract.UsersEntry.COLUMN_IS_ACTIVE,
            DatabaseContract.UsersEntry.COLUMN_RESET_TOKEN
        };
        
        String selection = DatabaseContract.UsersEntry._ID + " = ?";
        String[] selectionArgs = { String.valueOf(userId) };
        
        try (Cursor cursor = db.query(
                DatabaseContract.UsersEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.UsersEntry.COLUMN_NAME));
                String email = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.UsersEntry.COLUMN_EMAIL));
                String password = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.UsersEntry.COLUMN_PASSWORD));
                String gender = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.UsersEntry.COLUMN_GENDER));
                String dob = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.UsersEntry.COLUMN_DOB));
                String phone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.UsersEntry.COLUMN_PHONE));
                String address = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.UsersEntry.COLUMN_ADDRESS));
                int roleId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.UsersEntry.COLUMN_ROLE_ID));
                
                User user = new User(name, email, password, gender, dob, phone, address, roleId);
                
                // Set other fields if needed
                user.isActive = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.UsersEntry.COLUMN_IS_ACTIVE));
                user.resetToken = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.UsersEntry.COLUMN_RESET_TOKEN));
                
                return user;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving user from database", e);
        }
        
        return null;
    }

    /**
     * Get all users from the database
     * @return List of User objects
     */
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        String[] projection = { 
            DatabaseContract.UsersEntry._ID,
            DatabaseContract.UsersEntry.COLUMN_NAME,
            DatabaseContract.UsersEntry.COLUMN_EMAIL,
            DatabaseContract.UsersEntry.COLUMN_PASSWORD,
            DatabaseContract.UsersEntry.COLUMN_GENDER,
            DatabaseContract.UsersEntry.COLUMN_DOB,
            DatabaseContract.UsersEntry.COLUMN_PHONE,
            DatabaseContract.UsersEntry.COLUMN_ADDRESS,
            DatabaseContract.UsersEntry.COLUMN_ROLE_ID,
            DatabaseContract.UsersEntry.COLUMN_IS_ACTIVE,
            DatabaseContract.UsersEntry.COLUMN_RESET_TOKEN
        };
        
        try (Cursor cursor = db.query(
                DatabaseContract.UsersEntry.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.UsersEntry._ID));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.UsersEntry.COLUMN_NAME));
                    String email = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.UsersEntry.COLUMN_EMAIL));
                    String password = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.UsersEntry.COLUMN_PASSWORD));
                    String gender = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.UsersEntry.COLUMN_GENDER));
                    String dob = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.UsersEntry.COLUMN_DOB));
                    String phone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.UsersEntry.COLUMN_PHONE));
                    String address = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.UsersEntry.COLUMN_ADDRESS));
                    int roleId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.UsersEntry.COLUMN_ROLE_ID));
                    
                    User user = new User(name, email, password, gender, dob, phone, address, roleId);
                    user.id = id;
                    user.isActive = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.UsersEntry.COLUMN_IS_ACTIVE));
                    user.resetToken = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.UsersEntry.COLUMN_RESET_TOKEN));
                    
                    users.add(user);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving users from database", e);
        }
        
        return users;
    }

    /**
     * Update a user in the database
     * @param userId User ID to update
     * @param user Updated User object
     * @return true if update was successful, false otherwise
     */
    public boolean updateUser(int userId, User user) {
        if (userId <= 0 || user == null) {
            Log.e(TAG, "Invalid parameters for updateUser");
            return false;
        }
        
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            
            values.put(DatabaseContract.UsersEntry.COLUMN_NAME, user.name);
            values.put(DatabaseContract.UsersEntry.COLUMN_EMAIL, user.email);
            values.put(DatabaseContract.UsersEntry.COLUMN_GENDER, user.gender);
            values.put(DatabaseContract.UsersEntry.COLUMN_DOB, user.dob);
            values.put(DatabaseContract.UsersEntry.COLUMN_PHONE, user.phone);
            values.put(DatabaseContract.UsersEntry.COLUMN_ADDRESS, user.address);
            values.put(DatabaseContract.UsersEntry.COLUMN_ROLE_ID, user.roleId);
            values.put(DatabaseContract.UsersEntry.COLUMN_IS_ACTIVE, user.isActive);
            values.put(DatabaseContract.UsersEntry.COLUMN_UPDATED_AT, String.valueOf(System.currentTimeMillis()));
            
            // Only update password if it's not empty (user might not want to change password)
            if (user.password != null && !user.password.isEmpty()) {
                values.put(DatabaseContract.UsersEntry.COLUMN_PASSWORD, user.password);
            }
            
            String selection = DatabaseContract.UsersEntry._ID + " = ?";
            String[] selectionArgs = { String.valueOf(userId) };
            
            int count = db.update(
                    DatabaseContract.UsersEntry.TABLE_NAME,
                    values,
                    selection,
                    selectionArgs
            );
            
            Log.d(TAG, "User update result: Updated " + count + " rows for user ID " + userId);
            return count > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error updating user in database", e);
            return false;
        }
    }

    /**
     * Delete a user from the database
     * @param userId User ID to delete
     * @return true if deletion was successful, false otherwise
     */
    public boolean deleteUser(int userId) {
        if (userId <= 0) {
            Log.e(TAG, "Invalid userId for deleteUser: " + userId);
            return false;
        }
        
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            
            String selection = DatabaseContract.UsersEntry._ID + " = ?";
            String[] selectionArgs = { String.valueOf(userId) };
            
            int count = db.delete(
                    DatabaseContract.UsersEntry.TABLE_NAME,
                    selection,
                    selectionArgs
            );
            
            Log.d(TAG, "User deletion result: Deleted " + count + " rows for user ID " + userId);
            return count > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting user from database", e);
            return false;
        }
    }
    
    /**
     * Disable a user account instead of deleting it
     * @param userId User ID to disable
     * @return true if disabling was successful, false otherwise
     */
    public boolean disableUser(int userId) {
        if (userId <= 0) {
            Log.e(TAG, "Invalid userId for disableUser: " + userId);
            return false;
        }
        
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            
            values.put(DatabaseContract.UsersEntry.COLUMN_IS_ACTIVE, 0);
            values.put(DatabaseContract.UsersEntry.COLUMN_UPDATED_AT, String.valueOf(System.currentTimeMillis()));
            
            String selection = DatabaseContract.UsersEntry._ID + " = ?";
            String[] selectionArgs = { String.valueOf(userId) };
            
            int count = db.update(
                    DatabaseContract.UsersEntry.TABLE_NAME,
                    values,
                    selection,
                    selectionArgs
            );
            
            Log.d(TAG, "User disable result: Updated " + count + " rows for user ID " + userId);
            return count > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error disabling user in database", e);
            return false;
        }
    }
    
    /**
     * Enable a user account
     * @param userId User ID to enable
     * @return true if enabling was successful, false otherwise
     */
    public boolean enableUser(int userId) {
        if (userId <= 0) {
            Log.e(TAG, "Invalid userId for enableUser: " + userId);
            return false;
        }
        
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            
            values.put(DatabaseContract.UsersEntry.COLUMN_IS_ACTIVE, 1);
            values.put(DatabaseContract.UsersEntry.COLUMN_UPDATED_AT, String.valueOf(System.currentTimeMillis()));
            
            String selection = DatabaseContract.UsersEntry._ID + " = ?";
            String[] selectionArgs = { String.valueOf(userId) };
            
            int count = db.update(
                    DatabaseContract.UsersEntry.TABLE_NAME,
                    values,
                    selection,
                    selectionArgs
            );
            
            Log.d(TAG, "User enable result: Updated " + count + " rows for user ID " + userId);
            return count > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error enabling user in database", e);
            return false;
        }
    }

    /**
     * Delete a user from the database by email
     * @param email Email of the user to delete
     * @return true if deletion was successful, false otherwise
     */
    public boolean deleteUserByEmail(String email) {
        if (email == null || email.isEmpty()) {
            Log.e(TAG, "Invalid email for deleteUserByEmail: " + email);
            return false;
        }
        
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            
            String selection = DatabaseContract.UsersEntry.COLUMN_EMAIL + " = ?";
            String[] selectionArgs = { email };
            
            int count = db.delete(
                    DatabaseContract.UsersEntry.TABLE_NAME,
                    selection,
                    selectionArgs
            );
            
            Log.d(TAG, "User deletion result: Deleted " + count + " rows for user email " + email);
            return count > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting user from database", e);
            return false;
        }
    }
    
    /**
     * Force delete a user from the database by email by first deleting all related data
     * @param email Email of the user to delete
     * @return true if deletion was successful, false otherwise
     */
    public boolean forceDeleteUserByEmail(String email) {
        if (email == null || email.isEmpty()) {
            Log.e(TAG, "Invalid email for forceDeleteUserByEmail: " + email);
            return false;
        }
        
        SQLiteDatabase db = null;
        boolean success = false;
        
        try {
            // Get user ID first
            int userId = getUserIdByEmail(email);
            if (userId <= 0) {
                Log.e(TAG, "User not found with email: " + email);
                return false;
            }
            
            db = dbHelper.getWritableDatabase();
            
            // Start transaction
            db.beginTransaction();
            
            try {
                // Disable foreign key constraints for this transaction
                db.execSQL("PRAGMA foreign_keys = OFF");
                
                // Use raw SQL to delete all related records
                String[] transactionsSQL = {
                    "DELETE FROM " + DatabaseContract.TransactionsEntry.TABLE_NAME + 
                    " WHERE " + DatabaseContract.TransactionsEntry.COLUMN_USER_ID + " = " + userId + ";",
                    
                    "DELETE FROM " + DatabaseContract.BudgetEntry.TABLE_NAME + 
                    " WHERE " + DatabaseContract.BudgetEntry.COLUMN_USER_ID + " = " + userId + ";",
                    
                    "DELETE FROM " + DatabaseContract.SettingsEntry.TABLE_NAME + 
                    " WHERE " + DatabaseContract.SettingsEntry.COLUMN_USER_ID + " = " + userId + ";",
                    
                    "DELETE FROM " + DatabaseContract.LogsEntry.TABLE_NAME + 
                    " WHERE " + DatabaseContract.LogsEntry.COLUMN_USER_ID + " = " + userId + ";",
                    
                    "DELETE FROM " + DatabaseContract.ChatMessagesEntry.TABLE_NAME + 
                    " WHERE " + DatabaseContract.ChatMessagesEntry.COLUMN_USER_ID + " = " + userId + ";",
                    
                    "DELETE FROM " + DatabaseContract.UsersEntry.TABLE_NAME + 
                    " WHERE " + DatabaseContract.UsersEntry.COLUMN_EMAIL + " = '" + email + "';"
                };
                
                // Execute all deletion statements
                for (String sql : transactionsSQL) {
                    try {
                        db.execSQL(sql);
                        Log.d(TAG, "Successfully executed: " + sql);
                    } catch (Exception e) {
                        Log.e(TAG, "Error executing SQL: " + sql, e);
                        // Continue with other operations even if one fails
                    }
                }
                
                // Check if user was actually deleted
                String[] columns = { DatabaseContract.UsersEntry._ID };
                String selection = DatabaseContract.UsersEntry.COLUMN_EMAIL + " = ?";
                String[] selectionArgs = { email };
                
                try (Cursor cursor = db.query(
                        DatabaseContract.UsersEntry.TABLE_NAME,
                        columns,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        null)) {
                    
                    // User was successfully deleted if cursor is empty
                    success = (cursor == null || !cursor.moveToFirst());
                }
                
                // If we reach here, mark transaction as successful
                if (success) {
                    db.setTransactionSuccessful();
                    Log.d(TAG, "Successfully deleted user with email: " + email);
                }
            } finally {
                // Re-enable foreign key constraints
                db.execSQL("PRAGMA foreign_keys = ON");
                
                // End transaction
                db.endTransaction();
            }
            
            // If normal method failed, try alternative approach with direct SQL
            if (!success) {
                Log.d(TAG, "First deletion attempt failed. Trying alternative approach.");
                success = deleteUserWithDirectSQL(db, userId, email);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error force deleting user from database", e);
            success = false;
        }
        
        return success;
    }
    
    /**
     * Alternative method to delete user using direct SQL commands
     */
    private boolean deleteUserWithDirectSQL(SQLiteDatabase db, int userId, String email) {
        try {
            // Start a fresh transaction
            db.beginTransaction();
            
            try {
                // Disable foreign key constraints completely
                db.execSQL("PRAGMA foreign_keys = OFF");
                
                // Delete all related records with error handling for each step
                String[] tables = {
                    DatabaseContract.TransactionsEntry.TABLE_NAME,
                    DatabaseContract.BudgetEntry.TABLE_NAME,
                    DatabaseContract.SettingsEntry.TABLE_NAME,
                    DatabaseContract.LogsEntry.TABLE_NAME,
                    DatabaseContract.ChatMessagesEntry.TABLE_NAME
                };
                
                for (String table : tables) {
                    try {
                        db.execSQL("DELETE FROM " + table + " WHERE user_id = " + userId);
                        Log.d(TAG, "Deleted records from " + table + " for user ID " + userId);
                    } catch (Exception e) {
                        Log.e(TAG, "Error deleting from " + table, e);
                        // Continue with next table
                    }
                }
                
                // Finally delete the user
                try {
                    db.execSQL("DELETE FROM " + DatabaseContract.UsersEntry.TABLE_NAME + 
                              " WHERE " + DatabaseContract.UsersEntry.COLUMN_EMAIL + " = '" + email + "'");
                    Log.d(TAG, "Deleted user with email " + email);
                    
                    // Mark transaction successful
                    db.setTransactionSuccessful();
                    return true;
                } catch (Exception e) {
                    Log.e(TAG, "Error deleting user", e);
                    return false;
                }
            } finally {
                // Re-enable foreign key constraints
                db.execSQL("PRAGMA foreign_keys = ON");
                
                // End transaction
                db.endTransaction();
            }
        } catch (Exception e) {
            Log.e(TAG, "Fatal error in deleteUserWithDirectSQL", e);
            return false;
        }
    }
    
    /**
     * Get user ID by email
     * @param email Email of the user
     * @return User ID or -1 if not found
     */
    public int getUserIdByEmail(String email) {
        if (email == null || email.isEmpty()) {
            Log.e(TAG, "Invalid email for getUserIdByEmail: " + email);
            return -1;
        }
        
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        String[] projection = { DatabaseContract.UsersEntry._ID };
        String selection = DatabaseContract.UsersEntry.COLUMN_EMAIL + " = ?";
        String[] selectionArgs = { email };
        
        try (Cursor cursor = db.query(
                DatabaseContract.UsersEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.UsersEntry._ID));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting user ID by email", e);
        }
        
        return -1;
    }
}
