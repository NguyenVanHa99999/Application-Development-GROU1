package com.yourname.ssm.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.button.MaterialButton;
import com.yourname.ssm.repository.LoginUserRepository;
import com.yourname.ssm.R;
import com.yourname.ssm.MainActivity;
import com.yourname.ssm.ui.admin.AdminActivity;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    
    private TextInputEditText emailEditText, passwordEditText;
    private TextInputLayout emailInputLayout, passwordInputLayout;
    private MaterialButton loginButton;
    private TextView registerLink, forgotLink;
    private LoginUserRepository loginUserRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_login);
            
            // Initialize UI components
            initializeViews();
            
            // Setup click events
            setupClickListeners();
            
            // Initialize repository
            loginUserRepository = new LoginUserRepository(this);
            
            // Check for auto login
            checkAutoLogin();
        } catch (Exception e) {
            // Handle exceptions
            Toast.makeText(this, "Error initializing application", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            finish();
        }
    }

    private void initializeViews() {
        // Initialize views
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        loginButton = findViewById(R.id.loginButton);
        registerLink = findViewById(R.id.registerLink);
        forgotLink = findViewById(R.id.forgotLink);
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                // Validate input
                if (email.isEmpty()) {
                    emailInputLayout.setError("Email is required");
                    return;
                } else {
                    emailInputLayout.setError(null);
                }

                if (password.isEmpty()) {
                    passwordInputLayout.setError("Password is required");
                    return;
                } else {
                    passwordInputLayout.setError(null);
                }

                // Show processing message
                loginButton.setEnabled(false);
                loginButton.setText("Logging in...");

                try {
                    if (loginUserRepository.authenticateUser(email, password)) {
                        Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();

                        // Get role for navigation
                        final String role = loginUserRepository.getRole();
                        Log.d(TAG, "User authenticated - Role: " + role);
                        
                        // Delay the transition to proper activity based on role
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                navigateBasedOnRole(role);
                            }
                        }, 1000);  // 1 second delay
                    } else {
                        Toast.makeText(LoginActivity.this, "Invalid email or password", Toast.LENGTH_SHORT).show();
                        loginButton.setEnabled(true);
                        loginButton.setText("Login");
                    }
                } catch (Exception e) {
                    Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Login error: " + e.getMessage(), e);
                    loginButton.setEnabled(true);
                    loginButton.setText("Login");
                }
            }
        });

        // Handle registration link click
        registerLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        // Handle forgot password link click
        forgotLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
            }
        });
    }

    private void checkAutoLogin() {
        // Check if already logged in and navigate to appropriate activity
        if (loginUserRepository.isLoggedIn()) {
            String role = loginUserRepository.getRole();
            Log.d(TAG, "Auto login - User role: " + role);
            navigateBasedOnRole(role);
        }
    }
    
    private void navigateBasedOnRole(String role) {
        // Force clear any login state to ensure fresh values
        try {
            // First, forcefully check if email is admin@example.com
            String email = loginUserRepository.getEmail();
            boolean isAdminEmail = "admin@example.com".equals(email);
            
            // Double check the role directly from repository and force set if needed
            role = loginUserRepository.getRole();
            int userId = loginUserRepository.getUserId();
            String userType = loginUserRepository.getSharedPreferences().getString("user_type", "");
            
            Log.d(TAG, "⚠️ NAVIGATION CHECK - email=" + email + ", role=" + role + ", userId=" + userId + ", userType=" + userType);
            
            // CRITICAL FIX: If email is admin@example.com, ALWAYS set role to "1" and userType to "ADMIN"
            if (isAdminEmail) {
                Log.d(TAG, "⚠️ FORCING ADMIN ROLE for admin@example.com");
                loginUserRepository.getSharedPreferences().edit()
                    .putString("role", "1")
                    .putString("user_type", "ADMIN")
                    .commit();
                
                role = "1";
                userType = "ADMIN";
            }
            
            // Log admin check details for debugging
            Log.d(TAG, "⚠️ Admin check: isAdminEmail=" + isAdminEmail + ", role after check=" + role);
            
            // Force clear any cached activities before starting new ones
            if ("1".equals(role) || "ADMIN".equals(userType) || isAdminEmail) {
                // ADMIN USER FLOW - Exit if we're not admin to prevent wrong navigation
                if (!"1".equals(role)) {
                    Log.d(TAG, "⚠️ FIXING role from " + role + " to 1 for admin");
                    loginUserRepository.getSharedPreferences().edit()
                        .putString("role", "1")
                        .putString("user_type", "ADMIN")
                        .commit();
                    role = "1";
                }
                
                Log.d(TAG, "⚠️ ADMIN USER CONFIRMED - Navigating to AdminActivity");
                
                try {
                    Log.d(TAG, "⚠️ Starting AdminActivity...");
                    Intent intent = new Intent(LoginActivity.this, AdminActivity.class);
                    // Clear activity stack to prevent back navigation issues
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    Log.d(TAG, "⚠️ AdminActivity started");
                    finish();
                    return;
                } catch (Exception e) {
                    Log.e(TAG, "⚠️ Error starting AdminActivity: " + e.getMessage(), e);
                    Toast.makeText(this, "Error loading admin panel", Toast.LENGTH_SHORT).show();
                }
            } else {
                // REGULAR USER FLOW - Make sure we're really not admin
                // CRITICAL SAFETY CHECK: Make absolutely sure we don't navigate admin to MainActivity
                if (isAdminEmail || "1".equals(role) || "ADMIN".equals(userType)) {
                    Log.d(TAG, "⚠️ CRITICAL SECURITY ISSUE - Admin detected but about to navigate to MainActivity");
                    Log.d(TAG, "⚠️ FIXING by redirecting to AdminActivity");
                    
                    // Fix role and navigate to admin
                    loginUserRepository.getSharedPreferences().edit()
                        .putString("role", "1")
                        .putString("user_type", "ADMIN")
                        .commit();
                    
                    Intent intent = new Intent(LoginActivity.this, AdminActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                    return;
                }
                
                Log.d(TAG, "⚠️ REGULAR USER CONFIRMED - Navigating to MainActivity");
                
                try {
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    Log.d(TAG, "⚠️ MainActivity started");
                    finish();
                } catch (Exception e) {
                    Log.e(TAG, "⚠️ Error starting MainActivity: " + e.getMessage(), e);
                    Toast.makeText(this, "Error loading main app", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "⚠️ CRITICAL ERROR in navigation: " + e.getMessage(), e);
            Toast.makeText(this, "Navigation error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
