package com.yourname.ssm;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.yourname.ssm.repository.ForgotPasswordRepository;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextView backToLoginLink;
    private MaterialButton resetButton;
    private TextInputEditText emailEditText;
    private TextInputLayout emailInputLayout;
    private ForgotPasswordRepository forgotPasswordRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Thiết lập màu cho status bar
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(getResources().getColor(R.color.galaxy_blue));
        
        // Đặt decorView để status bar hiển thị đúng
        int flags = window.getDecorView().getSystemUiVisibility();
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR; // Đảm bảo icon status bar màu sáng trên nền tối
        window.getDecorView().setSystemUiVisibility(flags);
        
        setContentView(R.layout.activity_forgot_password);
        
        // Khởi tạo repository
        forgotPasswordRepository = new ForgotPasswordRepository(this);
        
        // Tìm các view
        backToLoginLink = findViewById(R.id.backToLoginLink);
        resetButton = findViewById(R.id.resetButton);
        emailEditText = findViewById(R.id.emailEditText);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        
        // Thiết lập sự kiện click
        backToLoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Tạo intent quay về màn hình đăng nhập
                Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
                startActivity(intent);
                finish(); // Đóng màn hình quên mật khẩu
            }
        });

        // Thiết lập sự kiện click cho nút reset password
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetPassword();
            }
        });
    }
    
    private void resetPassword() {
        // Xóa thông báo lỗi cũ
        emailInputLayout.setError(null);
        
        // Lấy email từ text input
        String email = emailEditText.getText().toString().trim();
        
        // Kiểm tra email có hợp lệ không
        if (email.isEmpty()) {
            emailInputLayout.setError("Email is required");
            return;
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError("Please enter a valid email address");
            return;
        }
        
        // Hiển thị thông báo đang xử lý
        resetButton.setEnabled(false);
        Toast.makeText(this, "Processing your request...", Toast.LENGTH_SHORT).show();
        
        // Gọi repository để reset password
        forgotPasswordRepository.resetPassword(email, new ForgotPasswordRepository.OnResetPasswordListener() {
            @Override
            public void onSuccess(String email, String newPassword) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        resetButton.setEnabled(true);
                        Toast.makeText(ForgotPasswordActivity.this, 
                            "Password reset email sent successfully!", Toast.LENGTH_LONG).show();
                        
                        // Delay chuyển về màn hình login
                        new android.os.Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        }, 2000); // 2 giây delay
                    }
                });
            }
            
            @Override
            public void onFailure(String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        resetButton.setEnabled(true);
                        Toast.makeText(ForgotPasswordActivity.this, 
                            "Failed to reset password: " + message, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
} 