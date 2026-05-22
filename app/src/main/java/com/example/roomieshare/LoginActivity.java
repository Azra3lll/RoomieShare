package com.example.roomieshare;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    private EditText emailEditText, passwordEditText;
    private Button loginButton, goToSignUpButton;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailEditText    = findViewById(R.id.editTextEmailLogin);
        passwordEditText = findViewById(R.id.editTextPasswordLogin);
        loginButton      = findViewById(R.id.buttonLogin);
        goToSignUpButton = findViewById(R.id.buttonGoToSignUp);
        progressBar      = findViewById(R.id.progressBarLogin);
        mAuth            = FirebaseAuth.getInstance();

        loginButton.setOnClickListener(v -> loginUser());

        goToSignUpButton.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
            finish();
        });

        // Forgot Password
        findViewById(R.id.buttonForgotPassword).setOnClickListener(v -> showForgotPasswordDialog());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }
    }

    private void showForgotPasswordDialog() {
        // Build a clean input dialog
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_forgot_password, null);

        new AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setView(dialogView)
            .setPositiveButton("Send Reset Email", (dialog, which) -> {
                EditText emailInput = dialogView.findViewById(R.id.forgotPasswordEmail);
                String email = emailInput.getText().toString().trim();
                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
                    return;
                }
                sendPasswordReset(email);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void sendPasswordReset(String email) {
        progressBar.setVisibility(View.VISIBLE);
        mAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener(task -> {
                progressBar.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    new AlertDialog.Builder(this)
                        .setTitle("Email Sent ✅")
                        .setMessage("A password reset link has been sent to:\n\n" + email
                            + "\n\nPlease also check your Spam/Junk folder if you don't see it.")
                        .setPositiveButton("OK", null)
                        .show();
                } else {
                    String msg = getPasswordResetError(task.getException());
                    new AlertDialog.Builder(this)
                        .setTitle("Could Not Send Email")
                        .setMessage(msg)
                        .setPositiveButton("OK", null)
                        .show();
                }
            });
    }

    private String getPasswordResetError(Exception e) {
        if (e == null) return "An unknown error occurred. Please try again.";
        String code = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

        if (code.contains("user-not-found") || code.contains("no user record")) {
            return "No account found with that email address. Please check the email and try again.";
        } else if (code.contains("invalid-email") || code.contains("badly formatted")) {
            return "The email address is not valid. Please enter a correct email.";
        } else if (code.contains("too-many-requests")) {
            return "Too many requests. Please wait a few minutes before trying again.";
        } else if (code.contains("network") || code.contains("unable to resolve")) {
            return "Network error. Please check your internet connection and try again.";
        } else if (code.contains("app-check") || code.contains("appcheck")) {
            return "Security check failed. Please make sure the app is installed correctly.";
        } else {
            return "Failed to send reset email.\n\nError: " + e.getMessage()
                + "\n\nIf this persists, please check that Email/Password sign-in is enabled in your Firebase Console.";
        }
    }

    private void loginUser() {
        String email    = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                progressBar.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    String errorMessage = "Login failed";
                    if (task.getException() != null) {
                        errorMessage += ": " + task.getException().getMessage();
                    }
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            });
    }
}