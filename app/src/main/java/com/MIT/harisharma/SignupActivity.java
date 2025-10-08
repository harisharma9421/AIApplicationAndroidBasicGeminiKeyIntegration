package com.MIT.harisharma;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {
    private TextInputEditText etFirstName, etLastName, etEmail, etPhoneNumber, etPassword, etConfirmPassword;
    private Spinner spinnerGender;
    private MaterialButton btnSignup;
    private TextView tvLogin;
    private ProgressBar progressBar;
    private com.google.android.gms.common.SignInButton btnGoogleSignUp;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient googleSignInClient;

    private static final int RC_GOOGLE_SIGN_UP = 2001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupGenderSpinner();
        initGoogleSignIn();
        setupClickListeners();
    }

    private void initViews() {
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        spinnerGender = findViewById(R.id.spinnerGender);
        btnSignup = findViewById(R.id.btnSignup);
        tvLogin = findViewById(R.id.tvLogin);
        progressBar = findViewById(R.id.progressBar);
        btnGoogleSignUp = findViewById(R.id.btnGoogleSignUp);
    }

    private void setupGenderSpinner() {
        String[] genders = {"Select Gender", "Male", "Female", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, genders);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnSignup.setOnClickListener(v -> validateAndSignup());
        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
        btnGoogleSignUp.setOnClickListener(v -> startGoogleSignUp());
    }

    private void initGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void startGoogleSignUp() {
        showLoading(true);
        // Force the Google account chooser to appear by clearing any cached account
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent intent = googleSignInClient.getSignInIntent();
            startActivityForResult(intent, RC_GOOGLE_SIGN_UP);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_GOOGLE_SIGN_UP) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken(), account);
            } catch (ApiException e) {
                showLoading(false);
                showSnackbar("Google sign in failed: " + e.getMessage());
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken, GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Ensure user document exists; create minimal record if missing
                            Map<String, Object> userDoc = new HashMap<>();
                            userDoc.put("userId", user.getUid());
                            userDoc.put("firstName", acct.getGivenName() != null ? acct.getGivenName() : "");
                            userDoc.put("lastName", acct.getFamilyName() != null ? acct.getFamilyName() : "");
                            userDoc.put("email", user.getEmail());
                            userDoc.put("phoneNumber", "");
                            userDoc.put("gender", "");

                            db.collection("users").document(user.getUid())
                                    .set(userDoc)
                                    .addOnSuccessListener(aVoid -> {
                                        showLoading(false);
                                        startActivity(new Intent(this, MainActivity.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        showLoading(false);
                                        showSnackbar("Failed to save user data");
                                    });
                        } else {
                            showLoading(false);
                            showSnackbar("Authentication succeeded, but user is null");
                        }
                    } else {
                        showLoading(false);
                        showSnackbar("Authentication Failed: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    private void validateAndSignup() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phoneNumber = etPhoneNumber.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();
        String gender = spinnerGender.getSelectedItem().toString();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() ||
                phoneNumber.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showSnackbar("Please fill all fields");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showSnackbar("Passwords don't match");
            return;
        }

        if (password.length() < 6) {
            showSnackbar("Password must be at least 6 characters");
            return;
        }

        if (gender.equals("Select Gender")) {
            showSnackbar("Please select gender");
            return;
        }

        // Normalize phone to E.164 so Forgot Password lookups match (expects +91...)
        if (!phoneNumber.startsWith("+")) {
            phoneNumber = "+91" + phoneNumber;
        }

        createUserAccount(firstName, lastName, email, phoneNumber, gender, password);
    }

    private void createUserAccount(String firstName, String lastName, String email,
                                   String phoneNumber, String gender, String password) {
        showLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToFirestore(user.getUid(), firstName, lastName,
                                    email, phoneNumber, gender);
                        }
                    } else {
                        showLoading(false);
                        showSnackbar("Registration failed: " + task.getException().getMessage());
                    }
                });
    }

    private void saveUserToFirestore(String userId, String firstName, String lastName,
                                     String email, String phoneNumber, String gender) {
        Map<String, Object> user = new HashMap<>();
        user.put("userId", userId);
        user.put("firstName", firstName);
        user.put("lastName", lastName);
        user.put("email", email);
        user.put("phoneNumber", phoneNumber);
        user.put("gender", gender);

        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    showSnackbar("Account created successfully!");
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    showSnackbar("Failed to save user data");
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSignup.setEnabled(!show);
    }

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }
}
