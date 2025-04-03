package com.yourname.ssm.ui.addspending.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.yourname.ssm.R;
import com.yourname.ssm.model.Category;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
    private static final String TAG = "CategoryAdapter";
    
    private final Context context;
    private final List<Category> categories;
    private OnCategoryClickListener listener;
    private int selectedPosition = -1;
    
    public CategoryAdapter(Context context, List<Category> categories) {
        this.context = context;
        this.categories = categories;
        Log.d(TAG, "CategoryAdapter created with " + (categories != null ? categories.size() : 0) + " categories");
        if (categories != null) {
            for (int i = 0; i < categories.size(); i++) {
                Category cat = categories.get(i);
                Log.d(TAG, "Category[" + i + "]: " + cat.getName() + ", icon: " + cat.getIconResourceId());
            }
        }
    }
    
    public interface OnCategoryClickListener {
        void onCategoryClick(Category category, int position);
    }
    
    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.listener = listener;
    }
    
    public void setSelectedPosition(int position) {
        if (position >= 0 && position < categories.size()) {
            int oldSelectedPosition = selectedPosition;
            selectedPosition = position;
            
            // Hủy chọn mục cũ
            if (oldSelectedPosition != -1 && oldSelectedPosition < categories.size()) {
                notifyItemChanged(oldSelectedPosition);
            }
            
            // Chọn mục mới
            if (selectedPosition != -1) {
                notifyItemChanged(selectedPosition);
            }
        }
    }
    
    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        try {
            if (position < 0 || position >= categories.size()) {
                Log.e(TAG, "Invalid position: " + position + ", categories size: " + categories.size());
                return;
            }
            
            Category category = categories.get(position);
            
            // Thiết lập tên danh mục
            holder.categoryName.setText(category.getName());
            
            // Thiết lập icon
            try {
                int iconResourceId = category.getIconResourceId();
                Log.d(TAG, "Setting up icon for category: " + category.getName() + ", IconResId: " + iconResourceId);
                
                // Đặt icon trực tiếp, không cần nhiều kiểm tra
                try {
                    holder.categoryIcon.setImageResource(iconResourceId);
                } catch (Resources.NotFoundException e) {
                    Log.e(TAG, "Resource not found for " + category.getName() + ", ID: " + iconResourceId);
                    holder.categoryIcon.setImageResource(android.R.drawable.ic_menu_help);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error setting icon for category: " + category.getName(), e);
                holder.categoryIcon.setImageResource(android.R.drawable.ic_menu_help);
            }
            
            // Thiết lập màu nền dựa vào trạng thái được chọn
            if (selectedPosition == position) {
                holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimaryLight));
                holder.categoryName.setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
                holder.categoryIcon.setColorFilter(ContextCompat.getColor(context, R.color.colorAccent));
            } else {
                holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.colorSurface));
                holder.categoryName.setTextColor(ContextCompat.getColor(context, R.color.colorTextSecondary));
                holder.categoryIcon.setColorFilter(ContextCompat.getColor(context, R.color.colorIconTint));
            }
            
            // Thiết lập sự kiện click
            holder.itemView.setOnClickListener(v -> {
                try {
                    // Cập nhật vị trí được chọn
                    setSelectedPosition(holder.getAdapterPosition());
                    
                    // Gọi callback
                    if (listener != null) {
                        listener.onCategoryClick(category, holder.getAdapterPosition());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error handling click", e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in onBindViewHolder", e);
        }
    }
    
    @Override
    public int getItemCount() {
        int size = categories != null ? categories.size() : 0;
        Log.d(TAG, "getItemCount called, returning " + size);
        return size;
    }
    
    // Phương thức kiểm tra resource ID có hợp lệ không
    private boolean isValidResource(int resourceId) {
        if (resourceId <= 0) {
            return false;
        }
        
        try {
            // Nếu resourceId không tồn tại, getResourceTypeName sẽ ném ngoại lệ
            context.getResources().getResourceTypeName(resourceId);
            return true;
        } catch (Resources.NotFoundException e) {
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking resource validity", e);
            return false;
        }
    }
    
    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView categoryIcon;
        TextView categoryName;
        
        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            categoryIcon = itemView.findViewById(R.id.categoryIcon);
            categoryName = itemView.findViewById(R.id.categoryName);
        }
    }
} 