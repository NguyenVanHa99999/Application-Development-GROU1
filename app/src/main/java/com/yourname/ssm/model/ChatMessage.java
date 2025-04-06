package com.yourname.ssm.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Model for chat messages
 */
public class ChatMessage {
    public static final int TYPE_SENT = 1;
    public static final int TYPE_RECEIVED = 2;
    
    // Message content types
    public static final int CONTENT_TEXT = 0;
    public static final int CONTENT_IMAGE = 1;
    
    private int id;
    private int userId;
    private String message;
    private int type; // 1 = sent, 2 = received
    private long timestamp;
    private String imageUrl; // URL of image if this is an image message
    private int contentType; // 0 = text, 1 = image

    public ChatMessage() {
        this.timestamp = System.currentTimeMillis();
        this.contentType = CONTENT_TEXT;
    }

    public ChatMessage(String message, int type, int userId) {
        this.message = message;
        this.type = type;
        this.userId = userId;
        this.timestamp = System.currentTimeMillis();
        this.contentType = CONTENT_TEXT;
    }
    
    public ChatMessage(String imageUrl, int type, int userId, boolean isImage) {
        this.imageUrl = imageUrl;
        this.type = type;
        this.userId = userId;
        this.timestamp = System.currentTimeMillis();
        this.contentType = CONTENT_IMAGE;
    }

    public ChatMessage(String message, int type, int userId, String imageUrl, int contentType) {
        this.message = message;
        this.type = type;
        this.userId = userId;
        this.imageUrl = imageUrl;
        this.contentType = contentType;
        this.timestamp = System.currentTimeMillis();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public int getContentType() {
        return contentType;
    }
    
    public void setContentType(int contentType) {
        this.contentType = contentType;
    }
    
    public boolean isImage() {
        return contentType == CONTENT_IMAGE;
    }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
} 