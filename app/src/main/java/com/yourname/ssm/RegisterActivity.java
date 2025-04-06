package com.yourname.ssm;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.yourname.ssm.model.User;
import com.yourname.ssm.repository.LoginUserRepository;

public class RegisterActivity extends AppCompatActivity {

    private TextView backToLoginLink;
    private MaterialButton registerButton;
    private TextInputEditText nameEditText, emailEditText, passwordEditText, confirmPasswordEditText, phoneEditText, addressEditText;
    private TextInputLayout nameLayout, emailLayout, passwordLayout, confirmPasswordLayout, phoneLayout, addressLayout;
    private EditText dobEditText;
    private RadioGroup genderRadioGroup;
    private LoginUserRepository loginUserRepository;

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
        
        setContentView(R.layout.activity_register);
        
        // Khởi tạo repository
        loginUserRepository = new LoginUserRepository(this);
        
        // Tìm và khởi tạo các view
        initializeViews();
        
        // Thiết lập sự kiện click
        backToLoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Tạo intent quay về màn hình đăng nhập
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish(); // Đóng màn hình đăng ký
            }
        });

        // Thiết lập sự kiện click cho nút đăng ký
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateForm()) {
                    registerUser();
                }
            }
        });
    }
    
    private void initializeViews() {
        backToLoginLink = findViewById(R.id.backToLoginLink);
        registerButton = findViewById(R.id.registerButton);
        
        // TextInputEditText
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        addressEditText = findViewById(R.id.addressEditText);
        
        // TextInputLayout for error display
        nameLayout = (TextInputLayout) nameEditText.getParent().getParent();
        emailLayout = (TextInputLayout) emailEditText.getParent().getParent();
        passwordLayout = (TextInputLayout) passwordEditText.getParent().getParent();
        confirmPasswordLayout = (TextInputLayout) confirmPasswordEditText.getParent().getParent();
        phoneLayout = (TextInputLayout) phoneEditText.getParent().getParent();
        addressLayout = (TextInputLayout) addressEditText.getParent().getParent();
        
        // Other views
        dobEditText = findViewById(R.id.dobEditText);
        genderRadioGroup = findViewById(R.id.genderRadioGroup);
    }
    
    private boolean validateForm() {
        boolean isValid = true;
        
        // Reset all error messages
        nameLayout.setError(null);
        emailLayout.setError(null);
        passwordLayout.setError(null);
        confirmPasswordLayout.setError(null);
        phoneLayout.setError(null);
        addressLayout.setError(null);
        
        // Get values
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String dob = dobEditText.getText().toString().trim();
        
        // Validate name
        if (name.isEmpty()) {
            nameLayout.setError("Name is required");
            isValid = false;
        }
        
        // Validate email
        if (email.isEmpty()) {
            emailLayout.setError("Email is required");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Enter a valid email address");
            isValid = false;
        } else if (loginUserRepository.isEmailExists(email)) {
            emailLayout.setError("Email already exists");
            isValid = false;
        }
        
        // Validate password
        if (password.isEmpty()) {
            passwordLayout.setError("Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            passwordLayout.setError("Password must be at least 6 characters");
            isValid = false;
        }
        
        // Validate confirm password
        if (confirmPassword.isEmpty()) {
            confirmPasswordLayout.setError("Confirm password is required");
            isValid = false;
        } else if (!confirmPassword.equals(password)) {
            confirmPasswordLayout.setError("Passwords do not match");
            isValid = false;
        }
        
        // Validate phone (optional but should be valid if provided)
        if (!phone.isEmpty() && !android.util.Patterns.PHONE.matcher(phone).matches()) {
            phoneLayout.setError("Enter a valid phone number");
            isValid = false;
        }
        
        // Validate gender selection
        if (genderRadioGroup.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select your gender", Toast.LENGTH_SHORT).show();
            isValid = false;
        }
        
        // Date of birth (optional validation)
        if (dob.isEmpty()) {
            Toast.makeText(this, "Please enter your date of birth", Toast.LENGTH_SHORT).show();
            isValid = false;
        }
        
        return isValid;
    }
    
    private void registerUser() {
        try {
            // Get form values
            String name = nameEditText.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String phone = phoneEditText.getText().toString().trim();
            String address = addressEditText.getText().toString().trim();
            String dob = dobEditText.getText().toString().trim();
            
            // Get gender
            int genderId = genderRadioGroup.getCheckedRadioButtonId();
            RadioButton selectedGender = findViewById(genderId);
            String gender = selectedGender.getText().toString();
            
            // Create user object
            User user = new User();
            user.setName(name);
            user.setEmail(email);
            user.setPassword(password);
            user.setPhone(phone);
            user.setAddress(address);
            user.setDob(dob);
            user.setGender(gender);
            user.setRole("student"); // Default role
            
            // Register the user
            long userId = loginUserRepository.registerUser(user);
            
            if (userId > 0) {
                Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                
                // Disable the register button to prevent multiple registrations
                registerButton.setEnabled(false);
                
                // Wait for 2 seconds before redirecting to login screen
                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Navigate to login activity
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }, 2000); // 2 seconds delay
            } else {
                Toast.makeText(this, "Đăng ký thất bại. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
} 