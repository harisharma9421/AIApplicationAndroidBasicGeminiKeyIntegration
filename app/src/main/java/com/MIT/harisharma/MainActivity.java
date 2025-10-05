package com.MIT.harisharma;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.util.Log;
import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private EditText etMessage;
    private MaterialButton btnSend;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private OpenAIService openAIService;

    private static final String GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            redirectToLogin();
            return;
        }

        initViews();
        setupRecyclerView();
        setupOpenAI();
        setupClickListeners();
        addWelcomeMessage();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        progressBar = findViewById(R.id.progressBar);

        // Setup MaterialToolbar - FIX 1: Use MaterialToolbar instead of Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // FIX 2: Add null check before setting title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("AI Language Translator");
        }
    }

    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);
    }

    private void setupOpenAI() {
        if (GEMINI_API_KEY == null || GEMINI_API_KEY.trim().isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("API key missing")
                    .setMessage("No Gemini API key found. Please add GEMINI_API_KEY to local.properties and rebuild.")
                    .setPositiveButton("OK", null)
                    .show();
            btnSend.setEnabled(false);
            return;
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://generativelanguage.googleapis.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(new OkHttpClient.Builder()
                        .addInterceptor(new HttpLoggingInterceptor()
                                .setLevel(HttpLoggingInterceptor.Level.BODY))
                        // Gemini uses API key as query param; no auth header required
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(90, TimeUnit.SECONDS)
                        .writeTimeout(60, TimeUnit.SECONDS)
                        .callTimeout(120, TimeUnit.SECONDS)
                        .build())
                .build();

        openAIService = retrofit.create(OpenAIService.class);
    }

    private void setupClickListeners() {
        btnSend.setOnClickListener(v -> sendMessage());

        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void addWelcomeMessage() {
        ChatMessage welcome = new ChatMessage(
                "Hello! I'm your AI Language Translator. I can translate text and provide grammar explanations. Just type your message!",
                false,
                System.currentTimeMillis()
        );
        messageList.add(welcome);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
    }

    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();

        if (messageText.isEmpty()) {
            return;
        }

        // Add user message
        ChatMessage userMessage = new ChatMessage(messageText, true, System.currentTimeMillis());
        messageList.add(userMessage);
        chatAdapter.notifyItemInserted(messageList.size() - 1);

        etMessage.setText("");
        scrollToBottom();

        // Show typing indicator
        showLoading(true);

        // Send to OpenAI
        sendToOpenAI(messageText);
    }

    private void sendToOpenAI(String message) {
        // Build Gemini request encouraging multilingual responses and explanations
        List<GeminiPart> parts = new ArrayList<>();
        parts.add(new GeminiPart("You are a multilingual assistant. Always answer in the user's language. " +
                "If asked for translation, provide the translation and a short grammar explanation." +
                "Also dont give huge responses if asked for translation."));
        parts.add(new GeminiPart(message));

        List<GeminiContent> contents = new ArrayList<>();
        contents.add(new GeminiContent(parts));

        GeminiRequest request = new GeminiRequest(contents);

        String model = BuildConfig.GEMINI_MODEL != null && !BuildConfig.GEMINI_MODEL.trim().isEmpty()
                ? BuildConfig.GEMINI_MODEL
                : "gemini-pro";

        openAIService.generateContent(model, request, GEMINI_API_KEY)
                .enqueue(new Callback<GeminiResponse>() {
                    @Override
                    public void onResponse(Call<GeminiResponse> call, Response<GeminiResponse> response) {
                        showLoading(false);
                        if (response.isSuccessful() && response.body() != null &&
                                response.body().candidates != null && !response.body().candidates.isEmpty() &&
                                response.body().candidates.get(0).content != null &&
                                response.body().candidates.get(0).content.parts != null &&
                                !response.body().candidates.get(0).content.parts.isEmpty() &&
                                response.body().candidates.get(0).content.parts.get(0).text != null) {
                            String aiResponse = response.body().candidates.get(0).content.parts.get(0).text;
                            addAIMessage(aiResponse.trim());
                        } else {
                            String errorBody = "";
                            try {
                                if (response.errorBody() != null) {
                                    errorBody = response.errorBody().string();
                                }
                            } catch (Exception ignored) {}
                            Log.e(TAG, "Gemini API error: code=" + response.code() + ", body=" + errorBody);
                            addAIMessage("Sorry, I'm having trouble processing your request right now.");
                        }
                    }

                    @Override
                    public void onFailure(Call<GeminiResponse> call, Throwable t) {
                        showLoading(false);
                        Log.e(TAG, "Gemini API failure", t);
                        addAIMessage("Sorry, there was an error connecting to the AI service.");
                    }
                });
    }

    private void addAIMessage(String message) {
        runOnUiThread(() -> {
            ChatMessage aiMessage = new ChatMessage(message, false, System.currentTimeMillis());
            messageList.add(aiMessage);
            chatAdapter.notifyItemInserted(messageList.size() - 1);
            scrollToBottom();
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSend.setEnabled(!show);
    }

    private void scrollToBottom() {
        if (messageList.size() > 0) {
            recyclerView.smoothScrollToPosition(messageList.size() - 1);
        }
    }

    private void redirectToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            showLogoutDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    mAuth.signOut();
                    redirectToLogin();
                })
                .setNegativeButton("No", null)
                .show();
    }
}
