package com.yourname.ssm;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.yourname.ssm.repository.LoginUserRepository;

public class LoginActivity extends AppCompatActivity {

    private TextView registerLink;
    private TextView forgotPasswordLink;
    private com.google.android.material.button.MaterialButton loginButton;
    private LoginUserRepository loginUserRepository;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Khởi tạo repository
        loginUserRepository = new LoginUserRepository(this);
        
        // Kiểm tra xem người dùng đã đăng nhập trước đó chưa
        if (loginUserRepository.isLoggedIn()) {
            // Nếu đã đăng nhập, chuyển thẳng đến MainActivity
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;  // Thoát khỏi onCreate để không tiếp tục khởi tạo giao diện
        }
        
        // Thiết lập màu cho status bar - phương pháp đầy đủ
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(getResources().getColor(R.color.galaxy_blue));
        
        // Đặt decorView để status bar hiển thị đúng
        int flags = window.getDecorView().getSystemUiVisibility();
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR; // Đảm bảo icon status bar màu sáng trên nền tối
        window.getDecorView().setSystemUiVisibility(flags);
        
        // Thiết lập background cho status bar
        window.setBackgroundDrawableResource(R.drawable.status_bar_blue);
        
        setContentView(R.layout.activity_login);
        
        // Khởi tạo các control
        registerLink = findViewById(R.id.registerLink);
        forgotPasswordLink = findViewById(R.id.forgotLink);
        loginButton = findViewById(R.id.loginButton);
        
        // Thiết lập sự kiện click cho link đăng ký
        registerLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Chuyển đến màn hình đăng ký
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
        
        // Thiết lập sự kiện click cho link quên mật khẩu
        forgotPasswordLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Chuyển đến màn hình quên mật khẩu
                Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
            }
        });
        
        // Thiết lập sự kiện click cho nút đăng nhập
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Lấy email và password từ input
                String email = "";
                String password = "";
                
                // Tìm EditText và lấy dữ liệu
                com.google.android.material.textfield.TextInputEditText emailEditText = findViewById(R.id.emailEditText);
                com.google.android.material.textfield.TextInputEditText passwordEditText = findViewById(R.id.passwordEditText);
                
                if (emailEditText != null && passwordEditText != null) {
                    email = emailEditText.getText().toString().trim();
                    password = passwordEditText.getText().toString().trim();
                }
                
                // Kiểm tra xem đã nhập đủ thông tin chưa
                if (email.isEmpty() || password.isEmpty()) {
                    // Hiển thị thông báo lỗi
                    android.widget.Toast.makeText(LoginActivity.this, 
                        "Vui lòng nhập email và mật khẩu", 
                        android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                
                try {
                    // Xác thực đăng nhập
                    boolean isAuthenticated = loginUserRepository.authenticateUser(email, password);
                    
                    if (isAuthenticated) {
                        // Đăng nhập thành công
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish(); // Đóng màn hình login
                    } else {
                        // Đăng nhập thất bại
                        android.widget.Toast.makeText(LoginActivity.this, 
                            "Email hoặc mật khẩu không đúng", 
                            android.widget.Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    // Xử lý lỗi
                    android.widget.Toast.makeText(LoginActivity.this, 
                        "Đã xảy ra lỗi: " + e.getMessage(), 
                        android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
} 