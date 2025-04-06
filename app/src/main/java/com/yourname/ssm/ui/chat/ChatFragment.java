package com.yourname.ssm.ui.chat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.LocaleList;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Base64;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.yourname.ssm.R;
import com.yourname.ssm.databinding.FragmentChatBinding;
import com.yourname.ssm.model.ChatMessage;
import com.yourname.ssm.utils.InputMethodUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;

public class ChatFragment extends Fragment {

    private static final String TAG = "ChatFragment";
    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int REQUEST_IMAGE_PICK = 102;
    private static final int REQUEST_PERMISSION_CAMERA = 103;
    private static final int REQUEST_PERMISSION_STORAGE = 104;

    private ChatViewModel chatViewModel;
    private RecyclerView recyclerView;
    private EditText messageEditText;
    private FloatingActionButton sendButton;
    private FloatingActionButton imageButton;
    private ChatAdapter chatAdapter;
    private FragmentChatBinding binding;
    
    private Uri photoUri;
    private String currentPhotoPath;

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
        imageButton = binding.buttonChatImage;
        
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
        // Add option to clear chat history
        menu.add(Menu.NONE, R.id.action_clear_chat, Menu.NONE, "Clear Chat History")
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
            .setTitle("Clear Chat History")
            .setMessage("Are you sure you want to delete the entire chat history? This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> {
                chatViewModel.clearChatHistory();
                Toast.makeText(requireContext(), "Chat history cleared", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
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
        imageButton.setOnClickListener(v -> showImageSourceDialog());
    }
    
    private void showImageSourceDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        
        new AlertDialog.Builder(requireContext())
            .setTitle("Choose Image")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    checkCameraPermissionAndTakePicture();
                } else {
                    checkStoragePermissionAndPickImage();
                }
            })
            .show();
    }
    
    private void checkCameraPermissionAndTakePicture() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSION_CAMERA);
        } else {
            takePicture();
        }
    }
    
    private void checkStoragePermissionAndPickImage() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION_STORAGE);
        } else {
            pickImageFromGallery();
        }
    }
    
    private void takePicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            try {
                File photoFile = createImageFile();
                
                if (photoFile != null) {
                    photoUri = FileProvider.getUriForFile(requireContext(),
                            "com.yourname.ssm.fileprovider", photoFile);
                    
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            } catch (IOException ex) {
                Log.e(TAG, "Error creating image file", ex);
                Toast.makeText(requireContext(), "Không thể tạo tệp ảnh", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(requireContext(), "Không tìm thấy ứng dụng camera", Toast.LENGTH_SHORT).show();
        }
    }
    
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireActivity().getExternalFilesDir(null);
        
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
    
    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Chọn ảnh"), REQUEST_IMAGE_PICK);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_PERMISSION_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePicture();
            } else {
                Toast.makeText(requireContext(), "Cần quyền truy cập camera để chụp ảnh", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_PERMISSION_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickImageFromGallery();
            } else {
                Toast.makeText(requireContext(), "Cần quyền truy cập bộ nhớ để chọn ảnh", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                if (photoUri != null) {
                    processAndSendImage(photoUri, null);
                }
            } else if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    processAndSendImage(selectedImageUri, null);
                }
            }
        }
    }
    
    private void processAndSendImage(Uri imageUri, String caption) {
        try {
            // Hiển thị thông báo đang xử lý
            Toast.makeText(requireContext(), "Đang xử lý ảnh...", Toast.LENGTH_SHORT).show();
            
            // Tạo thread mới để xử lý ảnh (để không block UI thread)
            new Thread(() -> {
                try {
                    // Đọc ảnh và chuyển đổi thành base64
                    Bitmap bitmap = getResizedBitmap(imageUri, 1024);
                    String base64Image = bitmapToBase64(bitmap);
                    
                    // Caption từ message edit text nếu có
                    final String finalCaption = caption != null ? caption : messageEditText.getText().toString().trim();
                    
                    // Xóa text trong edit text sau khi lấy
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> messageEditText.setText(""));
                    }
                    
                    // Lưu ảnh vào bộ nhớ ứng dụng
                    File savedImageFile = saveImageToAppStorage(bitmap);
                    final String localImagePath = savedImageFile.getAbsolutePath();
                    
                    // Gửi ảnh qua ViewModel
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> 
                            chatViewModel.sendImageMessage(base64Image, localImagePath, finalCaption));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing image", e);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> 
                            Toast.makeText(requireContext(), "Lỗi khi xử lý ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                }
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "Error starting image processing", e);
            Toast.makeText(requireContext(), "Không thể xử lý ảnh", Toast.LENGTH_SHORT).show();
        }
    }
    
    private Bitmap getResizedBitmap(Uri imageUri, int maxSize) throws IOException {
        InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
        
        // Đọc thông tin ảnh mà không tải toàn bộ ảnh
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();
        
        // Tính toán tỷ lệ giảm kích thước
        int scale = 1;
        while (options.outWidth / scale > maxSize || options.outHeight / scale > maxSize) {
            scale *= 2;
        }
        
        // Đọc lại với kích thước đã giảm
        options = new BitmapFactory.Options();
        options.inSampleSize = scale;
        inputStream = requireContext().getContentResolver().openInputStream(imageUri);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();
        
        return bitmap;
    }
    
    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }
    
    private File saveImageToAppStorage(Bitmap bitmap) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "CHAT_" + timeStamp + ".jpg";
        File storageDir = new File(requireContext().getFilesDir(), "chat_images");
        
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        
        File imageFile = new File(storageDir, imageFileName);
        
        FileOutputStream fos = new FileOutputStream(imageFile);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
        fos.close();
        
        return imageFile;
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