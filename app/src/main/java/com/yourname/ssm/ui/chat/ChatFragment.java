package com.yourname.ssm.ui.chat;

import android.content.Context;
import android.os.Bundle;
import android.os.LocaleList;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.yourname.ssm.R;
import com.yourname.ssm.databinding.FragmentChatBinding;
import com.yourname.ssm.model.ChatMessage;
import com.yourname.ssm.utils.InputMethodUtils;

import java.util.ArrayList;
import java.util.Locale;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;

public class ChatFragment extends Fragment {

    private static final String TAG = "ChatFragment";

    private ChatViewModel chatViewModel;
    private RecyclerView recyclerView;
    private EditText messageEditText;
    private FloatingActionButton sendButton;
    private ChatAdapter chatAdapter;
    private FragmentChatBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true); // Cho phép hiển thị menu trong fragment
        
        // Khởi tạo ViewModel trong onCreate để đảm bảo dữ liệu được load trước
        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        
        // Khởi tạo components
        recyclerView = binding.recyclerChat;
        messageEditText = binding.editChatMessage;
        sendButton = binding.buttonChatSend;
        
        setupRecyclerView();
        setupChatInput();
        
        // Tự động load chat history từ ViewModel
        chatViewModel.loadChatHistory();
        
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Setup click listeners
        setupClickListeners();
        
        // Observe LiveData
        observeViewModel();
        
        // Cấu hình bàn phím tiếng Việt
        setupVietnameseKeyboard();
    }
    
    private void setupVietnameseKeyboard() {
        try {
            // Sử dụng lớp tiện ích để cấu hình bàn phím tiếng Việt
            InputMethodUtils.setupVietnameseKeyboard(requireContext(), messageEditText);
            
            // Đảm bảo thêm một lần nữa khi fragment đã hiển thị hoàn toàn
            new Handler().postDelayed(() -> {
                if (isAdded() && !isDetached()) {
                    InputMethodUtils.showInputMethod(requireContext(), messageEditText);
                }
            }, 800);
        } catch (Exception e) {
            Log.e(TAG, "setupVietnameseKeyboard: Error", e);
        }
    }
    
    private void setupChatInput() {
        // Cấu hình EditText để sử dụng nút gửi trên bàn phím
        messageEditText.setImeOptions(EditorInfo.IME_ACTION_SEND);
        messageEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }
    
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        // Thêm tùy chọn để xóa lịch sử chat
        menu.add(Menu.NONE, R.id.action_clear_chat, Menu.NONE, "Xóa lịch sử chat")
            .setIcon(android.R.drawable.ic_menu_delete)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        
        super.onCreateOptionsMenu(menu, inflater);
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_clear_chat) {
            showClearHistoryConfirmationDialog();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void showClearHistoryConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
            .setTitle("Xóa lịch sử chat")
            .setMessage("Bạn có chắc chắn muốn xóa toàn bộ lịch sử chat này? Hành động này không thể hoàn tác.")
            .setPositiveButton("Xóa", (dialog, which) -> {
                chatViewModel.clearChatHistory();
                Toast.makeText(requireContext(), "Đã xóa lịch sử chat", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        
        // Tạo adapter với ImageClickListener để xử lý sự kiện click vào hình ảnh
        chatAdapter = new ChatAdapter(getContext(), new ArrayList<>(), imageUrl -> {
            // Hiển thị dialog xem hình ảnh đầy đủ
            showFullImageDialog(imageUrl);
        });
        
        recyclerView.setAdapter(chatAdapter);
    }

    /**
     * Hiển thị dialog xem hình ảnh đầy đủ
     * @param imageUrl URL của hình ảnh
     */
    private void showFullImageDialog(String imageUrl) {
        if (getContext() == null) return;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_fullscreen_image, null);
        
        PhotoView photoView = dialogView.findViewById(R.id.fullscreen_image);
        
        // Sử dụng Glide để tải hình ảnh
        Glide.with(this)
            .load(imageUrl)
            .into(photoView);
        
        builder.setView(dialogView);
        
        final AlertDialog dialog = builder.create();
        
        // Đóng dialog khi click vào hình ảnh
        photoView.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    private void setupClickListeners() {
        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void observeViewModel() {
        chatViewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            // Cập nhật adapter hiện tại với danh sách tin nhắn mới
            chatAdapter.updateMessages(messages);
            
            // Cuộn xuống tin nhắn mới nhất
            scrollToBottom();
        });
    }
    
    private void scrollToBottom() {
        if (chatAdapter != null && chatAdapter.getItemCount() > 0) {
            recyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
        }
    }

    private void sendMessage() {
        String messageText = messageEditText.getText().toString().trim();
        if (!messageText.isEmpty()) {
            // Gửi tin nhắn qua ViewModel
            chatViewModel.sendMessage(messageText);
            
            // Xóa nội dung sau khi gửi
            messageEditText.setText("");
            
            // Cuộn xuống tin nhắn mới nhất
            scrollToBottom();
        }
    }
} 