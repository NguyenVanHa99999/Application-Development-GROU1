package com.yourname.ssm.ui.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import com.google.android.material.navigation.NavigationView;

import com.google.android.material.textfield.TextInputLayout;
import com.yourname.ssm.R;
import com.yourname.ssm.model.Budget;
import com.yourname.ssm.model.User;
import com.yourname.ssm.repository.BudgetRepository;
import com.yourname.ssm.repository.LoginUserRepository;
import com.yourname.ssm.repository.UserRepository;
import com.yourname.ssm.ui.auth.LoginActivity;
import com.yourname.ssm.MainActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;

import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.io.DataOutputStream;
import android.content.pm.ResolveInfo;

public class SettingsFragment extends Fragment {
    private static final String TAG = "SettingsFragment";

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private static final int REQUEST_VIDEO_CAPTURE = 3;
    private static final int REQUEST_PERMISSIONS = 4;
    private static final int REQUEST_MANAGE_STORAGE_PERMISSION = 5;
    private static final String KEY_PROFILE_IMAGE = "profile_image";

    // UI Components - Profile
    private ImageView profileImageView;
    private ImageButton changeAvatarButton;
    private TextView userNameTextView, userEmailTextView;
    private Button editProfileButton, saveProfileButton;
    private TextInputLayout nameInputLayout, phoneInputLayout, addressInputLayout;
    private EditText nameEditText, phoneEditText, addressEditText;
    private LinearLayout genderLayout;
    private RadioGroup genderRadioGroup;
    private RadioButton maleRadioButton, femaleRadioButton;

    // UI Components - Password
    private TextInputLayout currentPasswordLayout, newPasswordLayout, confirmPasswordLayout;
    private EditText currentPasswordEditText, newPasswordEditText, confirmPasswordEditText;
    private Button changePasswordButton;

    // UI Components - Budget
    private TextView currentMonthTextView, currentBudgetTextView;
    private TextInputLayout budgetAmountLayout;
    private EditText budgetAmountEditText;
    private Button updateBudgetButton;

    // UI Components - Theme
    private RadioGroup themeRadioGroup;
    private RadioButton lightThemeRadioButton, darkThemeRadioButton;
    private Button applyThemeButton;

    // UI Components - Notifications
    private LinearLayout settingEmail;
    private androidx.appcompat.widget.SwitchCompat appNotificationSwitch;
    private boolean isAppNotificationEnabled = true;

    // UI Components - Logout
    private Button logoutButton;

    // Repositories
    private LoginUserRepository loginUserRepository;
    private UserRepository userRepository;
    private BudgetRepository budgetRepository;

    // User data
    private int userId;
    private Budget currentBudget;

    // Format utilities
    private NumberFormat currencyFormatter;
    private SimpleDateFormat dateFormatter;

    // State
    private boolean isEditingProfile = false;

    // CardViews (thay vì TabLayout)
    private androidx.cardview.widget.CardView profileCardView, passwordCardView, budgetCardView, themeCardView;

    // Camera related variables
    private Uri photoURI;
    private String currentPhotoPath;

    // Flag to track if avatar was manually updated 
    private boolean manualAvatarUpdate = false;
    
    // Thêm biến để theo dõi thông báo lỗi camera
    private View cameraErrorBubble;
    
    // Thêm biến để theo dõi trạng thái cập nhật dữ liệu
    private boolean isInitialLoad = true;
    private boolean hasManuallyChangedAvatar = false;

