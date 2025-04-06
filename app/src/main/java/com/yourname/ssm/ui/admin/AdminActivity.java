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
    private static final String TAG = "AdminActivity";

    private DrawerLayout drawer;
    private LoginUserRepository loginUserRepository;
    private TextView navHeaderUserName, navHeaderUserEmail, navHeaderUserId;
    private View headerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);
        
        // Change status bar color from purple to blue
        android.view.Window window = getWindow();
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(getResources().getColor(R.color.gradient_blue_end));
        
        Log.d(TAG, "AdminActivity.onCreate() started");

        // Initialize repository
        loginUserRepository = new LoginUserRepository(this);
        Log.d(TAG, "User role from repository: " + loginUserRepository.getRole());

        // Check if not admin, redirect to login
        if (!isAdmin()) {
            Toast.makeText(this, "Access denied!", Toast.LENGTH_SHORT).show();
            logout();
            return;
        }

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Set toolbar color to match main app
        toolbar.setBackgroundColor(getResources().getColor(R.color.gradient_blue_end));
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Admin Dashboard");
        }

        // Setup navigation drawer
        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Setup toggle button for drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // Setup user info in header
        headerView = navigationView.getHeaderView(0);
        navHeaderUserName = headerView.findViewById(R.id.header_username);
        navHeaderUserEmail = headerView.findViewById(R.id.header_email);
        navHeaderUserId = headerView.findViewById(R.id.header_user_id);
        updateNavigationHeader();

        // Always display UserManagementFragment by default
        navigationView.setCheckedItem(R.id.userManagement);
        loadUserManagementFragment();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "AdminActivity.onResume() called");
        
        // Verify login status before anything else
        if (!loginUserRepository.isLoggedIn()) {
            Log.d(TAG, "⚠️ User not logged in, redirecting to login");
            logout();
            return;
        }
        
        // Get current email, should be admin@example.com for admin
        String email = loginUserRepository.getEmail();
        
        // If email is admin@example.com, force correct role values
        if ("admin@example.com".equals(email)) {
            Log.d(TAG, "⚠️ Detected admin email, ensuring correct role values");
            loginUserRepository.getSharedPreferences().edit()
                .putString("role", "1")
                .putString("user_type", "ADMIN")
                .commit();
        }
        
        // Double-check if we're still admin
        if (!isAdmin()) {
            Log.d(TAG, "⚠️ User is not admin, redirecting to login");
            logout();
            return;
        }
        
        // Ensure UserManagementFragment is shown when activity resumes
        loadUserManagementFragment();
    }

    // Check if user is admin
    private boolean isAdmin() {
        String role = loginUserRepository.getRole();
        String userType = loginUserRepository.getSharedPreferences().getString("user_type", "");
        boolean isAdmin = "1".equals(role) || "ADMIN".equals(userType);
        
        // Log the role and admin status
        Log.d(TAG, "Checking admin status - Role: " + role + ", UserType: " + userType + ", isAdmin: " + isAdmin);
        
        // If not admin but the type indicates admin, fix the role value
        if (!isAdmin && "ADMIN".equals(userType)) {
            Log.d(TAG, "Fixing role value to match admin user type");
            loginUserRepository.getSharedPreferences().edit().putString("role", "1").commit();
            isAdmin = true;
        }
        
        // If we're still not an admin, show clear message about access rights
        if (!isAdmin) {
            Toast.makeText(this, 
                "Access denied! Account " + loginUserRepository.getEmail() + 
                " is not an Admin account.", 
                Toast.LENGTH_LONG).show();
        }
        
        return isAdmin;
    }

    // Update user info in navigation header
    private void updateNavigationHeader() {
        // Get user info from SharedPreferences or Database
        String email = loginUserRepository.getEmail();
        String roleId = loginUserRepository.getRole();
        String roleName = "1".equals(roleId) ? "Admin" : "Student";
        
        // Update UI - Only show roleName and email
        if (navHeaderUserName != null) {
            navHeaderUserName.setText(roleName);
        }

        if (navHeaderUserEmail != null) {
            navHeaderUserEmail.setText(email);
        }
        
        // Hide unnecessary info
        if (navHeaderUserId != null) {
            navHeaderUserId.setVisibility(View.GONE);
        }
        
        // Hide role TextView if present
        TextView navHeaderRole = headerView.findViewById(R.id.header_role);
        if (navHeaderRole != null) {
            navHeaderRole.setVisibility(View.GONE);
        }
        
        // Log info for debugging
        Log.d(TAG, "User info updated - Role: " + roleName + ", Email: " + email);
    }

    @Override
    public void onBackPressed() {
        // Handle back button when drawer is open
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation drawer item clicks
        int id = item.getItemId();
        Log.d(TAG, "Navigation item selected: " + id);

        if (id == R.id.userManagement) {
            loadUserManagementFragment();
        } else if (id == R.id.logout) {
            // Logout
            logout();
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    // Handle logout
    private void logout() {
        Log.d(TAG, "Logging out user");
        loginUserRepository.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    // Dedicated method to load UserManagementFragment
    private void loadUserManagementFragment() {
        Log.d(TAG, "Loading UserManagementFragment");
        UserManagementFragment fragment = new UserManagementFragment();
        loadFragment(fragment);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("User Management");
        }
    }

    // Load fragment
    private void loadFragment(Fragment fragment) {
        try {
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.admin_content_frame, fragment)
                    .commitAllowingStateLoss();
            Log.d(TAG, "Fragment loaded: " + fragment.getClass().getSimpleName());
        } catch (Exception e) {
            Log.e(TAG, "Error loading fragment: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading content", Toast.LENGTH_SHORT).show();
        }
    }
} 