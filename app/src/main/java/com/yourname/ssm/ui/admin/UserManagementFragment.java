package com.yourname.ssm.ui.admin;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.yourname.ssm.R;
import com.yourname.ssm.model.User;
import com.yourname.ssm.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class UserManagementFragment extends Fragment implements UserAdapter.UserAdapterListener {

    private static final String TAG = "UserManagementFragment";
    private RecyclerView recyclerUsers;
    private TextView emptyView;
    private FloatingActionButton fabAddUser;
    private UserRepository userRepository;
    private List<User> userList;
    private UserAdapter userAdapter;
    private AlertDialog userDialog;
    private View dialogView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize UI components
        recyclerUsers = view.findViewById(R.id.recyclerUsers);
        emptyView = view.findViewById(R.id.emptyView);
        fabAddUser = view.findViewById(R.id.fabAddUser);
        
        // Initialize repository
        userRepository = new UserRepository(getContext());
        
        // Setup RecyclerView
        recyclerUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerUsers.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        
        // Initialize adapter with empty list
        userList = new ArrayList<>();
        userAdapter = new UserAdapter(getContext(), userList, this);
        recyclerUsers.setAdapter(userAdapter);
        
        // Initialize dialog for adding/editing users
        dialogView = getLayoutInflater().inflate(R.layout.dialog_user_form, null);
        userDialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();
        
        // Setup click event for add user button
        fabAddUser.setOnClickListener(v -> showUserDialog(null));
        
        // Load user list
        loadUsers();
    }

    // Load users from database
    private void loadUsers() {
        // Get user list
        userList = userRepository.getAllUsers();
        
        // Display empty message if no users
        if (userList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerUsers.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerUsers.setVisibility(View.VISIBLE);
            
            // Update adapter
            userAdapter.updateUserList(userList);
        }
    }
    
    // Handle edit button click
    @Override
    public void onEditClick(User user) {
        showUserDialog(user);
    }
    
    // Handle delete button click
    @Override
    public void onDeleteClick(User user) {
        confirmDeleteUser(user);
    }

    // Show dialog for adding/editing user
    private void showUserDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_user_form, null);
        builder.setView(dialogView);

        // Initialize dialog components
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextInputLayout nameInputLayout = dialogView.findViewById(R.id.nameInputLayout);
        TextInputLayout emailInputLayout = dialogView.findViewById(R.id.emailInputLayout);
        TextInputLayout passwordInputLayout = dialogView.findViewById(R.id.passwordInputLayout);
        TextInputLayout phoneInputLayout = dialogView.findViewById(R.id.phoneInputLayout);
        TextInputEditText nameEditText = dialogView.findViewById(R.id.nameEditText);
        TextInputEditText emailEditText = dialogView.findViewById(R.id.emailEditText);
        TextInputEditText passwordEditText = dialogView.findViewById(R.id.passwordEditText);
        TextInputEditText phoneEditText = dialogView.findViewById(R.id.phoneEditText);
        RadioButton maleRadioButton = dialogView.findViewById(R.id.maleRadioButton);
        RadioButton femaleRadioButton = dialogView.findViewById(R.id.femaleRadioButton);
        Spinner roleSpinner = dialogView.findViewById(R.id.roleSpinner);
        Switch activeSwitch = dialogView.findViewById(R.id.activeSwitch);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button saveButton = dialogView.findViewById(R.id.saveButton);

        // Setup role spinner
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"Admin", "Student"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);

        // Set title and initial values
        if (user != null) {
            // Edit mode
            dialogTitle.setText("Edit User");
            nameEditText.setText(user.getName());
            emailEditText.setText(user.getEmail());
            phoneEditText.setText(user.getPhone());
            
            // Set gender
            if ("Male".equals(user.getGender()) || "Nam".equals(user.getGender())) {
                maleRadioButton.setChecked(true);
            } else {
                femaleRadioButton.setChecked(true);
            }
            
            // Set role (roleId: 1 = Admin, 2 = Student)
            roleSpinner.setSelection(user.getRoleId() - 1);
            
            // Set active status
            activeSwitch.setChecked(user.getIsActive() == 1);
        } else {
            // Add mode
            dialogTitle.setText("Add New User");
            maleRadioButton.setChecked(true);
            roleSpinner.setSelection(1); // Default to student
            activeSwitch.setChecked(true);
        }

        // Create dialog
        AlertDialog dialog = builder.create();

        // Setup cancel button click
        cancelButton.setOnClickListener(v -> dialog.dismiss());

        // Setup save button click
        saveButton.setOnClickListener(v -> {
            // Validate input data
            String name = nameEditText.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String phone = phoneEditText.getText().toString().trim();
            String gender = maleRadioButton.isChecked() ? "Male" : "Female";
            int roleId = roleSpinner.getSelectedItemPosition() + 1; // 1: Admin, 2: Student
            int isActive = activeSwitch.isChecked() ? 1 : 0;

            // Check required fields
            if (TextUtils.isEmpty(name)) {
                nameInputLayout.setError("Please enter a name");
                return;
            } else {
                nameInputLayout.setError(null);
            }

            if (TextUtils.isEmpty(email)) {
                emailInputLayout.setError("Please enter an email");
                return;
            } else {
                emailInputLayout.setError(null);
            }

            if (user == null && TextUtils.isEmpty(password)) {
                passwordInputLayout.setError("Please enter a password");
                return;
            } else {
                passwordInputLayout.setError(null);
            }

            if (user == null) {
                // Add new user
                if (userRepository.isEmailExists(email)) {
                    emailInputLayout.setError("This email already exists");
                    return;
                }
                
                // Create new User object
                User newUser = new User(name, email, hashPassword(password), gender, "", phone, "", roleId);
                newUser.isActive = isActive;
                
                long result = userRepository.insertUser(newUser);
                if (result > 0) {
                    Toast.makeText(getContext(), "User added successfully", Toast.LENGTH_SHORT).show();
                    loadUsers();
                    dialog.dismiss();
                } else {
                    Toast.makeText(getContext(), "Failed to add user", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Update existing user
                User updatedUser = new User(name, email, 
                    TextUtils.isEmpty(password) ? user.getPassword() : hashPassword(password), 
                    gender, user.getDob(), phone, user.getAddress(), roleId);
                updatedUser.isActive = isActive;
                
                boolean result = userRepository.updateUser(user.getId(), updatedUser);
                
                if (result) {
                    Toast.makeText(getContext(), "User updated successfully", Toast.LENGTH_SHORT).show();
                    loadUsers();
                    dialog.dismiss();
                } else {
                    Toast.makeText(getContext(), "Failed to update user", Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
    }

    // Show confirmation dialog for user deletion
    private void confirmDeleteUser(User user) {
        new AlertDialog.Builder(getContext())
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete this user?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Don't allow deleting admin accounts
                    if (user.getRoleId() == 1) {
                        Toast.makeText(getContext(), "Admin accounts cannot be deleted", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Delete user by email (email is unique) and all related data
                    boolean result = userRepository.forceDeleteUserByEmail(user.getEmail());
                    
                    if (result) {
                        Toast.makeText(getContext(), "User deleted successfully", Toast.LENGTH_SHORT).show();
                        loadUsers();
                    } else {
                        Toast.makeText(getContext(), "Failed to delete user", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Hash password using SHA-256 algorithm
     * @param password Password to hash
     * @return Hashed password string
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(
                    password.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Error hashing password", e);
            return password; // Return original password if error
        }
    }
    
    /**
     * Convert byte array to hex string
     * @param hash Byte array to convert
     * @return Hex string
     */
    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
} 