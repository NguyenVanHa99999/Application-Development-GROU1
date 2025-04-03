package com.yourname.ssm.api;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.ArrayList;

/**
 * Request for OpenAI GPT completion
 */
public class CompletionRequest {
    
    @SerializedName("model")
    private String model;
    
    @SerializedName("messages")
    private List<Message> messages;
    
    @SerializedName("max_tokens")
    private int maxTokens;
    
    @SerializedName("temperature")
    private double temperature;
    
    /**
     * Create a completion request with a user message
     * @param prompt User's message
     */
    public CompletionRequest(String prompt) {
        this.model = "gpt-4o-mini";
        this.messages = new ArrayList<>();
        this.messages.add(new Message("system", "You are an AI assistant that helps with personal finance, budgeting, saving, and general life questions. You can provide financial advice, explanations, and emotional support. Be concise, friendly, and helpful. Respond in Vietnamese."));
        this.messages.add(new Message("user", prompt));
        this.maxTokens = 500;
        this.temperature = 0.7;
    }
    
    /**
     * Create a completion request with custom history
     * @param messages List of message objects
     */
    public CompletionRequest(List<Message> messages) {
        this.model = "gpt-4o-mini";
        this.messages = messages;
        this.maxTokens = 500;
        this.temperature = 0.7;
    }
    
    /**
     * Message object for the chat completion API
     */
    public static class Message {
        @SerializedName("role")
        private String role;
        
        @SerializedName("content")
        private String content;
        
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
        
        public String getRole() {
            return role;
        }
        
        public String getContent() {
            return content;
        }
    }
} 