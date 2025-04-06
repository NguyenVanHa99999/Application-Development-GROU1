package com.yourname.ssm.ui.statistics;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yourname.ssm.R;
import com.yourname.ssm.model.Transaction;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionHistoryAdapter extends RecyclerView.Adapter<TransactionHistoryAdapter.TransactionViewHolder> {
    
    private final List<Transaction> transactions;
    private final OnTransactionClickListener listener;
    
    // Format dates
    private final SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    
    // Format currency
    private final NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    
    public interface OnTransactionClickListener {
        void onTransactionClick(Transaction transaction);
        void onEditClick(Transaction transaction);
        void onDeleteClick(Transaction transaction);
    }
    
    public TransactionHistoryAdapter(List<Transaction> transactions, OnTransactionClickListener listener) {
        this.transactions = transactions;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction_history, parent, false);
        return new TransactionViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);
        holder.bind(transaction);
    }
    
    @Override
    public int getItemCount() {
        return transactions.size();
    }
    
    class TransactionViewHolder extends RecyclerView.ViewHolder {
        
        private final ImageView categoryIcon;
        private final TextView categoryName;
        private final TextView amount;
        private final TextView date;
        private final TextView note;
        private final ImageView editButton;
        private final ImageView deleteButton;
        
        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryIcon = itemView.findViewById(R.id.iv_category_icon);
            categoryName = itemView.findViewById(R.id.tv_category_name);
            amount = itemView.findViewById(R.id.tv_amount);
            date = itemView.findViewById(R.id.tv_date);
            note = itemView.findViewById(R.id.tv_note);
            editButton = itemView.findViewById(R.id.btn_edit);
            deleteButton = itemView.findViewById(R.id.btn_delete);
        }
        
        public void bind(Transaction transaction) {
            try {
                // Set category icon
                try {
                    categoryIcon.setImageResource(transaction.getCategoryIcon());
                } catch (Exception e) {
                    // Fallback to default icon if resource not found
                    categoryIcon.setImageResource(R.drawable.ic_category_tech);
                }
                
                // Set category name - translate from Vietnamese to English
                String categoryDisplayName = getCategoryTranslation(transaction.getCategoryName());
                categoryName.setText(categoryDisplayName);
                
                // Set amount with appropriate color
                String amountText = (transaction.isIncome() ? "+" : "-") + 
                        currencyFormat.format(transaction.getAmount()) + "đ";
                amount.setText(amountText);
                amount.setTextColor(itemView.getContext().getResources().getColor(
                        transaction.isIncome() ? android.R.color.holo_green_dark : android.R.color.holo_red_dark
                ));
                
                // Format and set date
                try {
                    Date transactionDate = apiDateFormat.parse(transaction.getDate());
                    if (transactionDate != null) {
                        date.setText(displayDateFormat.format(transactionDate));
                    } else {
                        date.setText(transaction.getDate());
                    }
                } catch (ParseException e) {
                    date.setText(transaction.getDate());
                }
                
                // Set note
                if (transaction.getNote() != null && !transaction.getNote().isEmpty()) {
                    note.setText(transaction.getNote());
                    note.setVisibility(View.VISIBLE);
                } else {
                    note.setVisibility(View.GONE);
                }
                
                // Set click listeners
                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onTransactionClick(transaction);
                    }
                });
                
                editButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onEditClick(transaction);
                    }
                });
                
                deleteButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDeleteClick(transaction);
                    }
                });
            } catch (Exception e) {
                // Handle any errors
            }
        }
        
        // Helper method to translate Vietnamese category names to English
        private String getCategoryTranslation(String vietnameseName) {
            if (vietnameseName == null) return "";
            
            switch (vietnameseName) {
                case "Ăn uống": return "Food";
                case "Di chuyển": return "Transport";
                case "Mua sắm": return "Shopping";
                case "Giải trí": return "Entertainment"; 
                case "Y tế": return "Healthcare";
                case "Giáo dục": return "Education";
                case "Nhà ở": return "Housing";
                case "Hóa đơn & Tiện ích": return "Utilities";
                case "Du lịch": return "Travel";
                case "Khác": return "Other";
                case "Lương": return "Salary";
                case "Đầu tư": return "Investment";
                case "Quà tặng": return "Gift";
                case "Học bổng": return "Scholarship";
                case "Bán hàng": return "Sales";
                case "Thưởng": return "Bonus";
                case "Cho vay": return "Loan";
                case "Hoàn tiền": return "Refund";
                case "Thu nhập phụ": return "Part-time";
                case "Dịch vụ": return "Services";
                default: return vietnameseName; // Keep original if no translation
            }
        }
    }
} 