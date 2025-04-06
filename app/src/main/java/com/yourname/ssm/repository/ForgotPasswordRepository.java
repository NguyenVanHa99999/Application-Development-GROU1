package com.yourname.ssm.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import com.yourname.ssm.database.DatabaseContract;
import com.yourname.ssm.database.DatabaseHelper;
import com.yourname.ssm.model.ForgotPassword;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Properties;
import java.util.Random;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class ForgotPasswordRepository {
    private static final String TAG = "ForgotPasswordRepo";
    private DatabaseHelper dbHelper;
    private Context context;

    // Email credentials
    private static final String EMAIL_USERNAME = "hanvbh01194@fpt.edu.vn";
    private static final String EMAIL_PASSWORD = "qmvg xavc fcip ybmh";

    public ForgotPasswordRepository(Context context) {
        this.context = context;
        this.dbHelper = new DatabaseHelper(context);
    }

    public interface OnResetPasswordListener {
        void onSuccess(String email, String newPassword);
        void onFailure(String message);
    }

    public void resetPassword(String email, OnResetPasswordListener listener) {
        // Check if email exists in database
        if (!isEmailExists(email)) {
            listener.onFailure("Email not found in our records");
            return;
        }

        // Generate new password
        String newPassword = generateRandomPassword(8);
        String hashedPassword = hashPassword(newPassword);

        // Update password in database
        if (updatePasswordInDatabase(email, hashedPassword)) {
            // Send email with new password
            new SendEmailTask(email, newPassword, listener).execute();
        } else {
            listener.onFailure("Failed to reset password. Please try again.");
        }
    }

    private boolean isEmailExists(String email) {
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

        boolean exists = cursor != null && cursor.getCount() > 0;
        if (cursor != null) {
            cursor.close();
        }
        return exists;
    }

    private boolean updatePasswordInDatabase(String email, String hashedPassword) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.UsersEntry.COLUMN_PASSWORD, hashedPassword);

        String selection = DatabaseContract.UsersEntry.COLUMN_EMAIL + " = ?";
        String[] selectionArgs = { email };

        int count = db.update(
                DatabaseContract.UsersEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs
        );

        return count > 0;
    }

    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder sb = new StringBuilder();
        Random random = new SecureRandom();
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(chars.length());
            sb.append(chars.charAt(randomIndex));
        }
        return sb.toString();
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(
                    password.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedHash);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Error hashing password", e);
            return null;
        }
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private class SendEmailTask extends AsyncTask<Void, Void, Boolean> {
        private String email;
        private String newPassword;
        private OnResetPasswordListener listener;
        private String errorMessage;

        public SendEmailTask(String email, String newPassword, OnResetPasswordListener listener) {
            this.email = email;
            this.newPassword = newPassword;
            this.listener = listener;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(EMAIL_USERNAME, EMAIL_PASSWORD);
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(EMAIL_USERNAME));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
                message.setSubject("Password Reset - CampusExpense Management App");
                message.setText("Your password has been reset.\n\n" +
                        "Your new password is: " + newPassword + "\n\n" +
                        "Please change your password after logging in for security reasons.");

                Transport.send(message);
                return true;
            } catch (MessagingException e) {
                Log.e(TAG, "Error sending email", e);
                errorMessage = "Failed to send email: " + e.getMessage();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                listener.onSuccess(email, newPassword);
            } else {
                listener.onFailure(errorMessage);
            }
        }
    }
}