    // Biến cờ để theo dõi xem người dùng đã thao tác với avatar hay chưa
    // Khác với hasManuallyChangedAvatar - biến này chỉ dùng để xác định có hiển thị thông báo hay không
    private boolean shouldShowAvatarUpdateToast = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings, container, false);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        try {
            super.onViewCreated(view, savedInstanceState);
            
            // Khởi tạo các repository
            initializeRepositories();
            
            // Khởi tạo các thành phần UI
            initializeViews(view);
            
            // Thiết lập các button và loading data
            setupEventListeners();
            
            // Tìm và ẩn thông báo lỗi camera nếu có
            hideCameraErrorBubble();
            
            // Thêm listener để tự động xóa thông báo lỗi
            view.post(() -> {
                hideCameraErrorBubble();
                // Thêm handler để xóa sau một khoảng thời gian
                new Handler().postDelayed(this::hideCameraErrorBubble, 500);
            });
            
            // Tải dữ liệu trên các luồng riêng biệt để tránh quá tải UI thread
            new Thread(() -> {
                try {
                    loadUserData();
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> updateUserUI());
                }
                } catch (Exception e) {
                    // Log hoặc xử lý ngoại lệ
                }
            }).start();
            
            new Thread(() -> {
                try {
                    loadBudgetData();
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> updateBudgetUI());
                    }
                } catch (Exception e) {
                    // Log hoặc xử lý ngoại lệ
                }
            }).start();
            
            loadThemeSettings();
            loadNotificationSettings();
            
            // Kiểm tra xem có tham số OPEN_TAB được truyền vào không
            if (isAdded() && getArguments() != null && getArguments().containsKey("OPEN_TAB")) {
                int tabIndex = getArguments().getInt("OPEN_TAB", 0);
                // Mở tab tương ứng bằng cách hiển thị CardView tương ứng
                showCardView(tabIndex);
            }
        } catch (Exception e) {
            // Log hoặc xử lý ngoại lệ
        }
    }

    private void initializeViews(View root) {
        // Profile views
        profileImageView = root.findViewById(R.id.profileImageView);
        changeAvatarButton = root.findViewById(R.id.changeAvatarButton);
        userNameTextView = root.findViewById(R.id.userNameTextView);
        userEmailTextView = root.findViewById(R.id.userEmailTextView);
        editProfileButton = root.findViewById(R.id.editProfileButton);
        saveProfileButton = root.findViewById(R.id.saveProfileButton);
        
        nameInputLayout = root.findViewById(R.id.nameInputLayout);
        phoneInputLayout = root.findViewById(R.id.phoneInputLayout);
        addressInputLayout = root.findViewById(R.id.addressInputLayout);
        
        nameEditText = root.findViewById(R.id.nameEditText);
        phoneEditText = root.findViewById(R.id.phoneEditText);
        addressEditText = root.findViewById(R.id.addressEditText);
        
        genderLayout = root.findViewById(R.id.genderLayout);
        genderRadioGroup = root.findViewById(R.id.genderRadioGroup);
        maleRadioButton = root.findViewById(R.id.maleRadioButton);
        femaleRadioButton = root.findViewById(R.id.femaleRadioButton);

        // Password views
        currentPasswordLayout = root.findViewById(R.id.currentPasswordLayout);
        newPasswordLayout = root.findViewById(R.id.newPasswordLayout);
        confirmPasswordLayout = root.findViewById(R.id.confirmPasswordLayout);
        
        currentPasswordEditText = root.findViewById(R.id.currentPasswordEditText);
        newPasswordEditText = root.findViewById(R.id.newPasswordEditText);
        confirmPasswordEditText = root.findViewById(R.id.confirmPasswordEditText);
        
        changePasswordButton = root.findViewById(R.id.changePasswordButton);

        // Budget views
        currentMonthTextView = root.findViewById(R.id.currentMonthTextView);
        currentBudgetTextView = root.findViewById(R.id.currentBudgetTextView);
        budgetAmountLayout = root.findViewById(R.id.budgetAmountLayout);
        budgetAmountEditText = root.findViewById(R.id.budgetAmountEditText);
        updateBudgetButton = root.findViewById(R.id.updateBudgetButton);

        // Theme views
        themeRadioGroup = root.findViewById(R.id.themeRadioGroup);
        lightThemeRadioButton = root.findViewById(R.id.lightThemeRadioButton);
        darkThemeRadioButton = root.findViewById(R.id.darkThemeRadioButton);
        applyThemeButton = root.findViewById(R.id.applyThemeButton);

        // Notification views
        settingEmail = root.findViewById(R.id.setting_email);
        appNotificationSwitch = root.findViewById(R.id.app_notification_switch);

        // Logout button
        logoutButton = root.findViewById(R.id.logoutButton);

        // Lấy các CardView (thay vì container)
        // Giả sử các CardView có vị trí thứ tự như sau trong layout
        ViewGroup parentLayout = (ViewGroup) logoutButton.getParent();
        
        // CardViews nên là các view con đầu tiên trong ScrollView > LinearLayout
        profileCardView = (androidx.cardview.widget.CardView) parentLayout.getChildAt(0);
        passwordCardView = (androidx.cardview.widget.CardView) parentLayout.getChildAt(1);
        budgetCardView = (androidx.cardview.widget.CardView) parentLayout.getChildAt(2);
        themeCardView = (androidx.cardview.widget.CardView) parentLayout.getChildAt(3);

        // Initialize formatters
        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        currencyFormatter.setCurrency(java.util.Currency.getInstance("VND"));
        dateFormatter = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
    }

    private void initializeRepositories() {
        try {
            if (getContext() == null) return;
            
            loginUserRepository = new LoginUserRepository(requireContext());
            userRepository = new UserRepository(requireContext());
            budgetRepository = new BudgetRepository(requireContext());
            
            // Get current user ID
            if (loginUserRepository != null) {
                userId = loginUserRepository.getUserId();
            }
        } catch (Exception e) {
            // Log or handle exception
        }
    }

    private void setupEventListeners() {
        // Profile
        changeAvatarButton.setOnClickListener(v -> changeAvatarAction());
        
        editProfileButton.setOnClickListener(v -> {
            if (!isEditingProfile) {
                showEditProfileUI();
            } else {
                hideEditProfileUI();
            }
        });
        
        saveProfileButton.setOnClickListener(v -> saveProfileChanges());

        // Password
        changePasswordButton.setOnClickListener(v -> changePassword());

        // Budget
        updateBudgetButton.setOnClickListener(v -> updateBudget());

        // Theme
        applyThemeButton.setOnClickListener(v -> applyTheme());

        // Notifications
        settingEmail.setOnClickListener(v -> showEmailSettingsDialog());
        
        appNotificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveNotificationSettings(isChecked);
        });

        // Logout
        logoutButton.setOnClickListener(v -> {
            // Show confirmation dialog
            new AlertDialog.Builder(getContext())
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Logout", (dialog, which) -> performLogout())
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    // Phương thức mới để hiển thị CardView tương ứng dựa trên index
    private void showCardView(int index) {
        try {
            if (!isAdded() || getContext() == null) return;
            
            // Kiểm tra null trước khi sử dụng
            if (profileCardView == null || passwordCardView == null || 
                budgetCardView == null || themeCardView == null) {
                return;
            }
            
            // Mặc định hiển thị tất cả
            profileCardView.setVisibility(View.VISIBLE);
            passwordCardView.setVisibility(View.VISIBLE);
            budgetCardView.setVisibility(View.VISIBLE);
            themeCardView.setVisibility(View.VISIBLE);
            
            // Nếu index hợp lệ, scroll đến view tương ứng
            if (index >= 0 && index <= 3) {
                final View viewToFocus;
                switch (index) {
                    case 0:
                        viewToFocus = profileCardView;
                        break;
                    case 1:
                        viewToFocus = passwordCardView;
                        break;
                    case 2:
                        viewToFocus = budgetCardView;
                        break;
                    case 3:
                        viewToFocus = themeCardView;
                        break;
                    default:
                        viewToFocus = null;
                }
                
                if (viewToFocus != null) {
                    viewToFocus.requestFocus();
                    // Scroll đến view này
                    viewToFocus.post(() -> {
                        if (!isAdded() || getView() == null) return;
                        
                        View view = getView();
                        if (view != null && view instanceof ScrollView) {
                            ((ScrollView) view).smoothScrollTo(0, viewToFocus.getTop());
                        }
                    });
                }
            }
        } catch (Exception e) {
            // Log or handle exception
        }
    }

    // Profile methods
    private void loadUserData() {
        // Method already has minimal processing, just gets data from repository
    }

    private void showEditProfileUI() {
        isEditingProfile = true;
        editProfileButton.setText("Cancel");
        
        // Show edit fields
        nameInputLayout.setVisibility(View.VISIBLE);
        phoneInputLayout.setVisibility(View.VISIBLE);
        addressInputLayout.setVisibility(View.VISIBLE);
        genderLayout.setVisibility(View.VISIBLE);
        saveProfileButton.setVisibility(View.VISIBLE);
        
        // Set current values
        nameEditText.setText(userNameTextView.getText());
        // In a real app, you would populate from user data
        phoneEditText.setText("");
        addressEditText.setText("");
        
        // Select appropriate gender radio button based on user data
        // For now, default to male
        maleRadioButton.setChecked(true);
    }

    private void hideEditProfileUI() {
        isEditingProfile = false;
        editProfileButton.setText("Edit");
        
        // Hide edit fields
        nameInputLayout.setVisibility(View.GONE);
        phoneInputLayout.setVisibility(View.GONE);
        addressInputLayout.setVisibility(View.GONE);
        genderLayout.setVisibility(View.GONE);
        saveProfileButton.setVisibility(View.GONE);
    }

    private void saveProfileChanges() {
        String name = nameEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String gender = maleRadioButton.isChecked() ? "Male" : "Female";
        
        if (TextUtils.isEmpty(name)) {
            nameInputLayout.setError("Please enter your full name");
            return;
        }
        
        // Update UI
        userNameTextView.setText(name);
        
        // In a real app, you would update the user database here
        Toast.makeText(requireContext(), "Profile information updated successfully", Toast.LENGTH_SHORT).show();
        
        // Hide edit fields
        hideEditProfileUI();
    }

    // Method to check and request necessary permissions
    private void checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        
        // Add camera permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA);
        }
        
        // Add storage permissions based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13+, we need image/video specific permissions
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_VIDEO) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_VIDEO);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11+ we check if we have manage storage permission
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", requireActivity().getPackageName(), null);
                intent.setData(uri);
                startActivityForResult(intent, REQUEST_MANAGE_STORAGE_PERMISSION);
            }
        } else {
            // For Android 10 and below
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
        
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(
                    requireActivity(),
                    permissionsNeeded.toArray(new String[0]),
                    REQUEST_PERMISSIONS
            );
        } else {
            // All permissions granted, show picker
            showImagePickerDialog();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allPermissionsGranted = true;
            
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            
            if (allPermissionsGranted) {
                // For Android 11+, check if we need additional permissions
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    Uri uri = Uri.fromParts("package", requireActivity().getPackageName(), null);
                    intent.setData(uri);
                    startActivityForResult(intent, REQUEST_MANAGE_STORAGE_PERMISSION);
                } else {
                    showImagePickerDialog();
                }
            } else {
                Toast.makeText(requireContext(), "App needs access to take photos and videos", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
    
    private void showImagePickerDialog() {
        // Inflate custom dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_image_picker, null);
        
        // Create a dialog with the custom layout
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogView);
        
        final AlertDialog dialog = builder.create();
        dialog.show();
        
        // Set up button clicks
        Button cameraButton = dialogView.findViewById(R.id.btn_camera);
        Button galleryButton = dialogView.findViewById(R.id.btn_gallery);
        Button cancelButton = dialogView.findViewById(R.id.btn_cancel);
        
        cameraButton.setOnClickListener(v -> {
            dialog.dismiss();
            dispatchTakePictureIntent();
        });
        
        galleryButton.setOnClickListener(v -> {
            dialog.dismiss();
            openImagePicker();
        });
        
        cancelButton.setOnClickListener(v -> {
            dialog.dismiss();
        });
    }
    
    private void dispatchTakePictureIntent() {
        try {
            // First make sure we have camera permission
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) 
                    != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "App needs camera access", Toast.LENGTH_SHORT).show();
                checkAndRequestPermissions();
                return;
            }
            
            // Kiểm tra nếu đang chạy trên giả lập
            boolean isEmulator = Build.FINGERPRINT.contains("generic")
                    || Build.FINGERPRINT.contains("unknown")
                    || Build.MODEL.contains("google_sdk")
                    || Build.MODEL.contains("Emulator")
                    || Build.MODEL.contains("Android SDK built for x86")
                    || Build.MANUFACTURER.contains("Genymotion")
                    || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                    || "google_sdk".equals(Build.PRODUCT);
            
            if (isEmulator) {
                // Sử dụng intent đơn giản nhất trên giả lập
                simpleCameraCaptureForEmulator();
                return;
            }
            
            // Trên thiết bị thật, tiếp tục với code hiện tại
            boolean success = false;
            
            // Phương thức 1: Thử dùng ACTION_IMAGE_CAPTURE intent tiêu chuẩn
            try {
                Intent standardIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                
                // Kiểm tra xem có ứng dụng nào xử lý intent này không
                if (isIntentAvailable(standardIntent)) {
                    // Tạo file để lưu ảnh
                    File photoFile = createImageFile();
                    if (photoFile != null) {
                        photoURI = FileProvider.getUriForFile(
                                requireContext(),
                                "com.yourname.ssm.fileprovider",
                                photoFile);
                        
                        standardIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        startActivityForResult(standardIntent, REQUEST_IMAGE_CAPTURE);
                        success = true;
                        Log.d("SettingsFragment", "Launched camera with standard intent");
                    }
                }
            } catch (Exception e) {
                Log.e("SettingsFragment", "Error with standard camera intent: " + e.getMessage());
            }
            
            // Phương thức 2: Thử dùng intent ngầm định (implicit)
            if (!success) {
                try {
                    Intent implicitIntent = new Intent("android.media.action.IMAGE_CAPTURE");
                    if (isIntentAvailable(implicitIntent)) {
                        startActivityForResult(implicitIntent, REQUEST_IMAGE_CAPTURE);
                        success = true;
                        Log.d("SettingsFragment", "Launched camera with implicit intent");
                    }
                } catch (Exception e) {
                    Log.e("SettingsFragment", "Error with implicit camera intent: " + e.getMessage());
                }
            }
            
            // Phương thức 3: Thử mở camera package trực tiếp nếu biết
            if (!success) {
                try {
                    // Thử với một số package name phổ biến của camera
                    String[] commonCameraPackages = {
                            "com.sec.android.app.camera", // Samsung
                            "com.android.camera", // AOSP
                            "com.android.camera2", // AOSP newer
                            "com.huawei.camera", // Huawei
                            "com.sonyericsson.android.camera", // Sony
                            "com.miui.camera", // Xiaomi
                            "com.oneplus.camera", // OnePlus
                            "com.oppo.camera", // Oppo
                            "com.vivo.camera", // Vivo
                            "com.motorola.camera" // Motorola
                    };
                    
                    for (String packageName : commonCameraPackages) {
                        try {
                            Intent directIntent = new Intent();
                            directIntent.setPackage(packageName);
                            directIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                            
                            if (isIntentAvailable(directIntent)) {
                                startActivityForResult(directIntent, REQUEST_IMAGE_CAPTURE);
                                success = true;
                                Log.d("SettingsFragment", "Launched camera with direct package: " + packageName);
                                break;
                            }
                        } catch (Exception e) {
                            Log.e("SettingsFragment", "Error with direct package " + packageName + ": " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    Log.e("SettingsFragment", "Error trying direct camera packages: " + e.getMessage());
                }
            }
            
            // Nếu tất cả phương thức đều thất bại
            if (!success) {
                Log.e("SettingsFragment", "No camera apps found on this device after trying multiple methods");
                
                // Thử phương thức đơn giản nhất
                simpleCameraCaptureForEmulator();
            }
        } catch (Exception e) {
            Log.e("SettingsFragment", "Error launching camera: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error launching camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Phương thức chụp ảnh đơn giản cho giả lập
     */
    private void simpleCameraCaptureForEmulator() {
        try {
            // Intent đơn giản nhất để chụp ảnh
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // Không đặt các tham số phức tạp
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
            Log.d("SettingsFragment", "Đã mở camera với intent đơn giản");
        } catch (Exception e) {
            Log.e("SettingsFragment", "Error with simple camera intent: " + e.getMessage());
            Toast.makeText(requireContext(), "Chuyển sang chọn ảnh từ thư viện", Toast.LENGTH_SHORT).show();
            openImagePicker();
        }
    }

    private void dispatchTakeVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
        } else {
            Toast.makeText(requireContext(), "Không tìm thấy ứng dụng quay video", Toast.LENGTH_SHORT).show();
        }
    }
    
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        
        if (storageDir != null && !storageDir.exists()) {
            boolean created = storageDir.mkdirs();
            if (!created) {
                Log.e("SettingsFragment", "Failed to create directory: " + storageDir.getAbsolutePath());
            }
        }
        
        File image = File.createTempFile(
                imageFileName,   /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Chọn ảnh"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST) {
                if (data == null) {
                    Toast.makeText(requireContext(), "Error selecting image", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                try {
                    Uri imageUri = data.getData();
                    if (imageUri != null) {
                        // Đánh dấu đây là thao tác thủ công của người dùng
                        shouldShowAvatarUpdateToast = true;
                        processSelectedImage(imageUri);
                        hasManuallyChangedAvatar = true;
                    }
                } catch (Exception e) {
                    Log.e("SettingsFragment", "Error processing selected image", e);
                    Toast.makeText(requireContext(), "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                try {
                    Log.d("SettingsFragment", "Received image result, processing image...");
                    
                    // Đánh dấu đây là thao tác thủ công của người dùng
                    shouldShowAvatarUpdateToast = true;
                    
                    // Kiểm tra xem đang chạy trên giả lập không
                    boolean isEmulator = Build.FINGERPRINT.contains("generic")
                            || Build.FINGERPRINT.contains("unknown")
                            || Build.MODEL.contains("google_sdk")
                            || Build.MODEL.contains("Emulator")
                            || Build.MODEL.contains("Android SDK built for x86")
                            || Build.MANUFACTURER.contains("Genymotion")
                            || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                            || "google_sdk".equals(Build.PRODUCT);
                    
                    // Ưu tiên kiểm tra data từ intent (phương thức thường dùng trên giả lập)
                    if (data != null && data.getExtras() != null && data.getExtras().containsKey("data")) {
                        Log.d("SettingsFragment", "Processing image from thumbnail in intent data");
                        Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
                        if (thumbnail != null) {
                            // Áp dụng ảnh trực tiếp
                            Log.d("SettingsFragment", "Received thumbnail, size: " + 
                                    thumbnail.getWidth() + "x" + thumbnail.getHeight());
                            profileImageView.setImageBitmap(thumbnail);
                            saveImageToSharedPreferencesAndServer(thumbnail);
                            hasManuallyChangedAvatar = true;
                            return;
                        }
                    }
                    
                    // Phương thức 2: Kiểm tra URI đã lưu (thiết bị thật thường dùng)
                    if (photoURI != null) {
                        Log.d("SettingsFragment", "Processing image from URI: " + photoURI);
                        processSelectedImage(photoURI);
                        hasManuallyChangedAvatar = true;
                        return;
                    }
                    
                    // Phương thức 3: Kiểm tra file path
                    if (currentPhotoPath != null) {
                        Log.d("SettingsFragment", "Processing image from file path: " + currentPhotoPath);
                        File file = new File(currentPhotoPath);
                        if (file.exists()) {
                            Uri uri = Uri.fromFile(file);
                            processSelectedImage(uri);
                            hasManuallyChangedAvatar = true;
                            return;
                        } else {
                            Log.e("SettingsFragment", "File does not exist: " + currentPhotoPath);
                        }
                    }
                    
                    // Nếu không xử lý được ảnh
                    Toast.makeText(requireContext(), "Cannot process captured image, please try again", Toast.LENGTH_SHORT).show();
                    
                } catch (Exception e) {
                    Log.e("SettingsFragment", "Error processing captured image", e);
                    Toast.makeText(requireContext(), "Error processing captured image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == REQUEST_VIDEO_CAPTURE) {
                try {
                    // Đánh dấu đây là thao tác thủ công của người dùng
                    shouldShowAvatarUpdateToast = true;
                    
                    Uri videoUri = data.getData();
                    if (videoUri != null) {
                        // For video, we'll get a thumbnail to use as the avatar
                        Bitmap thumbnail = MediaStore.Video.Thumbnails.getThumbnail(
                                requireActivity().getContentResolver(),
                                Long.parseLong(videoUri.getLastPathSegment()),
                                MediaStore.Video.Thumbnails.MINI_KIND,
                                null);
                        
                        if (thumbnail != null) {
                            profileImageView.setImageBitmap(thumbnail);
                            saveImageToSharedPreferencesAndServer(thumbnail);
                            hasManuallyChangedAvatar = true;
                        }
                    }
                } catch (Exception e) {
                    Log.e("SettingsFragment", "Error processing video thumbnail", e);
                    Toast.makeText(requireContext(), "Error processing video: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == REQUEST_MANAGE_STORAGE_PERMISSION) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        showImagePickerDialog();
                    } else {
                        Toast.makeText(requireContext(), 
                            "Need storage permission to save image", 
                            Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(requireContext(), "Operation cancelled", Toast.LENGTH_SHORT).show();
        }
    }
    
    // Method to save bitmap to temporary file
    private File saveToTempFile(Bitmap bitmap) {
        try {
            File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_temp.jpg";
            File imageFile = new File(storageDir, imageFileName);
            
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
            
            return imageFile;
        } catch (Exception e) {
            Log.e("SettingsFragment", "Error saving bitmap to file: " + e.getMessage(), e);
            return null;
        }
    }
    
    private void processSelectedImage(Uri imageUri) {
        try {
            InputStream imageStream = requireActivity().getContentResolver().openInputStream(imageUri);
            Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
            
            // Resize the bitmap to save memory
            Bitmap resizedBitmap = resizeBitmap(selectedImage, 500);
            
            profileImageView.setImageBitmap(resizedBitmap);
            
            // Save the image to SharedPreferences and upload to database
            saveImageToSharedPreferencesAndServer(resizedBitmap);
            
            // Xóa thông báo ở đây vì nó sẽ được hiển thị trong saveImageToSharedPreferencesAndServer
        } catch (Exception e) {
            Log.e("SettingsFragment", "Error processing image", e);
            Toast.makeText(requireContext(), "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    // Method to save bitmap to SharedPreferences and upload to database
    private void saveImageToSharedPreferencesAndServer(final Bitmap bitmap) {
        // Đánh dấu là đã thay đổi ảnh đại diện thủ công
        manualAvatarUpdate = true;
        hasManuallyChangedAvatar = true;
        
        try {
            if (bitmap == null) {
                Log.e("SettingsFragment", "Bitmap null, cannot save and upload to database");
                return;
            }
            
            Log.d("SettingsFragment", "Starting to save avatar to SharedPreferences and sync with navigation drawer");
            
            // First save to SharedPreferences immediately on UI thread
            saveImageToSharedPreferences(bitmap);
            
            // Update the MainActivity's nav drawer
            updateMainActivityAvatar(bitmap);
            
            // Hiển thị thông báo CHỈ khi người dùng thực sự thao tác (từ camera/gallery)
            if (shouldShowAvatarUpdateToast) {
                Toast.makeText(requireContext(), "Avatar updated", Toast.LENGTH_SHORT).show();
                // Reset cờ để không hiển thị thông báo lần sau
                shouldShowAvatarUpdateToast = false;
            }
            
            // Then save to database in background to avoid UI blocking
            new Thread(() -> {
                try {
                    boolean dbSuccess = uploadImageToDatabase(bitmap);
                    if (!dbSuccess) {
                        // If database save failed, retry once
                        Log.d("SettingsFragment", "First save to database failed, retrying");
                        uploadImageToDatabase(bitmap);
                    }
                } catch (Exception e) {
                    Log.e("SettingsFragment", "Error in background thread for database upload", e);
                }
            }).start();
            
        } catch (Exception e) {
            Log.e("SettingsFragment", "Error saving and syncing image to database: " + e.getMessage(), e);
            if (shouldShowAvatarUpdateToast) {
                Toast.makeText(requireContext(), "Avatar saved but could not sync with database", Toast.LENGTH_SHORT).show();
                shouldShowAvatarUpdateToast = false;
            }
        }
    }
    
    // Method to upload image to database instead of server
    private boolean uploadImageToDatabase(Bitmap bitmap) {
        try {
            if (bitmap == null) {
                Log.e("SettingsFragment", "Bitmap null, cannot save to database");
                return false;
            }
            
            // Show progress on UI thread but only during manual update
            if (getActivity() != null && hasManuallyChangedAvatar) {
                getActivity().runOnUiThread(() -> {
                    // Không hiển thị toast ở đây nữa để tránh trùng lặp
                    // Toast.makeText(requireContext(), "Saving image to database...", Toast.LENGTH_SHORT).show();
                });
            }
            
            // Get user ID
            int userId = loginUserRepository.getUserId();
            if (userId <= 0) {
                Log.e("SettingsFragment", "Invalid user ID: " + userId);
                if (getActivity() != null && hasManuallyChangedAvatar) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Could not find user information", Toast.LENGTH_SHORT).show();
                    });
                }
                return false;
            }
            
            // Convert bitmap to base64 for storage
            String base64Image = bitmapToBase64(bitmap);
            if (base64Image == null) {
                Log.e("SettingsFragment", "Failed to convert image to base64");
                if (getActivity() != null && hasManuallyChangedAvatar) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Error processing image", Toast.LENGTH_SHORT).show();
                    });
                }
                return false;
            }
            
            // Save to SQLite database
            boolean success = userRepository.updateUserAvatar(userId, base64Image);
            
            // Chỉ hiển thị thông báo lỗi, không hiển thị thành công vì đã có thông báo từ saveImageToSharedPreferencesAndServer
            if (getActivity() != null && hasManuallyChangedAvatar && !success) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Error saving image to database", Toast.LENGTH_SHORT).show();
                });
            }
            
            return success;
            
        } catch (Exception e) {
            Log.e("SettingsFragment", "Error saving image to database: " + e.getMessage(), e);
            if (getActivity() != null && hasManuallyChangedAvatar) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
            return false;
        }
    }

    // Resize bitmap to save memory and improve performance
    private Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        float ratio = (float) width / (float) height;
        
        if (ratio > 1) {
            width = maxSize;
            height = (int) (width / ratio);
        } else {
            height = maxSize;
            width = (int) (height * ratio);
        }
        
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    // Password methods
    private void changePassword() {
        String currentPassword = currentPasswordEditText.getText().toString().trim();
        String newPassword = newPasswordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        
        // Validate inputs
        if (TextUtils.isEmpty(currentPassword)) {
            currentPasswordLayout.setError("Please enter current password");
            return;
        } else {
            currentPasswordLayout.setError(null);
        }
        
        if (TextUtils.isEmpty(newPassword)) {
            newPasswordLayout.setError("Please enter new password");
            return;
        } else {
            newPasswordLayout.setError(null);
        }
        
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordLayout.setError("Please confirm new password");
            return;
        } else {
            confirmPasswordLayout.setError(null);
        }
        
        if (!newPassword.equals(confirmPassword)) {
            confirmPasswordLayout.setError("Passwords do not match");
            return;
        } else {
            confirmPasswordLayout.setError(null);
        }
        
        // Verify current password
        String email = loginUserRepository.getEmail();
        if (!loginUserRepository.authenticateUser(email, currentPassword)) {
            currentPasswordLayout.setError("Current password is incorrect");
            return;
        }
        
        // In a real app, you would update the password in the database
        // For testing purposes, we'll just show a success message
        Toast.makeText(requireContext(), "Password updated successfully", Toast.LENGTH_SHORT).show();
        
        // Clear password fields
        currentPasswordEditText.setText("");
        newPasswordEditText.setText("");
        confirmPasswordEditText.setText("");
    }

    // Budget methods
    private void loadBudgetData() {
        try {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH) + 1; // Calendar months are 0-based
            
            // Get current budget
            if (budgetRepository != null && userId > 0) {
                currentBudget = budgetRepository.getBudgetForMonth(userId, year, month);
            }
        } catch (Exception e) {
            // Log hoặc xử lý ngoại lệ
        }
    }

    private void updateBudget() {
        String budgetAmountStr = budgetAmountEditText.getText().toString().trim();
        
        if (TextUtils.isEmpty(budgetAmountStr)) {
            budgetAmountLayout.setError("Please enter budget amount");
            return;
        } else {
            budgetAmountLayout.setError(null);
        }
        
        try {
            double newBudgetAmount = Double.parseDouble(budgetAmountStr);
            
            if (newBudgetAmount < 0) {
                budgetAmountLayout.setError("Budget cannot be negative");
                return;
            }
            
            // Update budget
            currentBudget.setLimit(newBudgetAmount);
            budgetRepository.updateBudget(currentBudget);
            
            // Update UI
            String formattedBudget = currencyFormatter.format(newBudgetAmount);
            currentBudgetTextView.setText(formattedBudget);
            
            // Clear input and show success message
            budgetAmountEditText.setText("");
            Toast.makeText(requireContext(), "Budget updated successfully", Toast.LENGTH_SHORT).show();
            
            // Thêm logic để quay lại Dashboard sau khi cập nhật (tùy chọn)
            new Handler().postDelayed(() -> {
                try {
                    // Cập nhật Dashboard trước khi quay về
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).refreshDashboard();
                    }
                    
                    // Quay lại fragment trước đó
                    requireActivity().getSupportFragmentManager().popBackStack();
                } catch (Exception e) {
                    // Log hoặc xử lý ngoại lệ nếu cần
                }
            }, 1000); // Delay 1 giây để người dùng đọc thông báo
            
        } catch (NumberFormatException e) {
            budgetAmountLayout.setError("Invalid number format");
        }
    }

    // Theme methods
    private void loadThemeSettings() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        String currentTheme = sharedPreferences.getString("theme", "light");
        
        // Select appropriate radio button
        if ("dark".equals(currentTheme)) {
            darkThemeRadioButton.setChecked(true);
        } else {
            lightThemeRadioButton.setChecked(true);
        }
    }

    private void applyTheme() {
        boolean isDarkTheme = darkThemeRadioButton.isChecked();
        
        // Save theme preference
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("theme", isDarkTheme ? "dark" : "light");
        editor.apply();
        
        // Apply theme
        int nightMode = isDarkTheme ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        AppCompatDelegate.setDefaultNightMode(nightMode);
        
        // Show message
        Toast.makeText(requireContext(), "Theme changed", Toast.LENGTH_SHORT).show();
    }

    // Logout method
    private void performLogout() {
        // Log out
        loginUserRepository.logout();
        
        // Return to login screen
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    // Thêm phương thức cập nhật UI dựa trên dữ liệu người dùng đã được tải
    private void updateUserUI() {
        if (!isAdded() || userNameTextView == null || userEmailTextView == null) return;
        
        try {
            // Tắt cờ hiển thị toast khi tải UI
            boolean originalToastFlag = shouldShowAvatarUpdateToast;
            shouldShowAvatarUpdateToast = false;
            
            // Set data to views
            String email = loginUserRepository.getEmail();
            userEmailTextView.setText(email);
            
            // Get user ID
            int userId = loginUserRepository.getUserId();
            
            // Load avatar from database
            String base64Avatar = userRepository.getUserAvatar(userId);
            if (base64Avatar != null && !base64Avatar.isEmpty()) {
                // If there's an avatar in the database, use it
                Bitmap avatarBitmap = base64ToBitmap(base64Avatar);
                if (avatarBitmap != null) {
                    profileImageView.setImageBitmap(avatarBitmap);
                    
                    // Also update the navigation drawer
                    if (getActivity() instanceof MainActivity) {
                        MainActivity mainActivity = (MainActivity) getActivity();
                        mainActivity.updateNavigationDrawerAvatar(avatarBitmap, null, null);
                    }
                    
                    Log.d("SettingsFragment", "Loaded avatar from database for user ID: " + userId);
                    shouldShowAvatarUpdateToast = false; // Đảm bảo không hiển thị toast
                    return;
                }
            }
            
            // If no avatar in database, set default based on role
            String role = loginUserRepository.getRole();
            if ("1".equals(role)) {
                profileImageView.setImageResource(R.drawable.ic_admin_avatar);
            } else {
                profileImageView.setImageResource(R.drawable.ic_student_avatar);
            }
            
            Log.d("SettingsFragment", "Using default avatar for user role: " + role);
            
            // Khôi phục lại giá trị cờ ban đầu
            shouldShowAvatarUpdateToast = originalToastFlag;
            
        } catch (Exception e) {
            Log.e("SettingsFragment", "Error loading user UI", e);
            shouldShowAvatarUpdateToast = false; // Đảm bảo tắt cờ khi có lỗi
        }
    }
    
    // Chuyển đổi chuỗi Base64 thành Bitmap
    private Bitmap base64ToBitmap(String base64String) {
        if (TextUtils.isEmpty(base64String)) return null;
        
        try {
            byte[] decodedString = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        } catch (Exception e) {
            Log.e("SettingsFragment", "Error decoding base64 to bitmap: " + e.getMessage());
            return null;
        }
    }

    // Thêm phương thức cập nhật UI ngân sách
    private void updateBudgetUI() {
        if (!isAdded() || currentMonthTextView == null || currentBudgetTextView == null) return;
        
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH));
            String formattedMonth = dateFormatter.format(calendar.getTime());
            currentMonthTextView.setText(formattedMonth);
            
            if (currentBudget != null) {
                String formattedBudget = currencyFormatter.format(currentBudget.getLimit());
                currentBudgetTextView.setText(formattedBudget);
            }
            
            // Ẩn thông báo lỗi camera nếu có
            hideCameraErrorBubble();
        } catch (Exception e) {
            // Log hoặc xử lý ngoại lệ
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Giải phóng tài nguyên
        profileImageView = null;
        userNameTextView = null;
        userEmailTextView = null;
        currentMonthTextView = null;
        currentBudgetTextView = null;
        
        loginUserRepository = null;
        userRepository = null;
        budgetRepository = null;
        
        currencyFormatter = null;
        dateFormatter = null;
    }

    // Lưu ảnh vào SharedPreferences
    private void saveImageToSharedPreferences(Bitmap bitmap) {
        try {
            if (bitmap == null) {
                Log.e("SettingsFragment", "Bitmap null, cannot save to SharedPreferences");
                return;
            }
            
            Log.d("SettingsFragment", "Starting to save avatar to SharedPreferences and sync with navigation drawer");
            
            // Lấy SharedPreferences
            SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("app_preferences", Context.MODE_PRIVATE);
            
            // Đọc các giá trị hiện tại trước
            String currentUsername = sharedPreferences.getString("username", null);
            String currentEmail = sharedPreferences.getString("email", null);
            
            // Ghi log thông tin hiện tại
            Log.d("SettingsFragment", "Current state before saving - Username: " + 
                  (currentUsername != null ? currentUsername : "null") + ", Email: " + 
                  (currentEmail != null ? currentEmail : "null"));
            
            // Lưu vào SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            
            String encodedImage = bitmapToBase64(bitmap);
            if (encodedImage != null) {
                // Lưu với cả hai key để đảm bảo đồng bộ
                editor.putString(KEY_PROFILE_IMAGE, encodedImage);
                editor.putString("profile_image", encodedImage); // Key được dùng trong MainActivity
                
                // QUAN TRỌNG: KHÔNG ghi đè username và email - chỉ cập nhật avatar
                // Nếu username và email đã tồn tại, giữ nguyên, không thay đổi
                
                boolean success = editor.commit(); // Sử dụng commit thay vì apply để đảm bảo lưu ngay lập tức
                Log.d("SettingsFragment", "Saved image to SharedPreferences: " + (success ? "success" : "failure"));
            
                // Cập nhật avatar trong MainActivity (navigation drawer) nhưng không hiển thị toast
                if (getActivity() instanceof MainActivity) {
                    // Tạm thời vô hiệu hóa cờ shouldShowAvatarUpdateToast
                    boolean originalToastFlag = shouldShowAvatarUpdateToast;
                    shouldShowAvatarUpdateToast = false;
                    
                    MainActivity mainActivity = (MainActivity) getActivity();
                    
                    // Gọi phương thức cập nhật avatar mà không thay đổi username và email
                    // Truyền null cho username và email để giữ nguyên giá trị hiện tại
                    mainActivity.updateNavigationDrawerAvatar(bitmap, null, null);
                    
                    // Khôi phục lại giá trị cờ
                    shouldShowAvatarUpdateToast = originalToastFlag;
                    
                    Log.d("SettingsFragment", "Updated avatar in navigation drawer, keeping existing username and email");
                    
                    // Debug: Kiểm tra xem cập nhật đã thành công chưa
                    try {
                        NavigationView navigationView = mainActivity.findViewById(R.id.navigation_view);
                        if (navigationView != null) {
                            View headerView = navigationView.getHeaderView(0);
                            if (headerView != null) {
                                ImageView navAvatar = headerView.findViewById(R.id.header_avatar);
                                TextView navUsername = headerView.findViewById(R.id.header_username);
                                TextView navEmail = headerView.findViewById(R.id.header_email);
                                
                                Log.d("SettingsFragment", "After update - Username: " + 
                                      (navUsername != null ? navUsername.getText() : "null") + 
                                      ", Email: " + (navEmail != null ? navEmail.getText() : "null"));
                            }
                        }
                    } catch (Exception e) {
                        Log.e("SettingsFragment", "Error checking navigation drawer after update", e);
                    }
                } else {
                    Log.e("SettingsFragment", "Activity is not MainActivity");
                }
            } else {
                Log.e("SettingsFragment", "Could not encode bitmap to base64");
            }
        } catch (Exception e) {
            Log.e("SettingsFragment", "Error saving image to SharedPreferences: " + e.getMessage(), e);
        }
    }

    // Chuyển đổi Bitmap thành chuỗi Base64
    private String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return null;
        
        try {
            java.io.ByteArrayOutputStream byteArrayOutputStream = new java.io.ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            return android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT);
        } catch (Exception e) {
            Log.e("SettingsFragment", "Error encoding bitmap to base64: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // Vô hiệu hóa cờ để không hiển thị toast khi onResume được gọi
        shouldShowAvatarUpdateToast = false;
        
        // Chỉ tải dữ liệu khi lần đầu khởi động hoặc khi thực sự cần thiết
        if (isInitialLoad) {
            isInitialLoad = false;
            loadUserFullData();
        } else if (!hasManuallyChangedAvatar) {
            // Nếu không phải lần đầu và không có thay đổi thủ công, không cập nhật avatar
            loadUserDataExceptAvatar();
        }
        
        // Luôn tải dữ liệu ngân sách vì nó có thể thay đổi
        new Thread(() -> {
            try {
                loadBudgetData();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> updateBudgetUI());
                }
            } catch (Exception e) {
                Log.e("SettingsFragment", "Error loading budget data in onResume", e);
            }
        }).start();
    }
    
    private void loadUserFullData() {
        new Thread(() -> {
            try {
                if (getActivity() != null) {
                    User user = userRepository.getUserById(userId);
                    if (user != null) {
                        getActivity().runOnUiThread(() -> {
                            // Vô hiệu hóa cờ trước khi cập nhật UI để ngăn hiển thị toast
                            shouldShowAvatarUpdateToast = false;
                            
                            updateAllUserFields(user);
                            
                            // Cập nhật cả avatar từ database
                            String base64Avatar = userRepository.getUserAvatar(userId);
                            if (base64Avatar != null && !base64Avatar.isEmpty()) {
                                Bitmap avatarBitmap = base64ToBitmap(base64Avatar);
                                if (avatarBitmap != null) {
                                    profileImageView.setImageBitmap(avatarBitmap);
                                }
                            }
                        });
                    }
                }
            } catch (Exception e) {
                Log.e("SettingsFragment", "Error loading full user data", e);
            }
        }).start();
    }
    
    private void loadUserDataExceptAvatar() {
        new Thread(() -> {
            try {
                if (getActivity() != null) {
                    User user = userRepository.getUserById(userId);
                    if (user != null) {
                        getActivity().runOnUiThread(() -> {
                            updateAllUserFields(user);
                        });
                    }
                }
            } catch (Exception e) {
                Log.e("SettingsFragment", "Error loading user data except avatar", e);
            }
        }).start();
    }
    
    private void updateAllUserFields(User user) {
        // Cập nhật tất cả các trường thông tin người dùng
        if (userNameTextView != null) userNameTextView.setText(user.getName());
        if (userEmailTextView != null) userEmailTextView.setText(user.getEmail());
        if (nameEditText != null) nameEditText.setText(user.getName());
        if (phoneEditText != null) phoneEditText.setText(user.getPhone());
        if (addressEditText != null) addressEditText.setText(user.getAddress());
                                
        // Cập nhật giới tính
        if (maleRadioButton != null && femaleRadioButton != null) {
            if ("Male".equals(user.getGender())) {
                maleRadioButton.setChecked(true);
            } else if ("Female".equals(user.getGender())) {
                femaleRadioButton.setChecked(true);
            }
        }
    }

    // Update MainActivity with the new avatar
    private void updateMainActivityAvatar(Bitmap bitmap) {
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            String username = loginUserRepository.getUsername();
            String email = loginUserRepository.getEmail();
            
            // Vô hiệu hóa cờ shouldShowAvatarUpdateToast tạm thời khi cập nhật từ MainActivity
            boolean originalToastFlag = shouldShowAvatarUpdateToast;
            shouldShowAvatarUpdateToast = false;
            
            mainActivity.updateNavigationDrawerAvatar(bitmap, username, email);
            
            // Khôi phục lại giá trị cờ
            shouldShowAvatarUpdateToast = originalToastFlag;
        }
    }

    private void showImageSourcePicker() {
        // Use the dialog_image_picker.xml layout instead of AlertDialog
        showImagePickerDialog();
    }

    private void changeAvatarAction() {
        // Hiển thị dialog chọn camera hoặc gallery
        showImagePickerDialog();
    }

    /**
     * Kiểm tra xem intent có khả dụng không
     */
    private boolean isIntentAvailable(Intent intent) {
        PackageManager packageManager = requireActivity().getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(
                intent, PackageManager.MATCH_DEFAULT_ONLY);
        return activities.size() > 0;
    }

    /**
     * Tìm và ẩn thông báo lỗi camera nếu có trong giao diện
     */
    private void hideCameraErrorBubble() {
        try {
            if (getView() == null) return;
            
            // Lặp qua tất cả các thành phần trong layout
            ViewGroup rootView = (ViewGroup) getView();
            removeErrorMessages(rootView);
            
        } catch (Exception e) {
            Log.e("SettingsFragment", "Error hiding camera error bubble", e);
        }
    }
    
    /**
     * Đệ quy tìm và xóa tất cả thông báo lỗi camera
     */
    private void removeErrorMessages(ViewGroup viewGroup) {
        try {
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                
                if (child instanceof ViewGroup) {
                    // Nếu là ViewGroup, duyệt đệ quy
                    removeErrorMessages((ViewGroup) child);
                } 
                else if (child instanceof TextView) {
                    // Nếu là TextView, kiểm tra nội dung
                    TextView textView = (TextView) child;
                    String text = textView.getText().toString();
                    if (text.contains("Could not find camera app")) {
                        textView.setVisibility(View.GONE);
                        Log.d("SettingsFragment", "Hidden camera error message");
                    }
                }
            }
        } catch (Exception e) {
            Log.e("SettingsFragment", "Error in removeErrorMessages", e);
        }
    }

    // Notification methods
    private void loadNotificationSettings() {
        if (!isAdded() || getContext() == null) return;
        
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("notification_settings", Context.MODE_PRIVATE);
        isAppNotificationEnabled = sharedPreferences.getBoolean("app_notifications_enabled", true);
        
        if (appNotificationSwitch != null) {
            appNotificationSwitch.setChecked(isAppNotificationEnabled);
        }
    }
    
    private void saveNotificationSettings(boolean isEnabled) {
        try {
            isAppNotificationEnabled = isEnabled;
            SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("notification_settings", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("app_notifications_enabled", isEnabled);
            editor.apply();
            
            // Update UI and show appropriate message
            if (!isEnabled) {
                Toast.makeText(requireContext(),
                        "App notifications and email disabled. You won't receive notifications via Gmail.",
                        Toast.LENGTH_SHORT).show();
                
                // Disable email settings when notifications are turned off
                settingEmail.setEnabled(false);
                settingEmail.setAlpha(0.5f);
            } else {
                Toast.makeText(requireContext(),
                        "App notifications enabled. You can receive notifications via Gmail if set up.",
                        Toast.LENGTH_SHORT).show();
                
                // Enable email settings when notifications are turned on
                settingEmail.setEnabled(true);
                settingEmail.setAlpha(1.0f);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving notification settings", e);
        }
    }
    
    private void showEmailSettingsDialog() {
        if (!isAdded() || getContext() == null) return;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_email_input, null);
        builder.setView(dialogView);
        
        final com.google.android.material.textfield.TextInputLayout emailInputLayout = 
                dialogView.findViewById(R.id.email_input_layout);
        final com.google.android.material.textfield.TextInputEditText emailEditText = 
                dialogView.findViewById(R.id.email_edit_text);
        
        // Load current email if available
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String currentEmail = sharedPreferences.getString("user_email", "");
        emailEditText.setText(currentEmail);
        
        builder.setTitle("Notification Email Settings")
               .setPositiveButton("Save", null) // Set in the show() method to prevent dialog from closing on error
               .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        
        final AlertDialog dialog = builder.create();
        dialog.show();
        
        // Override the positive button to validate before closing
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            
            if (email.isEmpty()) {
                emailInputLayout.setError("Please enter email");
                return;
            }
            
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInputLayout.setError("Invalid email format");
                return;
            }
            
            // Save the email
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("user_email", email);
            editor.apply();
            
            Toast.makeText(requireContext(), "Email saved", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }
} 