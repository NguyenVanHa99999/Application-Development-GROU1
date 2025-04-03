package com.yourname.ssm.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.yourname.ssm.database.DatabaseContract;
import com.yourname.ssm.database.DatabaseHelper;
import com.yourname.ssm.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ChatMessageRepository {
    private static final String TAG = "ChatMessageRepository";
    private final AtomicReference<DatabaseHelper> dbHelperRef = new AtomicReference<>();
    private final Context appContext;

    public ChatMessageRepository(Context context) {
        this.appContext = context.getApplicationContext();
    }
    
    // Lazy initialization của DatabaseHelper để tối ưu tài nguyên
    private DatabaseHelper getDbHelper() {
        DatabaseHelper dbHelper = dbHelperRef.get();
        if (dbHelper == null) {
            dbHelper = new DatabaseHelper(appContext);
            dbHelperRef.set(dbHelper);
        }
        return dbHelper;
    }
    
    /**
     * Lưu tin nhắn chat vào cơ sở dữ liệu
     * @param message Đối tượng tin nhắn cần lưu
     * @return ID của tin nhắn được tạo, hoặc -1 nếu thất bại
     */
    public long saveMessage(ChatMessage message) {
        SQLiteDatabase db = null;
        long result = -1;
        
        try {
            db = getDbHelper().getWritableDatabase();
            
            ContentValues values = new ContentValues();
            values.put(DatabaseContract.ChatMessagesEntry.COLUMN_USER_ID, message.getUserId());
            values.put(DatabaseContract.ChatMessagesEntry.COLUMN_TYPE, message.getType());
            values.put(DatabaseContract.ChatMessagesEntry.COLUMN_TIMESTAMP, message.getTimestamp());
            values.put(DatabaseContract.ChatMessagesEntry.COLUMN_CONTENT_TYPE, message.getContentType());
            
            // Save appropriate content based on content type
            if (message.isImage()) {
                values.put(DatabaseContract.ChatMessagesEntry.COLUMN_IMAGE_URL, message.getImageUrl());
                // Ensure message field is not null to prevent database errors
                values.put(DatabaseContract.ChatMessagesEntry.COLUMN_MESSAGE, "");
            } else {
                values.put(DatabaseContract.ChatMessagesEntry.COLUMN_MESSAGE, message.getMessage());
                // Ensure image_url field is not null to prevent database errors
                values.put(DatabaseContract.ChatMessagesEntry.COLUMN_IMAGE_URL, "");
            }
            
            result = db.insert(DatabaseContract.ChatMessagesEntry.TABLE_NAME, null, values);
            
            if (result != -1) {
                message.setId((int) result);
                Log.d(TAG, "Message saved successfully with ID: " + result + ", content: " + 
                      (message.isImage() ? "Image" : message.getMessage().substring(0, Math.min(20, message.getMessage().length())) + "..."));
            } else {
                Log.e(TAG, "Failed to save message: " + 
                      (message.isImage() ? "Image message" : message.getMessage()));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving message", e);
        }
        
        return result;
    }
    
    /**
     * Lấy tất cả tin nhắn chat của một người dùng
     * @param userId ID của người dùng
     * @return Danh sách tin nhắn chat
     */
    public List<ChatMessage> getMessagesForUser(int userId) {
        List<ChatMessage> messages = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;
   
        try {
            db = getDbHelper().getReadableDatabase();
            
            String[] projection = {
                    DatabaseContract.ChatMessagesEntry._ID,
                    DatabaseContract.ChatMessagesEntry.COLUMN_USER_ID,
                    DatabaseContract.ChatMessagesEntry.COLUMN_MESSAGE,
                    DatabaseContract.ChatMessagesEntry.COLUMN_TYPE,
                    DatabaseContract.ChatMessagesEntry.COLUMN_TIMESTAMP,
                    DatabaseContract.ChatMessagesEntry.COLUMN_IMAGE_URL,
                    DatabaseContract.ChatMessagesEntry.COLUMN_CONTENT_TYPE
            };
            
            String selection = DatabaseContract.ChatMessagesEntry.COLUMN_USER_ID + " = ?";
            String[] selectionArgs = { String.valueOf(userId) };
            
            String sortOrder = DatabaseContract.ChatMessagesEntry.COLUMN_TIMESTAMP + " ASC";
            
            cursor = db.query(
                    DatabaseContract.ChatMessagesEntry.TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder
            );
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    ChatMessage message = createChatMessageFromCursor(cursor);
                    messages.add(message);
                } while (cursor.moveToNext());
            }
            
            Log.d(TAG, "Retrieved " + messages.size() + " messages for user " + userId);
        } catch (Exception e) {
            Log.e(TAG, "Error getting chat messages", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return messages;
    }
    
    /**
     * Helper method to create a ChatMessage from database cursor
     */
    private ChatMessage createChatMessageFromCursor(Cursor cursor) {
        ChatMessage message = new ChatMessage();
        
        int idIndex = cursor.getColumnIndex(DatabaseContract.ChatMessagesEntry._ID);
        int userIdIndex = cursor.getColumnIndex(DatabaseContract.ChatMessagesEntry.COLUMN_USER_ID);
        int messageIndex = cursor.getColumnIndex(DatabaseContract.ChatMessagesEntry.COLUMN_MESSAGE);
        int typeIndex = cursor.getColumnIndex(DatabaseContract.ChatMessagesEntry.COLUMN_TYPE);
        int timestampIndex = cursor.getColumnIndex(DatabaseContract.ChatMessagesEntry.COLUMN_TIMESTAMP);
        int imageUrlIndex = cursor.getColumnIndex(DatabaseContract.ChatMessagesEntry.COLUMN_IMAGE_URL);
        int contentTypeIndex = cursor.getColumnIndex(DatabaseContract.ChatMessagesEntry.COLUMN_CONTENT_TYPE);
        
        if (idIndex >= 0) message.setId(cursor.getInt(idIndex));
        if (userIdIndex >= 0) message.setUserId(cursor.getInt(userIdIndex));
        if (typeIndex >= 0) message.setType(cursor.getInt(typeIndex));
        if (timestampIndex >= 0) message.setTimestamp(cursor.getLong(timestampIndex));
        
        // Set content based on content type
        int contentType = contentTypeIndex >= 0 ? cursor.getInt(contentTypeIndex) : ChatMessage.CONTENT_TEXT;
        message.setContentType(contentType);
        
        if (contentType == ChatMessage.CONTENT_IMAGE) {
            if (imageUrlIndex >= 0) message.setImageUrl(cursor.getString(imageUrlIndex));
        } else {
            if (messageIndex >= 0) message.setMessage(cursor.getString(messageIndex));
        }
        
        return message;
    }
    
    /**
     * Xóa tất cả tin nhắn của một người dùng
     * @param userId ID của người dùng
     * @return Số tin nhắn đã xóa
     */
    public int deleteAllMessagesForUser(int userId) {
        SQLiteDatabase db = null;
        int result = 0;
        
        try {
            db = getDbHelper().getWritableDatabase();
            
            String selection = DatabaseContract.ChatMessagesEntry.COLUMN_USER_ID + " = ?";
            String[] selectionArgs = { String.valueOf(userId) };
            
            result = db.delete(DatabaseContract.ChatMessagesEntry.TABLE_NAME, selection, selectionArgs);
            Log.d(TAG, "Deleted " + result + " messages for user " + userId);
        } catch (Exception e) {
            Log.e(TAG, "Error deleting chat messages", e);
        }
        
        return result;
    }
    
    /**
     * Đóng database helper
     */
    public void close() {
        DatabaseHelper dbHelper = dbHelperRef.getAndSet(null);
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    /**
     * Lấy các tin nhắn gần đây nhất của người dùng
     * @param userId ID của người dùng
     * @param limit Số lượng tin nhắn cần lấy
     * @return Danh sách tin nhắn gần đây
     */
    public List<ChatMessage> getRecentMessagesForUser(int userId, int limit) {
        List<ChatMessage> messages = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        
        try {
            db = getDbHelper().getReadableDatabase();
            
            String[] projection = {
                    DatabaseContract.ChatMessagesEntry._ID,
                    DatabaseContract.ChatMessagesEntry.COLUMN_USER_ID,
                    DatabaseContract.ChatMessagesEntry.COLUMN_MESSAGE,
                    DatabaseContract.ChatMessagesEntry.COLUMN_TYPE,
                    DatabaseContract.ChatMessagesEntry.COLUMN_TIMESTAMP,
                    DatabaseContract.ChatMessagesEntry.COLUMN_IMAGE_URL,
                    DatabaseContract.ChatMessagesEntry.COLUMN_CONTENT_TYPE
            };
            
            String selection = DatabaseContract.ChatMessagesEntry.COLUMN_USER_ID + " = ?";
            String[] selectionArgs = { String.valueOf(userId) };
            
            String sortOrder = DatabaseContract.ChatMessagesEntry.COLUMN_TIMESTAMP + " DESC";
            
            cursor = db.query(
                    DatabaseContract.ChatMessagesEntry.TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder,
                    String.valueOf(limit)
            );
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    ChatMessage message = createChatMessageFromCursor(cursor);
                    messages.add(0, message);
                } while (cursor.moveToNext());
            }
            
            Log.d(TAG, "Retrieved " + messages.size() + " recent messages for user " + userId);
        } catch (Exception e) {
            Log.e(TAG, "Error getting recent chat messages", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return messages;
    }
    
    /**
     * Lấy tin nhắn gần đây nhất của hệ thống (AI) gửi cho người dùng
     * 
     * @param userId ID của người dùng
     * @return Tin nhắn gần đây nhất từ AI, hoặc null nếu không có
     */
    public ChatMessage getLastAIMessageForUser(int userId) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        ChatMessage message = null;
        
        try {
            db = getDbHelper().getReadableDatabase();
            
            String[] projection = {
                    DatabaseContract.ChatMessagesEntry._ID,
                    DatabaseContract.ChatMessagesEntry.COLUMN_USER_ID,
                    DatabaseContract.ChatMessagesEntry.COLUMN_MESSAGE,
                    DatabaseContract.ChatMessagesEntry.COLUMN_TYPE,
                    DatabaseContract.ChatMessagesEntry.COLUMN_TIMESTAMP,
                    DatabaseContract.ChatMessagesEntry.COLUMN_IMAGE_URL,
                    DatabaseContract.ChatMessagesEntry.COLUMN_CONTENT_TYPE
            };
            
            // Chỉ lấy tin nhắn từ AI (TYPE_RECEIVED)
            String selection = DatabaseContract.ChatMessagesEntry.COLUMN_USER_ID + " = ? AND " +
                              DatabaseContract.ChatMessagesEntry.COLUMN_TYPE + " = ?";
            String[] selectionArgs = { 
                String.valueOf(userId), 
                String.valueOf(ChatMessage.TYPE_RECEIVED) 
            };
            
            String sortOrder = DatabaseContract.ChatMessagesEntry.COLUMN_TIMESTAMP + " DESC";
            
            cursor = db.query(
                    DatabaseContract.ChatMessagesEntry.TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder,
                    "1" // Chỉ lấy tin nhắn mới nhất
            );
            
            if (cursor != null && cursor.moveToFirst()) {
                message = createChatMessageFromCursor(cursor);
                Log.d(TAG, "Retrieved last AI message for user " + userId + ": " + 
                      (message.isImage() ? "Image" : message.getMessage().substring(0, Math.min(20, message.getMessage().length())) + "..."));
            } else {
                Log.d(TAG, "No AI messages found for user " + userId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting last AI message", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return message;
    }
    
    /**
     * Lấy tin nhắn gần đây nhất của người dùng gửi
     * 
     * @param userId ID của người dùng
     * @return Tin nhắn gần đây nhất từ người dùng, hoặc null nếu không có
     */
    public ChatMessage getLastUserMessageForUser(int userId) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        ChatMessage message = null;
        
        try {
            db = getDbHelper().getReadableDatabase();
            
            String[] projection = {
                    DatabaseContract.ChatMessagesEntry._ID,
                    DatabaseContract.ChatMessagesEntry.COLUMN_USER_ID,
                    DatabaseContract.ChatMessagesEntry.COLUMN_MESSAGE,
                    DatabaseContract.ChatMessagesEntry.COLUMN_TYPE,
                    DatabaseContract.ChatMessagesEntry.COLUMN_TIMESTAMP,
                    DatabaseContract.ChatMessagesEntry.COLUMN_IMAGE_URL,
                    DatabaseContract.ChatMessagesEntry.COLUMN_CONTENT_TYPE
            };
            
            // Chỉ lấy tin nhắn từ người dùng (TYPE_SENT)
            String selection = DatabaseContract.ChatMessagesEntry.COLUMN_USER_ID + " = ? AND " +
                              DatabaseContract.ChatMessagesEntry.COLUMN_TYPE + " = ?";
            String[] selectionArgs = { 
                String.valueOf(userId), 
                String.valueOf(ChatMessage.TYPE_SENT) 
            };
            
            String sortOrder = DatabaseContract.ChatMessagesEntry.COLUMN_TIMESTAMP + " DESC";
            
            cursor = db.query(
                    DatabaseContract.ChatMessagesEntry.TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder,
                    "1" // Chỉ lấy tin nhắn mới nhất
            );
            
            if (cursor != null && cursor.moveToFirst()) {
                message = createChatMessageFromCursor(cursor);
                Log.d(TAG, "Retrieved last user message for user " + userId + ": " + 
                      (message.isImage() ? "Image" : message.getMessage().substring(0, Math.min(20, message.getMessage().length())) + "..."));
            } else {
                Log.d(TAG, "No user messages found for user " + userId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting last user message", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return message;
    }
} 