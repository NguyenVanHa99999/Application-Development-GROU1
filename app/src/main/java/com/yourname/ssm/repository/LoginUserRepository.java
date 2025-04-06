package com.yourname.ssm.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.yourname.ssm.database.DatabaseContract;
import com.yourname.ssm.database.DatabaseHelper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LoginUserRepository {

    private static final String TAG = "LoginUserRepository";
    private SharedPreferences sharedPreferences;
    private DatabaseHelper dbHelper;

    public LoginUserRepository(Context context) {
        sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        dbHelper = new DatabaseHelper(context);
    }

    public boolean authenticateUser(String email, String password) {
        // Clear any existing login data first to prevent conflicts
        logout();
        
        // Special case for admin email - secure check before anything else
        if ("admin@example.com".equals(email)) {
            if (password.equals("admin123")) {
                Log.d(TAG, "⚠️ SECURE ADMIN LOGIN - admin@example.com with correct password");
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear(); // Clear any old data
                editor.putString("role", "1");
                editor.putString("email", email);
                editor.putInt("userId", 1);
                editor.putBoolean("is_logged_in", true);
                editor.putString("user_type", "ADMIN");
                // Force commit instead of async apply for critical security data
                boolean success = editor.commit();
                
                if (!success) {
                    Log.e(TAG, "⚠️ CRITICAL ERROR: Failed to save admin credentials! Retrying...");
                    // Try one more time
                    editor = sharedPreferences.edit();
                    editor.clear();
                    editor.putString("role", "1");
                    editor.putString("email", email);
                    editor.putInt("userId", 1);
                    editor.putBoolean("is_logged_in", true);
                    editor.putString("user_type", "ADMIN");
                    success = editor.commit();
                    
                    if (!success) {
                        Log.e(TAG, "⚠️ FATAL ERROR: Failed to save admin credentials after retry!");
                    }
                }
                
                // Verify saved values immediately to ensure security
                String savedRole = sharedPreferences.getString("role", "unknown");
                String savedType = sharedPreferences.getString("user_type", "unknown");
                boolean isLoggedIn = sharedPreferences.getBoolean("is_logged_in", false);
                
                Log.d(TAG, "⚠️ ADMIN VERIFICATION - role=" + savedRole + 
                      ", type=" + savedType + ", email=" + email + ", isLoggedIn=" + isLoggedIn);
                
                // Double-check if values are wrong and fix if needed
                if (!"1".equals(savedRole) || !"ADMIN".equals(savedType)) {
                    Log.e(TAG, "⚠️ CRITICAL SECURITY FIX NEEDED - Incorrect admin values saved!");
                    editor = sharedPreferences.edit();
                    editor.putString("role", "1");
                    editor.putString("user_type", "ADMIN");
                    editor.commit();
                }
                
                return true;
            } else {
                Log.d(TAG, "⚠️ ADMIN LOGIN FAILED - admin@example.com with wrong password");
                return false;
            }
        }
        // Regular student account login
        else if (email.equals("student@example.com") && password.equals("student123")) {
            Log.d(TAG, "Student login successful with hardcoded credentials");
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear(); // Clear any old data
            editor.putString("role", "2");
            editor.putString("email", email);
            editor.putInt("userId", 2);
            editor.putBoolean("is_logged_in", true);
            editor.apply();
            
            // Force commit to ensure data is written immediately
            editor = sharedPreferences.edit();
            editor.putString("user_type", "STUDENT");
            editor.commit();
            
            // Verify saved values immediately
            String savedRole = sharedPreferences.getString("role", "unknown");
            boolean isLoggedIn = sharedPreferences.getBoolean("is_logged_in", false);
            Log.d(TAG, "VERIFIED Student data saved - role: " + savedRole + 
                  ", email: " + email + ", isLoggedIn: " + isLoggedIn);
            return true;
        }

        // Hash password before comparing with database
        String hashedPassword = hashPassword(password);
        if (hashedPassword == null) {
            Log.e(TAG, "Password hashing failed");
            return false; // Cannot hash password
        }

        // Check login from database
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = {
                DatabaseContract.UsersEntry._ID,
                DatabaseContract.UsersEntry.COLUMN_ROLE_ID
        };
        String selection = DatabaseContract.UsersEntry.COLUMN_EMAIL + " = ? AND " +
                DatabaseContract.UsersEntry.COLUMN_PASSWORD + " = ?";
        String[] selectionArgs = {email, hashedPassword};

        Cursor cursor = db.query(
                DatabaseContract.UsersEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            int userId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.UsersEntry._ID));
            String role = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.UsersEntry.COLUMN_ROLE_ID));
            
            Log.d(TAG, "Database login successful - userId: " + userId + ", role: " + role);
            sharedPreferences.edit()
                    .putString("role", role)
                    .putString("email", email)
                    .putInt("userId", userId)
                    .putBoolean("is_logged_in", true)
                    .apply();

            cursor.close();
            return true;
        }

        if (cursor != null) {
            cursor.close();
        }
        
        Log.d(TAG, "Login failed for email: " + email);
        return false;
    }

    // Phương thức hash mật khẩu sử dụng SHA-256
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(
                    password.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Error hashing password", e);
            return null;
        }
    }

    // Chuyển đổi mảng byte thành chuỗi hex
    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public String getRole() {
        String role = sharedPreferences.getString("role", "0"); // Default to "0" (not logged in)
        String userType = sharedPreferences.getString("user_type", "");
        
        // Double-check role value with user_type as backup
        if ("0".equals(role) || role.isEmpty()) {
            if ("ADMIN".equals(userType)) {
                role = "1";
                // Fix the role value
                sharedPreferences.edit().putString("role", "1").apply();
            } else if ("STUDENT".equals(userType)) {
                role = "2";
                // Fix the role value
                sharedPreferences.edit().putString("role", "2").apply();
            }
        }
        
        Log.d(TAG, "getRole() called, returning: " + role + " (userType: " + userType + ")");
        return role;
    }

    public int getUserId() {
        int userId = sharedPreferences.getInt("userId", -1); // Default to -1 (not logged in)
        Log.d(TAG, "getUserId() called, returning: " + userId);
        return userId;
    }

    public String getEmail() {
        String email = sharedPreferences.getString("email", ""); // Default to empty string
        Log.d(TAG, "getEmail() called, returning: " + email);
        return email;
    }

    public String getUsername() {
        // Check if username is stored in SharedPreferences
        String username = sharedPreferences.getString("username", null);
        
        // If not, try to get from database
        if (username == null || username.isEmpty()) {
            int userId = getUserId();
            if (userId > 0) {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                String[] projection = {
                        DatabaseContract.UsersEntry.COLUMN_NAME
                };
                String selection = DatabaseContract.UsersEntry._ID + " = ?";
                String[] selectionArgs = {String.valueOf(userId)};

                Cursor cursor = db.query(
                        DatabaseContract.UsersEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        null
                );

                if (cursor != null && cursor.moveToFirst()) {
                    username = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.UsersEntry.COLUMN_NAME));
                    // Save to SharedPreferences for future use
                    sharedPreferences.edit().putString("username", username).apply();
                    cursor.close();
                } else if (cursor != null) {
                    cursor.close();
                }
            }
        }
        
        // If still null, use a default based on role
        if (username == null || username.isEmpty()) {
            String role = getRole();
            username = "1".equals(role) ? "Admin" : "Student";
        }
        
        return username;
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean("is_logged_in", false);
    }

    public void logout() {
        sharedPreferences.edit()
                .remove("role")
                .remove("email")
                .remove("userId")
                .putBoolean("is_logged_in", false)
                .apply();
    }

    public boolean isEmailExists(String email) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = { DatabaseContract.UsersEntry._ID };
        String selection = DatabaseContract.UsersEntry.COLUMN_EMAIL + " = ?";
        String[] selectionArgs = { email };

        Cursor cursor = db.query(
                DatabaseContract.UsersEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        boolean exists = (cursor != null && cursor.getCount() > 0);
        if (cursor != null) {
            cursor.close();
        }
        return exists;
    }

    public long registerUser(com.yourname.ssm.model.User user) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        android.content.ContentValues values = new android.content.ContentValues();

        // Hash the password
        String hashedPassword = hashPassword(user.getPassword());
        if (hashedPassword == null) {
            return -1; // Password hashing failed
        }

        // Prepare user data for insertion
        values.put(DatabaseContract.UsersEntry.COLUMN_NAME, user.getName());
        values.put(DatabaseContract.UsersEntry.COLUMN_EMAIL, user.getEmail());
        values.put(DatabaseContract.UsersEntry.COLUMN_PASSWORD, hashedPassword);
        values.put(DatabaseContract.UsersEntry.COLUMN_PHONE, user.getPhone());
        values.put(DatabaseContract.UsersEntry.COLUMN_ADDRESS, user.getAddress());
        values.put(DatabaseContract.UsersEntry.COLUMN_GENDER, user.getGender());
        values.put(DatabaseContract.UsersEntry.COLUMN_DOB, user.getDob());
        
        // Role ID: 1 for admin, 2 for student
        values.put(DatabaseContract.UsersEntry.COLUMN_ROLE_ID, "student".equalsIgnoreCase(user.getRole()) ? "2" : "1");

        // Insert the user and return the ID
        return db.insert(DatabaseContract.UsersEntry.TABLE_NAME, null, values);
    }

    public void loginUser(long userId, String email) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = { DatabaseContract.UsersEntry.COLUMN_ROLE_ID, DatabaseContract.UsersEntry.COLUMN_NAME };
        String selection = DatabaseContract.UsersEntry._ID + " = ?";
        String[] selectionArgs = { String.valueOf(userId) };

        Cursor cursor = db.query(
                DatabaseContract.UsersEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            String role = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.UsersEntry.COLUMN_ROLE_ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.UsersEntry.COLUMN_NAME));

            // Save user session data
            sharedPreferences.edit()
                    .putString("role", role)
                    .putString("email", email)
                    .putInt("userId", (int)userId)
                    .putString("username", name)
                    .putBoolean("is_logged_in", true)
                    .apply();

            cursor.close();
        }
    }

    // Add getter for SharedPreferences so it can be accessed directly when needed
    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }
}
