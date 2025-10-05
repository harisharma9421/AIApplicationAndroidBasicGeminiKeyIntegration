package com.MIT.harisharma;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.TimeUnit;

public class ForgotPasswordActivity extends AppCompatActivity {
    private TextInputEditText etPhoneNumber, etOtp, etNewPassword, etConfirmPassword;
    private MaterialButton btnSendOtp, btnVerifyOtp, btnResetPassword;
    private LinearLayout layoutPhoneSection, layoutOtpSection, layoutPasswordSection;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String verificationId;
    private String phoneNumber;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;
    private static final String TAG = "ForgotPassword";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupClickListeners();
        initPhoneAuthCallbacks();
    }

    private void initViews() {
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etOtp = findViewById(R.id.etOtp);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSendOtp = findViewById(R.id.btnSendOtp);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        layoutPhoneSection = findViewById(R.id.layoutPhoneSection);
        layoutOtpSection = findViewById(R.id.layoutOtpSection);
        layoutPasswordSection = findViewById(R.id.layoutPasswordSection);
        progressBar = findViewById(R.id.progressBar);

        // Initially show only phone section
        layoutOtpSection.setVisibility(View.GONE);
        layoutPasswordSection.setVisibility(View.GONE);
    }

    private void setupClickListeners() {
        btnSendOtp.setOnClickListener(v -> sendOtp());
        btnVerifyOtp.setOnClickListener(v -> verifyOtp());
        btnResetPassword.setOnClickListener(v -> resetPassword());

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void sendOtp() {
        phoneNumber = etPhoneNumber.getText().toString().trim();

        if (phoneNumber.isEmpty()) {
            showSnackbar("Phone number is required");
            return;
        }

        // Normalize: allow input with or without +91, ensure E.164
        if (!phoneNumber.startsWith("+")) {
            phoneNumber = "+91" + phoneNumber;
        }

        checkPhoneNumberExists(phoneNumber);
    }

    private void checkPhoneNumberExists(String phoneNumber) {
        showLoading(true);

        db.collection("users")
                .whereEqualTo("phoneNumber", phoneNumber)
                .get()
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        sendVerificationCode(phoneNumber);
                    } else {
                        showSnackbar("Phone number not registered");
                    }
                });
    }

    private void sendVerificationCode(String phoneNumber) {
        showLoading(true);

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(callbacks)
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void initPhoneAuthCallbacks() {
        callbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                showLoading(false);
                layoutOtpSection.setVisibility(View.VISIBLE);
                if (credential.getSmsCode() != null) {
                    etOtp.setText(credential.getSmsCode());
                }
                showSnackbar("OTP verified automatically");
                layoutPasswordSection.setVisibility(View.VISIBLE);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                showLoading(false);
                Log.e(TAG, "onVerificationFailed", e);
                String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                if (msg.toLowerCase().contains("billing")) {
                    showSnackbar("Phone verification requires billing or test numbers. Enable billing or add test phone numbers in Firebase Auth.");
                } else {
                    showSnackbar("Verification failed: " + msg);
                }
            }

            @Override
            public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                showLoading(false);
                ForgotPasswordActivity.this.verificationId = verificationId;
                resendToken = token;
                layoutOtpSection.setVisibility(View.VISIBLE);
                showSnackbar("OTP sent to " + phoneNumber);
            }
        };
    }

    private void verifyOtp() {
        String otp = etOtp.getText().toString().trim();

        if (otp.isEmpty()) {
            showSnackbar("OTP is required");
            return;
        }

        if (verificationId != null) {
            showLoading(true);
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);

            mAuth.signInWithCredential(credential)
                    .addOnCompleteListener(task -> {
                        showLoading(false);
                        if (task.isSuccessful()) {
                            showSnackbar("OTP verified successfully");
                            layoutPasswordSection.setVisibility(View.VISIBLE);
                            // Keep signed in temporarily until reset email is sent
                        } else {
                            Exception ex = task.getException();
                            Log.e(TAG, "verifyOtp failed", ex);
                            String reason = ex != null && ex.getMessage() != null ? ex.getMessage() : "Invalid OTP";
                            showSnackbar("OTP verification failed: " + reason);
                        }
                    });
        }
    }

    private void resetPassword() {
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showSnackbar("Please fill all fields");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showSnackbar("Passwords don't match");
            return;
        }

        if (newPassword.length() < 6) {
            showSnackbar("Password must be at least 6 characters");
            return;
        }

        updatePasswordInDatabase(newPassword);
    }

    private void updatePasswordInDatabase(String newPassword) {
        showLoading(true);

        db.collection("users")
                .whereEqualTo("phoneNumber", phoneNumber)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);
                        String email = document.getString("email");

                        mAuth.sendPasswordResetEmail(email)
                                .addOnCompleteListener(resetTask -> {
                                    showLoading(false);
                                    if (resetTask.isSuccessful()) {
                                        showSnackbar("Password reset email sent to " + email);
                                        // Now that reset email is sent, sign out the temporary phone auth user
                                        mAuth.signOut();
                                        finish();
                                    } else {
                                        Exception ex = resetTask.getException();
                                        Log.e(TAG, "sendPasswordResetEmail failed", ex);
                                        String reason = ex != null && ex.getMessage() != null ? ex.getMessage() : "Failed to send reset email";
                                        showSnackbar(reason);
                                    }
                                });
                    } else {
                        showLoading(false);
                        Exception ex = task.getException();
                        if (ex != null) {
                            Log.e(TAG, "checkPhoneNumberExists failed", ex);
                            showSnackbar("Lookup failed: " + ex.getMessage());
                        } else {
                            showSnackbar("User not found");
                        }
                    }
                })
                .addOnFailureListener(ex -> {
                    showLoading(false);
                    Log.e(TAG, "Firestore query failed", ex);
                    showSnackbar("Lookup error: " + ex.getMessage());
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }
}
