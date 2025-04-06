package com.yourname.ssm.ui.dashboard;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.yourname.ssm.model.Budget;
import com.yourname.ssm.model.Transaction;
import com.yourname.ssm.repository.BudgetRepository;
import com.yourname.ssm.repository.LoginUserRepository;
import com.yourname.ssm.repository.TransactionRepository;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;

public class DashboardViewModel extends AndroidViewModel {

    private final MutableLiveData<Budget> budget;
    private final MutableLiveData<Double> totalIncome;
    private final MutableLiveData<Double> totalExpense;
    private final MutableLiveData<List<Transaction>> recentTransactions;
    private final MutableLiveData<String> formattedBudget;
    private final MutableLiveData<String> formattedIncome;
    private final MutableLiveData<String> formattedExpense;
    private final MutableLiveData<Boolean> isOverBudget;
    private final MutableLiveData<Double> budgetPercentage;
    
    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final LoginUserRepository loginUserRepository;
    
    private int userId;
    private Calendar calendar;
    private NumberFormat currencyFormatter;

    public DashboardViewModel(Application application) {
        super(application);
        
        budget = new MutableLiveData<>();
        totalIncome = new MutableLiveData<>();
        totalExpense = new MutableLiveData<>();
        recentTransactions = new MutableLiveData<>();
        formattedBudget = new MutableLiveData<>();
        formattedIncome = new MutableLiveData<>();
        formattedExpense = new MutableLiveData<>();
        isOverBudget = new MutableLiveData<>();
        budgetPercentage = new MutableLiveData<>();
        
        budgetRepository = new BudgetRepository(application);
        transactionRepository = new TransactionRepository(application);
        loginUserRepository = new LoginUserRepository(application);
        
        // Get current user ID from LoginUserRepository
        userId = loginUserRepository.getUserId();
        
        calendar = Calendar.getInstance();
        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        
        loadData();
    }
    
    public void loadData() {
        try {
            // Get current month and year
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH) + 1; // Calendar months are 0-based
            
            // Make sure currencyFormatter is initialized
            if (currencyFormatter == null) {
                currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            }
            
            // Lấy lại userId mỗi khi load data để đảm bảo có thông tin mới nhất
            userId = loginUserRepository.getUserId();
            
            if (userId <= 0) {
                // Người dùng chưa đăng nhập, không thể tải dữ liệu
                return;
            }
            
            // Sử dụng background thread để tải dữ liệu
            new Thread(() -> {
                try {
                    // Load budget data
                    Budget currentBudget = budgetRepository.getBudgetForMonth(userId, year, month);
                    
                    // Format budget limit for display
                    String formattedBudgetValue = currencyFormatter.format(currentBudget.getLimit());
                    
                    // Load income and expense data
                    double incomeValue = transactionRepository.getTotalIncomeForMonth(userId, year, month);
                    double expenseValue = transactionRepository.getTotalExpenseForMonth(userId, year, month);
                    
                    // Format for display
                    String formattedIncomeValue = currencyFormatter.format(incomeValue);
                    String formattedExpenseValue = currencyFormatter.format(expenseValue);
                    
                    // Get recent transactions - lấy tối đa 10 giao dịch gần nhất
                    List<Transaction> transactions = transactionRepository.getTransactionsForMonth(userId, year, month);
                    
                    // Determine if budget is over limit and there are actual expenses
                    boolean isOverBudgetAndHasExpenses = currentBudget.isOverLimit() && expenseValue > 0;
                    
                    // Cập nhật LiveData trên main thread
                    new Handler(Looper.getMainLooper()).post(() -> {
                        budget.setValue(currentBudget);
                        formattedBudget.setValue(formattedBudgetValue);
                        isOverBudget.setValue(isOverBudgetAndHasExpenses);
                        budgetPercentage.setValue(currentBudget.getPercentageUsed());
                        
                        totalIncome.setValue(incomeValue);
                        totalExpense.setValue(expenseValue);
                        formattedIncome.setValue(formattedIncomeValue);
                        formattedExpense.setValue(formattedExpenseValue);
                        recentTransactions.setValue(transactions);
                    });
                } catch (Exception e) {
                    // Log lỗi và cập nhật giá trị mặc định để UI không bị trống
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Budget emptyBudget = new Budget(0, userId, year, month, 0, 0);
                        budget.setValue(emptyBudget);
                        formattedBudget.setValue(currencyFormatter.format(0));
                        isOverBudget.setValue(false);
                        budgetPercentage.setValue(0.0);
                        
                        totalIncome.setValue(0.0);
                        totalExpense.setValue(0.0);
                        formattedIncome.setValue(currencyFormatter.format(0));
                        formattedExpense.setValue(currencyFormatter.format(0));
                        recentTransactions.setValue(new ArrayList<>());
                    });
                }
            }).start();
        } catch (Exception e) {
            // Log lỗi
        }
    }
    
    public LiveData<Budget> getBudget() {
        return budget;
    }
    
    public LiveData<Double> getTotalIncome() {
        return totalIncome;
    }
    
    public LiveData<Double> getTotalExpense() {
        return totalExpense;
    }
    
    public LiveData<List<Transaction>> getRecentTransactions() {
        return recentTransactions;
    }
    
    public LiveData<String> getFormattedBudget() {
        return formattedBudget;
    }
    
    public LiveData<String> getFormattedIncome() {
        return formattedIncome;
    }
    
    public LiveData<String> getFormattedExpense() {
        return formattedExpense;
    }
    
    public LiveData<Boolean> getIsOverBudget() {
        return isOverBudget;
    }
    
    public LiveData<Double> getBudgetPercentage() {
        return budgetPercentage;
    }
    
    public void refreshData() {
        loadData();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        
        // Không cần thiết phải gán null cho các biến final
        // Garbage collector sẽ tự động xử lý
        calendar = null;
        // Không set currencyFormatter thành null vì có thể còn được sử dụng
    }
} 