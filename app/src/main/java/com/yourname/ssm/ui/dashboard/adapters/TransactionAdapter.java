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

    // Interface để xử lý sự kiện click
    public interface OnItemClickListener {
        void onItemClick(Transaction transaction);
    }
    
    // Phương thức để thiết lập listener
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
        
        // Set category name
        holder.categoryNameTextView.setText(transaction.getCategoryName());
        
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
            // Kiểm tra ID tài nguyên hợp lệ, nếu không thì dùng icon mặc định
            if (iconResourceId <= 0) {
                // Sử dụng icon mặc định dựa vào loại giao dịch
                iconResourceId = transaction.isIncome() 
                    ? R.drawable.thunhap  // Icon mặc định cho thu nhập
                    : R.drawable.chitieu; // Icon mặc định cho chi tiêu
            }
            holder.categoryIconImageView.setImageResource(iconResourceId);
        } catch (Exception e) {
            // Dùng icon mặc định dựa vào loại giao dịch
            holder.categoryIconImageView.setImageResource(
                transaction.isIncome() ? R.drawable.thunhap : R.drawable.chitieu
            );
        }
        
        // Thiết lập sự kiện click cho item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(transaction);
            }
        });
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