package com.yourname.ssm.utils;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.LocaleList;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.content.res.Configuration;

import java.util.Locale;

/**
 * Lớp tiện ích hỗ trợ cấu hình bàn phím cho các ngôn ngữ khác nhau
 */
public class InputMethodUtils {
    private static final String TAG = "InputMethodUtils";

    /**
     * Cấu hình bàn phím tiếng Việt cho EditText
     * 
     * @param context Context của ứng dụng
     * @param editText EditText cần cấu hình
     */
    public static void setupVietnameseKeyboard(Context context, EditText editText) {
        if (context == null || editText == null) {
            return;
        }

        try {
            // Đặt ngôn ngữ cục bộ cho EditText
            editText.setTextLocale(new Locale("vi", "VN"));
            
            // Đặt tùy chọn IME riêng
            editText.setPrivateImeOptions("nm:vi");

            // Thêm thuộc tính đa dòng nếu cần
            int inputType = editText.getInputType();
            editText.setInputType(inputType | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);

            // Hiện bàn phím ngay sau khi EditText có focus
            editText.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    showInputMethod(context, editText);
                }
            });

            // Đảm bảo bàn phím hiển thị khi bắt đầu
            new Handler().postDelayed(() -> {
                editText.requestFocus();
                showInputMethod(context, editText);
            }, 300);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up Vietnamese keyboard", e);
        }
    }

    /**
     * Hiển thị bàn phím với cấu hình ngôn ngữ Việt
     * 
     * @param context Context của ứng dụng
     * @param view View cần focus để hiển thị bàn phím
     */
    public static void showInputMethod(Context context, View view) {
        if (context == null || view == null) {
            return;
        }

        try {
            // Đặt cấu hình locale
            setVietnameseLocale(context);
            
            // Hiển thị bàn phím
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                // Đặt gợi ý locale nếu có thể
                if (view instanceof EditText && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    LocaleList localeList = new LocaleList(new Locale("vi", "VN"));
                    ((EditText) view).setImeHintLocales(localeList);
                }
                
                // Mở bàn phím với force
                imm.showSoftInput(view, InputMethodManager.SHOW_FORCED);
                
                // Phương pháp thay thế nếu phương pháp trên không hoạt động
                new Handler().postDelayed(() -> {
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
                }, 100);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing input method", e);
        }
    }

    /**
     * Đặt locale tiếng Việt cho context
     * 
     * @param context Context cần thay đổi locale
     */
    public static void setVietnameseLocale(Context context) {
        try {
            Locale vietnameseLocale = new Locale("vi", "VN");
            Locale.setDefault(vietnameseLocale);
            
            Configuration config = new Configuration(context.getResources().getConfiguration());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                LocaleList localeList = new LocaleList(vietnameseLocale);
                config.setLocales(localeList);
            } else {
                config.locale = vietnameseLocale;
            }
            
            context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
        } catch (Exception e) {
            Log.e(TAG, "Error setting Vietnamese locale", e);
        }
    }
} 