package com.yourname.ssm.ui.chat;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.yourname.ssm.model.ChatMessage;
import com.yourname.ssm.repository.ChatRepository;
import com.yourname.ssm.repository.ChatRepository.ChatCallback;
import com.yourname.ssm.repository.LoginUserRepository;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ChatViewModel extends AndroidViewModel {
    private static final String TAG = "ChatViewModel";
    
    private final MutableLiveData<List<ChatMessage>> messagesLiveData;
    private final ChatRepository chatRepository;
    private final LoginUserRepository loginUserRepository;
    private final int userId;
    
    public ChatViewModel(@NonNull Application application) {
        super(application);
        messagesLiveData = new MutableLiveData<>(new ArrayList<>());
        chatRepository = new ChatRepository(application);
        loginUserRepository = new LoginUserRepository(application);
        userId = loginUserRepository.getUserId();
        
        Log.d(TAG, "ChatViewModel initialized with userId: " + userId);
        
        // Tải lịch sử chat khi khởi tạo ViewModel
        loadChatHistory();
    }
    
    /**
     * Tải lịch sử tin nhắn của người dùng
     */
    public void loadChatHistory() {
        try {
            Log.d(TAG, "loadChatHistory: Loading chat history for user " + userId);
            
            // Lấy tin nhắn từ repository
            List<ChatMessage> messages = chatRepository.getMessageHistory(userId);
            
            // Kiểm tra xem đã chào hỏi chưa
            boolean hasGreeted = false;
            if (!messages.isEmpty()) {
                for (ChatMessage message : messages) {
                    if (message.getType() == ChatMessage.TYPE_RECEIVED && 
                        message.getMessage() != null && 
                        (message.getMessage().contains("Chào bạn!") || 
                         message.getMessage().contains("Hôm nay bạn cảm thấy thế nào?"))) {
                        hasGreeted = true;
                        break;
                    }
                }
            }
            
            // Cập nhật LiveData với tin nhắn từ lịch sử
            messagesLiveData.setValue(messages);
            
            // Nếu không có tin nhắn nào, gửi lời chào ban đầu
            if (messages.isEmpty()) {
                getInitialGreeting();
            } 
            // Nếu chưa có lời chào, gửi lời chào
            else if (!hasGreeted) {
                getInitialGreeting();
            }
            // Không gửi tin nhắn nếu đã có lịch sử và đã chào hỏi
        } catch (Exception e) {
            Log.e(TAG, "loadChatHistory: Error loading chat history", e);
        }
    }
    
    /**
     * Thêm tin nhắn vào danh sách và cập nhật LiveData
     */
    private void addMessageToList(ChatMessage message) {
        List<ChatMessage> currentMessages = messagesLiveData.getValue();
        if (currentMessages == null) {
            currentMessages = new ArrayList<>();
        }
        currentMessages.add(message);
        messagesLiveData.setValue(new ArrayList<>(currentMessages));
    }
    
    /**
     * Gửi tin nhắn của người dùng và nhận phản hồi từ AI
     * @param message Nội dung tin nhắn
     */
    public void sendMessage(String message) {
        try {
            Log.d(TAG, "sendMessage: Sending message: " + message);
            
            // Tạo tin nhắn mới
            ChatMessage userMessage = new ChatMessage(message, ChatMessage.TYPE_SENT, userId);
            
            // Lưu tin nhắn vào cơ sở dữ liệu
            chatRepository.saveUserMessage(userMessage);
            
            // Cập nhật LiveData
            addMessageToList(userMessage);
            
            // Lấy phản hồi từ AI
            chatRepository.getReplyToMessage(message, userId, this::addMessageToList);
        } catch (Exception e) {
            Log.e(TAG, "sendMessage: Error sending message", e);
        }
    }
    
    /**
     * Gửi tin nhắn hình ảnh và nhận phản hồi từ AI
     * @param base64Image Chuỗi base64 của hình ảnh
     * @param imagePath Đường dẫn local của hình ảnh
     * @param caption Chú thích (nếu có)
     */
    public void sendImageMessage(String base64Image, String imagePath, String caption) {
        try {
            Log.d(TAG, "sendImageMessage: Sending image with caption: " + caption);
            
            // Tạo đường dẫn file URI
            String fileUri = "file://" + imagePath;
            
            // Lưu tin nhắn hình ảnh vào cơ sở dữ liệu và cập nhật LiveData
            ChatMessage imageMessage = chatRepository.saveUserImageMessage(fileUri, caption, userId);
            addMessageToList(imageMessage);
            
            // Gửi hình ảnh đến AI và nhận phản hồi
            chatRepository.processImageMessage(base64Image, caption, userId, this::addMessageToList);
            
        } catch (Exception e) {
            Log.e(TAG, "sendImageMessage: Error sending image message", e);
        }
    }
    
    /**
     * Lấy lời chào ban đầu từ AI
     */
    private void getInitialGreeting() {
        try {
            Log.d(TAG, "getInitialGreeting: Getting initial greeting");
            
            chatRepository.getInitialGreeting(userId, this::addMessageToList);
        } catch (Exception e) {
            Log.e(TAG, "getInitialGreeting: Error getting initial greeting", e);
        }
    }
    
    /**
     * Xóa tất cả tin nhắn trong lịch sử chat
     */
    public void clearChatHistory() {
        try {
            Log.d(TAG, "clearChatHistory: Clearing chat history for user " + userId);
            
            int count = chatRepository.clearChatHistory(userId);
            Log.d(TAG, "clearChatHistory: Deleted " + count + " messages");
            
            // Cập nhật LiveData
            messagesLiveData.setValue(new ArrayList<>());
            
            // Gửi lời chào mới
            getInitialGreeting();
        } catch (Exception e) {
            Log.e(TAG, "clearChatHistory: Error clearing chat history", e);
        }
    }
    
    /**
     * Lấy LiveData chứa danh sách tin nhắn
     * @return LiveData của danh sách tin nhắn
     */
    public LiveData<List<ChatMessage>> getMessages() {
        return messagesLiveData;
    }
    
    @Override
    protected void onCleared() {
        // Dọn dẹp tài nguyên nếu cần khi ViewModel bị hủy
        super.onCleared();
    }
} 