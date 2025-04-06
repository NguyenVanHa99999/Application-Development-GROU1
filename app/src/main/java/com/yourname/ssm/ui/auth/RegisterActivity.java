package com.yourname.ssm.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.yourname.ssm.databinding.ActivityRegisterBinding;
import com.yourname.ssm.model.User;
import com.yourname.ssm.repository.UserRepository;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private UserRepository userRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        userRepo = new UserRepository(this);

        binding.registerButton.setOnClickListener(v -> {
            String name = binding.nameEditText.getText().toString().trim();
            String email = binding.emailEditText.getText().toString().trim();
            String pass = binding.passwordEditText.getText().toString().trim();
            String confirm = binding.confirmPasswordEditText.getText().toString().trim();
            String phone = binding.phoneEditText.getText().toString().trim();
            String address = binding.addressEditText.getText().toString().trim();
            String dob = binding.dobEditText.getText().toString().trim();
            String gender = binding.radioMale.isChecked() ? "Male" :
                    binding.radioFemale.isChecked() ? "Female" : "";
            
            // Mặc định là student (roleId = 2)
            int roleId = 2;

            if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || confirm.isEmpty() || gender.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!pass.equals(confirm)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (userRepo.isEmailExists(email)) {
                Toast.makeText(this, "Email already exists", Toast.LENGTH_SHORT).show();
                return;
            }

            String hashedPassword = hashPassword(pass);

            User user = new User(name, email, hashedPassword, gender, dob, phone, address, roleId);
            long result = userRepo.insertUser(user);

            if (result > 0) {
                Toast.makeText(this, "Registration successful! Redirecting to login...", Toast.LENGTH_LONG).show();

                // Chuyển sang LoginActivity sau 2 giây
                new Handler().postDelayed(() -> {
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }, 2000);
            } else {
                Toast.makeText(this, "Registration failed!", Toast.LENGTH_LONG).show();
            }
        });

        // Xử lý sự kiện click vào link quay lại đăng nhập
        binding.backToLoginLink.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return password;
        }
    }
}
