package com.yourname.ssm.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

public class ImageUtils {

    /**
     * Chuyển đổi ảnh bitmap thành chuỗi Base64
     *
     * @param bitmap Bitmap cần chuyển đổi
     * @return Chuỗi Base64 hoặc null nếu có lỗi
     */
    public static String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return null;

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Chuyển đổi chuỗi Base64 thành bitmap
     *
     * @param base64String Chuỗi Base64 cần chuyển đổi
     * @return Bitmap hoặc null nếu có lỗi
     */
    public static Bitmap base64ToBitmap(String base64String) {
        if (base64String == null || base64String.isEmpty()) return null;

        try {
            byte[] decodedString = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Chuyển đổi bitmap thành bitmap tròn
     *
     * @param bitmap Bitmap cần chuyển đổi
     * @return Bitmap hình tròn hoặc null nếu input là null
     */
    public static Bitmap getCircularBitmap(Bitmap bitmap) {
        if (bitmap == null) return null;

        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawOval(rectF, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    /**
     * Giảm kích thước bitmap
     *
     * @param bitmap      Bitmap cần resize
     * @param maxWidth    Chiều rộng tối đa
     * @param maxHeight   Chiều cao tối đa
     * @return Bitmap đã resize hoặc null nếu input là null
     */
    public static Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        if (bitmap == null) return null;

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float ratio = Math.min(
                (float) maxWidth / width,
                (float) maxHeight / height
        );

        int finalWidth = Math.round(width * ratio);
        int finalHeight = Math.round(height * ratio);

        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true);
    }
} 