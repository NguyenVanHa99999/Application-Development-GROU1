package com.yourname.ssm;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
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
import com.yourname.ssm.ui.auth.LoginActivity;
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
        setContentView(R.layout.activity_main);
        
        Log.d(TAG, "MainActivity onCreate called");
        
        try {
            // Khởi tạo repository và giao diện
            loginUserRepository = new LoginUserRepository(this);
            initViews();
            
            // Lấy tham chiếu đến ImageView trong header của navigation drawer
            View headerView = navigationView.getHeaderView(0);
            navHeaderAvatarImageView = headerView.findViewById(R.id.header_avatar);
            
            // Kiểm tra xem đã khởi tạo danh mục chưa
            SharedPreferences prefs = getSharedPreferences("app_data", MODE_PRIVATE);
            boolean categoriesInitialized = prefs.getBoolean("categories_initialized", false);
            
            // Nếu chưa khởi tạo danh mục, thực hiện khởi tạo
            if (!categoriesInitialized) {
                Log.d(TAG, "Initializing categories for the first time");
                resetAllData();
                
                // Lưu trạng thái đã khởi tạo
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("categories_initialized", true);
                editor.apply();
                
                Toast.makeText(this, "Khởi tạo danh mục thành công", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "Categories already initialized, skipping initialization");
            }
            
            // Cập nhật thông tin người dùng
            updateUserInfo();
            
            // Kiểm tra xem nguời dùng đã đăng nhập chưa
            if (!loginUserRepository.isLoggedIn()) {
                // Nếu người dùng chưa đăng nhập thì chuyển đến màn hình login
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                finish();
                return;
            }
            
            // Cập nhật hiển thị các menu item dựa vào vai trò người dùng
            updateMenuVisibility();
            
            // Thiết lập sự kiện click cho navigationView
            setupNavigationViewItemSelectedListener();
            
            // Mặc định chọn Dashboard
            navigationView.setCheckedItem(R.id.nav_dashboard);
            
            // Mặc định hiển thị Dashboard
            showDashboardFragment();
            
            // Tạo các danh mục mặc định nếu cần
            createDefaultCategories();
            
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

    private void initViews() {
        try {
            // Ánh xạ views
            toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

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

            if (id == R.id.dashboard) {
                showFragment(DashboardFragment.class, "dashboard");
            } else if (id == R.id.addSpending) {
                showFragment(AddSpendingFragment.class, "add_spending");
            } else if (id == R.id.statistics) {
                showFragment(StatisticsFragment.class, "statistics");
            } else if (id == R.id.chat) {
                showFragment(ChatFragment.class, "chat");
            } else if (id == R.id.settings) {
                showFragment(SettingsFragment.class, "settings");
            } else if (id == R.id.userManagement) {
                showFragment(UserManagementFragment.class, "user_management");
            } else if (id == R.id.logout) {
                logout();
            }

            // Đóng navigation drawer sau khi chọn
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "onNavigationItemSelected: Error handling navigation", e);
            Toast.makeText(this, "Lỗi điều hướng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void showFragment(Class<? extends Fragment> fragmentClass, String tag) {
        try {
            Log.d(TAG, "showFragment: Attempting to show fragment: " + tag);
            
            // Kiểm tra fragment đã tồn tại trong stack chưa
            FragmentManager fragmentManager = getSupportFragmentManager();
            if (fragmentManager == null) {
                Log.e(TAG, "showFragment: FragmentManager is null");
                Toast.makeText(this, "Không thể hiển thị màn hình vì lỗi nội bộ", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Fragment currentFragment = fragmentManager.findFragmentById(R.id.main_content);
            Fragment fragment = fragmentManager.findFragmentByTag(tag);
            
            // Nếu fragment đã tồn tại và đang hiển thị, không làm gì cả
            if (currentFragment != null && 
                fragmentClass.isInstance(currentFragment) && 
                tag.equals(currentFragment.getTag())) {
                Log.d(TAG, "showFragment: Fragment " + tag + " already displayed");
                return;
            }
            
            try {
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.setReorderingAllowed(true); // Tối ưu hóa giao dịch fragment
                
                // Nếu fragment đã tồn tại trong backstack, sử dụng lại
                if (fragment != null && fragment.isAdded()) {
                    Log.d(TAG, "showFragment: Reusing existing fragment: " + tag);
                    transaction.replace(R.id.main_content, fragment, tag);
                } else {
                    // Tạo mới fragment
                    try {
                        Log.d(TAG, "showFragment: Creating new fragment: " + tag);
                        fragment = fragmentClass.newInstance();
                        
                        // Lưu reference cho các fragment chính
                        if (fragmentClass == DashboardFragment.class) {
                            dashboardFragment = (DashboardFragment) fragment;
                        } else if (fragmentClass == AddSpendingFragment.class) {
                            addSpendingFragment = (AddSpendingFragment) fragment;
                        } else if (fragmentClass == StatisticsFragment.class) {
                            statisticsFragment = (StatisticsFragment) fragment;
                        } else if (fragmentClass == SettingsFragment.class) {
                            settingsFragment = (SettingsFragment) fragment;
                        }
                        
                        transaction.replace(R.id.main_content, fragment, tag);
                    } catch (InstantiationException e) {
                        Log.e(TAG, "showFragment: Error instantiating fragment", e);
                        Toast.makeText(this, "Lỗi khởi tạo màn hình: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    } catch (IllegalAccessException e) {
                        Log.e(TAG, "showFragment: Illegal access error", e);
                        Toast.makeText(this, "Lỗi truy cập màn hình: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                
                // Thử commit transaction ngay lập tức để phát hiện lỗi sớm
                transaction.commitNow();
                Log.d(TAG, "showFragment: Fragment " + tag + " displayed successfully");
                
            } catch (IllegalStateException e) {
                Log.e(TAG, "showFragment: Activity state error", e);
                
                // Thử sử dụng commitAllowingStateLoss nếu activity đang trong trạng thái saved
                try {
                    FragmentTransaction fallbackTransaction = fragmentManager.beginTransaction();
                    fallbackTransaction.replace(R.id.main_content, fragmentClass.newInstance(), tag);
                    fallbackTransaction.commitAllowingStateLoss();
                    Log.d(TAG, "showFragment: Used fallback method with commitAllowingStateLoss");
                } catch (Exception fallbackEx) {
                    Log.e(TAG, "showFragment: Fallback method also failed", fallbackEx);
                    Toast.makeText(this, "Không thể hiển thị màn hình lúc này, vui lòng thử lại", Toast.LENGTH_SHORT).show();
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "showFragment: Error displaying fragment", e);
            Toast.makeText(this, "Lỗi hiển thị màn hình: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void logout() {
        try {
            // Đăng xuất người dùng
            Log.d(TAG, "logout: Logging out user");
            loginUserRepository.logout();
            
            // Chuyển đến màn hình đăng nhập
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "logout: Error during logout", e);
            Toast.makeText(this, "Lỗi đăng xuất: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
        Log.d(TAG, "Resetting all data in database");
        
        try {
            // Tạo instance của các repository để thao tác với database
            CategoryRepository categoryRepository = new CategoryRepository(this);
            TransactionRepository transactionRepository = new TransactionRepository(this);
            
            // Lấy database
            SQLiteDatabase db = categoryRepository.getWritableDb();
            
            // Xóa giao dịch trước để tránh lỗi foreign key constraint
            Log.d(TAG, "Deleting all transactions");
            db.delete(DatabaseContract.TransactionsEntry.TABLE_NAME, null, null);
            
            // Kiểm tra xem danh mục đã tồn tại chưa
            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseContract.CategoriesEntry.TABLE_NAME, null);
            cursor.moveToFirst();
            int count = cursor.getInt(0);
            cursor.close();
            
            // Nếu đã có danh mục, không cần tạo lại
            if (count > 0) {
                Log.d(TAG, "Found " + count + " existing categories, skipping recreation");
                return;
            }
            
            // Xóa tất cả danh mục nếu có lỗi
            Log.d(TAG, "Deleting any existing categories");
            db.delete(DatabaseContract.CategoriesEntry.TABLE_NAME, null, null);
            
            // Tạo lại các danh mục
            Log.d(TAG, "Creating all categories");
            ContentValues values = new ContentValues();
            
            // DANH MỤC CHI TIÊU (10 danh mục)
            values.put(DatabaseContract.CategoriesEntry._ID, 1);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Ăn uống");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "expense");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_food);
            db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 2);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Di chuyển");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "expense");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_transport);
            db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 3);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Mua sắm");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "expense");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_shopping);
            db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 4);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Giải trí");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "expense");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_entertainment);
            db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 5);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Y tế");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "expense");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_health);
            db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 6);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Giáo dục");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "expense");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_education);
            db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 7);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Nhà ở");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "expense");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_housing);
            db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 8);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Du lịch");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "expense");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_travel);
            db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 9);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Cafe & Trà");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "expense");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_coffee);
            db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 10);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Tiện ích");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "expense");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_utilities);
            db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            
            // DANH MỤC THU NHẬP (10 danh mục)
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 11);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Lương");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "income");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_salary);
            db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 12);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Đầu tư");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "income");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_investment);
            db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 13);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Quà tặng");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "income");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_gift);
            db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 14);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Học bổng");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "income");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_education);
            db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 15);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Bán hàng");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "income");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_shopping);
            db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 16);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Thưởng");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "income");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_gift);
            db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 17);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Cho vay");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "income");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_loan);
            db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 18);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Hoàn tiền");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "income");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_tech);
            db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 19);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Thu nhập phụ");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "income");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_utilities);
            db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            
            values.clear();
            values.put(DatabaseContract.CategoriesEntry._ID, 20);
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, "Dịch vụ");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, "income");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, R.drawable.ic_category_tech);
            db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
            
            // Đóng connection để tránh memory leak
            if (categoryRepository != null) {
                categoryRepository.close();
            }
            
            Log.d(TAG, "Database reset with 20 categories (10 expense + 10 income)");
            
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
            
            // Khởi tạo và hiển thị fragment mặc định
            if (getSupportFragmentManager().findFragmentById(R.id.main_content) == null) {
                Log.d(TAG, "Setting up default fragment (Dashboard)");
                try {
                    // Sử dụng showFragment thay vì thao tác trực tiếp
                    showFragment(DashboardFragment.class, "dashboard");
                    navigationView.setCheckedItem(R.id.dashboard);
                } catch (Exception e) {
                    Log.e(TAG, "Error displaying default fragment", e);
                    // Hiển thị thông báo lỗi
                    Toast.makeText(this, "Lỗi hiển thị màn hình mặc định: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up navigation controller", e);
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
            Log.d(TAG, "Đang tải avatar đã lưu");
            
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
            if (dashboardFragment == null) {
                dashboardFragment = new DashboardFragment();
            }
            
            // Replace current fragment with dashboard
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_content, dashboardFragment)
                .commit();
                
            // Set title in toolbar
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.menu_dashboard);
            }
            
            Log.d(TAG, "Dashboard fragment displayed");
        } catch (Exception e) {
            Log.e(TAG, "Error showing dashboard fragment", e);
        }
    }
    
    private void createDefaultCategories() {
        try {
            // Create a repository for categories
            CategoryRepository categoryRepository = new CategoryRepository(this);
            
            // Check if categories exist
            if (categoryRepository.getCategoriesCount() == 0) {
                Log.d(TAG, "No categories found, creating default ones");
                
                // Create default expense categories
                List<Category> expenses = categoryRepository.getDefaultCategories(0);
                for (Category category : expenses) {
                    categoryRepository.addCategory(category);
                }
                
                // Create default income categories
                List<Category> incomes = categoryRepository.getDefaultCategories(1);
                for (Category category : incomes) {
                    categoryRepository.addCategory(category);
                }
                
                Log.d(TAG, "Default categories created successfully");
            } else {
                Log.d(TAG, "Categories already exist, skipping creation");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating default categories", e);
        }
    }
}
