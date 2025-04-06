package com.yourname.ssm.ui.chat;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;
import com.yourname.ssm.R;
import com.yourname.ssm.model.ChatMessage;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT_TEXT = 1;
    private static final int VIEW_TYPE_RECEIVED_TEXT = 2;
    private static final int VIEW_TYPE_SENT_IMAGE = 3;
    private static final int VIEW_TYPE_RECEIVED_IMAGE = 4;

    private final List<ChatMessage> messageList;
    private final Context context;
    private final ImageClickListener imageClickListener;

    public ChatAdapter(Context context, List<ChatMessage> messageList, ImageClickListener imageClickListener) {
        this.context = context;
        this.messageList = messageList;
        this.imageClickListener = imageClickListener;
    }

    public ChatAdapter(Context context, List<ChatMessage> messageList) {
        this(context, messageList, null);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        
        switch (viewType) {
            case VIEW_TYPE_SENT_TEXT:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_sent, parent, false);
                return new SentTextMessageHolder(view);
            case VIEW_TYPE_RECEIVED_TEXT:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_received, parent, false);
                return new ReceivedTextMessageHolder(view);
            case VIEW_TYPE_SENT_IMAGE:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_sent_image, parent, false);
                return new SentImageMessageHolder(view);
            case VIEW_TYPE_RECEIVED_IMAGE:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_received_image, parent, false);
                return new ReceivedImageMessageHolder(view);
            default:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_received, parent, false);
                return new ReceivedTextMessageHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);
        
        if (holder instanceof SentTextMessageHolder) {
            ((SentTextMessageHolder) holder).bind(message);
        } else if (holder instanceof ReceivedTextMessageHolder) {
            ((ReceivedTextMessageHolder) holder).bind(message);
        } else if (holder instanceof SentImageMessageHolder) {
            ((SentImageMessageHolder) holder).bind(message);
        } else if (holder instanceof ReceivedImageMessageHolder) {
            ((ReceivedImageMessageHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messageList.get(position);
        
        if (message.getType() == ChatMessage.TYPE_SENT) {
            return message.isImage() ? VIEW_TYPE_SENT_IMAGE : VIEW_TYPE_SENT_TEXT;
        } else {
            return message.isImage() ? VIEW_TYPE_RECEIVED_IMAGE : VIEW_TYPE_RECEIVED_TEXT;
        }
    }

    // Add message to adapter and notify
    public void addMessage(ChatMessage message) {
        messageList.add(message);
        notifyItemInserted(messageList.size() - 1);
    }

    // Update all messages
    public void updateMessages(List<ChatMessage> messages) {
        messageList.clear();
        messageList.addAll(messages);
        notifyDataSetChanged();
    }

    /**
     * Format timestamp to readable format
     * @param timestamp Timestamp in milliseconds
     * @return Formatted time string
     */
    private String formatTimestamp(long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", 
                java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(timestamp));
    }

    // ViewHolder for sent messages
    class SentTextMessageHolder extends RecyclerView.ViewHolder {
        private TextView messageText;
        private TextView timeText;
        
        SentTextMessageHolder(View itemView) {
            super(itemView);
            
            messageText = itemView.findViewById(R.id.text_message_body);
            timeText = itemView.findViewById(R.id.text_message_time);
        }
        
        void bind(ChatMessage message) {
            messageText.setText(message.getMessage());
            
            // Set the time
            long timestamp = message.getTimestamp();
            timeText.setText(ChatAdapter.this.formatTimestamp(timestamp));
        }
    }
    
    // ViewHolder for received messages
    class ReceivedTextMessageHolder extends RecyclerView.ViewHolder {
        private TextView messageText;
        private TextView timeText;
        
        ReceivedTextMessageHolder(View itemView) {
            super(itemView);
            
            messageText = itemView.findViewById(R.id.text_message_body);
            timeText = itemView.findViewById(R.id.text_message_time);
        }
        
        void bind(ChatMessage message) {
            // Check if the message is an image
            if (message.isImage()) {
                // Image messages should not be displayed by ReceivedTextMessageHolder
                // but to prevent errors if it happens
                messageText.setText("View Image");
            } else {
                messageText.setText(message.getMessage());
            }
            
            // Set the time
            long timestamp = message.getTimestamp();
            timeText.setText(ChatAdapter.this.formatTimestamp(timestamp));
        }
    }

    // Holder for sent image messages
    class SentImageMessageHolder extends RecyclerView.ViewHolder {
        ImageView imageMessage;

        SentImageMessageHolder(View itemView) {
            super(itemView);
            imageMessage = itemView.findViewById(R.id.image_message_sent);
            
            imageMessage.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && imageClickListener != null) {
                    imageClickListener.onImageClick(messageList.get(position).getImageUrl());
                }
            });
        }

        void bind(ChatMessage message) {
            Glide.with(context)
                .load(message.getImageUrl())
                .placeholder(R.drawable.placeholder_image)
                .into(imageMessage);
        }
    }

    // Holder for received image messages
    class ReceivedImageMessageHolder extends RecyclerView.ViewHolder {
        ImageView imageMessage;

        ReceivedImageMessageHolder(View itemView) {
            super(itemView);
            imageMessage = itemView.findViewById(R.id.image_message_received);
            
            imageMessage.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && imageClickListener != null) {
                    imageClickListener.onImageClick(messageList.get(position).getImageUrl());
                }
            });
        }

        void bind(ChatMessage message) {
            String imageUrl = message.getMessage();
            
            // Xử lý hình ảnh là data URL (base64)
            if (imageUrl.startsWith("data:image")) {
                try {
                    // Tách phần base64 từ data URL
                    String base64Image = imageUrl.split(",")[1];
                    
                    // Chuyển đổi base64 thành bitmap
                    byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    
                    // Hiển thị bitmap
                    imageMessage.setImageBitmap(bitmap);
                } catch (Exception e) {
                    Log.e("ChatAdapter", "Error loading base64 image", e);
                    // Hiển thị ảnh lỗi
                    imageMessage.setImageResource(R.drawable.ic_student_avatar);
                }
            } else {
                // Tải ảnh từ URL bằng Glide
                Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_student_avatar)
                    .error(R.drawable.ic_student_avatar)
                    .into(imageMessage);
            }
        }
    }
    
    // Interface for image click events
    public interface ImageClickListener {
        void onImageClick(String imageUrl);
    }
} 