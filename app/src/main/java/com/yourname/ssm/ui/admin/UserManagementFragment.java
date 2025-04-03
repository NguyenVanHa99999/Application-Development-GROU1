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
        
        // Khởi tạo các thành phần UI
        recyclerUsers = view.findViewById(R.id.recyclerUsers);
        emptyView = view.findViewById(R.id.emptyView);
        fabAddUser = view.findViewById(R.id.fabAddUser);
        
        // Khởi tạo repository
        userRepository = new UserRepository(getContext());
        
        // Thiết lập RecyclerView
        recyclerUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerUsers.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        
        // Khởi tạo adapter với danh sách rỗng ban đầu
        userList = new ArrayList<>();
        userAdapter = new UserAdapter(getContext(), userList, this);
        recyclerUsers.setAdapter(userAdapter);
        
        // Khởi tạo dialog để thêm/sửa người dùng
        dialogView = getLayoutInflater().inflate(R.layout.dialog_user_form, null);
        userDialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();
        
        // Thiết lập sự kiện click cho nút thêm người dùng
        fabAddUser.setOnClickListener(v -> showUserDialog(null));
        
        // Tải danh sách người dùng
        loadUsers();
    }

    // Tải danh sách người dùng từ database
    private void loadUsers() {
        // Lấy danh sách người dùng
        userList = userRepository.getAllUsers();
        
        // Hiển thị thông báo trống nếu không có người dùng
        if (userList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerUsers.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerUsers.setVisibility(View.VISIBLE);
            
            // Cập nhật adapter
            userAdapter.updateUserList(userList);
        }
    }
    
    // Xử lý sự kiện khi click vào nút sửa
    @Override
    public void onEditClick(User user) {
        showUserDialog(user);
    }
    
    // Xử lý sự kiện khi click vào nút xóa
    @Override
    public void onDeleteClick(User user) {
        confirmDeleteUser(user);
    }

    // Hiển thị dialog thêm/sửa người dùng
    private void showUserDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_user_form, null);
        builder.setView(dialogView);

        // Khởi tạo các thành phần trong dialog
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

        // Thiết lập spinner role
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"Admin", "Sinh viên"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);

        // Thiết lập tiêu đề và giá trị ban đầu
        if (user != null) {
            // Chế độ chỉnh sửa
            dialogTitle.setText("Chỉnh sửa người dùng");
            nameEditText.setText(user.getName());
            emailEditText.setText(user.getEmail());
            phoneEditText.setText(user.getPhone());
            
            // Thiết lập giới tính
            if ("Nam".equals(user.getGender())) {
                maleRadioButton.setChecked(true);
            } else {
                femaleRadioButton.setChecked(true);
            }
            
            // Thiết lập vai trò (roleId: 1 = Admin, 2 = Sinh viên)
            roleSpinner.setSelection(user.getRoleId() - 1);
            
            // Thiết lập trạng thái hoạt động
            activeSwitch.setChecked(user.getIsActive() == 1);
        } else {
            // Chế độ thêm mới
            dialogTitle.setText("Thêm người dùng mới");
            maleRadioButton.setChecked(true);
            roleSpinner.setSelection(1); // Mặc định là sinh viên
            activeSwitch.setChecked(true);
        }

        // Tạo dialog
        AlertDialog dialog = builder.create();

        // Thiết lập sự kiện click cho nút hủy
        cancelButton.setOnClickListener(v -> dialog.dismiss());

        // Thiết lập sự kiện click cho nút lưu
        saveButton.setOnClickListener(v -> {
            // Kiểm tra dữ liệu đầu vào
            String name = nameEditText.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String phone = phoneEditText.getText().toString().trim();
            String gender = maleRadioButton.isChecked() ? "Nam" : "Nữ";
            int roleId = roleSpinner.getSelectedItemPosition() + 1; // 1: Admin, 2: Sinh viên
            int isActive = activeSwitch.isChecked() ? 1 : 0;

            // Kiểm tra các trường bắt buộc
            if (TextUtils.isEmpty(name)) {
                nameInputLayout.setError("Vui lòng nhập tên");
                return;
            } else {
                nameInputLayout.setError(null);
            }

            if (TextUtils.isEmpty(email)) {
                emailInputLayout.setError("Vui lòng nhập email");
                return;
            } else {
                emailInputLayout.setError(null);
            }

            if (user == null && TextUtils.isEmpty(password)) {
                passwordInputLayout.setError("Vui lòng nhập mật khẩu");
                return;
            } else {
                passwordInputLayout.setError(null);
            }

            if (user == null) {
                // Thêm người dùng mới
                if (userRepository.isEmailExists(email)) {
                    emailInputLayout.setError("Email này đã tồn tại");
                    return;
                }
                
                // Tạo đối tượng User mới (sử dụng constructor phù hợp)
                User newUser = new User(name, email, hashPassword(password), gender, "", phone, "", roleId);
                newUser.isActive = isActive;
                
                long result = userRepository.insertUser(newUser);
                if (result > 0) {
                    Toast.makeText(getContext(), "Thêm người dùng thành công", Toast.LENGTH_SHORT).show();
                    loadUsers();
                    dialog.dismiss();
                } else {
                    Toast.makeText(getContext(), "Thêm người dùng thất bại", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Cập nhật người dùng hiện có
                User updatedUser = new User(name, email, 
                    TextUtils.isEmpty(password) ? user.getPassword() : hashPassword(password), 
                    gender, user.getDob(), phone, user.getAddress(), roleId);
                updatedUser.isActive = isActive;
                
                // Sử dụng ID thực từ database để cập nhật
                boolean result = userRepository.updateUser(user.getId(), updatedUser);
                
                if (result) {
                    Toast.makeText(getContext(), "Cập nhật người dùng thành công", Toast.LENGTH_SHORT).show();
                    loadUsers();
                    dialog.dismiss();
                } else {
                    Toast.makeText(getContext(), "Cập nhật người dùng thất bại", Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
    }

    // Hiển thị dialog xác nhận xóa người dùng
    private void confirmDeleteUser(User user) {
        new AlertDialog.Builder(getContext())
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa người dùng này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    // Không cho phép xóa tài khoản admin
                    if (user.getRoleId() == 1) {
                        Toast.makeText(getContext(), "Không thể xóa tài khoản admin", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Xóa người dùng bằng email (email là duy nhất) và tất cả dữ liệu liên quan
                    boolean result = userRepository.forceDeleteUserByEmail(user.getEmail());
                    
                    if (result) {
                        Toast.makeText(getContext(), "Xóa người dùng thành công", Toast.LENGTH_SHORT).show();
                        loadUsers();
                    } else {
                        Toast.makeText(getContext(), "Xóa người dùng thất bại", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Mã hóa mật khẩu bằng thuật toán SHA-256
     * @param password Mật khẩu cần mã hóa
     * @return Chuỗi mật khẩu đã được mã hóa
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(
                    password.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Error hashing password", e);
            return password; // Trả về mật khẩu gốc nếu có lỗi
        }
    }
    
    /**
     * Chuyển đổi mảng byte thành chuỗi hex
     * @param hash Mảng byte cần chuyển đổi
     * @return Chuỗi hex
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