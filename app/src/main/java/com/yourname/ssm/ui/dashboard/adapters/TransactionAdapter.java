package com.yourname.ssm.ui.dashboard.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.yourname.ssm.R;
import com.yourname.ssm.model.Transaction;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private Context context;
    private List<Transaction> transactions;
    private NumberFormat currencyFormatter;
    private SimpleDateFormat inputDateFormat;
    private SimpleDateFormat outputDateFormat;
    private OnItemClickListener listener;

    // Interface for handling click events
    public interface OnItemClickListener {
        void onItemClick(Transaction transaction);
    }
    
    // Method to set the listener
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public TransactionAdapter(Context context, List<Transaction> transactions) {
        this.context = context;
        this.transactions = transactions;
        this.currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        this.inputDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        this.outputDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);
        
        // Set category name - use resource strings if available
        String categoryName = transaction.getCategoryName();
        if (categoryName != null) {
            String resourceName = getCategoryResourceName(categoryName);
            if (resourceName != null) {
                int resourceId = context.getResources().getIdentifier(resourceName, "string", context.getPackageName());
                if (resourceId != 0) {
                    categoryName = context.getString(resourceId);
                }
            }
            holder.categoryNameTextView.setText(categoryName);
        } else {
            holder.categoryNameTextView.setText(transaction.isIncome() ? context.getString(R.string.income) : context.getString(R.string.expense));
        }
        
        // Set note (if available)
        if (transaction.getNote() != null && !transaction.getNote().isEmpty()) {
            holder.noteTextView.setText(transaction.getNote());
            holder.noteTextView.setVisibility(View.VISIBLE);
        } else {
            holder.noteTextView.setVisibility(View.GONE);
        }
        
        // Format and set date
        try {
            Date date = inputDateFormat.parse(transaction.getDate());
            holder.dateTextView.setText(outputDateFormat.format(date));
        } catch (ParseException e) {
            holder.dateTextView.setText(transaction.getDate());
        }
        
        // Set amount and color based on transaction type
        String formattedAmount;
        if (transaction.isIncome()) {
            formattedAmount = "+" + currencyFormatter.format(transaction.getAmount());
            holder.amountTextView.setTextColor(ContextCompat.getColor(context, R.color.colorIncome));
        } else {
            formattedAmount = "-" + currencyFormatter.format(transaction.getAmount());
            holder.amountTextView.setTextColor(ContextCompat.getColor(context, R.color.colorExpense));
        }
        holder.amountTextView.setText(formattedAmount);
        
        // Set category icon - handle invalid resource IDs
        try {
            int iconResourceId = transaction.getCategoryIcon();
            // Check for valid resource ID, use default if invalid
            if (iconResourceId <= 0) {
                // Use default icon based on transaction type
                iconResourceId = transaction.isIncome() 
                    ? R.drawable.thunhap  // Default icon for income
                    : R.drawable.chitieu; // Default icon for expense
            }
            holder.categoryIconImageView.setImageResource(iconResourceId);
        } catch (Exception e) {
            // Use default icon based on transaction type
            holder.categoryIconImageView.setImageResource(
                transaction.isIncome() ? R.drawable.thunhap : R.drawable.chitieu
            );
        }
        
        // Set click event for the item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(transaction);
            }
        });
    }

    // Helper method to map Vietnamese category names to resource strings
    private String getCategoryResourceName(String categoryName) {
        switch (categoryName) {
            case "Ăn uống": return "category_food";
            case "Di chuyển": return "category_transport";
            case "Mua sắm": return "category_shopping";
            case "Giải trí": return "category_entertainment"; 
            case "Y tế": return "category_health";
            case "Giáo dục": return "category_education";
            case "Nhà ở": return "category_housing";
            case "Hóa đơn & Tiện ích": return "category_utilities";
            case "Du lịch": return "category_travel";
            case "Khác": return "category_other";
            case "Lương": return "category_salary";
            case "Đầu tư": return "category_investment";
            case "Quà tặng": return "category_gift";
            case "Học bổng": return "category_scholarship";
            case "Bán hàng": return "category_sales";
            case "Thưởng": return "category_bonus";
            case "Cho vay": return "category_loan";
            case "Hoàn tiền": return "category_refund";
            case "Thu nhập phụ": return "category_part_time";
            case "Dịch vụ": return "category_service";
            default: return null;
        }
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public void updateData(List<Transaction> newTransactions) {
        this.transactions = newTransactions;
        notifyDataSetChanged();
    }

    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        ImageView categoryIconImageView;
        TextView categoryNameTextView;
        TextView noteTextView;
        TextView dateTextView;
        TextView amountTextView;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryIconImageView = itemView.findViewById(R.id.transactionCategoryIcon);
            categoryNameTextView = itemView.findViewById(R.id.transactionCategoryName);
            noteTextView = itemView.findViewById(R.id.transactionNote);
            dateTextView = itemView.findViewById(R.id.transactionDate);
            amountTextView = itemView.findViewById(R.id.transactionAmount);
        }
    }
} 