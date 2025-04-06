package com.yourname.ssm;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.yourname.ssm.model.Category;
import com.yourname.ssm.repository.CategoryRepository;
import com.yourname.ssm.repository.TransactionRepository;
import com.yourname.ssm.repository.UserRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SSMUnitTests {

    private Context context;
    private CategoryRepository categoryRepository;
    private UserRepository userRepository;
    private TransactionRepository transactionRepository;
    
    @Before
    public void setup() {
        context = ApplicationProvider.getApplicationContext();
        
        categoryRepository = new CategoryRepository(context);
        userRepository = new UserRepository(context);
        transactionRepository = new TransactionRepository(context);
    }
    
    @After
    public void tearDown() {
        // Close database connection after testing
        if (categoryRepository != null) {
            categoryRepository.close();
        }
    }
    
    // Test 1: Check default category creation
    @Test
    public void testGetDefaultCategories() {
        // Execute
        List<Category> expenseCategories = categoryRepository.getDefaultCategories(0);
        List<Category> incomeCategories = categoryRepository.getDefaultCategories(1);
        
        // Verify
        assertEquals("Expense categories must have 10 items", 10, expenseCategories.size());
        assertEquals("Income categories must have 10 items", 10, incomeCategories.size());
        
        // Check category names and IDs
        assertEquals("First expense category must be Food", "Ăn uống", expenseCategories.get(0).getName());
        assertEquals("First income category must be Salary", "Lương", incomeCategories.get(0).getName());
        assertEquals("First expense category ID must be 1", 1, expenseCategories.get(0).getId());
        assertEquals("First income category ID must be 11", 11, incomeCategories.get(0).getId());
    }
    
    // Test 2: Check adding category to database
    @Test
    public void testAddCategory() {
        // Prepare
        Category testCategory = new Category(100, "Test Category", R.drawable.ic_category_other, 0);
        
        // Execute
        long result = categoryRepository.addCategory(testCategory);
        
        // Verify
        assertTrue("Adding category must return successful result", result > 0);
        
        // Verify the category was added
        List<Category> allCategories = categoryRepository.getAllCategories();
        boolean found = false;
        for (Category category : allCategories) {
            if (category.getId() == 100) {
                found = true;
                assertEquals("Category name must match", "Test Category", category.getName());
                break;
            }
        }
        assertTrue("Category must be found after adding", found);
    }
    
    // Test 3: Check counting categories
    @Test
    public void testGetCategoriesCount() {
        // Prepare - add some categories
        categoryRepository.addCategory(new Category(101, "Test 1", R.drawable.ic_category_other, 0));
        categoryRepository.addCategory(new Category(102, "Test 2", R.drawable.ic_category_other, 0));
        
        // Execute
        int count = categoryRepository.getCategoriesCount();
        
        // Verify
        assertTrue("Category count must be greater than 0", count > 0);
    }
    
    // Test 4: Check Category isIncome method
    @Test
    public void testCategoryIsIncome() {
        // Prepare
        Category expenseCategory = new Category(200, "Test Expense", R.drawable.ic_category_other, 0);
        Category incomeCategory = new Category(201, "Test Income", R.drawable.ic_category_other, 1);
        
        // Verify
        assertFalse("Expense is not income", expenseCategory.isIncome());
        assertTrue("Income must return true", incomeCategory.isIncome());
    }
    
    // Test 5: Check Category isExpense method
    @Test
    public void testCategoryIsExpense() {
        // Prepare
        Category expenseCategory = new Category(200, "Test Expense", R.drawable.ic_category_other, 0);
        Category incomeCategory = new Category(201, "Test Income", R.drawable.ic_category_other, 1);
        
        // Verify
        assertTrue("Expense must return true", expenseCategory.isExpense());
        assertFalse("Income is not expense", incomeCategory.isExpense());
    }
    
    // Test 6: Check getting categories by type
    @Test
    public void testGetCategoriesByType() {
        // Prepare
        categoryRepository.addCategory(new Category(103, "Test Expense", R.drawable.ic_category_other, 0));
        categoryRepository.addCategory(new Category(104, "Test Income", R.drawable.ic_category_other, 1));
        
        // Execute
        List<Category> expenseCategories = categoryRepository.getCategoriesByType(0);
        List<Category> incomeCategories = categoryRepository.getCategoriesByType(1);
        
        // Verify
        assertFalse("Expense categories list must not be empty", expenseCategories.isEmpty());
        assertFalse("Income categories list must not be empty", incomeCategories.isEmpty());
        
        // Check category types
        for (Category category : expenseCategories) {
            assertEquals("Category must have expense type (0)", 0, category.getType());
        }
        
        for (Category category : incomeCategories) {
            assertEquals("Category must have income type (1)", 1, category.getType());
        }
    }
    
    // Test 7: Check Category setters/getters
    @Test
    public void testCategorySettersGetters() {
        // Create object with initial values
        Category category = new Category(300, "Original Name", R.drawable.ic_category_food, 0);
        
        // Check getters
        assertEquals(300, category.getId());
        assertEquals("Original Name", category.getName());
        assertEquals(R.drawable.ic_category_food, category.getIconResourceId());
        assertEquals(0, category.getType());
        
        // Update with setters
        category.setId(301);
        category.setName("New Name");
        category.setIconResourceId(R.drawable.ic_category_other);
        category.setType(1);
        
        // Check again after update
        assertEquals(301, category.getId());
        assertEquals("New Name", category.getName());
        assertEquals(R.drawable.ic_category_other, category.getIconResourceId());
        assertEquals(1, category.getType());
    }
    
    // Test 8: Check repository close() method
    @Test
    public void testRepositoryClose() {
        // Prepare
        CategoryRepository tempRepo = new CategoryRepository(context);
        
        // Verify
        // If method doesn't throw exception, consider it passed
        tempRepo.close();
        assertTrue(true);
    }
    
    // Test 9: Check getReadableDb and getWritableDb
    @Test
    public void testGetDatabases() {
        // Execute
        SQLiteDatabase readableDb = categoryRepository.getReadableDb();
        SQLiteDatabase writableDb = categoryRepository.getWritableDb();
        
        // Verify
        assertNotNull("Readable DB must not be null", readableDb);
        assertNotNull("Writable DB must not be null", writableDb);
        assertTrue("Readable DB must be open", readableDb.isOpen());
        assertTrue("Writable DB must be open", writableDb.isOpen());
    }
    
    // Test 10: Check update category
    @Test
    public void testUpdateCategory() {
        // Prepare
        Category category = new Category(400, "Test Update", R.drawable.ic_category_other, 0);
        
        // Add category
        categoryRepository.addCategory(category);
        
        // Update category
        category.setName("Updated Name");
        category.setIconResourceId(R.drawable.ic_category_food);
        
        // Execute update
        int rowsAffected = categoryRepository.updateCategory(category);
        
        // Get category after update
        List<Category> allCategories = categoryRepository.getAllCategories();
        Category updatedCategory = null;
        for (Category c : allCategories) {
            if (c.getId() == 400) {
                updatedCategory = c;
                break;
            }
        }
        
        // Verify
        assertNotNull("Updated category must exist", updatedCategory);
        assertEquals("Category name must be updated", "Updated Name", updatedCategory.getName());
        assertEquals("Category icon must be updated", R.drawable.ic_category_food, updatedCategory.getIconResourceId());
    }
} 