package com.yourname.ssm.ui.admin;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yourname.ssm.R;
import com.yourname.ssm.model.User;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;
    private Context context;
    private UserAdapterListener listener;

    // Interface for click event handling
    public interface UserAdapterListener {
        void onEditClick(User user);
        void onDeleteClick(User user);
    }

    public UserAdapter(Context context, List<User> userList, UserAdapterListener listener) {
        this.context = context;
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        
        // Display user data
        holder.textId.setText(String.valueOf(user.getId()));
        holder.textName.setText(user.getName());
        holder.textEmail.setText(user.getEmail());
        
        // Display role
        String roleText = user.getRoleId() == 1 ? "Admin" : "Student";
        holder.textRole.setText(roleText);
        
        // Display status
        if (user.getIsActive() == 1) {
            holder.textStatus.setText("Active");
            holder.textStatus.setTextColor(Color.parseColor("#4CAF50")); // Green
        } else {
            holder.textStatus.setText("Inactive");
            holder.textStatus.setTextColor(Color.parseColor("#F44336")); // Red
        }
        
        // Alternate row background for readability
        if (position % 2 == 0) {
            holder.itemView.setBackgroundColor(Color.parseColor("#F5F5F5"));
        } else {
            holder.itemView.setBackgroundColor(Color.WHITE);
        }
        
        // Handle button clicks
        holder.editButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(user);
            }
        });
        
        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList != null ? userList.size() : 0;
    }
    
    // Update user list
    public void updateUserList(List<User> newUserList) {
        this.userList = newUserList;
        notifyDataSetChanged();
    }

    // ViewHolder class
    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView textId, textName, textEmail, textRole, textStatus;
        ImageButton editButton, deleteButton;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            textId = itemView.findViewById(R.id.textId);
            textName = itemView.findViewById(R.id.textName);
            textEmail = itemView.findViewById(R.id.textEmail);
            textRole = itemView.findViewById(R.id.textRole);
            textStatus = itemView.findViewById(R.id.textStatus);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
} 