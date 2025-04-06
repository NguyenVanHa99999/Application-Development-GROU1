package com.yourname.ssm;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.database.sqlite.SQLiteDatabase;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.navigation.NavigationView;
import com.yourname.ssm.repository.LoginUserRepository;
import com.yourname.ssm.ui.addspending.AddSpendingFragment;
import com.yourname.ssm.LoginActivity;
import com.yourname.ssm.ui.dashboard.DashboardFragment;
import com.yourname.ssm.ui.settings.SettingsFragment;
import com.yourname.ssm.repository.CategoryRepository;
import com.yourname.ssm.database.DatabaseContract;
import com.yourname.ssm.repository.TransactionRepository;
import com.yourname.ssm.ui.statistics.StatisticsFragment;
import com.yourname.ssm.repository.UserRepository;
import com.yourname.ssm.model.User;
import com.yourname.ssm.model.Category;
import com.yourname.ssm.ui.chat.ChatFragment;

import java.util.List;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "MainActivity";
    private static final String KEY_PROFILE_IMAGE = "profile_image";
    
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private LoginUserRepository loginUserRepository;

    // Fragments
    private DashboardFragment dashboardFragment;
    private AddSpendingFragment addSpendingFragment;
    private StatisticsFragment statisticsFragment;
    private SettingsFragment settingsFragment;

    // Thêm biến để lưu trữ tham chiếu đến ImageView trong navigation drawer
    private ImageView navHeaderAvatarImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Khởi tạo repository trước để kiểm tra role
        loginUserRepository = new LoginUserRepository(this);
        
        // CRITICAL SECURITY CHECK: Nếu là admin, redirect to AdminActivity
        if (isAdminUser()) {
            Log.e(TAG, "⚠️ SECURITY ALERT - Admin user detected in MainActivity. Redirecting to AdminActivity");
            Toast.makeText(this, "Redirecting to admin panel...", Toast.LENGTH_SHORT).show();
            
            // Force cài đặt role về 1 và user_type về ADMIN
            loginUserRepository.getSharedPreferences().edit()
                .putString("role", "1")
                .putString("user_type", "ADMIN")
                .commit();
            
            // Redirect to AdminActivity
            Intent intent = new Intent(this, com.yourname.ssm.ui.admin.AdminActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }
        
        // Thiết lập màu cho status bar
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(getResources().getColor(R.color.gradient_blue_end));
        
        setContentView(R.layout.activity_main);
        
        Log.d(TAG, "MainActivity onCreate called");
        
        try {
            // Lấy SharedPreferences
            SharedPreferences prefs = getSharedPreferences("app_data", MODE_PRIVATE);
            
            // Kiểm tra container
            View mainContent = findViewById(R.id.main_content);
            if (mainContent != null) {
                Log.d(TAG, "DEBUG: main_content container found successfully");
            } else {
                Log.e(TAG, "DEBUG: main_content container NOT FOUND");
            }
            
            // Khởi tạo repository và giao diện
            initViews();
            
            // Lấy tham chiếu đến ImageView trong header của navigation drawer
            View headerView = navigationView.getHeaderView(0);
            navHeaderAvatarImageView = headerView.findViewById(R.id.header_avatar);
            
            // Kiểm tra xem đã khởi tạo danh mục chưa
            boolean categoriesInitialized = prefs.getBoolean("categories_initialized", false);
            
            // Nếu chưa khởi tạo danh mục hoặc cần reset bắt buộc
            if (!categoriesInitialized) {
                Log.d(TAG, "Initializing categories for the first time");
                // Tạo các danh mục mặc định
                createDefaultCategories();
                
                // Lưu trạng thái đã khởi tạo
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("categories_initialized", true);
                editor.apply();

                Toast.makeText(this, "Khởi tạo danh mục thành công", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "Categories already initialized, skipping initialization");
            }
            
            // Kiểm tra màu appbar và status bar hiện tại
            if (toolbar != null) {
                Log.d(TAG, "DEBUG: toolbar background color: " + Integer.toHexString(toolbar.getBackgroundTintList() != null ? 
                    toolbar.getBackgroundTintList().getDefaultColor() : 0));
            }
            
            // Cập nhật thông tin người dùng
            updateUserInfo();
            
            // Kiểm tra xem nguời dùng đã đăng nhập chưa
            if (!loginUserRepository.isLoggedIn()) {
                Log.d(TAG, "User not logged in, redirecting to login screen");
                // Nếu người dùng chưa đăng nhập thì chuyển đến màn hình login
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                finish();
                return;
            } else {
                Log.d(TAG, "User is logged in: " + loginUserRepository.getEmail() + 
                    ", userId: " + loginUserRepository.getUserId() +
                    ", role: " + loginUserRepository.getRole());
            }
            
            // Cập nhật hiển thị các menu item dựa vào vai trò người dùng
            updateMenuVisibility();
            
            // Thiết lập sự kiện click cho navigationView
            setupNavigationViewItemSelectedListener();
            
            // Mặc định chọn Dashboard
            navigationView.setCheckedItem(R.id.nav_dashboard);
            
            // Mặc định hiển thị Dashboard
            showDashboardFragment();
            
            // Tải avatar đã lưu (nếu có)
            loadSavedAvatar();
            
            // Thiết lập UI và navigation sau khi đã cập nhật thông tin
            setupUI();
            setupNavController();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing MainActivity", e);
            Toast.makeText(this, "Lỗi khởi tạo ứng dụng: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // SECURITY CHECK: Verify again that admin isn't in MainActivity when app resumes
        if (isAdminUser()) {
            Log.e(TAG, "⚠️ SECURITY ALERT - Admin user detected in MainActivity during onResume. Redirecting...");
            Toast.makeText(this, "Redirecting to admin panel...", Toast.LENGTH_SHORT).show();
            
            // Force cài đặt role về 1 và user_type về ADMIN
            loginUserRepository.getSharedPreferences().edit()
                .putString("role", "1")
                .putString("user_type", "ADMIN")
                .commit();
            
            // Redirect to AdminActivity
            Intent intent = new Intent(this, com.yourname.ssm.ui.admin.AdminActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }
    }

    private void initViews() {
        try {
            // Ánh xạ views
            toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            
            // Đặt tiêu đề chính xác là "CampusExpense Management"
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("CampusExpense Management");
                Log.d(TAG, "DEBUG: Đã set tiêu đề toolbar thành CampusExpense Management");
            }
            
            // Đảm bảo màu của toolbar được áp dụng đúng
            toolbar.setBackgroundColor(getResources().getColor(R.color.gradient_blue_end));
            Log.d(TAG, "DEBUG: Đã thiết lập màu toolbar thành gradient_blue_end: " + 
                  Integer.toHexString(getResources().getColor(R.color.gradient_blue_end)));
            
            // Thiết lập status bar color
            getWindow().setStatusBarColor(getResources().getColor(R.color.gradient_blue_end));
            Log.d(TAG, "DEBUG: Đã thiết lập màu status bar thành gradient_blue_end");

            drawerLayout = findViewById(R.id.drawer_layout);
            navigationView = findViewById(R.id.navigation_view);

            // Thiết lập navigation drawer
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawerLayout, toolbar,
                    R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();

            // Thiết lập listener cho navigation
            navigationView.setNavigationItemSelectedListener(this);
            
            Log.d(TAG, "initViews: Views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "initViews: Error initializing views", e);
            e.printStackTrace();
            throw e;
        }
    }

    private void updateUserInfo() {
        try {
            if (navigationView != null) {
                View headerView = navigationView.getHeaderView(0);
                TextView userNameTextView = headerView.findViewById(R.id.header_username);
                TextView userEmailTextView = headerView.findViewById(R.id.header_email);
                TextView userIdTextView = headerView.findViewById(R.id.header_user_id);
                TextView userRoleTextView = headerView.findViewById(R.id.header_role);

                String userRole = loginUserRepository.getRole();
                String userEmail = loginUserRepository.getEmail();
                
                // Xác định tên vai trò
                String roleName = "1".equals(userRole) ? "Admin" : "Student";

                // Cập nhật thông tin hiển thị - Chỉ hiển thị roleName và email
                if (userNameTextView != null) {
                    userNameTextView.setText(roleName);
                }

                if (userEmailTextView != null && userEmail != null) {
                    userEmailTextView.setText(userEmail);
                }
                
                // Ẩn các thông tin không cần thiết
                if (userIdTextView != null) {
                    userIdTextView.setVisibility(View.GONE);
                }
                
                if (userRoleTextView != null) {
                    userRoleTextView.setVisibility(View.GONE);
                }
                
                // Lưu thông tin vào SharedPreferences để có thể sử dụng lại sau này
                SharedPreferences sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("role", userRole);
                editor.putString("email", userEmail);
                editor.apply();
                
                Log.d(TAG, "updateUserInfo: Updated user info - Role: " + roleName + 
                          ", Email: " + userEmail);
            }
        } catch (Exception e) {
            Log.e(TAG, "updateUserInfo: Error updating user info", e);
        }
    }
    
    private void updateMenuVisibility() {
        try {
            // Hiển thị menu item dựa vào vai trò người dùng
            String role = loginUserRepository.getRole();
            boolean isAdmin = "1".equals(role);
            
            // Menu dành cho Admin
            navigationView.getMenu().findItem(R.id.userManagement).setVisible(isAdmin);
            
            // Menu cho người dùng thông thường
            boolean isStudent = "2".equals(role) || (!isAdmin);
            navigationView.getMenu().findItem(R.id.dashboard).setVisible(isStudent);
            navigationView.getMenu().findItem(R.id.addSpending).setVisible(isStudent);
            navigationView.getMenu().findItem(R.id.statistics).setVisible(isStudent);
            navigationView.getMenu().findItem(R.id.settings).setVisible(isStudent);
            
            Log.d(TAG, "updateMenuVisibility: Updated menu for role: " + role);
        } catch (Exception e) {
            Log.e(TAG, "updateMenuVisibility: Error updating menu visibility", e);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        try {
            // Xử lý các mục trong navigation drawer
            int id = item.getItemId();
            
            Log.d(TAG, "onNavigationItemSelected: Selected item ID: " + id);
            
            // Đóng navigation drawer trước khi chuyển trang
            DrawerLayout drawer = findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
            
            // Xử lý các mục menu
            if (id == R.id.dashboard) {
                new Handler().postDelayed(() -> showDashboardFragment(), 300);
                return true;
            } else if (id == R.id.addSpending) {
                showFragment(AddSpendingFragment.class, "add_spending");
                return true;
            } else if (id == R.id.statistics) {
                showFragment(StatisticsFragment.class, "statistics");
                return true;
            } else if (id == R.id.chat) {
                new Handler().postDelayed(() -> {
                    try {
                        Fragment chatFragment = new ChatFragment();
                        getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                            .replace(R.id.main_content, chatFragment, "chat")
                            .commitAllowingStateLoss();
                        if (getSupportActionBar() != null) {
                            getSupportActionBar().setTitle("CampusExpense Management");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error showing chat fragment", e);
                        Toast.makeText(MainActivity.this, "Lỗi hiển thị màn hình trò chuyện", Toast.LENGTH_SHORT).show();
                    }
                }, 300);
                return true;
            } else if (id == R.id.settings) {
                showFragment(SettingsFragment.class, "settings");
                return true;
            } else if (id == R.id.userManagement) {
                new Handler().postDelayed(() -> {
                    try {
                        Fragment userManagementFragment = new UserManagementFragment();
                        getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                            .replace(R.id.main_content, userManagementFragment, "user_management")
                            .commitAllowingStateLoss();
                        if (getSupportActionBar() != null) {
                            getSupportActionBar().setTitle("CampusExpense Management");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error showing user management fragment", e);
                        Toast.makeText(MainActivity.this, "Lỗi hiển thị màn hình quản lý người dùng", Toast.LENGTH_SHORT).show();
                    }
                }, 300);
                return true;
            } else if (id == R.id.logout) {
                logout();
                return true;
            }
            
            return false;
        } catch (Exception e) {
            Log.e(TAG, "onNavigationItemSelected: Error handling navigation", e);
            e.printStackTrace();
            Toast.makeText(this, "Lỗi điều hướng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void showFragment(Class<? extends Fragment> fragmentClass, String tag) {
        try {
            Log.d(TAG, "DEBUG: showFragment gọi cho: " + fragmentClass.getSimpleName() + " với tag: " + tag);
            
            // Kiểm tra container main_content
            View mainContent = findViewById(R.id.main_content);
            if (mainContent == null) {
                Log.e(TAG, "DEBUG: Container main_content không tồn tại!");
                Toast.makeText(this, "Lỗi: Không tìm thấy container", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Lấy fragment manager
            FragmentManager fragmentManager = getSupportFragmentManager();
            
            // Bắt đầu giao dịch fragment
            FragmentTransaction ft = fragmentManager.beginTransaction();
            
            // Thiết lập animation chuyển cảnh
            ft.setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            );
            
            Fragment fragmentToShow = null;
            
            // Tạo instance mới của fragment mỗi lần gọi 
            // Luôn tạo mới để tránh vấn đề tái sử dụng fragment cũ
            try {
                Log.d(TAG, "DEBUG: Đang tạo mới instance của fragment: " + fragmentClass.getSimpleName());
                fragmentToShow = fragmentClass.newInstance();
                Log.d(TAG, "DEBUG: Đã tạo mới instance thành công");
            } catch (Exception e) {
                Log.e(TAG, "Error creating fragment instance: " + fragmentClass.getSimpleName(), e);
                e.printStackTrace();
                Toast.makeText(this, "Lỗi tải trang: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Sử dụng replace thay vì add
            ft.replace(R.id.main_content, fragmentToShow, tag);
            Log.d(TAG, "DEBUG: Đã sử dụng replace cho fragment: " + fragmentToShow.getClass().getSimpleName());
            
            // Commit giao dịch fragment
            ft.commitAllowingStateLoss();
            
            // Thiết lập tiêu đề luôn là "CampusExpense Management" 
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("CampusExpense Management");
            }
            
            // Lưu trạng thái fragment hiện tại vào biến tương ứng
            if (fragmentClass == DashboardFragment.class) {
                dashboardFragment = (DashboardFragment) fragmentToShow;
            } else if (fragmentClass == AddSpendingFragment.class) {
                addSpendingFragment = (AddSpendingFragment) fragmentToShow;
            } else if (fragmentClass == StatisticsFragment.class) {
                statisticsFragment = (StatisticsFragment) fragmentToShow;
            } else if (fragmentClass == SettingsFragment.class) {
                settingsFragment = (SettingsFragment) fragmentToShow;
            }
            
            // Log debug info
            Log.d(TAG, "SUCCESS: Đã hiển thị fragment: " + (fragmentToShow != null ? fragmentToShow.getClass().getSimpleName() : "null"));
        } catch (Exception e) {
            Log.e(TAG, "Error showing fragment", e);
            e.printStackTrace();
            Toast.makeText(this, "Lỗi hiển thị nội dung: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void logout() {
        try {
            Log.d(TAG, "Logging out user: " + loginUserRepository.getEmail());
            
            // Đăng xuất khỏi repository (xóa thông tin đăng nhập)
            if (loginUserRepository != null) {
                loginUserRepository.logout();
                Log.d(TAG, "User logged out successfully");
            }
            
            // Chuyển tới màn hình login
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error during logout", e);
            Toast.makeText(this, "Đã xảy ra lỗi khi đăng xuất: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
    
    /**
     * Phương thức này được gọi từ AddSpendingFragment sau khi thêm giao dịch thành công
     * để cập nhật dữ liệu trên Dashboard
     */
    public void refreshDashboard() {
        try {
            Log.d(TAG, "refreshDashboard: Refreshing dashboard data");
            
            // Kiểm tra fragment có tồn tại không
            FragmentManager fragmentManager = getSupportFragmentManager();
            DashboardFragment dashboardFragment = (DashboardFragment) fragmentManager.findFragmentByTag("dashboard");
            
            if (dashboardFragment != null) {
                Log.d(TAG, "refreshDashboard: Dashboard fragment found, refreshing data");
                dashboardFragment.refreshData();
            } else {
                Log.d(TAG, "refreshDashboard: Dashboard fragment not in stack");
                // Nếu không tìm thấy fragment, không làm gì cả
            }
        } catch (Exception e) {
            Log.e(TAG, "refreshDashboard: Error refreshing dashboard", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        try {
            // Giải phóng tài nguyên
            if (loginUserRepository != null) {
                loginUserRepository = null;
            }
            
            Log.d(TAG, "onDestroy: Resources released");
        } catch (Exception e) {
            Log.e(TAG, "onDestroy: Error releasing resources", e);
        }
    }
    
    // Placeholder fragment class for user management that might be added later
    public static class UserManagementFragment extends Fragment {
        public UserManagementFragment() {
            // Required empty public constructor
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_user_management, container, false);
        }
    }

    private void resetAllData() {
        Log.d(TAG, "=========== BẮT ĐẦU RESET TẤT CẢ DỮ LIỆU ===========");
        
        try {
            // Tạo instance của các repository để thao tác với database
            CategoryRepository categoryRepository = new CategoryRepository(this);
            TransactionRepository transactionRepository = new TransactionRepository(this);
            
            // Lấy database
            SQLiteDatabase db = categoryRepository.getWritableDb();
            
            // Xóa giao dịch trước để tránh lỗi foreign key constraint
            int transactionsDeleted = db.delete(DatabaseContract.TransactionsEntry.TABLE_NAME, null, null);
            Log.d(TAG, "Đã xóa " + transactionsDeleted + " giao dịch");
            
            // Xóa tất cả ngân sách
            int budgetsDeleted = db.delete(DatabaseContract.BudgetEntry.TABLE_NAME, null, null);
            Log.d(TAG, "Đã xóa " + budgetsDeleted + " ngân sách");
            
            // Đếm số danh mục hiện có
            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseContract.CategoriesEntry.TABLE_NAME, null);
            cursor.moveToFirst();
            int count = cursor.getInt(0);
            cursor.close();
            Log.d(TAG, "Số danh mục hiện có: " + count);
            
            // Xóa tất cả danh mục 
            int categoriesDeleted = db.delete(DatabaseContract.CategoriesEntry.TABLE_NAME, null, null);
            Log.d(TAG, "Đã xóa " + categoriesDeleted + " danh mục cũ");
            
            // Tạo lại các danh mục
            Log.d(TAG, "Bắt đầu tạo lại danh mục");
            ContentValues values = new ContentValues();
            
            // DANH MỤC CHI TIÊU (10 danh mục)
            values.put(DatabaseContract.CategoriesEntry._ID, 1);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Ăn uống");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "expense");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_food);
            long result = db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            Log.d(TAG, "Thêm danh mục Ăn uống: " + result);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 2);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Di chuyển");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "expense");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_transport);
            result = db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            Log.d(TAG, "Thêm danh mục Di chuyển: " + result);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 3);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Mua sắm");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "expense");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_shopping);
            result = db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            Log.d(TAG, "Thêm danh mục Mua sắm: " + result);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 4);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Giải trí");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "expense");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_entertainment);
            result = db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            Log.d(TAG, "Thêm danh mục Giải trí: " + result);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 5);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Y tế");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "expense");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_health);
            result = db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            Log.d(TAG, "Thêm danh mục Y tế: " + result);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 6);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Giáo dục");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "expense");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_education);
            result = db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            Log.d(TAG, "Thêm danh mục Giáo dục: " + result);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 7);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Nhà ở");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "expense");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_housing);
            result = db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            Log.d(TAG, "Thêm danh mục Nhà ở: " + result);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 8);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Du lịch");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "expense");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_travel);
            result = db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            Log.d(TAG, "Thêm danh mục Du lịch: " + result);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 9);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Cafe & Trà");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "expense");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_coffee);
            result = db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            Log.d(TAG, "Thêm danh mục Cafe & Trà: " + result);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 10);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Tiện ích");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "expense");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_utilities);
            result = db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            Log.d(TAG, "Thêm danh mục Tiện ích: " + result);
            
            // DANH MỤC THU NHẬP (10 danh mục)
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 11);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Lương");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "income");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_salary);
            result = db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            Log.d(TAG, "Thêm danh mục Lương: " + result);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 12);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Đầu tư");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "income");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_investment);
            result = db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            Log.d(TAG, "Thêm danh mục Đầu tư: " + result);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 13);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Quà tặng");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "income");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_gift);
            result = db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            Log.d(TAG, "Thêm danh mục Quà tặng: " + result);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 14);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Học bổng");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "income");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_education);
            result = db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            Log.d(TAG, "Thêm danh mục Học bổng: " + result);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 15);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Bán hàng");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "income");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_shopping);
            result = db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            Log.d(TAG, "Thêm danh mục Bán hàng: " + result);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 16);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Thưởng");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "income");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_gift);
            result = db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            Log.d(TAG, "Thêm danh mục Thưởng: " + result);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 17);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Cho vay");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "income");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_loan);
            result = db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            Log.d(TAG, "Thêm danh mục Cho vay: " + result);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 18);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Hoàn tiền");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "income");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_tech);
            result = db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            Log.d(TAG, "Thêm danh mục Hoàn tiền: " + result);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 19);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Thu nhập phụ");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "income");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_utilities);
            result = db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            Log.d(TAG, "Thêm danh mục Thu nhập phụ: " + result);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 20);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Dịch vụ");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "income");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_tech);
            result = db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            Log.d(TAG, "Thêm danh mục Dịch vụ: " + result);
            
            // Đóng connection để tránh memory leak
            if (categoryRepository != null) {
                categoryRepository.close();
            }
            
            // Đếm lại số danh mục sau khi thêm
            db = categoryRepository.getReadableDb();
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseContract.CategoriesEntry.TABLE_NAME, null);
            cursor.moveToFirst();
            int newCount = cursor.getInt(0);
            cursor.close();
            Log.d(TAG, "Số danh mục sau khi thêm mới: " + newCount);
            
            Log.d(TAG, "Database reset with 20 categories (10 expense + 10 income)");
            
            // Gọi createDefaultCategories để đảm bảo tất cả danh mục được thêm
            createDefaultCategories();
            
            // Thông báo UI cập nhật
            runOnUiThread(() -> {
                Toast.makeText(this, "Đã khởi tạo lại danh mục", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "UI notified of database reset");
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error resetting database from MainActivity", e);
            
            runOnUiThread(() -> {
                Toast.makeText(this, "Lỗi khởi tạo lại danh mục: " + e.getMessage(), 
                                     Toast.LENGTH_SHORT).show();
            });
        } finally {
            Log.d(TAG, "=========== KẾT THÚC RESET TẤT CẢ DỮ LIỆU ===========");
        }
    }

    private void createDefaultCategories() {
        Log.d(TAG, "=========== BẮT ĐẦU TẠO DANH MỤC MẶC ĐỊNH ===========");
        
        try {
            // Create a repository for categories
            CategoryRepository categoryRepository = new CategoryRepository(this);
            
            // Kiểm tra số lượng danh mục hiện có
            int existingCount = categoryRepository.getCategoriesCount();
            Log.d(TAG, "Số danh mục hiện có: " + existingCount);
            
            if (existingCount >= 20) {
                Log.d(TAG, "Đã có đủ danh mục (" + existingCount + "), không cần tạo lại");
                Log.d(TAG, "=========== KẾT THÚC TẠO DANH MỤC MẶC ĐỊNH ===========");
                return;
            }
            
            // Xóa dữ liệu cũ nếu có ít hơn 20 danh mục
            Log.d(TAG, "Xóa tất cả danh mục cũ và tạo lại từ đầu");
            SQLiteDatabase db = categoryRepository.getWritableDb();
            
            // Xóa giao dịch trước để tránh lỗi foreign key constraint
            int transactionsDeleted = db.delete(DatabaseContract.TransactionsEntry.TABLE_NAME, null, null);
            Log.d(TAG, "Đã xóa " + transactionsDeleted + " giao dịch cũ");
            
            // Xóa tất cả ngân sách
            int budgetsDeleted = db.delete(DatabaseContract.BudgetEntry.TABLE_NAME, null, null);
            Log.d(TAG, "Đã xóa " + budgetsDeleted + " ngân sách cũ");
            
            // Xóa tất cả danh mục
            int categoriesDeleted = db.delete(DatabaseContract.CategoriesEntry.TABLE_NAME, null, null);
            Log.d(TAG, "Đã xóa " + categoriesDeleted + " danh mục cũ");
            
            // Bắt đầu tạo danh mục mới
            Log.d(TAG, "Bắt đầu thêm danh mục chi tiêu");
            List<Category> expenseCategories = categoryRepository.getDefaultCategories(0);
            for (Category category : expenseCategories) {
                long result = categoryRepository.addCategory(category);
                if (result <= 0) {
                    Log.e(TAG, "Lỗi thêm danh mục chi tiêu: " + category.getName() + ", ID: " + category.getId());
                    // Thử thêm trực tiếp vào database nếu thêm qua repository thất bại
                    ContentValues values = new ContentValues();
                    values.put(DatabaseContract.CategoriesEntry._ID, category.getId());
                    values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, category.getName());
                    values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "expense");
                    values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, category.getIconResourceId());
                    db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
                    Log.d(TAG, "Đã thử thêm trực tiếp danh mục chi tiêu: " + category.getName());
                } else {
                    Log.d(TAG, "Đã thêm danh mục chi tiêu: " + category.getName() + ", ID: " + category.getId());
                }
            }
            
            Log.d(TAG, "Bắt đầu thêm danh mục thu nhập");
            List<Category> incomeCategories = categoryRepository.getDefaultCategories(1);
            for (Category category : incomeCategories) {
                long result = categoryRepository.addCategory(category);
                if (result <= 0) {
                    Log.e(TAG, "Lỗi thêm danh mục thu nhập: " + category.getName() + ", ID: " + category.getId());
                    // Thử thêm trực tiếp vào database nếu thêm qua repository thất bại
                    ContentValues values = new ContentValues();
                    values.put(DatabaseContract.CategoriesEntry._ID, category.getId());
                    values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, category.getName());
                    values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "income");
                    values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, category.getIconResourceId());
                    db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
                    Log.d(TAG, "Đã thử thêm trực tiếp danh mục thu nhập: " + category.getName());
                } else {
                    Log.d(TAG, "Đã thêm danh mục thu nhập: " + category.getName() + ", ID: " + category.getId());
                }
            }
            
            // Kiểm tra kết quả
            int finalCount = categoryRepository.getCategoriesCount();
            Log.d(TAG, "Tổng số danh mục sau khi tạo: " + finalCount);
            
            // Thông báo nếu vẫn chưa đủ danh mục
            if (finalCount < 20) {
                Log.e(TAG, "CẢNH BÁO: Vẫn chưa đủ danh mục sau khi tạo (" + finalCount + "/20)");
                Toast.makeText(this, "Khởi tạo danh mục không đầy đủ (" + finalCount + "/20)", Toast.LENGTH_LONG).show();
            } else {
                Log.d(TAG, "Đã khởi tạo đủ " + finalCount + " danh mục");
                Toast.makeText(this, "Đã khởi tạo " + finalCount + " danh mục thành công", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi tạo danh mục mặc định", e);
            Toast.makeText(this, "Lỗi tạo danh mục: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            Log.d(TAG, "=========== KẾT THÚC TẠO DANH MỤC MẶC ĐỊNH ===========");
        }
    }

    private void setupUI() {
        try {
            Log.d(TAG, "Setting up UI components");
            
            // Kiểm tra người dùng đã đăng nhập
            if (!loginUserRepository.isLoggedIn()) {
                Log.d(TAG, "No user logged in, redirecting to LoginActivity");
                // Nếu chưa đăng nhập, chuyển hướng đến màn hình đăng nhập
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                finish();
                return;
            }
            
            // Ẩn hiện các mục menu dựa vào vai trò
            updateMenuVisibility();
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up UI", e);
            Toast.makeText(this, "Lỗi khởi tạo giao diện: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void setupNavController() {
        try {
            Log.d(TAG, "Setting up navigation controller");
            
            // Kiểm tra và xác nhận container main_content
            View contentView = findViewById(R.id.main_content);
            if (contentView == null) {
                Log.e(TAG, "DEBUG: Container main_content không tìm thấy trong setupNavController");
                Toast.makeText(this, "Lỗi: Không tìm thấy container", Toast.LENGTH_SHORT).show();
                return;
            } else {
                Log.d(TAG, "DEBUG: Đã tìm thấy container main_content");
            }
            
            // Khởi tạo và hiển thị fragment mặc định
            if (getSupportFragmentManager().findFragmentById(R.id.main_content) == null) {
                Log.d(TAG, "DEBUG: Không tìm thấy fragment hiện tại, đang tạo fragment mặc định (Dashboard)");
                try {
                    // Tạo fragment mới
                    if (dashboardFragment == null) {
                        dashboardFragment = new DashboardFragment();
                    }
                    
                    // Sử dụng replace thay vì add
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.replace(R.id.main_content, dashboardFragment, "dashboard");
                    transaction.commitAllowingStateLoss();
                    
                    Log.d(TAG, "DEBUG: Đã sử dụng replace để thêm dashboard fragment");
                    
                    // Cập nhật navigation view
                    navigationView.setCheckedItem(R.id.dashboard);
                    
                    // Đặt tiêu đề cố định
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setTitle("CampusExpense Management");
                    }
                    
                    Log.d(TAG, "Đã thiết lập fragment mặc định và tiêu đề");
                } catch (Exception e) {
                    Log.e(TAG, "Error displaying default fragment", e);
                    e.printStackTrace();
                    // Hiển thị thông báo lỗi
                    Toast.makeText(this, "Lỗi hiển thị màn hình mặc định: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.d(TAG, "DEBUG: Đã có fragment trong container");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up navigation controller", e);
            e.printStackTrace();
            Toast.makeText(this, "Lỗi thiết lập điều hướng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Phương thức cập nhật avatar trong navigation drawer
     * @param bitmap Ảnh mới cho avatar
     * @param username Tên người dùng (null nếu không thay đổi)
     * @param email Email người dùng (null nếu không thay đổi)
     */
    public void updateNavigationDrawerAvatar(Bitmap bitmap, String username, String email) {
        try {
            if (bitmap == null) {
                Log.e(TAG, "Bitmap null, không thể cập nhật avatar trong navigation drawer");
                return;
            }
            
            // Lấy NavigationView và headerView
            NavigationView navigationView = findViewById(R.id.navigation_view);
            if (navigationView == null) {
                Log.e(TAG, "NavigationView null, không thể cập nhật avatar");
                return;
            }
            
            View headerView = navigationView.getHeaderView(0);
            if (headerView == null) {
                Log.e(TAG, "NavigationView header null, không thể cập nhật avatar");
                return;
            }
            
            // Cập nhật avatar
            ImageView avatarImageView = headerView.findViewById(R.id.header_avatar);
            if (avatarImageView != null) {
                avatarImageView.setImageBitmap(bitmap);
                Log.d(TAG, "Đã cập nhật avatar trong navigation drawer");
            }
            
            // Cập nhật username nếu cần
            if (username != null) {
                TextView usernameTextView = headerView.findViewById(R.id.header_username);
                if (usernameTextView != null) {
                    usernameTextView.setText(username);
                    Log.d(TAG, "Đã cập nhật username trong navigation drawer: " + username);
                }
            }
            
            // Cập nhật email nếu cần
            if (email != null) {
                TextView emailTextView = headerView.findViewById(R.id.header_email);
                if (emailTextView != null) {
                    emailTextView.setText(email);
                    Log.d(TAG, "Đã cập nhật email trong navigation drawer: " + email);
                }
            }
            
            // KHÔNG hiển thị toast thông báo ở đây
            // Toast.makeText(this, "Đã cập nhật ảnh đại diện", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi cập nhật avatar trong navigation drawer: " + e.getMessage(), e);
        }
    }
    
    /**
     * Chuyển đổi Bitmap thành chuỗi Base64
     */
    private String bitmapToBase64(Bitmap bitmap) {
        java.io.ByteArrayOutputStream byteArrayOutputStream = new java.io.ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT);
    }
    
    /**
     * Chuyển đổi chuỗi Base64 thành Bitmap
     */
    private Bitmap base64ToBitmap(String encodedImage) {
        if (TextUtils.isEmpty(encodedImage)) return null;
        
        try {
            byte[] decodedBytes = android.util.Base64.decode(encodedImage, android.util.Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Tải avatar từ SharedPreferences khi mở ứng dụng
     */
    private void loadSavedAvatar() {
        try {
            // Lấy tham chiếu đến ImageView trong header
            View headerView = navigationView.getHeaderView(0);
            if (headerView == null) {
                Log.e(TAG, "Header view null, không thể tải avatar");
                return;
            }
            
            ImageView headerAvatar = headerView.findViewById(R.id.header_avatar);
            if (headerAvatar == null) {
                Log.e(TAG, "Header avatar ImageView null");
                return;
            }
            
            // Lấy ID người dùng hiện tại
            int userId = loginUserRepository.getUserId();
            if (userId <= 0) {
                Log.e(TAG, "Invalid user ID: " + userId);
                return;
            }
            
            // Tạo repository để truy cập database
            UserRepository userRepository = new UserRepository(this);
            
            // Tải avatar từ database
            new Thread(() -> {
                try {
                    String base64Avatar = userRepository.getUserAvatar(userId);
                    if (base64Avatar != null && !base64Avatar.isEmpty()) {
                        // Nếu có avatar trong database, chuyển đổi từ Base64 sang Bitmap
                        byte[] decodedBytes = android.util.Base64.decode(base64Avatar, android.util.Base64.DEFAULT);
                        Bitmap avatarBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                        
                        // Cập nhật UI trên main thread
                        runOnUiThread(() -> {
                            // Cập nhật avatar trong navigation header
                            headerAvatar.setImageBitmap(avatarBitmap);
                            Log.d(TAG, "Avatar đã được tải từ database");
                        });
                    } else {
                        // Nếu không có avatar trong database, thử tải từ SharedPreferences
                        SharedPreferences sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE);
                        String savedImage = sharedPreferences.getString("profile_image", null);
                        
                        if (savedImage != null && !savedImage.isEmpty()) {
                            // Nếu có avatar trong SharedPreferences, chuyển đổi từ Base64 sang Bitmap
                            byte[] decodedBytes = android.util.Base64.decode(savedImage, android.util.Base64.DEFAULT);
                            Bitmap avatarBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                            
                            // Cập nhật UI trên main thread
                            runOnUiThread(() -> {
                                // Cập nhật avatar trong navigation header
                                headerAvatar.setImageBitmap(avatarBitmap);
                                Log.d(TAG, "Avatar đã được tải từ SharedPreferences");
                            });
                            
                            // Lưu vào database để đồng bộ
                            userRepository.updateUserAvatar(userId, savedImage);
                            Log.d(TAG, "Avatar đã được lưu từ SharedPreferences vào database");
                        } else {
                            // Nếu không có avatar trong cả database và SharedPreferences, sử dụng avatar mặc định
                            String role = loginUserRepository.getRole();
                            runOnUiThread(() -> {
                                if ("1".equals(role)) {
                                    headerAvatar.setImageResource(R.drawable.ic_admin_avatar);
                                } else {
                                    headerAvatar.setImageResource(R.drawable.ic_student_avatar);
                                }
                                Log.d(TAG, "Sử dụng avatar mặc định cho vai trò: " + role);
                            });
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error loading avatar", e);
                }
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "Error in loadSavedAvatar", e);
        }
    }

    private void setupNavigationViewItemSelectedListener() {
        try {
            // Set the listener for navigation item selection
            navigationView.setNavigationItemSelectedListener(this);
            Log.d(TAG, "Navigation item selected listener setup complete");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up navigation view listener", e);
        }
    }
    
    private void showDashboardFragment() {
        try {
            showFragment(DashboardFragment.class, "dashboard");
            Log.d(TAG, "ShowDashboardFragment: Sử dụng phương thức showFragment để hiển thị dashboard");
        } catch (Exception e) {
            Log.e(TAG, "Error showing dashboard fragment", e);
            Toast.makeText(this, "Lỗi khi hiển thị màn hình chính: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Check if the user is an admin based on multiple criteria
    private boolean isAdminUser() {
        try {
            if (loginUserRepository == null) {
                Log.e(TAG, "⚠️ loginUserRepository is null in isAdminUser check");
                return false;
            }
            
            // Check for admin email
            String email = loginUserRepository.getEmail();
            if ("admin@example.com".equals(email)) {
                Log.d(TAG, "⚠️ Admin detected by email: " + email);
                return true;
            }
            
            // Check role and user_type
            String role = loginUserRepository.getRole();
            String userType = loginUserRepository.getSharedPreferences().getString("user_type", "");
            
            boolean isAdmin = "1".equals(role) || "ADMIN".equals(userType);
            Log.d(TAG, "⚠️ Admin check: role=" + role + ", userType=" + userType + ", isAdmin=" + isAdmin);
            
            return isAdmin;
        } catch (Exception e) {
            Log.e(TAG, "⚠️ Error checking admin status: " + e.getMessage(), e);
            return false;
        }
    }
}
