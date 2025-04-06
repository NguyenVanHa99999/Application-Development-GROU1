package com.yourname.ssm.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.yourname.ssm.R;
import com.yourname.ssm.repository.ForgotPasswordRepository;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputLayout emailInputLayout;
    private TextInputEditText emailEditText;
    private Button resetButton;
    private TextView backToLoginLink;
    private ForgotPasswordRepository forgotPasswordRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Initialize views
        emailInputLayout = findViewById(R.id.emailInputLayout);
        emailEditText = findViewById(R.id.emailEditText);
        resetButton = findViewById(R.id.resetButton);
        backToLoginLink = findViewById(R.id.backToLoginLink);

        forgotPasswordRepository = new ForgotPasswordRepository(this);

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailEditText.getText().toString().trim();

                if (email.isEmpty()) {
                    emailInputLayout.setError("Email cannot be empty");
                    return;
                } else {
                    emailInputLayout.setError(null);
                }

                // Disable button to prevent multiple clicks
                resetButton.setEnabled(false);
                resetButton.setText("Processing...");

                // Request password reset
                forgotPasswordRepository.resetPassword(email, new ForgotPasswordRepository.OnResetPasswordListener() {
                    @Override
                    public void onSuccess(String email, String newPassword) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ForgotPasswordActivity.this,
                                        "Password reset email sent to " + email,
                                        Toast.LENGTH_LONG).show();

                                // Re-enable button
                                resetButton.setEnabled(true);
                                resetButton.setText("Reset Password");

                                // Return to login screen after 2 seconds
                                new android.os.Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        finish();
                                    }
                                }, 2000);
                            }
                        });
                    }

                    @Override
                    public void onFailure(String message) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ForgotPasswordActivity.this,
                                        message,
                                        Toast.LENGTH_LONG).show();

                                // Re-enable button
                                resetButton.setEnabled(true);
                                resetButton.setText("Reset Password");
                            }
                        });
                    }
                });
            }
        });

        // Back to login link
        backToLoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
