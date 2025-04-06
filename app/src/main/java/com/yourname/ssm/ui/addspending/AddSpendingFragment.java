package com.yourname.ssm.ui.addspending;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.GridLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputLayout;
import com.yourname.ssm.R;
import com.yourname.ssm.model.Category;
import com.yourname.ssm.model.Transaction;
import com.yourname.ssm.repository.CategoryRepository;
import com.yourname.ssm.repository.LoginUserRepository;
import com.yourname.ssm.repository.TransactionRepository;
import com.yourname.ssm.MainActivity;
import com.yourname.ssm.database.DatabaseContract;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AddSpendingFragment extends Fragment {
    private static final String TAG = "AddSpendingFragment";
    
    // UI Components
    private RadioGroup transactionTypeRadioGroup;
    private RadioButton expenseRadioButton, incomeRadioButton;
    private TextInputLayout amountInputLayout, dateInputLayout, noteInputLayout;
    private EditText amountEditText, dateEditText, noteEditText;
    private GridLayout categoryGrid;
    private Button saveButton;
    
    // Data
    private CategoryRepository categoryRepository;
    private TransactionRepository transactionRepository;
    private LoginUserRepository loginUserRepository;
    private List<Category> expenseCategories = new ArrayList<>();
    private List<Category> incomeCategories = new ArrayList<>();
    private Category selectedCategory;
    private Calendar selectedDate;
    private int userId;
    
    // Formatters
    private SimpleDateFormat dateFormat;
    private NumberFormat currencyFormat;
    
    // Thread safety
    private final AtomicBoolean isLoading = new AtomicBoolean(false);
    private volatile boolean isDestroyed = false;
    
    // Executor for background tasks - sử dụng một executor duy nhất
    private static final Executor executor = Executors.newSingleThreadExecutor();
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Khởi tạo formatters
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        
        View view = inflater.inflate(R.layout.fragment_add_spending, container, false);
        
        try {
            // Initialize basic views
            initViews(view);
            initRepositories();
            
            // Setup initial UI state
            if (selectedDate == null) {
                selectedDate = Calendar.getInstance();
                if (dateEditText != null) {
                    dateEditText.setText(dateFormat.format(selectedDate.getTime()));
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreateView", e);
            Toast.makeText(requireContext(), "An error occurred while loading the view", Toast.LENGTH_SHORT).show();
        }
        
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated called");
        
        try {
            // Reset destroyed flag
            isDestroyed = false;
            
            // Khởi tạo formatters trước
            dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            
            // Khởi tạo calendar với ngày hiện tại
            selectedDate = Calendar.getInstance();
            
            // Ánh xạ views trước khi làm bất cứ việc gì khác
            initViews(view);
            
            // Hiển thị ngày hiện tại
            if (dateEditText != null) {
                dateEditText.setText(dateFormat.format(selectedDate.getTime()));
            }
            
            // Setup UI và listeners trước khi tải dữ liệu từ database
            setupDatePicker();
            setupTransactionTypeRadioGroup();
            setupSaveButton();
            setupCategoryClickListeners();
                
            // Khởi tạo repositories và tải dữ liệu từ database sau cùng
            initRepositories();

            // Tải danh mục trực tiếp, không gọi resetAndLoadCategories để tránh xóa danh mục hiện có
            loadCategories();
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing fragment", e);
            safeShowToast("Error initializing: " + e.getMessage(), Toast.LENGTH_SHORT);
            safePopBackStack();
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        
        try {
            // No need to set LayoutManager for GridLayout since we're using the XML defined one
            setupCategoryClickListeners();
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume", e);
        }
    }
    
    private void initRepositories() {
        Log.d(TAG, "Initializing repositories");
        try {
            // Khởi tạo repositories
            categoryRepository = new CategoryRepository(requireContext());
            transactionRepository = new TransactionRepository(requireContext());
            loginUserRepository = new LoginUserRepository(requireContext());
            
            // Lấy userId
            userId = loginUserRepository.getUserId();
            Log.d(TAG, "Got user ID: " + userId);
            
            if (userId <= 0) {
                safeShowToast("Please login to use this feature", Toast.LENGTH_LONG);
                safePopBackStack();
                return;
            }
            
            // Tải danh mục từ DB
            if (!isDestroyed) {
                isLoading.set(true);
                loadCategories();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing repositories", e);
            safeShowToast("Error loading data: " + e.getMessage(), Toast.LENGTH_SHORT);
            safePopBackStack();
        }
    }
    
    private void initViews(View view) {
        try {
        transactionTypeRadioGroup = view.findViewById(R.id.transactionTypeRadioGroup);
        expenseRadioButton = view.findViewById(R.id.expenseRadioButton);
        incomeRadioButton = view.findViewById(R.id.incomeRadioButton);
        
        amountInputLayout = view.findViewById(R.id.amountInputLayout);
        dateInputLayout = view.findViewById(R.id.dateInputLayout);
        noteInputLayout = view.findViewById(R.id.noteInputLayout);
        
        amountEditText = view.findViewById(R.id.amountEditText);
        dateEditText = view.findViewById(R.id.dateEditText);
        noteEditText = view.findViewById(R.id.noteEditText);
        
        categoryGrid = view.findViewById(R.id.categoryGrid);
        saveButton = view.findViewById(R.id.saveButton);
        
            Log.d(TAG, "Views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            throw e; // Re-throw to be caught by caller
        }
    }
    
    private void loadCategories() {
        if (isDestroyed) {
            Log.d(TAG, "Fragment destroyed, skipping loadCategories");
            return;
        }
        
        try {
            // Tải danh mục trong background thread
            executor.execute(() -> {
                try {
                    if (isDestroyed) return;
                    
                    Log.d(TAG, "Starting to load categories");
                    
        // Load danh mục chi tiêu
                    List<Category> expenses = categoryRepository.getCategoriesByType(0);
                    Log.d(TAG, "Loaded " + (expenses != null ? expenses.size() : 0) + " expense categories from repository");
                    
                    // Nếu không có danh mục, tạo danh mục mặc định
                    if (expenses == null || expenses.isEmpty()) {
                        Log.d(TAG, "No expense categories found, creating default ones");
                        
                        // Tạo danh mục mặc định
                        expenses = new ArrayList<>();
                        expenses.add(new Category(1, "Ăn uống", R.drawable.ic_category_food, 0));
                        expenses.add(new Category(2, "Di chuyển", R.drawable.ic_category_transport, 0));
                        expenses.add(new Category(3, "Mua sắm", R.drawable.ic_category_shopping, 0));
                        expenses.add(new Category(4, "Giải trí", R.drawable.ic_category_entertainment, 0));
                        expenses.add(new Category(5, "Y tế", R.drawable.ic_category_health, 0));
                        expenses.add(new Category(6, "Giáo dục", R.drawable.ic_category_education, 0));
                        expenses.add(new Category(7, "Nhà ở", R.drawable.ic_category_housing, 0));
                        expenses.add(new Category(8, "Du lịch", R.drawable.ic_category_travel, 0));
                        expenses.add(new Category(9, "Cafe & Trà", R.drawable.ic_category_coffee, 0));
                        expenses.add(new Category(10, "Tiện ích", R.drawable.ic_category_utilities, 0));
                        
                        // Lưu các danh mục vào database
                        for (Category category : expenses) {
                            categoryRepository.addCategory(category);
                        }
        }
        
        // Load danh mục thu nhập
                    List<Category> incomes = categoryRepository.getCategoriesByType(1);
                    Log.d(TAG, "Loaded " + (incomes != null ? incomes.size() : 0) + " income categories from repository");
                    
                    // Nếu không có danh mục, tạo danh mục mặc định
                    if (incomes == null || incomes.isEmpty()) {
                        Log.d(TAG, "No income categories found, creating default ones");
                        
                        // Tạo danh mục mặc định
                        incomes = new ArrayList<>();
                        incomes.add(new Category(11, "Lương", R.drawable.ic_category_salary, 1));
                        incomes.add(new Category(12, "Đầu tư", R.drawable.ic_category_investment, 1));
                        incomes.add(new Category(13, "Quà tặng", R.drawable.ic_category_gift, 1));
                        incomes.add(new Category(14, "Học bổng", R.drawable.ic_category_education, 1));
                        incomes.add(new Category(15, "Bán hàng", R.drawable.ic_category_shopping, 1));
                        incomes.add(new Category(16, "Thưởng", R.drawable.ic_category_gift, 1));
                        incomes.add(new Category(17, "Cho vay", R.drawable.ic_category_loan, 1));
                        incomes.add(new Category(18, "Hoàn tiền", R.drawable.ic_category_tech, 1));
                        incomes.add(new Category(19, "Thu nhập phụ", R.drawable.ic_category_utilities, 1));
                        incomes.add(new Category(20, "Dịch vụ", R.drawable.ic_category_tech, 1));
                        
                        // Lưu các danh mục vào database
                        for (Category category : incomes) {
                            categoryRepository.addCategory(category);
                        }
                    }
                    
                    // Lưu lại danh sách
                    final List<Category> finalExpenses = expenses;
                    final List<Category> finalIncomes = incomes;
                    
                    // Kiểm tra lại một lần nữa để đảm bảo không cập nhật UI nếu fragment đã bị hủy
                    if (isDestroyed) return;
                    
                    // Chuyển về main thread để cập nhật UI
                    postToMainThread(() -> {
                        if (isDestroyed) return;
                        
                        try {
                            if (isDestroyed) return;
                            
                            // Lưu lại danh sách
                            expenseCategories.clear();
                            expenseCategories.addAll(finalExpenses);
                            
                            incomeCategories.clear();
                            incomeCategories.addAll(finalIncomes);
                            
                            Log.d(TAG, "Categories loaded to UI. Expenses: " + expenseCategories.size() + 
                                   ", Incomes: " + incomeCategories.size());
                            
                            // Khởi tạo adapters
                            setupCategoryAdapters();
                            isLoading.set(false);
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating UI after loading categories", e);
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error in loadCategories background task", e);
                    if (!isDestroyed) {
                        postToMainThread(() -> 
                            safeShowToast("Error loading categories: " + e.getMessage(), Toast.LENGTH_SHORT));
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error executing loadCategories", e);
            if (!isDestroyed) {
                safeShowToast("Error loading categories: " + e.getMessage(), Toast.LENGTH_SHORT);
            }
        }
    }
    
    private void setupCategoryAdapters() {
        if (isDestroyed || getContext() == null || categoryGrid == null) {
            Log.d(TAG, "Skipping setupCategoryAdapters: Fragment destroyed or views null");
            return;
        }
        
        // The category UI is now statically defined in XML, so we just need to set up click handlers
        setupCategoryClickListeners();
        
        // Set default selected category (first one)
        if (categoryGrid.getChildCount() > 0) {
            handleCategorySelection(0);
        }
    }
    
    private void setupDatePicker() {
        if (isDestroyed || dateEditText == null) return;
        
        try {
        dateEditText.setOnClickListener(v -> {
                if (isDestroyed) return;
                
                try {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                                try {
                                    if (isDestroyed) return;
                                    
                        selectedDate.set(Calendar.YEAR, year);
                        selectedDate.set(Calendar.MONTH, month);
                        selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        dateEditText.setText(dateFormat.format(selectedDate.getTime()));
                                } catch (Exception e) {
                                    Log.e(TAG, "Error setting date", e);
                                }
                    },
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
                } catch (Exception e) {
                    Log.e(TAG, "Error showing date picker", e);
                    safeShowToast("Error showing calendar", Toast.LENGTH_SHORT);
                }
        });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up date picker", e);
        }
    }
    
    private void setupTransactionTypeRadioGroup() {
        try {
            transactionTypeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                if (isDestroyed || categoryGrid == null) return;
                
                try {
                    if (checkedId == R.id.expenseRadioButton) {
                        // Switch to expense categories
                        Log.d(TAG, "Selected expense categories tab");
                        
                        // Since we're using static GridLayout, we don't need to swap adapters
                        // Just update the UI to reflect expense categories
                        updateCategoryUI(true);
                        
                    } else if (checkedId == R.id.incomeRadioButton) {
                        // Switch to income categories
                        Log.d(TAG, "Selected income categories tab");
                        
                        // Since we're using static GridLayout, we don't need to swap adapters
                        // Just update the UI to reflect income categories
                        updateCategoryUI(false);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in radio group listener", e);
                }
            });
            
            // Initialize with expense selected
            expenseRadioButton.setChecked(true);
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up transaction type radio group", e);
        }
    }
    
    // New method to update category UI based on transaction type
    private void updateCategoryUI(boolean isExpense) {
        try {
            if (categoryGrid == null || categoryGrid.getChildCount() == 0) {
                Log.e(TAG, "Cannot update categories: Grid is null or empty");
                return;
            }
            
            // Get reference to all child views
            List<View> categoryViews = new ArrayList<>();
            for (int i = 0; i < categoryGrid.getChildCount(); i++) {
                categoryViews.add(categoryGrid.getChildAt(i));
            }
            
            // First hide all category items
            for (View view : categoryViews) {
                view.setVisibility(View.GONE);
            }
            
            if (isExpense) {
                // Set up expense categories
                setupExpenseCategories(categoryViews);
            } else {
                // Set up income categories
                setupIncomeCategories(categoryViews);
            }
            
            // Reset all backgrounds
            for (View categoryItem : categoryViews) {
                if (categoryItem instanceof LinearLayout) {
                    categoryItem.setBackgroundResource(android.R.color.transparent);
                }
            }
            
            // Select the first visible category by default
            for (int i = 0; i < categoryViews.size(); i++) {
                if (categoryViews.get(i).getVisibility() == View.VISIBLE) {
                    handleCategorySelection(i);
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating category UI", e);
        }
    }
    
    private void setupExpenseCategories(List<View> categoryViews) {
        // Only show the first 10 categories which are expense categories
        for (int i = 0; i < Math.min(10, categoryViews.size()); i++) {
            View view = categoryViews.get(i);
            view.setVisibility(View.VISIBLE);
            
            if (view instanceof LinearLayout) {
                LinearLayout layout = (LinearLayout) view;
                
                // Get the image and text views
                ImageView iconView = (ImageView) layout.getChildAt(0);
                TextView textView = (TextView) layout.getChildAt(1);
                
                // Set the appropriate icon and text based on index
                switch (i) {
                    case 0: // Food
                        iconView.setImageResource(R.drawable.ic_category_food);
                        iconView.setTag(R.drawable.ic_category_food);
                        textView.setText(R.string.category_food);
                        break;
                    case 1: // Transport
                        iconView.setImageResource(R.drawable.ic_category_transport);
                        iconView.setTag(R.drawable.ic_category_transport);
                        textView.setText(R.string.category_transport);
                        break;
                    case 2: // Shopping
                        iconView.setImageResource(R.drawable.ic_category_shopping);
                        iconView.setTag(R.drawable.ic_category_shopping);
                        textView.setText(R.string.category_shopping);
                        break;
                    case 3: // Entertainment
                        iconView.setImageResource(R.drawable.ic_category_entertainment);
                        iconView.setTag(R.drawable.ic_category_entertainment);
                        textView.setText(R.string.category_entertainment);
                        break;
                    case 4: // Health
                        iconView.setImageResource(R.drawable.ic_category_health);
                        iconView.setTag(R.drawable.ic_category_health);
                        textView.setText(R.string.category_health);
                        break;
                    case 5: // Education
                        iconView.setImageResource(R.drawable.ic_category_education);
                        iconView.setTag(R.drawable.ic_category_education);
                        textView.setText(R.string.category_education);
                        break;
                    case 6: // Housing
                        iconView.setImageResource(R.drawable.ic_category_housing);
                        iconView.setTag(R.drawable.ic_category_housing);
                        textView.setText(R.string.category_housing);
                        break;
                    case 7: // Travel
                        iconView.setImageResource(R.drawable.ic_category_travel);
                        iconView.setTag(R.drawable.ic_category_travel);
                        textView.setText(R.string.category_travel);
                        break;
                    case 8: // Cafe & Tea
                        iconView.setImageResource(R.drawable.ic_category_coffee);
                        iconView.setTag(R.drawable.ic_category_coffee);
                        textView.setText(R.string.category_cafe);
                        break;
                    case 9: // Utilities
                        iconView.setImageResource(R.drawable.ic_category_utilities);
                        iconView.setTag(R.drawable.ic_category_utilities);
                        textView.setText(R.string.category_utilities);
                        break;
                }
                
                // Ensure icons are black
                iconView.setColorFilter(getResources().getColor(R.color.black));
            }
        }
    }
    
    private void setupIncomeCategories(List<View> categoryViews) {
        // Only show the first 10 categories which will be used for income categories
        for (int i = 0; i < Math.min(10, categoryViews.size()); i++) {
            View view = categoryViews.get(i);
            view.setVisibility(View.VISIBLE);
            
            if (view instanceof LinearLayout) {
                LinearLayout layout = (LinearLayout) view;
                
                // Get the image and text views
                ImageView iconView = (ImageView) layout.getChildAt(0);
                TextView textView = (TextView) layout.getChildAt(1);
                
                // Set the appropriate icon and text based on index
                switch (i) {
                    case 0: // Salary
                        iconView.setImageResource(R.drawable.ic_category_salary);
                        iconView.setTag(R.drawable.ic_category_salary);
                        textView.setText(R.string.category_salary);
                        break;
                    case 1: // Investment
                        iconView.setImageResource(R.drawable.ic_category_investment);
                        iconView.setTag(R.drawable.ic_category_investment);
                        textView.setText(R.string.category_investment);
                        break;
                    case 2: // Gift
                        iconView.setImageResource(R.drawable.ic_category_gift);
                        iconView.setTag(R.drawable.ic_category_gift);
                        textView.setText(R.string.category_gift);
                        break;
                    case 3: // Scholarship
                        iconView.setImageResource(R.drawable.ic_category_education);
                        iconView.setTag(R.drawable.ic_category_education);
                        textView.setText(R.string.category_scholarship);
                        break;
                    case 4: // Sales
                        iconView.setImageResource(R.drawable.ic_category_shopping);
                        iconView.setTag(R.drawable.ic_category_shopping);
                        textView.setText(R.string.category_sales);
                        break;
                    case 5: // Bonus
                        iconView.setImageResource(R.drawable.ic_category_gift);
                        iconView.setTag(R.drawable.ic_category_gift);
                        textView.setText(R.string.category_bonus);
                        break;
                    case 6: // Loan
                        iconView.setImageResource(R.drawable.ic_category_loan);
                        iconView.setTag(R.drawable.ic_category_loan);
                        textView.setText(R.string.category_loan);
                        break;
                    case 7: // Refund
                        iconView.setImageResource(R.drawable.ic_category_refund);
                        iconView.setTag(R.drawable.ic_category_refund);
                        textView.setText(R.string.category_refund);
                        break;
                    case 8: // Part-time job
                        iconView.setImageResource(R.drawable.ic_category_part_time);
                        iconView.setTag(R.drawable.ic_category_part_time);
                        textView.setText(R.string.category_part_time);
                        break;
                    case 9: // Services
                        iconView.setImageResource(R.drawable.ic_category_tech);
                        iconView.setTag(R.drawable.ic_category_tech);
                        textView.setText(R.string.category_service);
                        break;
                }
                
                // Ensure icons are black
                iconView.setColorFilter(getResources().getColor(R.color.black));
            }
        }
    }
    
    private void setupSaveButton() {
        if (isDestroyed || saveButton == null) return;
        
        try {
        saveButton.setOnClickListener(v -> {
                if (isDestroyed) return;
                
                try {
                    // Chỉ cho phép khi không có tác vụ khác đang chạy
                    if (isLoading.get()) {
                        safeShowToast("Processing, please wait", Toast.LENGTH_SHORT);
                        return;
                    }
                    
            if (validateInputs()) {
                saveTransaction();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in save button click", e);
                    safeShowToast("Error: " + e.getMessage(), Toast.LENGTH_SHORT);
            }
        });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up save button", e);
        }
    }
    
    private boolean validateInputs() {
        if (isDestroyed) return false;
        
        boolean isValid = true;
        
        try {
        // Kiểm tra số tiền
            if (amountEditText == null || amountInputLayout == null) {
                Log.e(TAG, "Amount views are null");
                return false;
            }
            
        String amountText = amountEditText.getText().toString().trim();
        if (TextUtils.isEmpty(amountText)) {
            amountInputLayout.setError("Please enter amount");
            isValid = false;
        } else {
            try {
                double amount = Double.parseDouble(amountText);
                if (amount <= 0) {
                    amountInputLayout.setError("Amount must be greater than 0");
                    isValid = false;
                } else {
                    amountInputLayout.setError(null);
                }
            } catch (NumberFormatException e) {
                amountInputLayout.setError("Invalid amount");
                isValid = false;
            }
        }
        
        // Kiểm tra ngày
            if (dateEditText == null || dateInputLayout == null) {
                Log.e(TAG, "Date views are null");
                return false;
            }
            
        String dateText = dateEditText.getText().toString().trim();
        if (TextUtils.isEmpty(dateText)) {
            dateInputLayout.setError("Please select a date");
            isValid = false;
        } else {
            dateInputLayout.setError(null);
        }
        
        // Kiểm tra danh mục
        if (selectedCategory == null) {
                safeShowToast("Please select a category", Toast.LENGTH_SHORT);
                isValid = false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error validating inputs", e);
            isValid = false;
        }
        
        return isValid;
    }
    
    private void saveTransaction() {
        if (isDestroyed) return;
        
        try {
            // Vô hiệu hoá nút save
            if (saveButton != null) {
                saveButton.setEnabled(false);
            }
            
            // Đặt trạng thái đang xử lý
            isLoading.set(true);
            
            // Lấy dữ liệu từ form
            double amount = Double.parseDouble(amountEditText.getText().toString().trim());
            String note = noteEditText != null ? noteEditText.getText().toString().trim() : "";
            
            // Format date for DB (YYYY-MM-DD)
            SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String date = dbDateFormat.format(selectedDate.getTime());
            
            // Xác định loại giao dịch
            boolean isIncome = incomeRadioButton != null && incomeRadioButton.isChecked();
            Log.d(TAG, "Creating new transaction: type=" + (isIncome ? "income" : "expense") + 
                      ", amount=" + amount + ", date=" + date);
            
            // Tạo đối tượng Transaction
            Transaction transaction = new Transaction();
            transaction.setUserId(userId);
            transaction.setAmount(amount);
            transaction.setDate(date);
            transaction.setIncome(isIncome);
            
            // Thiết lập danh mục
            if (selectedCategory != null) {
                Log.d(TAG, "Using category: " + selectedCategory.getName() + 
                         " (id=" + selectedCategory.getId() + ")");
            transaction.setCategoryId(selectedCategory.getId());
            transaction.setCategoryName(selectedCategory.getName());
            transaction.setCategoryIcon(selectedCategory.getIconResourceId());
            } else {
                // Sử dụng danh mục mặc định - cần đảm bảo ID này tồn tại trong DB
                Log.d(TAG, "Using default category");
                SQLiteDatabase db = categoryRepository.getReadableDb();
                
                // Tìm ID của danh mục phù hợp đã có trong DB
                String selection = DatabaseContract.CategoriesEntry.COLUMN_TYPE + " = ?";
                String[] selectionArgs = { isIncome ? "income" : "expense" };
                String[] projection = { DatabaseContract.CategoriesEntry._ID, 
                                       DatabaseContract.CategoriesEntry.COLUMN_NAME,
                                       DatabaseContract.CategoriesEntry.COLUMN_ICON };
                
                Cursor cursor = null;
                try {
                    cursor = db.query(
                        DatabaseContract.CategoriesEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null, null, null,
                        "1"  // Chỉ lấy một kết quả
                    );
                    
                    if (cursor != null && cursor.moveToFirst()) {
                        int idColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.CategoriesEntry._ID);
                        int nameColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.CategoriesEntry.COLUMN_NAME);
                        int iconColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.CategoriesEntry.COLUMN_ICON);
                        
                        int id = cursor.getInt(idColumnIndex);
                        String name = cursor.getString(nameColumnIndex);
                        int icon = cursor.getInt(iconColumnIndex);
                        
                        transaction.setCategoryId(id);
                        transaction.setCategoryName(name);
                        transaction.setCategoryIcon(icon);
                        Log.d(TAG, "Found default category: " + name + " (id=" + id + ")");
                    } else {
                        // Không tìm thấy danh mục nào phù hợp, thử tạo một danh mục mới
                        Log.d(TAG, "No matching category found, creating new default category");
                        
                        Category newCategory;
                        if (isIncome) {
                            newCategory = new Category(0, "Lương", R.drawable.ic_category_salary, 1);
                        } else {
                            newCategory = new Category(0, "Chi tiêu khác", R.drawable.ic_category_utilities, 0);
                        }
                        
                        // Lưu danh mục mới và lấy ID
                        long newCategoryId = categoryRepository.addCategory(newCategory);
                        if (newCategoryId > 0) {
                            transaction.setCategoryId((int) newCategoryId);
                            transaction.setCategoryName(newCategory.getName());
                            transaction.setCategoryIcon(newCategory.getIconResourceId());
                            Log.d(TAG, "Created new default category with id: " + newCategoryId);
                        } else {
                            // Nếu không thể tạo danh mục mới, sử dụng ID mặc định an toàn
                            if (isIncome) {
                                transaction.setCategoryId(11); // ID của Lương (phải tồn tại trong DB)
                                transaction.setCategoryName("Lương");
                            } else {
                                transaction.setCategoryId(1); // ID của Ăn uống (phải tồn tại trong DB)
                                transaction.setCategoryName("Ăn uống");
                            }
                            transaction.setCategoryIcon(android.R.drawable.ic_menu_help);
                            Log.d(TAG, "Using hardcoded safe category id: " + transaction.getCategoryId());
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
            
            // Thiết lập ghi chú
            transaction.setNote(note);
            
            // Final reference cho sử dụng trong lambda
            final Transaction finalTransaction = transaction;
            
            // Lưu vào database trong background thread
            executor.execute(() -> {
                try {
                    if (isDestroyed) {
                        Log.d(TAG, "Fragment destroyed, skipping transaction save");
                        return;
                    }
                    
                    // Thêm giao dịch vào DB
                    long result = transactionRepository.addTransaction(finalTransaction);
                    Log.d(TAG, "Transaction save result: " + result);
                    
                    // Về main thread để cập nhật UI
                    postToMainThread(() -> {
                        if (isDestroyed) return;
                        
                        try {
                            handleTransactionResult(result, isIncome);
                        } catch (Exception e) {
                            Log.e(TAG, "Error handling transaction result", e);
                            safeShowToast("Error processing result: " + e.getMessage(), Toast.LENGTH_SHORT);
                        } finally {
                            // Reset trạng thái
                            isLoading.set(false);
                            if (saveButton != null) {
                                saveButton.setEnabled(true);
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error saving transaction", e);
                    
                    // Về main thread để hiển thị thông báo
                    postToMainThread(() -> {
                        if (isDestroyed) return;
                        
                        safeShowToast("Error saving transaction: " + e.getMessage(), Toast.LENGTH_SHORT);
                        
                        // Reset trạng thái
                        isLoading.set(false);
                        if (saveButton != null) {
                            saveButton.setEnabled(true);
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in saveTransaction", e);
            safeShowToast("Error: " + e.getMessage(), Toast.LENGTH_SHORT);
            
            // Reset trạng thái
            isLoading.set(false);
            if (saveButton != null) {
                saveButton.setEnabled(true);
            }
        }
    }
    
    private void handleTransactionResult(long result, boolean isIncome) {
        try {
            if (result > 0) {
                // Lưu thành công
                safeShowToast("Saved successfully", Toast.LENGTH_SHORT);
                
                // Reset form
                if (amountEditText != null) amountEditText.setText("");
                if (noteEditText != null) noteEditText.setText("");
                
                // Reset ngày về hiện tại
                selectedDate = Calendar.getInstance();
                if (dateEditText != null) {
                dateEditText.setText(dateFormat.format(selectedDate.getTime()));
                }
                
                // Reset danh mục được chọn - use the handleCategorySelection method
                if (categoryGrid != null && categoryGrid.getChildCount() > 0) {
                    handleCategorySelection(0); // Select the first category
                }
                
                // Thông báo cho DashboardFragment cập nhật dữ liệu
                if (!isDestroyed && getActivity() instanceof MainActivity) {
                    try {
                        MainActivity mainActivity = (MainActivity) getActivity();
                        mainActivity.refreshDashboard();
                        Log.d(TAG, "Notified MainActivity to refresh dashboard");
                    } catch (Exception e) {
                        Log.e(TAG, "Error refreshing dashboard", e);
                    }
                }
            } else {
                // Lưu thất bại
                safeShowToast("Error: Unable to save transaction", Toast.LENGTH_SHORT);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling transaction result", e);
            safeShowToast("Error processing result: " + e.getMessage(), Toast.LENGTH_SHORT);
        }
    }
    
    // Helper methods
    private void postToMainThread(Runnable action) {
        try {
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    if (!isDestroyed) {
                        action.run();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in main thread action", e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error posting to main thread", e);
        }
    }
    
    private void safeShowToast(String message, int duration) {
        try {
            if (!isDestroyed && getContext() != null) {
                Toast.makeText(getContext(), message, duration).show();
                Log.d(TAG, "Toast shown: " + message);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing toast: " + message, e);
        }
    }
    
    private void safePopBackStack() {
        try {
            if (!isDestroyed && getActivity() != null) {
                getActivity().onBackPressed();
                Log.d(TAG, "Navigated back");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error navigating back", e);
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called");
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isDestroyed = true;
        
        try {
            // No need to clear adapters for GridLayout
            
            // Clear references
            categoryGrid = null;
            selectedCategory = null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroyView", e);
        }
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy called");
        isDestroyed = true;
        
        // Giải phóng tài nguyên
        try {
            // Đóng repositories để tránh rò rỉ DB connections
            if (categoryRepository != null) {
                // Chỉ đóng repository khi fragment thực sự bị hủy,
                // không đóng trong onDestroyView vì fragment có thể được tái sử dụng
                categoryRepository.close();
                categoryRepository = null;
            }
            
            if (transactionRepository != null) {
                transactionRepository.close();
                transactionRepository = null;
            }
            
            if (loginUserRepository != null) {
                loginUserRepository = null;
            }
            
            // Giải phóng danh sách
            if (expenseCategories != null) {
                expenseCategories.clear();
                expenseCategories = null;
            }
            
            if (incomeCategories != null) {
                incomeCategories.clear();
                incomeCategories = null;
            }
            
            selectedDate = null;
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up resources", e);
        }
        
        super.onDestroy();
    }
    
    private void resetAndLoadCategories() {
        if (isDestroyed || getContext() == null) {
            Log.d(TAG, "Fragment destroyed or context null, skipping resetAndLoadCategories");
            return;
        }
        
        try {
            Log.d(TAG, "Resetting categories and reloading");
            
            // Clear existing lists
            expenseCategories.clear();
            incomeCategories.clear();
            
            // Clear selected category
            selectedCategory = null;
            
            // Load categories
            loadCategories();
            
            // Select the first category by default for the current transaction type
            if (!expenseCategories.isEmpty() && expenseRadioButton.isChecked()) {
                selectedCategory = expenseCategories.get(0);
                if (categoryGrid.getChildCount() > 0) {
                    handleCategorySelection(0);
                }
            } else if (!incomeCategories.isEmpty() && incomeRadioButton.isChecked()) {
                selectedCategory = incomeCategories.get(0);
                if (categoryGrid.getChildCount() > 0) {
                    handleCategorySelection(0);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in resetAndLoadCategories", e);
        }
    }
    
    private void resetDatabase() {
        try {
            Log.d(TAG, "Resetting categories in database");
            SQLiteDatabase db = categoryRepository.getWritableDb();
            
            // Xóa tất cả danh mục hiện tại
            db.delete(DatabaseContract.CategoriesEntry.TABLE_NAME, null, null);
            
            // Thêm danh mục chi tiêu mặc định
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
            
            Log.d(TAG, "Database reset completed with 20 categories (10 expense + 10 income)");
        } catch (Exception e) {
            Log.e(TAG, "Error resetting database", e);
        }
    }

    // Add new method to calculate responsive columns
    private int calculateOptimalColumnCount() {
        if (getContext() == null) return 4; // default fallback
        
        // Get screen width
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        float density = getResources().getDisplayMetrics().density;
        
        // Target width for each item (90dp)
        float itemWidthDp = 90f;
        float itemWidthPx = itemWidthDp * density;
        
        // Calculate columns (min 3, max 6)
        int columns = Math.max(3, Math.min(6, (int)(screenWidth / itemWidthPx)));
        
        Log.d(TAG, "Calculated optimal column count: " + columns + 
              " (screen width: " + screenWidth + "px, density: " + density + ")");
        
        return columns;
    }

    // New method to set up click listeners for category items
    private void setupCategoryClickListeners() {
        // Get all child views from the grid
        for (int i = 0; i < categoryGrid.getChildCount(); i++) {
            View categoryItem = categoryGrid.getChildAt(i);
            
            if (categoryItem instanceof LinearLayout) {
                final int position = i;
                categoryItem.setOnClickListener(v -> {
                    handleCategorySelection(position);
                });
            }
        }
    }
    
    private void handleCategorySelection(int position) {
        try {
            // Reset all category backgrounds
            for (int i = 0; i < categoryGrid.getChildCount(); i++) {
                View categoryItem = categoryGrid.getChildAt(i);
                if (categoryItem instanceof LinearLayout && categoryItem.getVisibility() == View.VISIBLE) {
                    // Reset background
                    categoryItem.setBackgroundResource(android.R.color.transparent);
                    
                    // Reset text color to black
                    TextView categoryText = (TextView) ((LinearLayout) categoryItem).getChildAt(1);
                    if (categoryText != null) {
                        categoryText.setTextColor(getResources().getColor(R.color.black));
                    }
                    
                    // Reset icon color to black
                    ImageView iconView = (ImageView) ((LinearLayout) categoryItem).getChildAt(0);
                    if (iconView != null) {
                        iconView.setColorFilter(getResources().getColor(R.color.black));
                    }
                }
            }
            
            // Highlight selected category
            View selectedView = categoryGrid.getChildAt(position);
            if (selectedView instanceof LinearLayout && selectedView.getVisibility() == View.VISIBLE) {
                selectedView.setBackgroundResource(R.color.colorPrimaryLight);
                
                // Set text color to white
                TextView categoryText = (TextView) ((LinearLayout) selectedView).getChildAt(1);
                if (categoryText != null) {
                    categoryText.setTextColor(getResources().getColor(R.color.white));
                }
                
                // Set icon color to white
                ImageView iconView = (ImageView) ((LinearLayout) selectedView).getChildAt(0);
                if (iconView != null) {
                    iconView.setColorFilter(getResources().getColor(R.color.white));
                }
                
                // Set the selected category based on position and transaction type
                String categoryName = categoryText.getText().toString();
                
                // Use the constructor with required parameters
                int categoryId = position + 1;
                int iconResourceId = 0; // Use a default or resolve from the ImageView
                
                if (iconView != null && iconView.getTag() instanceof Integer) {
                    iconResourceId = (Integer) iconView.getTag();
                }
                
                // Determine if it's expense or income based on radio button selection
                int type = expenseRadioButton.isChecked() ? 0 : 1; // 0 for expense, 1 for income
                
                // If it's income, adjust the category ID to start after expense categories
                if (type == 1) {
                    categoryId += 10; // Make income category IDs start from 11
                }
                
                selectedCategory = new Category(categoryId, categoryName, iconResourceId, type);
                Log.d(TAG, "Selected category: " + categoryName + " (id=" + categoryId + ", type=" + (type == 0 ? "expense" : "income") + ")");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling category selection", e);
        }
    }
} 