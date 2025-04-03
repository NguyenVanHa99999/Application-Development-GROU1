package com.yourname.ssm.api;

import android.util.Log;

import com.yourname.ssm.BuildConfig;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Client for API services
 */
public class OpenAIClient {
    private static final String TAG = "AIClient";
    private static final String OPENAI_BASE_URL = "https://api.openai.com/";
    
    // API Key
    private static final String OPENAI_API_KEY = BuildConfig.OPENAI_API_KEY;
    
    private final OpenAIService openAIService;
    private final OkHttpClient httpClient;
    
    private static OpenAIClient instance;
    
    private OpenAIClient() {
        // Create logging interceptor for debugging
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        // Create HTTP client with interceptor
        httpClient = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();
        
        // Create Retrofit instance for OpenAI
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(OPENAI_BASE_URL)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        
        // Create service
        openAIService = retrofit.create(OpenAIService.class);
    }
    
    /**
     * Get singleton instance
     * @return OpenAIClient instance
     */
    public static synchronized OpenAIClient getInstance() {
        if (instance == null) {
            instance = new OpenAIClient();
        }
        return instance;
    }
    
    /**
     * Generate text completion from a prompt
     * @param prompt User's message
     * @param callback Callback for result
     */
    public void generateCompletion(String prompt, final CompletionCallback callback) {
        CompletionRequest request = new CompletionRequest(prompt);
        
        openAIService.generateCompletion("Bearer " + OPENAI_API_KEY, request).enqueue(new Callback<CompletionResponse>() {
            @Override
            public void onResponse(Call<CompletionResponse> call, Response<CompletionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String message = response.body().getFirstMessage();
                    if (message != null && !message.isEmpty()) {
                        callback.onSuccess(message);
                    } else {
                        callback.onFailure("No message returned");
                    }
                } else {
                    callback.onFailure("API error: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<CompletionResponse> call, Throwable t) {
                Log.e(TAG, "Error generating completion", t);
                callback.onFailure(t.getMessage());
            }
        });
    }
    
    /**
     * Callback for text completion
     */
    public interface CompletionCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }
} 