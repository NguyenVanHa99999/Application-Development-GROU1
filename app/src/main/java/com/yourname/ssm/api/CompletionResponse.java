package com.yourname.ssm.api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Response from OpenAI GPT completion API
 */
public class CompletionResponse {
    
    @SerializedName("id")
    private String id;
    
    @SerializedName("object")
    private String object;
    
    @SerializedName("created")
    private long created;
    
    @SerializedName("model")
    private String model;
    
    @SerializedName("choices")
    private List<Choice> choices;
    
    @SerializedName("usage")
    private Usage usage;
    
    /**
     * Get the first response message content
     * @return Text content of the first response message or empty string if no choices
     */
    public String getFirstMessage() {
        if (choices != null && !choices.isEmpty() && choices.get(0).getMessage() != null) {
            return choices.get(0).getMessage().getContent();
        }
        return "";
    }
    
    public String getId() {
        return id;
    }
    
    public long getCreated() {
        return created;
    }
    
    public List<Choice> getChoices() {
        return choices;
    }
    
    public Usage getUsage() {
        return usage;
    }
    
    /**
     * Choice object from the completion response
     */
    public static class Choice {
        
        @SerializedName("index")
        private int index;
        
        @SerializedName("message")
        private Message message;
        
        @SerializedName("finish_reason")
        private String finishReason;
        
        public int getIndex() {
            return index;
        }
        
        public Message getMessage() {
            return message;
        }
        
        public String getFinishReason() {
            return finishReason;
        }
    }
    
    /**
     * Message object from the completion response
     */
    public static class Message {
        
        @SerializedName("role")
        private String role;
        
        @SerializedName("content")
        private String content;
        
        public String getRole() {
            return role;
        }
        
        public String getContent() {
            return content;
        }
    }
    
    /**
     * Usage statistics from the completion response
     */
    public static class Usage {
        
        @SerializedName("prompt_tokens")
        private int promptTokens;
        
        @SerializedName("completion_tokens")
        private int completionTokens;
        
        @SerializedName("total_tokens")
        private int totalTokens;
        
        public int getPromptTokens() {
            return promptTokens;
        }
        
        public int getCompletionTokens() {
            return completionTokens;
        }
        
        public int getTotalTokens() {
            return totalTokens;
        }
    }
} 