package com.yourname.ssm.ui.dashboard;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.yourname.ssm.R;
import com.yourname.ssm.model.Budget;
import com.yourname.ssm.model.Transaction;
import com.yourname.ssm.MainActivity;
import com.yourname.ssm.ui.dashboard.adapters.BannerAdapter;
import com.yourname.ssm.ui.dashboard.adapters.TransactionAdapter;
import com.yourname.ssm.ui.settings.SettingsFragment;
import com.yourname.ssm.utils.EmailService;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.text.NumberFormat;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private static final String TAG = "DashboardFragment";

    // UI Components
    private TextView budgetAmountText;
    private TextView budgetWarningText;
    private ProgressBar budgetProgressBar;
    private TextView incomeAmountText;
    private TextView expenseAmountText;
    private ViewPager2 bannerViewPager;
    private RecyclerView recentTransactionsRecyclerView;
    private TextView noTransactionsText;
    private ImageButton addFundsButton;

    // View Model
    private DashboardViewModel dashboardViewModel;

    // Adapters
    private TransactionAdapter transactionAdapter;
    private BannerAdapter bannerAdapter;

    // Banner auto-scroll variables
    private int currentBannerPosition = 0;
    private Timer timer;
    private final long DELAY_MS = 500;
    private final long PERIOD_MS = 3000;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ViewPager2.OnPageChangeCallback bannerPageChangeCallback;

    // Banner image resources
    private final int[] bannerImages = {
            R.drawable.banner1,
            R.drawable.banner2,
            R.drawable.banner3
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Initialize UI Components
        budgetAmountText = root.findViewById(R.id.budgetAmountText);
        budgetWarningText = root.findViewById(R.id.budgetWarningText);
        budgetProgressBar = root.findViewById(R.id.budgetProgressBar);
        incomeAmountText = root.findViewById(R.id.incomeAmountText);
        expenseAmountText = root.findViewById(R.id.expenseAmountText);
        bannerViewPager = root.findViewById(R.id.bannerViewPager);
        recentTransactionsRecyclerView = root.findViewById(R.id.recentTransactionsRecyclerView);
        noTransactionsText = root.findViewById(R.id.noTransactionsText);
        addFundsButton = root.findViewById(R.id.addFundsButton);

        // Initialize ViewModel
        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        // Setup Banner Carousel
        setupBannerCarousel();

        // Setup TransactionAdapter
        setupTransactionList();

        // Setup click listeners
        setupClickListeners();

        // Observe LiveData
        observeViewModel();

        return root;
    }

    private void setupBannerCarousel() {
        if (getContext() == null || !isAdded() || bannerViewPager == null) return;
        
        try {
            // Reduce ViewPager buffer size to reduce memory usage
            bannerViewPager.setOffscreenPageLimit(1);
            
            // Enhance page transition effects
            int pageMargin = getResources().getDimensionPixelOffset(R.dimen.viewpager_margin);
            int pageOffset = getResources().getDimensionPixelOffset(R.dimen.viewpager_offset);
            
            bannerViewPager.setPageTransformer((page, position) -> {
                float myOffset = position * -(2 * pageOffset + pageMargin);
                if (position < -1) {
                    page.setTranslationX(-myOffset);
                } else if (position <= 1) {
                    float scaleFactor = Math.max(0.8f, 1 - Math.abs(position));
                    page.setTranslationX(myOffset);
                    page.setScaleY(scaleFactor);
                    page.setAlpha(scaleFactor);
                } else {
                    page.setTranslationX(myOffset);
                }
            });
            
            // Initialize banner adapter
            bannerAdapter = new BannerAdapter(getContext(), bannerImages);
            
            // Set banner click listener
            bannerAdapter.setOnBannerClickListener(position -> {
                // Handle banner click event
                if (getContext() != null) {
                    // Show notification or navigate based on selected banner
                    String bannerMessage = "You selected banner " + (position + 1);
                    
                    // Add navigation logic based on banner here
                    // Example: open web page, show promotion details, etc.
                    
                    Toast.makeText(getContext(), bannerMessage, Toast.LENGTH_SHORT).show();
                }
            });
            
            bannerViewPager.setAdapter(bannerAdapter);

            // Auto-scroll behavior
            bannerPageChangeCallback = new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    currentBannerPosition = position;
                }
            };
            
            bannerViewPager.registerOnPageChangeCallback(bannerPageChangeCallback);

            startBannerAutoScroll();
        } catch (Exception e) {
            // Log or handle exception
            Log.e(TAG, "Error setting up banner carousel", e);
        }
    }

    private void startBannerAutoScroll() {
        if (getContext() == null || !isAdded()) return;

        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded() || getContext() == null) return;
                
                try {
                    if (bannerViewPager != null) {
                        if (currentBannerPosition == bannerImages.length - 1) {
                            currentBannerPosition = 0;
                        } else {
                            currentBannerPosition++;
                        }
                        bannerViewPager.setCurrentItem(currentBannerPosition, true);
                    }
                } catch (Exception e) {
                    // Log or handle exception
                }
            }
        };

        try {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (handler != null && isAdded() && getContext() != null) {
                        handler.post(runnable);
                    }
                }
            }, DELAY_MS, PERIOD_MS);
        } catch (Exception e) {
            // Log or handle exception
        }
    }

    private void setupTransactionList() {
        if (getContext() == null || !isAdded() || recentTransactionsRecyclerView == null) return;
        
        try {
            // Initialize adapter with empty list
            transactionAdapter = new TransactionAdapter(getContext(), new ArrayList<>());
            
            // Set click listener for transaction items
            transactionAdapter.setOnItemClickListener(transaction -> {
                if (getContext() != null) {
                    // Display transaction details
                    String message = "Details: " + transaction.getCategoryName() + 
                            " - " + (transaction.isIncome() ? "+" : "-") + 
                            NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(transaction.getAmount());
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    
                    // Show detail dialog or navigate to detail screen
                    // TODO: Implement transaction detail view
                }
            });
            
            recentTransactionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recentTransactionsRecyclerView.setAdapter(transactionAdapter);
        } catch (Exception e) {
            // Log or handle exception
        }
    }

    private void setupClickListeners() {
        // Setup navigation to transaction detail screen when an item is clicked
        transactionAdapter.setOnItemClickListener(transaction -> {
            // Navigate to transaction detail screen when clicked
            // Navigation.findNavController(view).navigate(R.id.action_dashboardFragment_to_transactionDetailFragment);
        });

        // Add budget button
        addFundsButton.setOnClickListener(v -> {
            // Navigate to SettingsFragment
            SettingsFragment settingsFragment = new SettingsFragment();
            Bundle args = new Bundle();
            // Add parameter to let SettingsFragment know which tab to open
            args.putInt("OPEN_TAB", 2); // 2 = budget tab
            settingsFragment.setArguments(args);
            
            // Replace the current fragment
            requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_content, settingsFragment)
                .addToBackStack(null)
                .commit();
        });
    }

    private void observeViewModel() {
        if (getContext() == null || !isAdded() || dashboardViewModel == null) return;
        
        try {
            // Observe budget data
            dashboardViewModel.getBudget().observe(getViewLifecycleOwner(), budget -> {
                if (budget != null && isAdded()) {
                    updateBudgetUI(budget);
                }
            });

            // Observe formatted budget
            dashboardViewModel.getFormattedBudget().observe(getViewLifecycleOwner(), formattedBudget -> {
                if (formattedBudget != null && isAdded() && budgetAmountText != null) {
                    budgetAmountText.setText(formattedBudget);
                }
            });

            // Observe income
            dashboardViewModel.getFormattedIncome().observe(getViewLifecycleOwner(), income -> {
                if (income != null && isAdded() && incomeAmountText != null) {
                    incomeAmountText.setText(income);
                }
            });

            // Observe expense
            dashboardViewModel.getFormattedExpense().observe(getViewLifecycleOwner(), expense -> {
                if (expense != null && isAdded() && expenseAmountText != null) {
                    expenseAmountText.setText(expense);
                }
            });

            // Observe budget percentage
            dashboardViewModel.getBudgetPercentage().observe(getViewLifecycleOwner(), percentage -> {
                if (percentage != null && isAdded()) {
                    updateBudgetProgress(percentage);
                }
            });

            // Observe is over budget
            dashboardViewModel.getIsOverBudget().observe(getViewLifecycleOwner(), isOver -> {
                if (isOver != null && isOver && isAdded()) {
                    sendBudgetWarningEmail();
                }
            });

            // Observe recent transactions
            dashboardViewModel.getRecentTransactions().observe(getViewLifecycleOwner(), transactions -> {
                if (isAdded()) {
                    updateTransactionsList(transactions);
                }
            });
        } catch (Exception e) {
            // Log or handle exception
        }
    }

    private void updateBudgetUI(Budget budget) {
        if (budget == null || !isAdded() || getContext() == null) return;
        
        try {
            double percentage = budget.getPercentageUsed();
            
            // Update progress bar
            if (budgetProgressBar != null) {
                budgetProgressBar.setProgress((int) percentage);
            }
            
            // Update warning text
            if (budgetWarningText != null) {
                String warningText = getString(R.string.used_budget_percentage, (int)percentage);
                budgetWarningText.setText(warningText);
                
                // Change color based on percentage
                if (percentage >= 90) {
                    budgetWarningText.setTextColor(Color.RED);
                } else if (percentage >= 70) {
                    budgetWarningText.setTextColor(Color.parseColor("#FF9800")); // Orange
                } else {
                    budgetWarningText.setTextColor(Color.parseColor("#2E7D32")); // Green
                }
            }
        } catch (Exception e) {
            // Log or handle exception
        }
    }

    private void updateBudgetProgress(double percentage) {
        if (!isAdded() || getContext() == null || budgetProgressBar == null) return;
        
        try {
            budgetProgressBar.setProgress((int) percentage);
        } catch (Exception e) {
            // Log or handle exception
        }
    }

    private void updateTransactionsList(List<Transaction> transactions) {
        if (!isAdded() || getContext() == null) return;
        
        try {
            if (noTransactionsText != null && recentTransactionsRecyclerView != null && transactionAdapter != null) {
                if (transactions == null || transactions.isEmpty()) {
                    noTransactionsText.setVisibility(View.VISIBLE);
                    recentTransactionsRecyclerView.setVisibility(View.GONE);
                } else {
                    noTransactionsText.setVisibility(View.GONE);
                    recentTransactionsRecyclerView.setVisibility(View.VISIBLE);
                    transactionAdapter.updateData(transactions);
                }
            }
        } catch (Exception e) {
            // Log or handle exception
        }
    }

    private void sendBudgetWarningEmail() {
        try {
            // Check if app notifications are enabled
            SharedPreferences notificationPrefs = requireActivity().getSharedPreferences("notification_settings", Context.MODE_PRIVATE);
            boolean appNotificationsEnabled = notificationPrefs.getBoolean("app_notifications_enabled", true);
            
            if (!appNotificationsEnabled) {
                // If app notifications are disabled, don't send email and don't show any notification
                Log.d("DashboardFragment", "Notifications and emails have been disabled");
                return; // Skip completely - no email, no toast
            }
            
            // Get user email from shared preferences
            SharedPreferences preferences = getContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            String userEmail = preferences.getString("user_email", null);
            
            // Check setting for email notifications
            if (preferences.getBoolean("disable_email_notifications", false)) {
                Log.d("DashboardFragment", "Notifications and emails have been disabled");
                return;
            }
            
            if (userEmail != null && !userEmail.isEmpty()) {
                // Use EmailService to send email directly
                new EmailService().sendBudgetWarningEmail(userEmail);
                
                // Notify user
                Log.d("DashboardFragment", "Budget warning email has been sent to: " + userEmail);
                Toast.makeText(getContext(), "You have exceeded your monthly budget. A warning email has been sent.", Toast.LENGTH_LONG).show();
            } else {
                // No email configured
                Toast.makeText(getContext(), "You have exceeded your monthly budget. Please set up an email to receive alerts.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e("DashboardFragment", "Error sending warning email: " + e.getMessage(), e);
        }
    }

    @Override
    public void onDestroyView() {
        // Stop timer to prevent memory leaks
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        
        // Remove callbacks to prevent leaks
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        
        // Unregister ViewPager2 callback
        if (bannerViewPager != null && bannerPageChangeCallback != null) {
            bannerViewPager.unregisterOnPageChangeCallback(bannerPageChangeCallback);
            bannerPageChangeCallback = null;
        }
        
        // Clear adapters
        if (bannerViewPager != null) {
            bannerViewPager.setAdapter(null);
        }
        
        if (recentTransactionsRecyclerView != null) {
            recentTransactionsRecyclerView.setAdapter(null);
        }
        
        // Null out views
        budgetAmountText = null;
        budgetWarningText = null;
        budgetProgressBar = null;
        incomeAmountText = null;
        expenseAmountText = null;
        bannerViewPager = null;
        recentTransactionsRecyclerView = null;
        noTransactionsText = null;
        addFundsButton = null;
        
        super.onDestroyView();
    }
    
    @Override
    public void onDestroy() {
        // Clear all adapters and data
        bannerAdapter = null;
        transactionAdapter = null;
        
        // Clear the viewmodel
        dashboardViewModel = null;
        
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            // Refresh data when returning to this fragment
            if (dashboardViewModel != null) {
                dashboardViewModel.loadData(); // Call loadData directly instead of refreshData
            }
            
            // Restart banner auto-scroll if needed
            if (timer == null && isAdded() && getContext() != null && bannerViewPager != null) {
                startBannerAutoScroll();
            }
        } catch (Exception e) {
            // Log or handle exception
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            // Stop banner auto-scroll when fragment is not visible
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
        } catch (Exception e) {
            // Log or handle exception
        }
    }
    
    /**
     * Update dashboard data
     * Called from MainActivity when refresh is needed
     */
    public void refreshData() {
        try {
            if (dashboardViewModel != null) {
                dashboardViewModel.refreshData();
                
                // Load data
                dashboardViewModel.loadData();
                
                // Show confirmation
                Toast.makeText(getContext(), "Data updated", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            // Log error
        }
    }
} 