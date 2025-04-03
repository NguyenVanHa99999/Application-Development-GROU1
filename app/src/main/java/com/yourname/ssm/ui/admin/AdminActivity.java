package com.yourname.ssm.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.navigation.NavigationView;
import com.yourname.ssm.R;
import com.yourname.ssm.repository.LoginUserRepository;
import com.yourname.ssm.ui.auth.LoginActivity;

public class AdminActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawer;
    private LoginUserRepository loginUserRepository;
    private TextView navHeaderUserName, navHeaderUserEmail, navHeaderUserId;
    private View headerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Khởi tạo repository
        loginUserRepository = new LoginUserRepository(this);

        // Kiểm tra nếu không phải admin thì chuyển về màn hình đăng nhập
        if (!isAdmin()) {
            Toast.makeText(this, "Không có quyền truy cập!", Toast.LENGTH_SHORT).show();
            logout();
            return;
        }

        // Thiết lập toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Quản trị hệ thống");
        }

        // Thiết lập navigation drawer
        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Thiết lập toggle button cho drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // Thiết lập thông tin người dùng trong header
        headerView = navigationView.getHeaderView(0);
        navHeaderUserName = headerView.findViewById(R.id.header_username);
        navHeaderUserEmail = headerView.findViewById(R.id.header_email);
        navHeaderUserId = headerView.findViewById(R.id.header_user_id);
        updateNavigationHeader();

        // Mặc định hiển thị màn hình quản lý người dùng
        if (savedInstanceState == null) {
            navigationView.setCheckedItem(R.id.userManagement);
            loadFragment(new UserManagementFragment());
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Quản lý người dùng");
            }
        }
    }

    // Kiểm tra xem người dùng có phải là admin không
    private boolean isAdmin() {
        String role = loginUserRepository.getRole();
        boolean isAdmin = "1".equals(role);
        
        // Hiển thị thông báo rõ ràng hơn về quyền truy cập
        if (!isAdmin) {
            Toast.makeText(this, 
                "Bạn không có quyền truy cập! Tài khoản " + loginUserRepository.getEmail() + 
                " không phải là tài khoản Admin.", 
                Toast.LENGTH_LONG).show();
        }
        
        return isAdmin;
    }

    // Cập nhật thông tin người dùng trong navigation header
    private void updateNavigationHeader() {
        // Lấy thông tin người dùng từ SharedPreferences hoặc Database
        String email = loginUserRepository.getEmail();
        String roleId = loginUserRepository.getRole();
        String roleName = "1".equals(roleId) ? "Admin" : "Student";
        
        // Cập nhật UI - Chỉ hiển thị roleName và email
        if (navHeaderUserName != null) {
            navHeaderUserName.setText(roleName);
        }

        if (navHeaderUserEmail != null) {
            navHeaderUserEmail.setText(email);
        }
        
        // Ẩn các thông tin không cần thiết
        if (navHeaderUserId != null) {
            navHeaderUserId.setVisibility(View.GONE);
        }
        
        // Ẩn role TextView nếu có
        TextView navHeaderRole = headerView.findViewById(R.id.header_role);
        if (navHeaderRole != null) {
            navHeaderRole.setVisibility(View.GONE);
        }
        
        // Log thông tin để debug
        Log.d("AdminActivity", "User info - Role: " + roleName + ", Email: " + email);
    }

    @Override
    public void onBackPressed() {
        // Xử lý nút back khi drawer đang mở
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Xử lý các lựa chọn menu trong navigation drawer
        int id = item.getItemId();

        if (id == R.id.userManagement) {
            loadFragment(new UserManagementFragment());
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Quản lý người dùng");
            }
        } else if (id == R.id.logout) {
            // Đăng xuất
            logout();
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    // Xử lý đăng xuất
    private void logout() {
        loginUserRepository.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Load fragment
    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.admin_content_frame, fragment)
                .commit();
    }
} 