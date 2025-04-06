package com.yourname.ssm.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * Retrofit service interface for OpenAI API
 */
public interface OpenAIService {
    
    /**
     * Generate a text completion using GPT models
     * @param authHeader OpenAI API key in format "Bearer YOUR_API_KEY"
     * @param request Request body containing prompt and model parameters
     * @return Response from OpenAI API
     */
    @POST("v1/chat/completions")
    Call<CompletionResponse> generateCompletion(
            @Header("Authorization") String authHeader,
            @Body CompletionRequest request
    );
} 