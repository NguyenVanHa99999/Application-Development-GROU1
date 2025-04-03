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
        // Kiểm tra đăng nhập cứng
        if (email.equals("admin@example.com") && password.equals("admin123")) {
            sharedPreferences.edit()
                    .putString("role", "1")
                    .putString("email", email)
                    .putInt("userId", 1)
                    .apply(); // Admin
            return true;
        } else if (email.equals("student@example.com") && password.equals("student123")) {
            sharedPreferences.edit()
                    .putString("role", "2")
                    .putString("email", email)
                    .putInt("userId", 2)
                    .apply(); // Student
            return true;
        }

        // Hash mật khẩu trước khi so sánh với database
        String hashedPassword = hashPassword(password);
        if (hashedPassword == null) {
            return false; // Không thể hash mật khẩu
        }

        // Kiểm tra đăng nhập từ database
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

            sharedPreferences.edit()
                    .putString("role", role)
                    .putString("email", email)
                    .putInt("userId", userId)
                    .apply();

            cursor.close();
            return true;
        }

        if (cursor != null) {
            cursor.close();
        }

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
        return sharedPreferences.getString("role", "0"); // Default to "0" (not logged in)
    }

    public int getUserId() {
        return sharedPreferences.getInt("userId", -1); // Default to -1 (not logged in)
    }

    public String getEmail() {
        return sharedPreferences.getString("email", ""); // Default to empty string
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
        String role = getRole();
        return !"0".equals(role);
    }

    public void logout() {
        sharedPreferences.edit()
                .remove("role")
                .remove("email")
                .remove("userId")
                .apply();
    }
}
