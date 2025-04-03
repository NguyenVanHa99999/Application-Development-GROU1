package com.yourname.ssm.utils;

import android.os.AsyncTask;
import android.util.Log;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Utility class for sending email notifications through SMTP using Gmail.
 */
public class EmailService {
    
    private static final String TAG = "EmailService";
    
    // Cấu hình SMTP Gmail
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String EMAIL_FROM = "hanvbh01194@fpt.edu.vn"; // Email đã được cung cấp
    private static final String EMAIL_PASSWORD = "yngb tqya arso uhpu"; // App Password đã được cung cấp
    private static final boolean SMTP_AUTH = true;
    private static final boolean SMTP_STARTTLS = true;
    
    /**
     * Gửi email cảnh báo khi người dùng vượt quá ngân sách
     * @param email Email người nhận
     */
    public static void sendBudgetWarningEmail(String email) {
        String subject = "Cảnh báo ngân sách - SSM";
        String message = "Xin chào,\n\n" +
                "Hệ thống SSM phát hiện bạn đã vượt quá ngân sách được thiết lập cho tháng này.\n\n" +
                "Vui lòng đăng nhập vào ứng dụng SSM để kiểm tra chi tiết và điều chỉnh kế hoạch chi tiêu " +
                "hoặc cập nhật ngân sách nếu cần thiết.\n\n" +
                "Trân trọng,\n" +
                "SSM - Ứng dụng Quản lý Chi tiêu Sinh viên";
        
        sendEmail(email, subject, message);
    }
    
    /**
     * Gửi email thông qua Gmail SMTP
     * @param to Email người nhận
     * @param subject Tiêu đề email
     * @param messageContent Nội dung email
     */
    private static void sendEmail(final String to, final String subject, final String messageContent) {
        // Sử dụng AsyncTask để gửi email trong background
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    // Thiết lập thuộc tính cho kết nối SMTP
                    Properties props = new Properties();
                    props.put("mail.smtp.host", SMTP_HOST);
                    props.put("mail.smtp.port", SMTP_PORT);
                    props.put("mail.smtp.auth", SMTP_AUTH);
                    props.put("mail.smtp.starttls.enable", SMTP_STARTTLS);
                    
                    // Tạo phiên xác thực
                    Session session = Session.getInstance(props, new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD);
                        }
                    });
                    
                    // Tạo đối tượng message
                    Message message = new MimeMessage(session);
                    message.setFrom(new InternetAddress(EMAIL_FROM));
                    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
                    message.setSubject(subject);
                    message.setText(messageContent);
                    
                    // Gửi email
                    Transport.send(message);
                    
                    Log.i(TAG, "Email đã được gửi thành công đến: " + to);
                    return true;
                } catch (MessagingException e) {
                    Log.e(TAG, "Lỗi khi gửi email: " + e.getMessage(), e);
                    return false;
                }
            }
            
            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                    Log.d(TAG, "Email gửi thành công");
                } else {
                    Log.e(TAG, "Không thể gửi email");
                }
            }
        }.execute();
    }
}