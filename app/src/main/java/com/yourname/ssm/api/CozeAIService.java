package com.yourname.ssm.api;

import android.util.Log;

import com.yourname.ssm.model.ChatMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Service class for interacting with Coze AI API
 */
public class CozeAIService {
    private static final String TAG = "CozeAIService";
    private static final String COZE_BASE_URL = "https://api.coze.com/v1";
    private static final String API_KEY = "pat_al57deXqzV6njpL6yPtSQck5ZI35dTTWkKnzO9fEPIIR1j3zsfJerSN57fKXocey";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    
    // OkHttp client with longer timeouts for API calls
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    
    public interface CozeAICallback {
        void onSuccess(String message);
        void onFailure(String errorMessage);
    }
    
    /**
     * Send a message to Coze AI and get a response
     * @param prompt The user message
     * @param messageHistory Previous messages for context (optional)
     * @param callback Callback to handle the response
     */
    public static void sendMessage(String prompt, JSONArray messageHistory, CozeAICallback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            
            // Format the messages according to Coze API requirements
            JSONArray messages = new JSONArray();
            
            // Add message history if available
            if (messageHistory != null && messageHistory.length() > 0) {
                for (int i = 0; i < messageHistory.length(); i++) {
                    messages.put(messageHistory.getJSONObject(i));
                }
            }
            
            // Add current user message
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.put(userMessage);
            
            // Add required parameters
            requestBody.put("messages", messages);
            requestBody.put("model", "gpt-3.5-turbo");
            requestBody.put("max_tokens", 1000);
            requestBody.put("temperature", 0.7);
            
            // Create request
            Request request = new Request.Builder()
                    .url(COZE_BASE_URL + "/chat/completions")
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody.toString(), JSON_MEDIA_TYPE))
                    .build();
            
            // Execute request asynchronously
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "API call failed: " + e.getMessage(), e);
                    callback.onFailure("Failed to connect to AI service: " + e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (!response.isSuccessful()) {
                            callback.onFailure("API error: " + response.code() + " " + response.message());
                            return;
                        }
                        
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        
                        // Extract AI response
                        String aiMessage = extractMessageFromResponse(jsonResponse);
                        callback.onSuccess(aiMessage);
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing response: " + e.getMessage(), e);
                        callback.onFailure("Error processing AI response: " + e.getMessage());
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error creating request body: " + e.getMessage(), e);
            callback.onFailure("Error preparing AI request: " + e.getMessage());
        }
    }
    
    /**
     * Send an image to Coze AI for analysis
     * @param base64Image Base64-encoded image data
     * @param prompt Additional text prompt
     * @param callback Callback to handle the response
     */
    public static void sendImageMessage(String base64Image, String prompt, CozeAICallback callback) {
        try {
            JSONObject requestBody = new JSONObject();
            
            // Format messages according to Coze API
            JSONArray messages = new JSONArray();
            
            // Create a message with text and image content
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            
            // Create content array that includes both text and image
            JSONArray contentArray = new JSONArray();
            
            // Add text content
            JSONObject textContent = new JSONObject();
            textContent.put("type", "text");
            textContent.put("text", prompt);
            contentArray.put(textContent);
            
            // Add image content
            JSONObject imageContent = new JSONObject();
            imageContent.put("type", "image_url");
            
            JSONObject imageUrl = new JSONObject();
            imageUrl.put("url", "data:image/jpeg;base64," + base64Image);
            
            imageContent.put("image_url", imageUrl);
            contentArray.put(imageContent);
            
            // Add content array to user message
            userMessage.put("content", contentArray);
            messages.put(userMessage);
            
            // Add required parameters
            requestBody.put("messages", messages);
            requestBody.put("model", "gpt-4-vision-preview");
            requestBody.put("max_tokens", 1000);
            
            // Create request
            Request request = new Request.Builder()
                    .url(COZE_BASE_URL + "/chat/completions")
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody.toString(), JSON_MEDIA_TYPE))
                    .build();
            
            // Log the actual API request for debugging
            Log.d(TAG, "Image API request: " + requestBody.toString());
            
            // Execute request asynchronously
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Image API call failed: " + e.getMessage(), e);
                    callback.onFailure("Failed to send image to AI service: " + e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (!response.isSuccessful()) {
                            String errorResponse = response.body().string();
                            Log.e(TAG, "API error response: " + errorResponse);
                            callback.onFailure("API error: " + response.code() + " " + response.message());
                            return;
                        }
                        
                        String responseBody = response.body().string();
                        Log.d(TAG, "API response: " + responseBody);
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        
                        // Extract AI response
                        String aiMessage = extractMessageFromResponse(jsonResponse);
                        callback.onSuccess(aiMessage);
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing image response: " + e.getMessage(), e);
                        callback.onFailure("Error processing AI image response: " + e.getMessage());
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error creating image request: " + e.getMessage(), e);
            callback.onFailure("Error preparing AI image request: " + e.getMessage());
        }
    }
    
    /**
     * Helper method to extract message from Coze API response
     */
    private static String extractMessageFromResponse(JSONObject response) throws JSONException {
        try {
            // Parse based on Coze API response format
            if (response.has("choices")) {
                JSONArray choices = response.getJSONArray("choices");
                if (choices.length() > 0) {
                    JSONObject firstChoice = choices.getJSONObject(0);
                    if (firstChoice.has("message")) {
                        JSONObject message = firstChoice.getJSONObject("message");
                        if (message.has("content")) {
                            return message.getString("content");
                        }
                    }
                }
            }
            
            // Fallback to returning the entire response as string if structure doesn't match
            return "AI couldn't process your request. Please try again later.";
        } catch (Exception e) {
            Log.e(TAG, "Error extracting message: " + e.getMessage(), e);
            return "Error processing AI response: " + e.getMessage();
        }
    }
} 