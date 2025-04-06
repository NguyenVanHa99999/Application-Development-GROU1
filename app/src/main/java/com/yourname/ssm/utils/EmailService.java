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
    
    // Gmail SMTP configuration
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String EMAIL_FROM = "hanvbh01194@fpt.edu.vn"; // Provided email
    private static final String EMAIL_PASSWORD = "qmvg xavc fcip ybmh"; // Provided app password
    private static final boolean SMTP_AUTH = true;
    private static final boolean SMTP_STARTTLS = true;
    
    /**
     * Send budget warning email when user exceeds their budget
     * @param email Recipient email
     */
    public static void sendBudgetWarningEmail(String email) {
        String subject = "Budget Alert - CampusExpense Management";
        String message = "Hello,\n\n" +
                "The CampusExpense Management system has detected that you have exceeded your budget for this month.\n\n" +
                "Please log in to the CampusExpense Management app to check the details and adjust your spending plan " +
                "or update your budget if necessary.\n\n" +
                "Best regards,\n" +
                "CampusExpense Management - Student Expense Management App";
        
        sendEmail(email, subject, message);
    }
    
    /**
     * Send email via Gmail SMTP
     * @param to Recipient email
     * @param subject Email subject
     * @param messageContent Email content
     */
    private static void sendEmail(final String to, final String subject, final String messageContent) {
        // Use AsyncTask to send email in background
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    // Setup properties for SMTP connection
                    Properties props = new Properties();
                    props.put("mail.smtp.host", SMTP_HOST);
                    props.put("mail.smtp.port", SMTP_PORT);
                    props.put("mail.smtp.auth", SMTP_AUTH);
                    props.put("mail.smtp.starttls.enable", SMTP_STARTTLS);
                    
                    // Create authentication session
                    Session session = Session.getInstance(props, new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD);
                        }
                    });
                    
                    // Create message object
                    Message message = new MimeMessage(session);
                    message.setFrom(new InternetAddress(EMAIL_FROM));
                    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
                    message.setSubject(subject);
                    message.setText(messageContent);
                    
                    // Send email
                    Transport.send(message);
                    
                    Log.i(TAG, "Email successfully sent to: " + to);
                    return true;
                } catch (MessagingException e) {
                    Log.e(TAG, "Error sending email: " + e.getMessage(), e);
                    return false;
                }
            }
            
            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                    Log.d(TAG, "Email sent successfully");
                } else {
                    Log.e(TAG, "Unable to send email");
                }
            }
        }.execute();
    }
}