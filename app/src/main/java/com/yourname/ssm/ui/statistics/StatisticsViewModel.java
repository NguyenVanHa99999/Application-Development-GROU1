package com.yourname.ssm.ui.statistics;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.yourname.ssm.model.Transaction;
import com.yourname.ssm.repository.TransactionRepository;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class StatisticsViewModel extends AndroidViewModel {
    private static final String TAG = "StatisticsViewModel";

    private final MutableLiveData<List<Transaction>> transactionsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Double> totalExpenseLiveData = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> avgExpenseLiveData = new MutableLiveData<>(0.0);
    private final MutableLiveData<Integer> transactionCountLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    
    // Thêm dữ liệu cho biểu đồ tròn
    private Map<String, Double> categoryValues = new HashMap<>();
    private double totalCategoryExpense = 0.0;

    private final Executor executor = Executors.newSingleThreadExecutor();
    private TransactionRepository transactionRepository;
    private final NumberFormat currencyFormat;

    public StatisticsViewModel(@NonNull Application application) {
        super(application);
        transactionRepository = new TransactionRepository(application);
        currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    }

    public LiveData<List<Transaction>> getTransactions() {
        return transactionsLiveData;
    }

    public LiveData<Double> getTotalExpense() {
        return totalExpenseLiveData;
    }

    public LiveData<Double> getAvgExpense() {
        return avgExpenseLiveData;
    }

    public LiveData<Integer> getTransactionCount() {
        return transactionCountLiveData;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessageLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoadingLiveData;
    }
    
    // Phương thức mới để lấy dữ liệu cho biểu đồ tròn
    public Map<String, Double> getCategoryValues() {
        return categoryValues;
    }
    
    public void setCategoryValues(Map<String, Double> values) {
        if (values != null) {
            this.categoryValues = values;
        }
    }
    
    public double getTotalCategoryExpense() {
        return totalCategoryExpense;
    }
    
    public void setTotalExpense(double expense) {
        this.totalCategoryExpense = expense;
    }

    public void loadTransactionsForDateRange(int userId, String fromDate, String toDate) {
        isLoadingLiveData.setValue(true);

        executor.execute(() -> {
            try {
                List<Transaction> transactions = transactionRepository.getTransactionsForDateRange(userId, fromDate, toDate);
                
                // Tính toán thống kê
                double totalExpense = 0;
                int expenseCount = 0;
                
                // Tính toán chi tiêu theo danh mục
                Map<String, Double> tempCategoryMap = new HashMap<>();
                
                for (Transaction transaction : transactions) {
                    if (transaction.isExpense()) {
                        totalExpense += transaction.getAmount();
                        expenseCount++;
                        
                        // Nhóm theo danh mục
                        String category = transaction.getCategoryName();
                        if (category == null || category.isEmpty()) {
                            category = "Khác";
                        }
                        
                        // Cập nhật chi tiêu cho danh mục
                        double currentAmount = tempCategoryMap.getOrDefault(category, 0.0);
                        tempCategoryMap.put(category, currentAmount + transaction.getAmount());
                    }
                }
                
                // Cập nhật dữ liệu danh mục cho biểu đồ tròn
                categoryValues = tempCategoryMap;
                totalCategoryExpense = totalExpense;
                
                double avgExpense = expenseCount > 0 ? totalExpense / expenseCount : 0;
                
                // Cập nhật LiveData
                transactionsLiveData.postValue(transactions);
                totalExpenseLiveData.postValue(totalExpense);
                avgExpenseLiveData.postValue(avgExpense);
                transactionCountLiveData.postValue(expenseCount);
                isLoadingLiveData.postValue(false);
            } catch (Exception e) {
                Log.e(TAG, "Error loading transactions: " + e.getMessage());
                errorMessageLiveData.postValue("Lỗi tải dữ liệu: " + e.getMessage());
                isLoadingLiveData.postValue(false);
            }
        });
    }

    public String formatCurrency(double amount) {
        return currencyFormat.format(amount) + "đ";
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (transactionRepository != null) {
            transactionRepository.close();
            transactionRepository = null;
        }
    }
} 