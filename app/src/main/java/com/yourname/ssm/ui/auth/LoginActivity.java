package com.yourname.ssm.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
            
            // Khởi tạo các thành phần UI
            initializeViews();
            
            // Thiết lập sự kiện click
            setupClickListeners();
            
            // Khởi tạo repository
            loginUserRepository = new LoginUserRepository(this);
            
            // Kiểm tra đăng nhập tự động
            checkAutoLogin();
        } catch (Exception e) {
            // Xử lý ngoại lệ nếu có
            Toast.makeText(this, "Có lỗi xảy ra khi khởi động ứng dụng", Toast.LENGTH_SHORT).show();
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
                    emailInputLayout.setError("Email không được để trống");
                    return;
                } else {
                    emailInputLayout.setError(null);
                }

                if (password.isEmpty()) {
                    passwordInputLayout.setError("Mật khẩu không được để trống");
                    return;
                } else {
                    passwordInputLayout.setError(null);
                }

                // Hiển thị thông báo đang xử lý
                loginButton.setEnabled(false);
                loginButton.setText("Đang đăng nhập...");

                try {
                    if (loginUserRepository.authenticateUser(email, password)) {
                        Toast.makeText(LoginActivity.this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();

                        // Delay the transition to proper activity based on role
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                // Lấy vai trò để chuyển hướng
                                String role = loginUserRepository.getRole();
                                int userId = loginUserRepository.getUserId();
                                
                                Intent intent;
                                if ("1".equals(role)) {
                                    // Admin role
                                    intent = new Intent(LoginActivity.this, AdminActivity.class);
                                } else {
                                    // Student or other roles
                                    intent = new Intent(LoginActivity.this, MainActivity.class);
                                }
                                
                                // Pass user ID to the next activity if needed
                                intent.putExtra("USER_ID", userId);
                                intent.putExtra("USER_ROLE", role);
                                
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            }
                        }, 1000);  // 1 second delay
                    } else {
                        Toast.makeText(LoginActivity.this, "Email hoặc mật khẩu không đúng", Toast.LENGTH_SHORT).show();
                        loginButton.setEnabled(true);
                        loginButton.setText("Đăng nhập");
                    }
                } catch (Exception e) {
                    Toast.makeText(LoginActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    loginButton.setEnabled(true);
                    loginButton.setText("Đăng nhập");
                }
            }
        });

        // Xử lý sự kiện click vào link đăng ký
        registerLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        // Xử lý sự kiện click vào link quên mật khẩu
        forgotLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
            }
        });
    }

    private void checkAutoLogin() {
        // Kiểm tra nếu đã đăng nhập thì chuyển thẳng vào activity tương ứng với vai trò
        if (loginUserRepository.isLoggedIn()) {
            String role = loginUserRepository.getRole();
            int userId = loginUserRepository.getUserId();
            
            Intent intent;
            if ("1".equals(role)) {
                // Admin role
                intent = new Intent(LoginActivity.this, AdminActivity.class);
            } else {
                // Student or other roles
                intent = new Intent(LoginActivity.this, MainActivity.class);
            }
            
            // Pass user ID to the next activity if needed
            intent.putExtra("USER_ID", userId);
            intent.putExtra("USER_ROLE", role);
            
            startActivity(intent);
            finish();
        }
    }
}
