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
import com.yourname.ssm.repository.LoginUserRepository;
import com.yourname.ssm.repository.TransactionRepository;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
    private static final String TAG = "CategoryAdapter";
    
    private final Context context;
    private final List<Category> categories;
    private OnCategoryClickListener listener;
    private int selectedPosition = -1;
    private int selectedCategoryId = -1;
    private OnCategorySelectedListener categorySelectedListener;
    private TransactionRepository transactionRepository;
    private LoginUserRepository loginUserRepository;
    private int userId;
    
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
    
    // Constructor mới hỗ trợ callback
    public CategoryAdapter(List<Category> categories, Context context, OnCategorySelectedListener listener) {
        this.context = context;
        this.categories = categories;
        this.categorySelectedListener = listener;
        
        // Initialize repositories
        this.transactionRepository = new TransactionRepository(context);
        this.loginUserRepository = new LoginUserRepository(context);
        this.userId = loginUserRepository.getUserId();
        
        Log.d(TAG, "CategoryAdapter created with " + (categories != null ? categories.size() : 0) + " categories and listener");
    }
    
    public CategoryAdapter(List<Category> categories, Context context) {
        this.context = context;
        this.categories = categories;
        Log.d(TAG, "CategoryAdapter created with " + (categories != null ? categories.size() : 0) + " categories");
        
        // Log more details
        if (categories != null) {
            for (int i = 0; i < Math.min(categories.size(), 5); i++) {
                Category cat = categories.get(i);
                Log.d(TAG, "Category[" + i + "]: " + cat.getName() + ", icon: " + cat.getIconResourceId());
            }
            if (categories.size() > 5) {
                Log.d(TAG, "... and " + (categories.size() - 5) + " more categories");
            }
        }
    }
    
    // Constructor mới để hỗ trợ tham số ngược lại như đang sử dụng trong AddSpendingFragment
    public CategoryAdapter(Context context, List<Category> categories, OnCategoryClickListener listener) {
        this.context = context;
        this.categories = categories;
        this.listener = listener;
        Log.d(TAG, "CategoryAdapter created with reverse params (context, categories, listener)");
        
        // Log chi tiết về danh mục
        if (categories != null) {
            Log.d(TAG, "Categories count: " + categories.size());
            for (int i = 0; i < Math.min(categories.size(), 3); i++) {
                Category cat = categories.get(i);
                Log.d(TAG, "Category[" + i + "]: " + cat.getName() + ", icon: " + cat.getIconResourceId());
            }
        } else {
            Log.e(TAG, "Categories list is NULL!");
        }
    }
    
    public interface OnCategoryClickListener {
        void onCategoryClick(Category category, int position);
    }
    
    // Interface mới cho listener dạng lambda
    public interface OnCategorySelectedListener {
        void onCategorySelected(Category category);
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
    
    // Phương thức mới - đặt selectedCategoryId
    public void setSelectedCategoryId(int categoryId) {
        this.selectedCategoryId = categoryId;
        
        // Cập nhật selectedPosition dựa trên categoryId
        if (categories != null) {
            for (int i = 0; i < categories.size(); i++) {
                if (categories.get(i).getId() == categoryId) {
                    setSelectedPosition(i);
                    break;
                }
            }
        }
        
        // Force refresh to ensure immediate visual update
        notifyDataSetChanged();
        Log.d(TAG, "Selected category ID set to: " + categoryId);
    }
    
    // Phương thức mới - cập nhật danh sách category
    public void updateCategories(List<Category> newCategories) {
        if (newCategories != null) {
            this.categories.clear();
            this.categories.addAll(newCategories);
            
            // Khôi phục danh mục đã chọn nếu có
            if (selectedCategoryId != -1) {
                for (int i = 0; i < categories.size(); i++) {
                    if (categories.get(i).getId() == selectedCategoryId) {
                        setSelectedPosition(i);
                        break;
                    }
                }
            } else if (!categories.isEmpty()) {
                // Auto select the first item if nothing was previously selected
                setSelectedPosition(0);
                selectedCategoryId = categories.get(0).getId();
            }
            
            notifyDataSetChanged();
            Log.d(TAG, "Categories updated: " + categories.size() + " items");
        }
    }
    
    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        try {
            // Đảm bảo nạp layout đúng
            View view = LayoutInflater.from(context).inflate(R.layout.item_category, parent, false);
            Log.d(TAG, "Created new ViewHolder with item_category layout");
            return new CategoryViewHolder(view);
        } catch (Exception e) {
            Log.e(TAG, "Error inflating item_category layout", e);
            // Nếu có lỗi, thử cách khác
            View fallbackView = new CardView(context);
            fallbackView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT));
            return new CategoryViewHolder(fallbackView);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        try {
            Log.d(TAG, "onBindViewHolder position: " + position);
            
            if (position < 0 || position >= categories.size()) {
                Log.e(TAG, "Invalid position: " + position + ", categories size: " + categories.size());
                return;
            }
            
            Category category = categories.get(position);
            Log.d(TAG, "Binding category: " + category.getName() + ", iconResourceId: " + category.getIconResourceId());
            
            // Kiểm tra xem holder có các view cần thiết không
            if (holder.categoryName == null || holder.categoryIcon == null || holder.cardView == null) {
                Log.e(TAG, "Holder views are null - categoryName: " + (holder.categoryName == null) +
                         ", categoryIcon: " + (holder.categoryIcon == null) +
                         ", cardView: " + (holder.cardView == null));
                return;
            }
            
            // Thiết lập tên danh mục - sử dụng tên gốc tiếng Việt
            holder.categoryName.setText(category.getName());
            
            // Get and display the amount spent on this category
            if (holder.categoryAmount != null && transactionRepository != null) {
                double amount = transactionRepository.getTotalAmountByCategoryForCurrentMonth(userId, category.getId());
                if (amount > 0) {
                    holder.categoryAmount.setVisibility(View.VISIBLE);
                    // Format amount with Vietnamese locale
                    java.text.NumberFormat formatter = java.text.NumberFormat.getIntegerInstance(new java.util.Locale("vi", "VN"));
                    holder.categoryAmount.setText(formatter.format(amount) + "đ");
                    
                    // Set text color based on expense or income
                    if (category.isExpense()) {
                        holder.categoryAmount.setTextColor(ContextCompat.getColor(context, R.color.colorExpense));
                    } else {
                        holder.categoryAmount.setTextColor(ContextCompat.getColor(context, R.color.colorIncome));
                    }
                } else {
                    holder.categoryAmount.setVisibility(View.GONE);
                }
            }
            
            // Thiết lập icon
            try {
                int iconResourceId = category.getIconResourceId();
                
                // Verify if the resource exists
                boolean isValidResource = isValidResource(iconResourceId);
                
                if (isValidResource) {
                    holder.categoryIcon.setImageResource(iconResourceId);
                    Log.d(TAG, "Icon set successfully for " + category.getName() + ": " + iconResourceId);
                } else {
                    Log.w(TAG, "Invalid icon resource for " + category.getName() + ": " + iconResourceId);
                    
                    // Use a default icon or set a system icon
                    holder.categoryIcon.setImageResource(android.R.drawable.ic_menu_info_details);
                }
                
                // Always ensure icon is visible
                holder.categoryIcon.setVisibility(View.VISIBLE);
                
            } catch (Resources.NotFoundException e) {
                Log.e(TAG, "Resource not found for " + category.getName() + ", ID: " + category.getIconResourceId());
                holder.categoryIcon.setImageResource(android.R.drawable.ic_menu_help);
            } catch (Exception e) {
                Log.e(TAG, "Error setting icon for category: " + category.getName(), e);
                holder.categoryIcon.setImageResource(android.R.drawable.ic_menu_help);
            }
            
            // Thiết lập màu nền dựa vào trạng thái được chọn hoặc selectedCategoryId
            boolean isSelected = selectedPosition == position || 
                                (selectedCategoryId != -1 && category.getId() == selectedCategoryId);
            
            // Apply consistent styling
            if (isSelected) {
                // Selected item style
                holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimaryLight));
                holder.categoryName.setTextColor(ContextCompat.getColor(context, R.color.text_color));
                holder.categoryIcon.setColorFilter(ContextCompat.getColor(context, R.color.icon_tint_black));
                Log.d(TAG, "Applied selected style to " + category.getName());
            } else {
                // Normal item style
                holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_background));
                holder.categoryName.setTextColor(ContextCompat.getColor(context, R.color.text_color));
                holder.categoryIcon.setColorFilter(ContextCompat.getColor(context, R.color.icon_tint_black));
                Log.d(TAG, "Applied normal style to " + category.getName());
            }
            
            // Let the CardView use match_parent width to fill the grid cell
            // This ensures even sized cards across different screen sizes 
            ViewGroup.LayoutParams params = holder.cardView.getLayoutParams();
            if (params != null) {
                // Make card fill its grid cell with small margin
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                holder.cardView.setLayoutParams(params);
            }
            
            // Thiết lập sự kiện click
            holder.itemView.setOnClickListener(v -> {
                try {
                    Log.d(TAG, "Category clicked: " + category.getName());
                    
                    // Cập nhật vị trí được chọn
                    int oldSelectedPosition = selectedPosition;
                    int newPosition = holder.getAdapterPosition();
                    
                    if (newPosition != RecyclerView.NO_POSITION) {
                        setSelectedPosition(newPosition);
                        
                        // Đặt selectedCategoryId
                        selectedCategoryId = category.getId();
                        
                        Log.d(TAG, "Selection changed from position " + oldSelectedPosition + 
                               " to " + newPosition + " (categoryId=" + selectedCategoryId + ")");
                        
                        // Gọi callback kiểu cũ
                        if (listener != null) {
                            listener.onCategoryClick(category, newPosition);
                        }
                        
                        // Gọi callback kiểu mới
                        if (categorySelectedListener != null) {
                            categorySelectedListener.onCategorySelected(category);
                        }
                    } else {
                        Log.e(TAG, "Invalid adapter position: " + newPosition);
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
    
    // Helper method to convert dp to pixels
    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
    
    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        private static final String HOLDER_TAG = "CategoryViewHolder";
        
        CardView cardView;
        ImageView categoryIcon;
        TextView categoryName;
        TextView categoryAmount;
        
        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            try {
                cardView = (CardView) itemView;
                categoryIcon = itemView.findViewById(R.id.categoryIcon);
                categoryName = itemView.findViewById(R.id.categoryName);
                categoryAmount = itemView.findViewById(R.id.categoryAmount);
                
                if (categoryIcon == null) {
                    Log.e(HOLDER_TAG, "categoryIcon not found in layout");
                }
                if (categoryName == null) {
                    Log.e(HOLDER_TAG, "categoryName not found in layout");
                }
                if (categoryAmount == null) {
                    Log.e(HOLDER_TAG, "categoryAmount not found in layout");
                }
            } catch (Exception e) {
                Log.e(HOLDER_TAG, "Error initializing ViewHolder", e);
            }
        }
    }
} 