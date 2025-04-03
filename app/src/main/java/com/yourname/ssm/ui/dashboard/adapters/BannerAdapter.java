package com.yourname.ssm.ui.dashboard.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yourname.ssm.R;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {
    private static final String TAG = "BannerAdapter";
    
    private final Context context;
    private final int[] bannerImages;
    private OnBannerClickListener onBannerClickListener;

    public interface OnBannerClickListener {
        void onBannerClick(int position);
    }

    public BannerAdapter(Context context, int[] bannerImages) {
        this.context = context;
        this.bannerImages = bannerImages;
    }

    public void setOnBannerClickListener(OnBannerClickListener listener) {
        this.onBannerClickListener = listener;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        try {
            View view = LayoutInflater.from(context).inflate(R.layout.item_banner, parent, false);
            return new BannerViewHolder(view);
        } catch (Exception e) {
            Log.e(TAG, "Error creating banner view holder", e);
            // Fallback view in case of error
            View errorView = new View(context);
            errorView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            return new BannerViewHolder(errorView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        try {
            if (position >= 0 && position < bannerImages.length) {
                holder.bannerImageView.setImageResource(bannerImages[position]);
                
                // Add click listener
                holder.itemView.setOnClickListener(v -> {
                    try {
                        if (onBannerClickListener != null) {
                            onBannerClickListener.onBannerClick(position);
                        } else {
                            // Default behavior: show toast with banner position
                            Toast.makeText(context, "Banner " + (position + 1), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error handling banner click", e);
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error binding banner view holder", e);
        }
    }

    @Override
    public int getItemCount() {
        return bannerImages.length;
    }

    public static class BannerViewHolder extends RecyclerView.ViewHolder {
        final ImageView bannerImageView;

        public BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            bannerImageView = itemView.findViewById(R.id.bannerImageView);
        }
    }
} 