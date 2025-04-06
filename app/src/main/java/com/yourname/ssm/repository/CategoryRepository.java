package com.yourname.ssm.repository;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.yourname.ssm.R;
import com.yourname.ssm.database.DatabaseContract;
import com.yourname.ssm.database.DatabaseHelper;
import com.yourname.ssm.model.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class CategoryRepository {
    private static final String TAG = "CategoryRepository";
    private final AtomicReference<DatabaseHelper> dbHelperRef = new AtomicReference<>();
    private final Context appContext;
    
    public CategoryRepository(Context context) {
        // Lưu context ứng dụng để tránh rò rỉ bộ nhớ
        this.appContext = context.getApplicationContext();
    }
    
    // Lazy initialization của DatabaseHelper để đảm bảo quản lý tài nguyên tốt hơn
    private DatabaseHelper getDbHelper() {
        DatabaseHelper dbHelper = dbHelperRef.get();
        if (dbHelper == null) {
            dbHelper = new DatabaseHelper(appContext);
            dbHelperRef.set(dbHelper);
        }
        return dbHelper;
    }
    
    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        
        try {
            db = getDbHelper().getReadableDatabase();
            String query = "SELECT * FROM " + DatabaseContract.CategoriesEntry.TABLE_NAME;
            cursor = db.rawQuery(query, null);
            categories = getCategoriesFromCursor(cursor);
        } catch (Exception e) {
            Log.e(TAG, "Error getting all categories", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return categories;
    }

    public List<Category> getCategoriesByType(int type) {
        List<Category> categories = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        
        try {
            db = getDbHelper().getReadableDatabase();
            
            String[] projection = {
                    DatabaseContract.CategoriesEntry._ID,
                    DatabaseContract.CategoriesEntry.COLUMN_NAME,
                    DatabaseContract.CategoriesEntry.COLUMN_TYPE,
                    DatabaseContract.CategoriesEntry.COLUMN_ICON
            };
            
            String selection = DatabaseContract.CategoriesEntry.COLUMN_TYPE + " = ?";
            String[] selectionArgs = { type == 1 ? "income" : "expense" };
            
            Log.d(TAG, "Querying categories for type: " + selectionArgs[0]);
            
            cursor = db.query(
                    DatabaseContract.CategoriesEntry.TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    null
            );
            
            if (cursor != null && cursor.moveToFirst()) {
                Log.d(TAG, "Found " + cursor.getCount() + " categories of type: " + selectionArgs[0]);
                do {
                    try {
                        int idColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.CategoriesEntry._ID);
                        int nameColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.CategoriesEntry.COLUMN_NAME);
                        int typeColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.CategoriesEntry.COLUMN_TYPE);
                        int iconColumnIndex = cursor.getColumnIndexOrThrow(DatabaseContract.CategoriesEntry.COLUMN_ICON);
                        
                        String typeString = cursor.getString(typeColumnIndex);
                        // Chuyển đổi từ string sang int type
                        int categoryType = "income".equals(typeString) ? 1 : 0;
                        
                        int id = cursor.getInt(idColumnIndex);
                        String name = cursor.getString(nameColumnIndex);
                        int iconResourceId = cursor.getInt(iconColumnIndex);
                        
                        // Xác minh resource ID
                        boolean iconValid = isResourceValid(iconResourceId);
                        
                        if (!iconValid) {
                            Log.w(TAG, "Invalid resource ID: " + iconResourceId + " for category: " + name);
                            
                            // Gán icon mặc định dựa vào loại
                            if (categoryType == 1) { // Thu nhập
                                switch (name) {
                                    case "Lương": iconResourceId = R.drawable.ic_category_salary; break;
                                    case "Đầu tư": iconResourceId = R.drawable.ic_category_investment; break;
                                    case "Quà tặng": iconResourceId = R.drawable.ic_category_gift; break;
                                    case "Học bổng": iconResourceId = R.drawable.ic_category_education; break;
                                    case "Bán hàng": iconResourceId = R.drawable.ic_category_shopping; break;
                                    case "Thưởng": iconResourceId = R.drawable.ic_category_gift; break;
                                    case "Cho vay": iconResourceId = R.drawable.ic_category_loan; break;
                                    case "Hoàn tiền": iconResourceId = R.drawable.ic_category_tech; break;
                                    case "Thu nhập phụ": iconResourceId = R.drawable.ic_category_utilities; break;
                                    case "Dịch vụ": iconResourceId = R.drawable.ic_category_tech; break;
                                    default: iconResourceId = R.drawable.ic_category_salary; break;
                                }
                            } else { // Chi tiêu
                                switch (name) {
                                    case "Ăn uống": iconResourceId = R.drawable.ic_category_food; break;
                                    case "Di chuyển": iconResourceId = R.drawable.ic_category_transport; break;
                                    case "Mua sắm": iconResourceId = R.drawable.ic_category_shopping; break;
                                    case "Giải trí": iconResourceId = R.drawable.ic_category_entertainment; break;
                                    case "Y tế": iconResourceId = R.drawable.ic_category_health; break;
                                    case "Giáo dục": iconResourceId = R.drawable.ic_category_education; break;
                                    case "Nhà ở": iconResourceId = R.drawable.ic_category_housing; break;
                                    case "Du lịch": iconResourceId = R.drawable.ic_category_travel; break;
                                    case "Cafe & Trà": iconResourceId = R.drawable.ic_category_coffee; break;
                                    case "Tiện ích": iconResourceId = R.drawable.ic_category_utilities; break;
                                    default: iconResourceId = R.drawable.ic_category_utilities; break;
                                }
                            }
                            
                            // Cập nhật DB với icon mới
                            ContentValues values = new ContentValues();
                            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, iconResourceId);
                            
                            db.update(DatabaseContract.CategoriesEntry.TABLE_NAME, values, 
                                      DatabaseContract.CategoriesEntry._ID + "=?", 
                                      new String[]{String.valueOf(id)});
                            
                            Log.d(TAG, "Updated resource ID for category: " + name + " to " + iconResourceId);
                        }
                        
                        Category category = new Category(id, name, iconResourceId, categoryType);
                        categories.add(category);
                        
                        Log.d(TAG, "Added category: " + category.getName() + 
                                ", type: " + category.getType() + 
                                ", icon: " + category.getIconResourceId());
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing category row", e);
                    }
                } while (cursor.moveToNext());
            } else {
                Log.d(TAG, "No categories found for type: " + selectionArgs[0]);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying categories", e);
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing cursor", e);
                }
            }
        }
        
        return categories;
    }

    public long insertCategory(Category category) {
        SQLiteDatabase db = getDbHelper().getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, category.getName());
        values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, category.getIconResourceId());
        values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, category.isIncome() ? "income" : "expense");

        return db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
    }

    public int updateCategory(Category category) {
        SQLiteDatabase db = getDbHelper().getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, category.getName());
        values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, category.getIconResourceId());
        values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, category.isIncome() ? "income" : "expense");

        return db.update(
            DatabaseContract.CategoriesEntry.TABLE_NAME, 
            values, 
            DatabaseContract.CategoriesEntry._ID + " = ?", 
            new String[]{String.valueOf(category.getId())}
        );
    }

    public int deleteCategory(int categoryId) {
        SQLiteDatabase db = getDbHelper().getWritableDatabase();
        return db.delete(
            DatabaseContract.CategoriesEntry.TABLE_NAME, 
            DatabaseContract.CategoriesEntry._ID + " = ?", 
            new String[]{String.valueOf(categoryId)}
        );
    }

    public List<Category> getDefaultCategories(int type) {
        List<Category> defaultCategories = new ArrayList<>();
        
        Log.d(TAG, "============= STARTING TO CREATE DEFAULT CATEGORIES ==============");
        Log.d(TAG, "Creating default categories for type: " + (type == 1 ? "income" : "expense") + " [" + type + "]");
        
        try {
        if (type == 0) { // Expense
                // Ensure there are 10 expense categories
            defaultCategories.add(new Category(1, "Food & Dining", R.drawable.ic_category_food, 0));
            defaultCategories.add(new Category(2, "Transportation", R.drawable.ic_category_transport, 0));
            defaultCategories.add(new Category(3, "Shopping", R.drawable.ic_category_shopping, 0));
            defaultCategories.add(new Category(4, "Entertainment", R.drawable.ic_category_entertainment, 0));
            defaultCategories.add(new Category(5, "Healthcare", R.drawable.ic_category_health, 0));
            defaultCategories.add(new Category(6, "Education", R.drawable.ic_category_education, 0));
            defaultCategories.add(new Category(7, "Housing", R.drawable.ic_category_housing, 0));
            defaultCategories.add(new Category(8, "Bills & Utilities", R.drawable.ic_category_utilities, 0));
            defaultCategories.add(new Category(9, "Travel", R.drawable.ic_category_travel, 0));
            defaultCategories.add(new Category(10, "Other", R.drawable.ic_category_other, 0));
                
                // Log data for debugging
                for (Category cat : defaultCategories) {
                    Log.d(TAG, "Created expense category: " + cat.getId() + 
                          ", name: " + cat.getName() +
                          ", icon: " + cat.getIconResourceId() + 
                          ", type: " + cat.getType());
                }
            } else if (type == 1) { // Income
                // Ensure there are 10 income categories
                defaultCategories.add(new Category(11, "Salary", R.drawable.ic_category_salary, 1));
                defaultCategories.add(new Category(12, "Investment", R.drawable.ic_category_investment, 1));
                defaultCategories.add(new Category(13, "Gifts", R.drawable.ic_category_gift, 1));
                defaultCategories.add(new Category(14, "Scholarship", R.drawable.ic_category_education, 1));
                defaultCategories.add(new Category(15, "Sales", R.drawable.ic_category_shopping, 1));
                defaultCategories.add(new Category(16, "Bonus", R.drawable.ic_category_bonus, 1));
                defaultCategories.add(new Category(17, "Lending", R.drawable.ic_category_loan, 1));
                defaultCategories.add(new Category(18, "Refunds", R.drawable.ic_category_refund, 1));
                defaultCategories.add(new Category(19, "Side Income", R.drawable.ic_category_other, 1));
                defaultCategories.add(new Category(20, "Services", R.drawable.ic_category_tech, 1));
                
                // Log data for debugging
                for (Category cat : defaultCategories) {
                    Log.d(TAG, "Created income category: " + cat.getId() + 
                          ", name: " + cat.getName() +
                          ", icon: " + cat.getIconResourceId() + 
                          ", type: " + cat.getType());
                }
            }
            
            Log.d(TAG, "Created " + defaultCategories.size() + " default categories for type " + type);
            } catch (Exception e) {
            Log.e(TAG, "Error creating default categories for type " + type, e);
        }
        
        Log.d(TAG, "============= FINISHED CREATING DEFAULT CATEGORIES ==============");
        return defaultCategories;
    }
    
    // Hàm kiểm tra resource ID có hợp lệ không
    private boolean isValidResourceId(int resourceId) {
        try {
            if (resourceId <= 0) return false;
            appContext.getResources().getResourceName(resourceId);
            return true;
        } catch (Resources.NotFoundException e) {
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking resource: " + resourceId, e);
            return false;
        }
    }

    private List<Category> getCategoriesFromCursor(Cursor cursor) {
        List<Category> categories = new ArrayList<>();
        
        try {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int idIndex = cursor.getColumnIndexOrThrow(DatabaseContract.CategoriesEntry._ID);
                    int nameIndex = cursor.getColumnIndexOrThrow(DatabaseContract.CategoriesEntry.COLUMN_NAME);
                    int iconIndex = cursor.getColumnIndexOrThrow(DatabaseContract.CategoriesEntry.COLUMN_ICON);
                    int typeIndex = cursor.getColumnIndexOrThrow(DatabaseContract.CategoriesEntry.COLUMN_TYPE);
                    
                    int id = cursor.getInt(idIndex);
                    String name = cursor.getString(nameIndex);
                    int iconResId = cursor.getInt(iconIndex);
                    
                    // Xác định loại dựa trên chuỗi
                    String typeStr = cursor.getString(typeIndex);
                    int type = "income".equals(typeStr) ? 1 : 0;
                    
                    // Kiểm tra và sửa ID tài nguyên không hợp lệ
                    if (iconResId <= 0) {
                        // Gán ID icon mặc định dựa trên loại
                        iconResId = (type == 1) ? android.R.drawable.ic_menu_agenda : android.R.drawable.ic_menu_edit; 
                    }
                    
                    Category category = new Category(id, name, iconResId, type);
                    categories.add(category);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting categories from cursor", e);
        }
        
        return categories;
    }

    public long addCategory(Category category) {
        SQLiteDatabase db = null;
        long result = -1;
        
        try {
            Log.d(TAG, "Bắt đầu thêm danh mục: ID " + category.getId() + ", " + category.getName() + ", type: " + (category.isIncome() ? "income" : "expense") + ", icon: " + category.getIconResourceId());
            
            db = getDbHelper().getWritableDatabase();
            
            ContentValues values = new ContentValues();
            values.put(DatabaseContract.CategoriesEntry._ID, category.getId());
            values.put(DatabaseContract.CategoriesEntry.COLUMN_NAME, category.getName());
            values.put(DatabaseContract.CategoriesEntry.COLUMN_TYPE, category.isIncome() ? "income" : "expense");
            values.put(DatabaseContract.CategoriesEntry.COLUMN_ICON, category.getIconResourceId());
            
            // Kiểm tra xem ID này đã tồn tại chưa
            Cursor cursor = null;
            try {
                cursor = db.query(
                        DatabaseContract.CategoriesEntry.TABLE_NAME,
                        new String[]{DatabaseContract.CategoriesEntry._ID},
                        DatabaseContract.CategoriesEntry._ID + "=?",
                        new String[]{String.valueOf(category.getId())},
                        null, null, null);
                
                if (cursor != null && cursor.getCount() > 0) {
                    // Nếu ID đã tồn tại, cập nhật thay vì thêm mới
                    result = db.update(
                            DatabaseContract.CategoriesEntry.TABLE_NAME,
                            values,
                            DatabaseContract.CategoriesEntry._ID + "=?",
                            new String[]{String.valueOf(category.getId())}
                    );
                    Log.d(TAG, "CẬP NHẬT danh mục đã tồn tại: " + category.getId() + " - " + category.getName() + " - Kết quả: " + result);
                } else {
                    // Nếu ID chưa tồn tại, thêm mới
                    result = db.insert(DatabaseContract.CategoriesEntry.TABLE_NAME, null, values);
                    Log.d(TAG, "THÊM DANH MỤC MỚI: " + category.getId() + " - " + category.getName() + " - Kết quả: " + result);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            
            if (result <= 0) {
                Log.e(TAG, "Thêm/cập nhật danh mục THẤT BẠI: " + category.getId() + " - " + category.getName());
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi thêm/cập nhật danh mục: " + category.getName() + ", ID: " + category.getId(), e);
        }
        
        return result;
    }

    public void close() {
        DatabaseHelper dbHelper = dbHelperRef.getAndSet(null);
        if (dbHelper != null) {
            dbHelper.close();
            Log.d(TAG, "DatabaseHelper closed");
        }
    }

    // Truy cập SQLiteDatabase với quyền ghi
    public SQLiteDatabase getWritableDb() {
        return getDbHelper().getWritableDatabase();
    }

    // Truy cập SQLiteDatabase với quyền đọc
    public SQLiteDatabase getReadableDb() {
        return getDbHelper().getReadableDatabase();
    }

    /**
     * Get the total count of categories in the database
     * @return The number of category records
     */
    public int getCategoriesCount() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        int count = 0;
        
        try {
            db = getDbHelper().getReadableDatabase();
            String query = "SELECT COUNT(*) FROM " + DatabaseContract.CategoriesEntry.TABLE_NAME;
            cursor = db.rawQuery(query, null);
            
            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getInt(0);
                Log.d(TAG, "Total categories count: " + count);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting categories count", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return count;
    }

    // Helper method to check if a resource ID is valid
    private boolean isResourceValid(int resourceId) {
        if (resourceId <= 0) {
            return false;
        }
        
        try {
            // Check if the resource exists
            appContext.getResources().getResourceEntryName(resourceId);
            return true;
        } catch (Resources.NotFoundException e) {
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking resource validity", e);
            return false;
        }
    }
} 