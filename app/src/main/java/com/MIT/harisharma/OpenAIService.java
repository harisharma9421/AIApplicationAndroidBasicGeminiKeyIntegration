package com.MIT.harisharma;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

// Gemini REST (via REST v1beta generateContent)
public interface OpenAIService {
    @POST("v1beta/models/{model}:generateContent")
    Call<GeminiResponse> generateContent(@retrofit2.http.Path("model") String model, @Body GeminiRequest request, @Query("key") String apiKey);
}

class GeminiRequest {
    public List<GeminiContent> contents;

    public GeminiRequest(List<GeminiContent> contents) {
        this.contents = contents;
    }
}

class GeminiContent {
    public List<GeminiPart> parts;

    public GeminiContent(List<GeminiPart> parts) {
        this.parts = parts;
    }
}

class GeminiPart {
    public String text;

    public GeminiPart(String text) {
        this.text = text;
    }
}

class GeminiResponse {
    public List<Candidate> candidates;

    public static class Candidate {
        public GeminiContent content;
    }
}
