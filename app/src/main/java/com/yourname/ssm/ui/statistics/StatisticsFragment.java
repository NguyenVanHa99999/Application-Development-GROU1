package com.yourname.ssm.ui.statistics;

import android.app.DatePickerDialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.interfaces.datasets.IPieDataSet;
import com.github.mikephil.charting.renderer.PieChartRenderer;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.github.mikephil.charting.animation.ChartAnimator;
import com.yourname.ssm.R;
import com.yourname.ssm.model.Transaction;
import com.yourname.ssm.repository.LoginUserRepository;
import com.yourname.ssm.repository.TransactionRepository;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class StatisticsFragment extends Fragment {

    private static final String TAG = "StatisticsFragment";
    
    // UI Components
    private TextView statisticsTitle;
    private TextView dateFromTextView;
    private TextView dateToTextView;
    private Button applyFilterButton;
    private Button viewHistoryButton;
    private BarChart barChart;
    private PieChart pieChart;
    private TextView totalExpenseTextView;
    private TextView avgExpenseTextView;
    private TextView transactionCountTextView;
    
    // Data
    private TransactionRepository transactionRepository;
    private LoginUserRepository loginUserRepository;
    private int userId;
    private StatisticsViewModel viewModel;
    
    // Date filters
    private Calendar fromDate = Calendar.getInstance();
    private Calendar toDate = Calendar.getInstance();
    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    
    // Background operations
    private final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);
        
        try {
            // Khởi tạo repository
            transactionRepository = new TransactionRepository(requireContext());
            loginUserRepository = new LoginUserRepository(requireContext());
            userId = loginUserRepository.getUserId();
            
            // Khởi tạo ViewModel
            viewModel = new ViewModelProvider(this).get(StatisticsViewModel.class);
            
            // Ánh xạ các view
            initViews(view);
            
            // Thiết lập ngày mặc định (tháng hiện tại)
            setupDefaultDates();
            
            // Thiết lập listener cho các component
            setupListeners();
            
            // Load dữ liệu và hiển thị ban đầu
            loadData();
            
            return view;
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreateView", e);
            Toast.makeText(requireContext(), "Lỗi khởi tạo màn hình: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return view;
        }
    }
    
    private void initViews(View view) {
        try {
            statisticsTitle = view.findViewById(R.id.statistics_title);
            dateFromTextView = view.findViewById(R.id.tv_date_from);
            dateToTextView = view.findViewById(R.id.tv_date_to);
            applyFilterButton = view.findViewById(R.id.btn_apply_filter);
            viewHistoryButton = view.findViewById(R.id.btn_view_history);
            barChart = view.findViewById(R.id.bar_chart);
            pieChart = view.findViewById(R.id.pie_chart);
            totalExpenseTextView = view.findViewById(R.id.tv_total_expense);
            avgExpenseTextView = view.findViewById(R.id.tv_avg_expense);
            transactionCountTextView = view.findViewById(R.id.tv_transaction_count);
            
            // Thiết lập các biểu đồ
            setupBarChart();
            setupPieChart();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            throw e;
        }
    }
    
    private void setupDefaultDates() {
        // Thiết lập ngày đầu tháng hiện tại
        fromDate.set(Calendar.DAY_OF_MONTH, 1);
        
        // Thiết lập ngày cuối tháng hiện tại
        toDate = (Calendar) fromDate.clone();
        toDate.add(Calendar.MONTH, 1);
        toDate.add(Calendar.DAY_OF_MONTH, -1);
        
        // Cập nhật UI
        dateFromTextView.setText(displayDateFormat.format(fromDate.getTime()));
        dateToTextView.setText(displayDateFormat.format(toDate.getTime()));
        
        // Cập nhật tiêu đề
        updateTitle();
    }
    
    private void updateTitle() {
        // Hiển thị rõ hơn về khoảng thời gian được chọn
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
        SimpleDateFormat dayMonthFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
        
        // Kiểm tra xem khoảng thời gian có phải là cả tháng không
        boolean isEntireMonth = isEntireMonth(fromDate, toDate);
        
        if (isEntireMonth) {
            // Nếu là cả tháng, hiển thị "Tháng MM/YYYY"
            int month = fromDate.get(Calendar.MONTH) + 1;
            int year = fromDate.get(Calendar.YEAR);
            statisticsTitle.setText("Chi tiêu tháng " + month + "/" + year);
        } else {
            // Nếu không, hiển thị khoảng thời gian cụ thể
            String fromDateStr = dayMonthFormat.format(fromDate.getTime());
            String toDateStr = dayMonthFormat.format(toDate.getTime());
            statisticsTitle.setText("Chi tiêu từ " + fromDateStr + " đến " + toDateStr);
        }
    }
    
    // Kiểm tra xem khoảng thời gian có phải là cả tháng không
    private boolean isEntireMonth(Calendar from, Calendar to) {
        // Kiểm tra xem from có phải là ngày đầu tháng không
        boolean isFirstDayOfMonth = from.get(Calendar.DAY_OF_MONTH) == 1;
        
        // Kiểm tra xem to có phải là ngày cuối tháng không
        Calendar lastDayOfMonth = (Calendar) from.clone();
        lastDayOfMonth.add(Calendar.MONTH, 1);
        lastDayOfMonth.add(Calendar.DAY_OF_MONTH, -1);
        
        boolean isLastDayOfMonth = to.get(Calendar.DAY_OF_MONTH) == lastDayOfMonth.get(Calendar.DAY_OF_MONTH) &&
                                  to.get(Calendar.MONTH) == lastDayOfMonth.get(Calendar.MONTH) &&
                                  to.get(Calendar.YEAR) == lastDayOfMonth.get(Calendar.YEAR);
        
        return isFirstDayOfMonth && isLastDayOfMonth;
    }
    
    private void setupListeners() {
        // Listener cho chọn ngày bắt đầu
        dateFromTextView.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        fromDate.set(Calendar.YEAR, year);
                        fromDate.set(Calendar.MONTH, month);
                        fromDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        dateFromTextView.setText(displayDateFormat.format(fromDate.getTime()));
                        
                        // Nếu ngày bắt đầu > ngày kết thúc, cập nhật ngày kết thúc
                        if (fromDate.after(toDate)) {
                            toDate.setTime(fromDate.getTime());
                            dateToTextView.setText(displayDateFormat.format(toDate.getTime()));
                        }
                    },
                    fromDate.get(Calendar.YEAR),
                    fromDate.get(Calendar.MONTH),
                    fromDate.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
        
        // Listener cho chọn ngày kết thúc
        dateToTextView.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        toDate.set(Calendar.YEAR, year);
                        toDate.set(Calendar.MONTH, month);
                        toDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        dateToTextView.setText(displayDateFormat.format(toDate.getTime()));
                        
                        // Nếu ngày kết thúc < ngày bắt đầu, cập nhật ngày bắt đầu
                        if (toDate.before(fromDate)) {
                            fromDate.setTime(toDate.getTime());
                            dateFromTextView.setText(displayDateFormat.format(fromDate.getTime()));
                        }
                    },
                    toDate.get(Calendar.YEAR),
                    toDate.get(Calendar.MONTH),
                    toDate.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
        
        // Listener cho nút áp dụng lọc
        applyFilterButton.setOnClickListener(v -> {
            loadData();
            updateTitle();
        });
        
        // Listener cho nút xem lịch sử giao dịch
        viewHistoryButton.setOnClickListener(v -> {
            showTransactionHistory();
        });
    }
    
    private void showTransactionHistory() {
        try {
            TransactionHistoryFragment historyFragment = new TransactionHistoryFragment();
            Bundle args = new Bundle();
            args.putString("from_date", apiDateFormat.format(fromDate.getTime()));
            args.putString("to_date", apiDateFormat.format(toDate.getTime()));
            historyFragment.setArguments(args);
            
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.main_content, historyFragment, "transaction_history");
            transaction.addToBackStack(null);
            transaction.commit();
        } catch (Exception e) {
            Log.e(TAG, "Error showing transaction history", e);
            Toast.makeText(requireContext(), "Lỗi mở lịch sử giao dịch: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void loadData() {
        if (userId <= 0) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để xem thống kê", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // Hiển thị trạng thái đang tải
            showLoading(true);
            
            // Cập nhật tiêu đề thể hiện khoảng thời gian được chọn
            updateTitle();
            
            // Truy vấn dữ liệu trên background thread
            executor.execute(() -> {
                try {
                    String fromDateStr = apiDateFormat.format(fromDate.getTime());
                    String toDateStr = apiDateFormat.format(toDate.getTime());
                    
                    // Lấy danh sách giao dịch từ repository
                    List<Transaction> transactions = transactionRepository.getTransactionsForDateRange(userId, fromDateStr, toDateStr);
                    
                    // Log dữ liệu đã lấy từ DB để dễ dàng debug
                    Log.d(TAG, "Đã lấy " + transactions.size() + " giao dịch từ " + fromDateStr + " đến " + toDateStr);
                    
                    // Tạo map chứa tất cả các ngày trong khoảng thời gian, mỗi ngày một entry
                    Map<String, List<Transaction>> dailyTransactions = new TreeMap<>();
                    Map<String, Double> categoryExpenses = new HashMap<>();
                    
                    // Khởi tạo map với tất cả các ngày trong khoảng thời gian
                    Calendar currentDate = (Calendar) fromDate.clone();
                    while (!currentDate.after(toDate)) {
                        String dateKey = apiDateFormat.format(currentDate.getTime());
                        dailyTransactions.put(dateKey, new ArrayList<>());
                        currentDate.add(Calendar.DAY_OF_MONTH, 1);
                    }
                    
                    double totalExpense = 0;
                    int expenseCount = 0;
                    
                    for (Transaction transaction : transactions) {
                        if (transaction.isExpense()) {
                            // Thêm vào tổng chi tiêu
                            totalExpense += transaction.getAmount();
                            expenseCount++;
                            
                            // Nhóm theo ngày
                            String dayKey = transaction.getDate();
                            if (dailyTransactions.containsKey(dayKey)) {
                                dailyTransactions.get(dayKey).add(transaction);
                            }
                            
                            // Nhóm theo danh mục
                            String categoryKey = transaction.getCategoryName();
                            if (categoryKey == null || categoryKey.trim().isEmpty()) {
                                categoryKey = "Khác";
                            }
                            
                            // Log để debug
                            Log.d(TAG, "Giao dịch: Danh mục=" + categoryKey + ", Số tiền=" + transaction.getAmount());
                            
                            double currentAmount = categoryExpenses.getOrDefault(categoryKey, 0.0);
                            categoryExpenses.put(categoryKey, currentAmount + transaction.getAmount());
                        }
                    }
                    
                    // Log kết quả nhóm theo danh mục
                    for (Map.Entry<String, Double> entry : categoryExpenses.entrySet()) {
                        Log.d(TAG, "Danh mục: " + entry.getKey() + ", Tổng chi tiêu: " + entry.getValue());
                    }
                    
                    // Tính trung bình chi tiêu hàng ngày
                    double avgExpense = expenseCount > 0 ? totalExpense / expenseCount : 0;
                    
                    // Chuẩn bị dữ liệu cho bar chart
                    List<BarEntry> barEntries = prepareBarChartData(dailyTransactions);
                    
                    // Chuẩn bị dữ liệu cho pie chart
                    List<PieEntry> pieEntries = preparePieChartData(categoryExpenses, totalExpense);
                    
                    // Cập nhật UI trên main thread
                    final double finalTotalExpense = totalExpense;
                    final double finalAvgExpense = avgExpense;
                    final int finalExpenseCount = expenseCount;
                    
                    requireActivity().runOnUiThread(() -> {
                        try {
                            // Cập nhật thống kê chi tiêu
                            updateStatistics(finalTotalExpense, finalAvgExpense, finalExpenseCount);
                            
                            // Cập nhật và hiển thị biểu đồ cột
                            updateBarChart(barEntries);
                            
                            // Cập nhật và hiển thị biểu đồ tròn
                            updatePieChart(pieEntries);
                            
                            showLoading(false);
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating UI with data", e);
                            Toast.makeText(requireContext(), "Lỗi cập nhật giao diện: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error loading data", e);
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        showLoading(false);
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in loadData", e);
            Toast.makeText(requireContext(), "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            showLoading(false);
        }
    }
    
    private List<BarEntry> prepareBarChartData(Map<String, List<Transaction>> dailyTransactions) {
        List<BarEntry> entries = new ArrayList<>();
        int index = 0;
        
        // Xử lý mỗi ngày trong khoảng thời gian, kể cả những ngày không có giao dịch
        for (Map.Entry<String, List<Transaction>> entry : dailyTransactions.entrySet()) {
            List<Transaction> dayTransactions = entry.getValue();
            
            if (dayTransactions.isEmpty()) {
                // Ngày không có giao dịch
                // Sử dụng giá trị 0 cho tất cả các tham số của cột
                entries.add(new BarEntry(index++, 0f));
                continue;
            }
            
            // Tính tổng chi tiêu trong ngày
            double dayTotal = 0;
            
            for (Transaction t : dayTransactions) {
                if (t.isExpense()) {
                    double amount = t.getAmount();
                    dayTotal += amount;
                }
            }
            
            // Nếu không có giao dịch chi tiêu nào
            if (dayTotal == 0) {
                entries.add(new BarEntry(index++, 0f));
                continue;
            }
            
            // Sử dụng tổng chi tiêu trong ngày cho chiều cao của cột
            entries.add(new BarEntry(index++, (float) dayTotal));
        }
        
        return entries;
    }
    
    private List<PieEntry> preparePieChartData(Map<String, Double> categoryExpenses, double totalExpense) {
        List<PieEntry> entries = new ArrayList<>();
        
        // Nếu không có dữ liệu, trả về danh sách rỗng
        if (categoryExpenses.isEmpty() || totalExpense == 0) {
            Log.d(TAG, "Không có dữ liệu chi tiêu trong khoảng thời gian từ " 
                  + displayDateFormat.format(fromDate.getTime()) 
                  + " đến " 
                  + displayDateFormat.format(toDate.getTime()));
            return entries;
        }
        
        // Sắp xếp theo giá trị chi tiêu (từ cao xuống thấp)
        List<Map.Entry<String, Double>> sortedList = new ArrayList<>(categoryExpenses.entrySet());
        sortedList.sort((o1, o2) -> Double.compare(o2.getValue(), o1.getValue()));
        
        // Thêm các danh mục vào biểu đồ
        for (Map.Entry<String, Double> entry : sortedList) {
            String category = entry.getKey();
            double amount = entry.getValue();
            
            float percentage = (float) (amount / totalExpense * 100);
            Log.d(TAG, "Danh mục: " + category + ", Số tiền: " + amount + ", Tỷ lệ: " + percentage + "%");
            
            entries.add(new PieEntry(percentage, category, amount));
        }
        
        return entries;
    }
    
    private void updateStatistics(double totalExpense, double avgExpense, int transactionCount) {
        try {
            // Format tiền tệ
            NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
            
            // Cập nhật text views
            totalExpenseTextView.setText(currencyFormat.format(totalExpense) + "đ");
            avgExpenseTextView.setText(currencyFormat.format(avgExpense) + "đ");
            transactionCountTextView.setText(String.valueOf(transactionCount));
        } catch (Exception e) {
            Log.e(TAG, "Error updating statistics UI", e);
        }
    }
    
    private void setupBarChart() {
        barChart.setBackgroundColor(Color.WHITE);
        barChart.getDescription().setEnabled(false);
        
        // Tắt các tương tác không cần thiết
        barChart.setPinchZoom(false);
        barChart.setDoubleTapToZoomEnabled(false);
        barChart.setDrawGridBackground(false);
        
        // Thiết lập X axis
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        
        // Thiết lập Y axis bên trái
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setDrawAxisLine(true);
        leftAxis.setGridColor(Color.rgb(200, 200, 200)); // Màu xám nhạt hơn cho lưới  
        leftAxis.setTextColor(Color.GRAY);   // Màu xám cho chữ
        leftAxis.setAxisLineColor(Color.GRAY); // Màu xám cho trục
        leftAxis.setDrawZeroLine(false); // Ẩn đường 0
        
        // Thiết lập Y axis bên phải (ẩn)
        barChart.getAxisRight().setEnabled(false);
        
        // Thiết lập Legend
        Legend legend = barChart.getLegend();
        legend.setEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.VERTICAL);
        legend.setDrawInside(true);
        
        // Cấu hình tương tác
        barChart.setTouchEnabled(true);
        barChart.setDragEnabled(true);
        barChart.setScaleEnabled(false);
        
        // Bật chế độ cuộn ngang
        barChart.setVisibleXRangeMaximum(7); // Hiển thị tối đa 7 cột cùng lúc
        barChart.moveViewToX(0); // Di chuyển đến vị trí ban đầu
    }
    
    private void updateBarChart(List<BarEntry> entries) {
        if (entries.isEmpty()) {
            barChart.setNoDataText("Không có dữ liệu");
            barChart.invalidate();
            return;
        }
        
        // Tạo dataset
        BarDataSet dataSet = new BarDataSet(entries, "Chi tiêu theo ngày");
        dataSet.setDrawValues(true);
        
        // Tạo mảng màu cho các cột
        int[] colors = new int[] {
            Color.rgb(76, 175, 80),    // Green
            Color.rgb(255, 235, 59),   // Yellow
            Color.rgb(255, 193, 7),    // Amber
            Color.rgb(255, 152, 0),    // Orange
            Color.rgb(255, 87, 34),    // Deep Orange
            Color.rgb(121, 85, 72),    // Brown
            Color.rgb(156, 39, 176),   // Purple
            Color.rgb(63, 81, 181),    // Indigo
            Color.rgb(33, 150, 243),   // Blue
            Color.rgb(3, 169, 244)     // Light Blue  
        };
        dataSet.setColors(colors);
        
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(10f);
        
        // Định dạng text giá trị
        dataSet.setValueFormatter(new IValueFormatter() {
            @Override
            public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
                // Ẩn nhãn khi giá trị là 0
                if (value == 0f) {
                    return "";
                }
                return String.format("%,.0f", value) + "đ";
            }
        });
        
        // Cài đặt để không hiển thị điểm trên đường
        dataSet.setDrawIcons(false);
        
        // Tạo dữ liệu
        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f); // Giảm độ rộng của cột để tăng khoảng cách giữa các cột
        barChart.setFitBars(true);
        
        // Format trục X
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextSize(10f); // Giảm kích thước chữ để tránh chồng lấp
        xAxis.setTextColor(Color.DKGRAY);
        xAxis.setLabelRotationAngle(45f);
        
        // Thiết lập nhãn ngày tháng cho trục X
        List<String> dateLabels = formatDates();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dateLabels));
        
        // Hiển thị tất cả các nhãn ngày
        xAxis.setLabelCount(dateLabels.size());
        
        // Cấu hình trục Y
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f); // Giá trị min của trục Y
        leftAxis.setTextColor(Color.DKGRAY);
        leftAxis.setGridColor(Color.LTGRAY);
        leftAxis.setGridLineWidth(0.7f);
        
        // Hide 0 value on Y-axis by setting minimum slightly above zero
        leftAxis.setDrawZeroLine(false); 
        
        // Định dạng giá trị trục Y
        leftAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                // Trả về chuỗi rỗng khi giá trị là 0
                if (value == 0f) {
                    return "";
                }
                return String.format("%,.0f", value) + "đ";
            }
        });
        
        // Cập nhật biểu đồ
        barChart.setData(data);
        barChart.animateY(1000);
        
        // Thiết lập chế độ cuộn - chỉ hiển thị 7 cột cùng lúc
        barChart.setVisibleXRangeMaximum(7);
        barChart.setDragEnabled(true);
        barChart.moveViewToX(0);
        
        // Ẩn mô tả
        barChart.getDescription().setEnabled(false);
        
        // Ẩn legend
        barChart.getLegend().setEnabled(false);
        
        barChart.invalidate();
    }
    
    private List<String> formatDates() {
        List<String> formattedDates = new ArrayList<>();
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
        
        // Clone để không làm ảnh hưởng đến fromDate gốc
        Calendar currentDate = (Calendar) fromDate.clone();
        
        // Tạo nhãn cho mỗi ngày trong khoảng thời gian
        while (!currentDate.after(toDate)) {
            formattedDates.add(outputFormat.format(currentDate.getTime()));
            currentDate.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        return formattedDates;
    }
    
    private void setupPieChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleColor(Color.WHITE);
        pieChart.setTransparentCircleAlpha(110);
        pieChart.setHoleRadius(45f);
        pieChart.setTransparentCircleRadius(50f);
        pieChart.setDrawCenterText(true);
        pieChart.setCenterTextSize(14f);
        pieChart.setCenterTextColor(Color.DKGRAY);
        pieChart.setRotationEnabled(true);
        pieChart.setRotationAngle(0);
        pieChart.setHighlightPerTapEnabled(true);
        pieChart.setEntryLabelColor(Color.WHITE);
        pieChart.setEntryLabelTextSize(12f);
        pieChart.setDrawEntryLabels(false);
        
        // Thiết lập legend
        Legend legend = pieChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.CENTER);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        legend.setOrientation(Legend.LegendOrientation.VERTICAL);
        legend.setDrawInside(false);
        legend.setTextSize(13f); // Tăng kích thước chữ
        legend.setXEntrySpace(7f);
        legend.setYEntrySpace(7f); // Tăng khoảng cách giữa các mục
        legend.setWordWrapEnabled(true);
        legend.setMaxSizePercent(0.7f); // Giới hạn kích thước legend
    }
    
    private void updatePieChart(List<PieEntry> entries) {
        if (entries.isEmpty()) {
            pieChart.setNoDataText("Không có dữ liệu cho khoảng thời gian này");
            pieChart.invalidate();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "Danh mục chi tiêu");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueFormatter(new PercentFormatter());

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setDrawCenterText(true);
        pieChart.setCenterText("Chi tiêu\ntheo danh mục");
        pieChart.animateY(1000);

        Legend legend = pieChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.CENTER);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.VERTICAL);
        legend.setDrawInside(false);
        legend.setTextSize(12f);
        legend.setXEntrySpace(7f);
        legend.setYEntrySpace(5f);
        legend.setWordWrapEnabled(true);

        pieChart.setOnChartValueSelectedListener(new com.github.mikephil.charting.listener.OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, com.github.mikephil.charting.highlight.Highlight h) {
                if (e instanceof PieEntry) {
                    PieEntry pe = (PieEntry) e;
                    String category = pe.getLabel();
                    showTransactionHistoryForCategory(category);
                }
            }

            @Override
            public void onNothingSelected() {
                // Do nothing
            }
        });

        pieChart.invalidate();
    }
    
    private void showTransactionHistoryForCategory(String category) {
        try {
            TransactionHistoryFragment historyFragment = new TransactionHistoryFragment();
            Bundle args = new Bundle();
            args.putString("from_date", apiDateFormat.format(fromDate.getTime()));
            args.putString("to_date", apiDateFormat.format(toDate.getTime()));
            args.putString("category", category);
            historyFragment.setArguments(args);
            
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.main_content, historyFragment, "transaction_history");
            transaction.addToBackStack(null);
            transaction.commit();
            
            Toast.makeText(requireContext(), "Hiển thị chi tiêu loại: " + category, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing transaction history for category: " + category, e);
            Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    // Hiển thị chi tiết chi tiêu cho mỗi danh mục
    private void updateExpenseSummary(List<PieEntry> entries, TextView summaryTextView) {
        // Không cần thực hiện nếu không có dữ liệu
        if (entries == null || entries.isEmpty() || summaryTextView == null) {
            return;
        }
        
        NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        double totalAmount = 0;
        
        // Tính tổng số tiền
        for (PieEntry entry : entries) {
            Object data = entry.getData();
            if (data instanceof Number) {
                totalAmount += ((Number) data).doubleValue();
            }
        }
        
        // Cập nhật tổng số tiền
        summaryTextView.setText(currencyFormat.format(totalAmount) + "đ");
    }
    
    private void showLoading(boolean isLoading) {
        // Có thể thêm ProgressBar và xử lý logic hiển thị ở đây
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Đóng các resources nếu cần
        if (transactionRepository != null) {
            transactionRepository.close();
        }
    }
    
    /**
     * Lớp tùy chỉnh để vẽ biểu đồ tròn với các hiệu ứng đặc biệt
     */
    private static class CustomPieChartRenderer extends PieChartRenderer {
        public CustomPieChartRenderer(PieChart chart, com.github.mikephil.charting.animation.ChartAnimator animator, 
                                     com.github.mikephil.charting.utils.ViewPortHandler viewPortHandler) {
            super(chart, animator, viewPortHandler);
        }
        
        @Override
        public void drawValues(Canvas c) {
            // Gọi phương thức của lớp cha để vẽ các giá trị
            super.drawValues(c);
        }
        
        @Override
        public void drawExtras(Canvas c) {
            // Gọi phương thức của lớp cha để vẽ các thành phần bổ sung
            super.drawExtras(c);
        }
    }
} 