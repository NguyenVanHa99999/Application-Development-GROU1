package com.yourname.ssm.ui.statistics;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.yourname.ssm.R;
import com.yourname.ssm.model.Category;
import com.yourname.ssm.model.Transaction;
import com.yourname.ssm.repository.CategoryRepository;
import com.yourname.ssm.repository.LoginUserRepository;
import com.yourname.ssm.repository.TransactionRepository;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class TransactionHistoryFragment extends Fragment {
    private static final String TAG = "TransactionHistoryFrag";
    
    // UI components
    private ImageButton backButton;
    private EditText searchEditText;
    private ImageButton clearSearchButton;
    private Chip dateChip;
    private Chip expenseChip;
    private Chip incomeChip;
    private Chip categoryChip;
    private RecyclerView transactionsRecyclerView;
    private TextView emptyStateTextView;
    
    // Data
    private TransactionRepository transactionRepository;
    private CategoryRepository categoryRepository;
    private LoginUserRepository loginUserRepository;
    private int userId;
    private List<Transaction> allTransactions = new ArrayList<>();
    private List<Transaction> filteredTransactions = new ArrayList<>();
    private TransactionHistoryAdapter adapter;
    
    // Filters
    private String searchQuery = "";
    private String fromDate;
    private String toDate;
    private String selectedType = "";
    private int selectedCategoryId = -1;
    
    // Utils
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transaction_history, container, false);
        
        try {
            // Initialize repositories
            transactionRepository = new TransactionRepository(requireContext());
            categoryRepository = new CategoryRepository(requireContext());
            loginUserRepository = new LoginUserRepository(requireContext());
            userId = loginUserRepository.getUserId();
            
            // Get data from arguments
            if (getArguments() != null) {
                fromDate = getArguments().getString("from_date");
                toDate = getArguments().getString("to_date");
            }
            
            // Initialize views
            initViews(view);
            
            // Set up listeners
            setupListeners();
            
            // Load data
            loadTransactions();
            
            return view;
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreateView", e);
            Toast.makeText(requireContext(), "Error initializing screen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return view;
        }
    }
    
    private void initViews(View view) {
        try {
            backButton = view.findViewById(R.id.btn_back);
            searchEditText = view.findViewById(R.id.et_search);
            clearSearchButton = view.findViewById(R.id.btn_clear_search);
            dateChip = view.findViewById(R.id.chip_date);
            expenseChip = view.findViewById(R.id.chip_expense);
            incomeChip = view.findViewById(R.id.chip_income);
            categoryChip = view.findViewById(R.id.chip_category);
            transactionsRecyclerView = view.findViewById(R.id.rv_transactions);
            emptyStateTextView = view.findViewById(R.id.tv_empty_state);
            
            // Thiết lập RecyclerView
            transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            adapter = new TransactionHistoryAdapter(filteredTransactions, new TransactionHistoryAdapter.OnTransactionClickListener() {
                @Override
                public void onTransactionClick(Transaction transaction) {
                    showTransactionDetailDialog(transaction);
                }
                
                @Override
                public void onEditClick(Transaction transaction) {
                    showEditTransactionDialog(transaction);
                }
                
                @Override
                public void onDeleteClick(Transaction transaction) {
                    showDeleteConfirmationDialog(transaction);
                }
            });
            transactionsRecyclerView.setAdapter(adapter);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            throw e;
        }
    }
    
    private void setupListeners() {
        // Back button
        backButton.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        
        // Search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                searchQuery = s.toString().trim().toLowerCase();
                clearSearchButton.setVisibility(searchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                applyFilters();
            }
        });
        
        // Clear search button
        clearSearchButton.setOnClickListener(v -> {
            searchEditText.setText("");
            hideKeyboard();
        });
        
        // Date filter
        dateChip.setOnClickListener(v -> showDateFilterDialog());
        
        // Type filters
        expenseChip.setOnClickListener(v -> {
            selectedType = expenseChip.isChecked() ? "expense" : "";
            
            // Nếu chọn expense, hủy chọn income
            if (expenseChip.isChecked() && incomeChip.isChecked()) {
                incomeChip.setChecked(false);
            }
            
            applyFilters();
        });
        
        incomeChip.setOnClickListener(v -> {
            selectedType = incomeChip.isChecked() ? "income" : "";
            
            // Nếu chọn income, hủy chọn expense
            if (incomeChip.isChecked() && expenseChip.isChecked()) {
                expenseChip.setChecked(false);
            }
            
            applyFilters();
        });
        
        // Category filter
        categoryChip.setOnClickListener(v -> showCategoryFilterDialog());
    }
    
    private void loadTransactions() {
        if (userId <= 0) {
            Toast.makeText(requireContext(), "Please login to view transaction history", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // Show loading state
            showLoading(true);
            
            // Query data on background thread
            executor.execute(() -> {
                try {
                    // Get transaction list from repository
                    List<Transaction> transactions = transactionRepository.getTransactionsForDateRange(userId, fromDate, toDate);
                    
                    // Update data
                    allTransactions = transactions;
                    
                    // Apply filters
                    requireActivity().runOnUiThread(() -> {
                        applyFilters();
                        showLoading(false);
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error loading transactions", e);
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Error loading data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        showLoading(false);
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in loadTransactions", e);
            Toast.makeText(requireContext(), "Error loading data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            showLoading(false);
        }
    }
    
    private void applyFilters() {
        try {
            List<Transaction> result = new ArrayList<>(allTransactions);
            
            // Áp dụng lọc theo loại (expense/income)
            if (!TextUtils.isEmpty(selectedType)) {
                result = result.stream()
                    .filter(transaction -> selectedType.equals(transaction.getType()))
                    .collect(Collectors.toList());
            }
            
            // Áp dụng lọc theo danh mục
            if (selectedCategoryId > 0) {
                result = result.stream()
                    .filter(transaction -> transaction.getCategoryId() == selectedCategoryId)
                    .collect(Collectors.toList());
            }
            
            // Áp dụng tìm kiếm
            if (!TextUtils.isEmpty(searchQuery)) {
                result = result.stream()
                    .filter(transaction -> 
                        (transaction.getCategoryName() != null && 
                         transaction.getCategoryName().toLowerCase().contains(searchQuery)) ||
                        (transaction.getNote() != null && 
                         transaction.getNote().toLowerCase().contains(searchQuery)))
                    .collect(Collectors.toList());
            }
            
            // Cập nhật danh sách và UI
            filteredTransactions.clear();
            filteredTransactions.addAll(result);
            adapter.notifyDataSetChanged();
            
            // Hiển thị empty state nếu không có kết quả
            if (filteredTransactions.isEmpty()) {
                emptyStateTextView.setVisibility(View.VISIBLE);
                transactionsRecyclerView.setVisibility(View.GONE);
            } else {
                emptyStateTextView.setVisibility(View.GONE);
                transactionsRecyclerView.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error applying filters", e);
            Toast.makeText(requireContext(), "Error filtering data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showDateFilterDialog() {
        try {
            Calendar startCalendar = Calendar.getInstance();
            Calendar endCalendar = Calendar.getInstance();
            
            try {
                if (fromDate != null) {
                    Date date = apiDateFormat.parse(fromDate);
                    startCalendar.setTime(date);
                }
                
                if (toDate != null) {
                    Date date = apiDateFormat.parse(toDate);
                    endCalendar.setTime(date);
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing dates", e);
            }
            
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_date_filter, null);
            builder.setView(dialogView);
            
            TextView fromDateTextView = dialogView.findViewById(R.id.tv_from_date);
            TextView toDateTextView = dialogView.findViewById(R.id.tv_to_date);
            
            fromDateTextView.setText(displayDateFormat.format(startCalendar.getTime()));
            toDateTextView.setText(displayDateFormat.format(endCalendar.getTime()));
            
            // Listener cho chọn ngày bắt đầu
            fromDateTextView.setOnClickListener(v -> {
                DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        startCalendar.set(Calendar.YEAR, year);
                        startCalendar.set(Calendar.MONTH, month);
                        startCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        fromDateTextView.setText(displayDateFormat.format(startCalendar.getTime()));
                    },
                    startCalendar.get(Calendar.YEAR),
                    startCalendar.get(Calendar.MONTH),
                    startCalendar.get(Calendar.DAY_OF_MONTH)
                );
                datePickerDialog.show();
            });
            
            // Listener cho chọn ngày kết thúc
            toDateTextView.setOnClickListener(v -> {
                DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        endCalendar.set(Calendar.YEAR, year);
                        endCalendar.set(Calendar.MONTH, month);
                        endCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        toDateTextView.setText(displayDateFormat.format(endCalendar.getTime()));
                    },
                    endCalendar.get(Calendar.YEAR),
                    endCalendar.get(Calendar.MONTH),
                    endCalendar.get(Calendar.DAY_OF_MONTH)
                );
                datePickerDialog.show();
            });
            
            AlertDialog dialog = builder.create();
            
            // Nút Áp dụng
            dialogView.findViewById(R.id.btn_apply).setOnClickListener(v -> {
                fromDate = apiDateFormat.format(startCalendar.getTime());
                toDate = apiDateFormat.format(endCalendar.getTime());
                dateChip.setChecked(true);
                loadTransactions();
                dialog.dismiss();
            });
            
            // Nút Hủy
            dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> {
                dialog.dismiss();
            });
            
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing date filter dialog", e);
            Toast.makeText(requireContext(), "Error showing date filter dialog: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showCategoryFilterDialog() {
        try {
            executor.execute(() -> {
                try {
                    // Lấy danh sách danh mục
                    List<Category> categories = categoryRepository.getAllCategories();
                    
                    requireActivity().runOnUiThread(() -> {
                        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                        builder.setTitle("Select category");
                        
                        // Thêm Tất cả vào đầu danh sách
                        List<String> categoryNames = new ArrayList<>();
                        categoryNames.add("All");
                        
                        // Thêm các danh mục từ CSDL
                        List<Integer> categoryIds = new ArrayList<>();
                        categoryIds.add(-1); // -1 là giá trị cho "Tất cả"
                        
                        for (Category category : categories) {
                            categoryNames.add(category.getName());
                            categoryIds.add(category.getId());
                        }
                        
                        String[] namesArray = categoryNames.toArray(new String[0]);
                        
                        builder.setItems(namesArray, (dialog, which) -> {
                            selectedCategoryId = categoryIds.get(which);
                            categoryChip.setChecked(selectedCategoryId != -1);
                            applyFilters();
                        });
                        
                        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
                        
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error loading categories", e);
                    requireActivity().runOnUiThread(() -> 
                        Toast.makeText(requireContext(), "Error loading categories: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error showing category filter dialog", e);
            Toast.makeText(requireContext(), "Error showing category filter dialog: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showTransactionDetailDialog(Transaction transaction) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_transaction_detail, null);
            builder.setView(dialogView);
            
            // Ánh xạ các view trong dialog
            TextView titleTextView = dialogView.findViewById(R.id.tv_dialog_title);
            TextView dateTextView = dialogView.findViewById(R.id.tv_transaction_date);
            TextView categoryTextView = dialogView.findViewById(R.id.tv_transaction_category);
            TextView amountTextView = dialogView.findViewById(R.id.tv_transaction_amount);
            TextView noteTextView = dialogView.findViewById(R.id.tv_transaction_note);
            
            // Thiết lập dữ liệu
            titleTextView.setText(transaction.isIncome() ? "Transaction Details" : "Expense Details");
            dateTextView.setText(transaction.getDate());
            categoryTextView.setText(transaction.getCategoryName());
            
            // Format tiền tệ
            java.text.NumberFormat currencyFormat = java.text.NumberFormat.getNumberInstance(new Locale("vi", "VN"));
            amountTextView.setText(currencyFormat.format(transaction.getAmount()) + "đ");
            amountTextView.setTextColor(getResources().getColor(
                transaction.isIncome() ? android.R.color.holo_green_dark : android.R.color.holo_red_dark
            ));
            
            // Hiển thị ghi chú nếu có
            if (!TextUtils.isEmpty(transaction.getNote())) {
                noteTextView.setText(transaction.getNote());
            } else {
                noteTextView.setText("No note");
            }
            
            // Nút Đóng
            dialogView.findViewById(R.id.btn_close).setOnClickListener(v -> {
                AlertDialog dialog = (AlertDialog) dialogView.getTag();
                if (dialog != null) {
                    dialog.dismiss();
                }
            });
            
            // Tạo và hiển thị dialog
            AlertDialog dialog = builder.create();
            dialogView.setTag(dialog);
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing transaction detail dialog", e);
            Toast.makeText(requireContext(), "Error showing transaction detail dialog: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showEditTransactionDialog(Transaction transaction) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_transaction, null);
            builder.setView(dialogView);
            
            // Ánh xạ các view trong dialog
            TextView titleTextView = dialogView.findViewById(R.id.tv_dialog_title);
            EditText amountEditText = dialogView.findViewById(R.id.et_amount);
            EditText noteEditText = dialogView.findViewById(R.id.et_note);
            TextView categoryTextView = dialogView.findViewById(R.id.tv_category);
            TextView dateTextView = dialogView.findViewById(R.id.tv_date);
            
            // Thiết lập dữ liệu
            titleTextView.setText("Edit " + (transaction.isIncome() ? "Income" : "Expense"));
            amountEditText.setText(String.valueOf(transaction.getAmount()));
            noteEditText.setText(transaction.getNote());
            categoryTextView.setText(transaction.getCategoryName());
            dateTextView.setText(transaction.getDate());
            
            // Listener cho chọn danh mục
            final int[] selectedCategoryId = {transaction.getCategoryId()};
            categoryTextView.setOnClickListener(v -> {
                executor.execute(() -> {
                    try {
                        // Lấy danh sách danh mục theo loại
                        List<Category> categories = categoryRepository.getCategoriesByType(
                            transaction.isIncome() ? 1 : 0 // 1 = income, 0 = expense
                        );
                        
                        requireActivity().runOnUiThread(() -> {
                            AlertDialog.Builder catBuilder = new AlertDialog.Builder(requireContext());
                            catBuilder.setTitle("Select category");
                            
                            String[] categoryNames = categories.stream()
                                .map(Category::getName)
                                .toArray(String[]::new);
                            
                            catBuilder.setItems(categoryNames, (dialog, which) -> {
                                selectedCategoryId[0] = categories.get(which).getId();
                                categoryTextView.setText(categories.get(which).getName());
                            });
                            
                            catBuilder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
                            
                            AlertDialog catDialog = catBuilder.create();
                            catDialog.show();
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error loading categories", e);
                        requireActivity().runOnUiThread(() -> 
                            Toast.makeText(requireContext(), "Error loading categories: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
                    }
                });
            });
            
            // Listener cho chọn ngày
            Calendar calendar = Calendar.getInstance();
            try {
                Date date = apiDateFormat.parse(transaction.getDate());
                calendar.setTime(date);
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing date", e);
            }
            
            final Calendar[] selectedDate = {calendar};
            dateTextView.setOnClickListener(v -> {
                DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        selectedDate[0].set(Calendar.YEAR, year);
                        selectedDate[0].set(Calendar.MONTH, month);
                        selectedDate[0].set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        dateTextView.setText(apiDateFormat.format(selectedDate[0].getTime()));
                    },
                    selectedDate[0].get(Calendar.YEAR),
                    selectedDate[0].get(Calendar.MONTH),
                    selectedDate[0].get(Calendar.DAY_OF_MONTH)
                );
                datePickerDialog.show();
            });
            
            AlertDialog dialog = builder.create();
            
            // Nút Lưu
            dialogView.findViewById(R.id.btn_save).setOnClickListener(v -> {
                try {
                    // Lấy dữ liệu từ form
                    double amount = Double.parseDouble(amountEditText.getText().toString());
                    String note = noteEditText.getText().toString().trim();
                    String date = apiDateFormat.format(selectedDate[0].getTime());
                    
                    // Cập nhật đối tượng giao dịch
                    transaction.setAmount(amount);
                    transaction.setNote(note);
                    transaction.setDate(date);
                    transaction.setCategoryId(selectedCategoryId[0]);
                    
                    // Cập nhật vào CSDL
                    executor.execute(() -> {
                        try {
                            boolean success = transactionRepository.updateTransaction(transaction);
                            
                            requireActivity().runOnUiThread(() -> {
                                if (success) {
                                    Toast.makeText(requireContext(), "Transaction updated successfully", Toast.LENGTH_SHORT).show();
                                    loadTransactions();
                                } else {
                                    Toast.makeText(requireContext(), "Error updating transaction", Toast.LENGTH_SHORT).show();
                                }
                                dialog.dismiss();
                            });
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating transaction", e);
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), "Error updating transaction: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            });
                        }
                    });
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(TAG, "Error saving transaction", e);
                    Toast.makeText(requireContext(), "Error saving transaction: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
            
            // Nút Hủy
            dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
            
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing edit transaction dialog", e);
            Toast.makeText(requireContext(), "Error showing edit transaction dialog: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showDeleteConfirmationDialog(Transaction transaction) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Confirm deletion");
            builder.setMessage("Are you sure you want to delete this transaction?");
            
            builder.setPositiveButton("Delete", (dialog, which) -> {
                executor.execute(() -> {
                    try {
                        boolean success = transactionRepository.deleteTransaction(transaction.getId());
                        
                        requireActivity().runOnUiThread(() -> {
                            if (success) {
                                Toast.makeText(requireContext(), "Transaction deleted", Toast.LENGTH_SHORT).show();
                                loadTransactions();
                            } else {
                                Toast.makeText(requireContext(), "Error deleting transaction", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error deleting transaction", e);
                        requireActivity().runOnUiThread(() -> 
                            Toast.makeText(requireContext(), "Error deleting transaction: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
                    }
                });
            });
            
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
            
            AlertDialog dialog = builder.create();
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing delete confirmation dialog", e);
            Toast.makeText(requireContext(), "Error showing delete confirmation dialog: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null && searchEditText != null) {
                imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error hiding keyboard", e);
        }
    }
    
    private void showLoading(boolean isLoading) {
        // Có thể thêm ProgressBar và xử lý logic hiển thị ở đây
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Đóng các resources
        if (transactionRepository != null) {
            transactionRepository.close();
        }
        if (categoryRepository != null) {
            categoryRepository.close();
        }
    }
} 