package com.yourname.ssm.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.yourname.ssm.model.Budget;
import com.yourname.ssm.model.ChatMessage;
import com.yourname.ssm.model.Transaction;
import com.yourname.ssm.api.OpenAIClient;
import com.yourname.ssm.api.CozeAIService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.text.NumberFormat;
import java.util.Random;

public class ChatRepository {
    private static final String TAG = "ChatRepository";
    
    private final Context context;
    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final Handler handler;
    private NumberFormat currencyFormatter;
    private List<String> financialTips;
    private boolean usingCozeAI = true;  // Flag to control which AI service to use

    public ChatRepository(Context context) {
        this.context = context;
        this.budgetRepository = new BudgetRepository(context);
        this.transactionRepository = new TransactionRepository(context);
        this.chatMessageRepository = new ChatMessageRepository(context);
        this.handler = new Handler(Looper.getMainLooper());
        this.currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        initFinancialTips();
    }

    private void initFinancialTips() {
        financialTips = new ArrayList<>();
        financialTips.add("Create a monthly spending plan to better control your personal finances.");
        financialTips.add("Try to save 20% of your income for future investments and savings.");
        financialTips.add("When shopping, ask yourself if you really need the item or just want it.");
        financialTips.add("Follow the 50/30/20 rule: 50% for essentials, 30% for wants, and 20% for savings.");
        financialTips.add("Limit credit card usage to avoid unnecessary debt.");
        financialTips.add("Track your daily expenses to know where your money is going.");
        financialTips.add("Set specific and realistic financial goals to make them easier to achieve.");
        financialTips.add("Good personal finance management helps reduce financial stress and anxiety.");
        financialTips.add("When shopping online, wait at least 24 hours before purchasing to avoid impulse buying.");
    }
    
    /**
     * Lấy danh sách tin nhắn đã lưu trữ
     * @param userId ID của người dùng
     * @return Danh sách tin nhắn
     */
    public List<ChatMessage> getMessageHistory(int userId) {
        return chatMessageRepository.getMessagesForUser(userId);
    }

    /**
     * Lấy lời chào mặc định từ AI
     * @param userId ID người dùng
     * @param callback Callback khi nhận được tin nhắn
     */
    public void getInitialGreeting(int userId, final ChatCallback callback) {
        // Kiểm tra lịch sử chat trước
        List<ChatMessage> history = getMessageHistory(userId);
        if (!history.isEmpty()) {
            // Nếu đã có lịch sử chat, không cần gửi lời chào nữa
            return;
        }
        
        if (usingCozeAI) {
            // Sử dụng Coze AI để lấy lời chào
            CozeAIService.sendMessage("Hello", null, new CozeAIService.CozeAICallback() {
                @Override
                public void onSuccess(String message) {
                    // Tạo tin nhắn với phản hồi từ Coze AI
                    final ChatMessage greeting = new ChatMessage(message, ChatMessage.TYPE_RECEIVED, userId);
                    
                    // Lưu tin nhắn vào cơ sở dữ liệu
                    chatMessageRepository.saveMessage(greeting);
                    
                    // Gửi callback trên main thread
                    handler.post(() -> callback.onMessageReceived(greeting));
                }

                @Override
                public void onFailure(String errorMessage) {
                    // Fallback to default greeting if Coze AI fails
                    Log.e(TAG, "Coze AI error: " + errorMessage);
                    sendDefaultGreeting(userId, callback);
                }
            });
        } else {
            // Sử dụng lời chào mặc định nếu không dùng Coze AI
            sendDefaultGreeting(userId, callback);
        }
    }
    
    private void sendDefaultGreeting(int userId, ChatCallback callback) {
        // Tạo lời chào mặc định
        ChatMessage greeting = new ChatMessage(
                "Hello! I'm an AI financial assistant. I can help you analyze your spending, provide personal finance advice, or simply chat about your financial concerns. What would you like to ask?",
                ChatMessage.TYPE_RECEIVED,
                userId
        );
        
        // Lưu lời chào vào cơ sở dữ liệu
        chatMessageRepository.saveMessage(greeting);
        
        // Gửi callback
        handler.postDelayed(() -> callback.onMessageReceived(greeting), 500);
    }

    /**
     * Lấy phản hồi cho tin nhắn của người dùng
     * @param userMessage Nội dung tin nhắn
     * @param userId ID người dùng
     * @param callback Callback khi nhận được tin nhắn
     */
    public void getReplyToMessage(String userMessage, int userId, final ChatCallback callback) {
        if (usingCozeAI) {
            // Lấy lịch sử tin nhắn để cung cấp context cho AI
            try {
                // Get recent message history
                List<ChatMessage> history = chatMessageRepository.getRecentMessagesForUser(userId, 10);
                JSONArray messageHistory = convertMessagesToJson(history);
                
                // Call Coze AI
                CozeAIService.sendMessage(userMessage, messageHistory, new CozeAIService.CozeAICallback() {
                    @Override
                    public void onSuccess(String message) {
                        // Create message with Coze AI response
                        final ChatMessage aiMessage = new ChatMessage(message, ChatMessage.TYPE_RECEIVED, userId);
                        
                        // Save message to database
                        chatMessageRepository.saveMessage(aiMessage);
                        
                        // Send callback on main thread
                        handler.post(() -> callback.onMessageReceived(aiMessage));
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        // Fallback to simple reply if Coze AI fails
                        Log.e(TAG, "Coze AI error: " + errorMessage);
                        sendFallbackReply(userMessage, userId, callback);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error preparing message history for Coze AI", e);
                sendFallbackReply(userMessage, userId, callback);
            }
        } else {
            // Fallback to the old method if not using Coze AI
            sendFallbackReply(userMessage, userId, callback);
        }
    }
    
    private void sendFallbackReply(String userMessage, int userId, ChatCallback callback) {
        // Phân tích tin nhắn của người dùng để xác định nội dung phản hồi
        String reply = generateReply(userMessage.toLowerCase(), userId);

        // Tạo tin nhắn phản hồi
        final ChatMessage aiMessage = new ChatMessage(reply, ChatMessage.TYPE_RECEIVED, userId);
        
        // Lưu tin nhắn vào cơ sở dữ liệu
        chatMessageRepository.saveMessage(aiMessage);

        // Thêm độ trễ nhỏ để tạo cảm giác AI đang xử lý
        int delay = calculateReplyDelay(reply);
        handler.postDelayed(() -> callback.onMessageReceived(aiMessage), delay);
    }
    
    /**
     * Xử lý tin nhắn hình ảnh từ người dùng
     * @param base64Image Hình ảnh được mã hóa Base64
     * @param caption Chú thích kèm theo (nếu có)
     * @param userId ID người dùng
     * @param callback Callback khi nhận được phản hồi
     */
    public void processImageMessage(String base64Image, String caption, int userId, final ChatCallback callback) {
        if (usingCozeAI) {
            // Kết hợp caption và request cho AI phân tích ảnh
            String prompt = caption != null && !caption.isEmpty() 
                ? caption 
                : "Analyze this image and provide financial insights if possible.";
            
            CozeAIService.sendImageMessage(base64Image, prompt, new CozeAIService.CozeAICallback() {
                @Override
                public void onSuccess(String message) {
                    // Tạo tin nhắn với phản hồi từ Coze AI
                    final ChatMessage aiMessage = new ChatMessage(message, ChatMessage.TYPE_RECEIVED, userId);
                    
                    // Lưu tin nhắn vào cơ sở dữ liệu
                    chatMessageRepository.saveMessage(aiMessage);
                    
                    // Gửi callback trên main thread
                    handler.post(() -> callback.onMessageReceived(aiMessage));
                }

                @Override
                public void onFailure(String errorMessage) {
                    // Phản hồi lỗi nếu không thể xử lý ảnh
                    Log.e(TAG, "Coze AI image processing error: " + errorMessage);
                    
                    // Tạo tin nhắn lỗi
                    final ChatMessage errorMsg = new ChatMessage(
                            "I cannot process the image at this time. Sorry for the inconvenience.",
                            ChatMessage.TYPE_RECEIVED,
                            userId
                    );
                    
                    // Lưu tin nhắn vào cơ sở dữ liệu
                    chatMessageRepository.saveMessage(errorMsg);
                    
                    // Gửi callback
                    handler.post(() -> callback.onMessageReceived(errorMsg));
                }
            });
        } else {
            // Phản hồi nếu không hỗ trợ xử lý ảnh
            final ChatMessage notSupportedMessage = new ChatMessage(
                    "I cannot process images at this time. Please describe in text instead.",
                    ChatMessage.TYPE_RECEIVED,
                    userId
            );
            
            // Lưu tin nhắn vào cơ sở dữ liệu
            chatMessageRepository.saveMessage(notSupportedMessage);
            
            // Gửi callback
            handler.postDelayed(() -> callback.onMessageReceived(notSupportedMessage), 800);
        }
    }
    
    /**
     * Chuyển đổi danh sách tin nhắn sang định dạng JSON để gửi cho API
     */
    private JSONArray convertMessagesToJson(List<ChatMessage> messages) {
        JSONArray jsonArray = new JSONArray();
        try {
            for (ChatMessage message : messages) {
                JSONObject jsonMessage = new JSONObject();
                jsonMessage.put("role", message.getType() == ChatMessage.TYPE_SENT ? "user" : "assistant");
                jsonMessage.put("content", message.getMessage());
                jsonArray.put(jsonMessage);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error converting messages to JSON", e);
        }
        return jsonArray;
    }
    
    /**
     * Lưu tin nhắn người dùng vào cơ sở dữ liệu
     * @param message Tin nhắn cần lưu
     */
    public void saveUserMessage(ChatMessage message) {
        chatMessageRepository.saveMessage(message);
    }
    
    /**
     * Lưu tin nhắn hình ảnh từ người dùng
     * @param imageUrl URL của hình ảnh
     * @param caption Chú thích (nếu có)
     * @param userId ID người dùng
     * @return Tin nhắn đã tạo
     */
    public ChatMessage saveUserImageMessage(String imageUrl, String caption, int userId) {
        ChatMessage imageMessage = new ChatMessage(
                caption != null && !caption.isEmpty() ? caption : "",
                ChatMessage.TYPE_SENT,
                userId,
                imageUrl,
                ChatMessage.CONTENT_IMAGE
        );
        
        chatMessageRepository.saveMessage(imageMessage);
        return imageMessage;
    }
    
    /**
     * Xóa tất cả tin nhắn chat của một người dùng
     * @param userId ID người dùng
     * @return Số tin nhắn đã xóa
     */
    public int clearChatHistory(int userId) {
        return chatMessageRepository.deleteAllMessagesForUser(userId);
    }

    private String generateReply(String userMessage, int userId) {
        // Phân tích nội dung tin nhắn để đưa ra phản hồi phù hợp
        userMessage = userMessage.toLowerCase().trim();
        
        // Kiểm tra lịch sử để tránh lặp lại câu chào
        List<ChatMessage> recentMessages = chatMessageRepository.getRecentMessagesForUser(userId, 5);
        boolean hasGreeted = false;
        // Kiểm tra context của cuộc trò chuyện
        boolean hasRecentSpendingContext = false;
        boolean hasRecentBudgetContext = false;
        boolean hasRecentSavingContext = false;
        
        // Đếm số lần AI đã chào trong 5 tin nhắn gần nhất và kiểm tra context
        for (ChatMessage msg : recentMessages) {
            if (msg.getType() == ChatMessage.TYPE_RECEIVED && 
                msg.getMessage() != null) {
                
                if (msg.getMessage().contains("Chào bạn!") || 
                    msg.getMessage().contains("Xin chào!") ||
                    msg.getMessage().contains("Hôm nay bạn cảm thấy thế nào?")) {
                    hasGreeted = true;
                }
                
                // Kiểm tra nếu tin nhắn gần đây nói về vấn đề chi tiêu
                if (msg.getMessage().contains("chi tiêu") || 
                    msg.getMessage().contains("tiêu nhiều") ||
                    msg.getMessage().contains("tiết kiệm") ||
                    msg.getMessage().contains("ngân sách") ||
                    msg.getMessage().contains("30-ngày không mua sắm")) {
                    hasRecentSpendingContext = true;
                }
                
                // Kiểm tra nếu tin nhắn gần đây nói về ngân sách
                if (msg.getMessage().contains("ngân sách") ||
                    msg.getMessage().contains("tổng ngân sách") ||
                    msg.getMessage().contains("đã sử dụng") ||
                    msg.getMessage().contains("còn lại")) {
                    hasRecentBudgetContext = true;
                }
                
                // Kiểm tra nếu tin nhắn gần đây nói về tiết kiệm
                if (msg.getMessage().contains("tiết kiệm") ||
                    msg.getMessage().contains("tích lũy") ||
                    msg.getMessage().contains("đầu tư")) {
                    hasRecentSavingContext = true;
                }
            }
        }
        
        // Kiểm tra nếu người dùng đang hỏi cách giải quyết vấn đề
        boolean isAskingHowToSolve = containsAny(userMessage, 
            "lam sao", "làm sao", "làm thế nào", "lam the nao", 
            "giải quyết", "giai quyet", "xử lý", "xu ly", 
            "khắc phục", "khac phuc", "cách nào", "cach nao");
        
        // Từ khóa về chi tiêu quá nhiều
        String[] spendingTooMuchPatterns = {
            "tieu nhieu", "tiêu nhiều", "tốn nhiều", "ton nhieu", 
            "chi nhieu", "chi nhiều", "tieu qua nhieu", "tiêu quá nhiều", 
            "ton qua nhieu", "tốn quá nhiều", "tieu nhieu qua", "tiêu nhiều quá", 
            "tieu het tien", "tiêu hết tiền", "ton kem", "tốn kém",
            "hao phi", "hao phí", "xa xi", "xa xỉ", "mua sam nhieu", "mua sắm nhiều"
        };

        // Từ khóa về cảm xúc tiêu cực
        String[] negativeEmotionPatterns = {
            "buon", "buồn", "chan", "chán", "stress", "ap luc", "áp lực", 
            "lo lang", "lo lắng", "cang thang", "căng thẳng", "met moi", "mệt mỏi", 
            "kho khan", "khó khăn", "that vong", "thất vọng", "dau long", "đau lòng",
            "ko vui", "không vui", "ko khoe", "không khỏe", "ko on", "không ổn"
        };

        // Từ khóa về chi tiêu và quản lý
        String[] spendingManagementPatterns = {
            "chi tieu", "chi tiêu", "tieu", "tiêu", "ton", "tốn", "mua", 
            "quan ly", "quản lý", "tien", "tiền", "thu nhap", "thu nhập"
        };

        // Từ khóa về ngân sách
        String[] budgetPatterns = {
            "ngan sach", "ngân sách", "budget", "han muc", "hạn mức", 
            "vuot", "vượt", "du toan", "dự toán", "ke hoach", "kế hoạch"
        };

        // Từ khóa về tiết kiệm
        String[] savingPatterns = {
            "tiet kiem", "tiết kiệm", "tich luy", "tích lũy", "tien", "tiền", 
            "tai chinh", "tài chính", "dau tu", "đầu tư", "sinh loi", "sinh lời"
        };

        // Từ khóa về lời khuyên
        String[] advicePatterns = {
            "loi khuyen", "lời khuyên", "meo", "mẹo", "tip", "goi y", "gợi ý",
            "giup do", "giúp đỡ", "tu van", "tư vấn", "huong dan", "hướng dẫn"
        };

        // Từ khóa về báo cáo
        String[] reportPatterns = {
            "bao cao", "báo cáo", "thong ke", "thống kê", "tong ket", "tổng kết",
            "phan tich", "phân tích", "xem lai", "xem lại", "ket qua", "kết quả"
        };

        // Nếu người dùng đang hỏi cách giải quyết và có context về chi tiêu
        if (isAskingHowToSolve && hasRecentSpendingContext) {
            return getDetailedSpendingSolution(userId);
        }
        
        // Nếu người dùng đang hỏi cách giải quyết và có context về ngân sách
        if (isAskingHowToSolve && hasRecentBudgetContext) {
            return getDetailedBudgetSolution(userId);
        }
        
        // Nếu người dùng đang hỏi cách giải quyết và có context về tiết kiệm
        if (isAskingHowToSolve && hasRecentSavingContext) {
            return getDetailedSavingSolution();
        }

        // Kiểm tra các pattern về chi tiêu nhiều (ưu tiên cao nhất)
        if (containsAnyPattern(userMessage, spendingTooMuchPatterns)) {
            return getSpendingAdvice(userId);
        }
        // Nếu là tin nhắn chào hỏi đầu tiên và chưa chào
        else if ((userMessage.isEmpty() || isGreeting(userMessage)) && !hasGreeted) {
            return "Chào bạn! Tôi là trợ lý AI về tài chính của bạn. Tôi có thể giúp bạn theo dõi chi tiêu, đưa ra lời khuyên về tài chính, hoặc đơn giản là trò chuyện. Bạn cần hỗ trợ gì hôm nay?";
        } 
        // Nếu người dùng nói về tâm trạng buồn, stress, tiêu cực
        else if (containsAnyPattern(userMessage, negativeEmotionPatterns)) {
            return getStressReliefResponse(userId, userMessage);
        } 
        // Nếu người dùng nói về chi tiêu 
        else if (containsAnyPattern(userMessage, spendingManagementPatterns)) {
            return getSpendingAdvice(userId);
        } 
        // Nếu người dùng nhắc đến ngân sách, budget
        else if (containsAnyPattern(userMessage, budgetPatterns)) {
            return getBudgetStatus(userId);
        } 
        // Nếu người dùng nhắc đến tiết kiệm, tiền
        else if (containsAnyPattern(userMessage, savingPatterns)) {
            return getSavingAdvice();
        } 
        // Nếu người dùng cần lời khuyên, mẹo
        else if (containsAnyPattern(userMessage, advicePatterns)) {
            return getRandomFinancialTip();
        } 
        // Nếu người dùng muốn xem báo cáo, thống kê
        else if (containsAnyPattern(userMessage, reportPatterns)) {
            return getFinancialSummary(userId);
        } 
        // Nếu người dùng cảm ơn
        else if (containsAny(userMessage, "cảm ơn", "cam on", "thank", "thanks", "cam ta", "cảm tạ")) {
            return "Rất vui khi được trò chuyện và hỗ trợ bạn! Nếu bạn cần thêm thông tin về quản lý tài chính hoặc cần lời khuyên, tôi luôn sẵn sàng.";
        } 
        // Nếu người dùng tạm biệt
        else if (containsAny(userMessage, "hẹn gặp lại", "hen gap lai", "tạm biệt", "tam biet", "bye", "bai", "gặp lại sau", "gap lai sau")) {
            return "Tạm biệt! Rất vui được giúp đỡ bạn. Hãy nhớ quay lại khi bạn cần tư vấn về tài chính nhé!";
        } 
        // Nếu người dùng muốn xóa lịch sử
        else if (containsAny(userMessage, "xóa lịch sử", "xoa lich su", "clear history", "xóa tin nhắn", "xoa tin nhan", "dọn dẹp", "don dep")) {
            clearChatHistory(userId);
            return "Tôi đã xóa lịch sử trò chuyện của chúng ta. Bạn có thể bắt đầu cuộc trò chuyện mới.";
        } 
        // Các trường hợp khác, phân tích và tạo câu trả lời phù hợp
        else {
            return handleEmpathicResponse(userMessage, userId);
        }
    }
    
    private boolean isGreeting(String message) {
        return containsAny(message, "xin chào", "chào", "hello", "hi", "hey", "chao", "xin chao", "alo", "a lô", "chao buoi sang", "chào buổi sáng");
    }

    /**
     * Tạo phản hồi đồng cảm dựa trên nội dung tin nhắn của người dùng
     * @param message Tin nhắn của người dùng
     * @param userId ID của người dùng
     * @return Câu trả lời đồng cảm
     */
    private String handleEmpathicResponse(String message, int userId) {
        // Kiểm tra lịch sử tin nhắn gần đây
        List<ChatMessage> recentMessages = chatMessageRepository.getRecentMessagesForUser(userId, 5);
        
        // Phân tích cảm xúc cụ thể - mở rộng từ khóa không dấu
        boolean isSad = containsAny(message, "buồn", "buon", "chán", "chan", "khổ", "kho", "đau", "dau", "khóc", "khoc", "thất vọng", "that vong");
        boolean isWorried = containsAny(message, "lo", "sợ", "so", "ngại", "ngai", "áp lực", "ap luc", "căng thẳng", "cang thang", "lo âu", "lo au");
        boolean isTired = containsAny(message, "mệt", "met", "kiệt sức", "kiet suc", "chán nản", "chan nan", "không còn sức", "khong con suc", "mỏi", "moi");
        boolean isHappy = containsAny(message, "vui", "hạnh phúc", "hanh phuc", "phấn khởi", "phan khoi", "tuyệt vời", "tuyet voi", "thích", "thich", "tốt", "tot");
        boolean asksQuestion = message.contains("?") || containsAny(message, "làm sao", "lam sao", "thế nào", "the nao", "tại sao", "tai sao", 
            "bằng cách nào", "bang cach nao", "bao nhiêu", "bao nhieu", "khi nào", "khi nao", "ở đâu", "o dau");
        boolean isAboutFinances = containsAny(message, "tiền", "tien", "chi tiêu", "chi tieu", "ngân sách", "ngan sach", "đầu tư", "dau tu", 
            "tiết kiệm", "tiet kiem", "thu nhập", "thu nhap", "chi phí", "chi phi");
        
        // Trường hợp người dùng nói là buồn - mở rộng câu trả lời
        if (isSad) {
            String[] sadResponses = {
                "Tôi rất tiếc khi nghe bạn đang cảm thấy buồn. Đôi khi, việc chia sẻ nỗi buồn với người khác có thể giúp bạn cảm thấy nhẹ nhõm hơn. Bạn có muốn chia sẻ điều gì đang làm bạn buồn không?",
                "Tôi hiểu cảm giác buồn bã có thể rất khó khăn. Hãy nhớ rằng mọi cảm xúc đều tạm thời và sẽ qua đi. Tôi ở đây để lắng nghe bạn nếu bạn muốn trò chuyện.",
                "Khi cảm thấy buồn, đôi khi việc làm những điều nhỏ nhặt mà bạn yêu thích có thể giúp ích. Như nghe một bài hát bạn thích, hoặc trò chuyện với người thân. Bạn đã thử làm gì để cảm thấy tốt hơn chưa?",
                "Nỗi buồn là một phần tự nhiên của cuộc sống, nhưng bạn không phải đối mặt với nó một mình. Tôi luôn ở đây để lắng nghe và hỗ trợ bạn vượt qua những khoảng thời gian khó khăn này.",
                "Tôi hiểu rằng đôi khi cuộc sống có thể rất khó khăn. Tâm trạng buồn là điều tự nhiên, nhưng hãy nhớ rằng bạn không đơn độc. Dành chút thời gian cho bản thân và thực hiện những hoạt động bạn yêu thích có thể giúp cải thiện tâm trạng."
            };
            return getRandomResponse(sadResponses);
        }
        // Trường hợp người dùng hỏi về tài chính
        else if (isAboutFinances && asksQuestion) {
            String[] financeResponses = {
                "Về vấn đề tài chính của bạn, tôi nghĩ việc lập kế hoạch chi tiêu và theo dõi chi phí hàng ngày là rất quan trọng. Bạn có thể sử dụng ứng dụng này để ghi lại các khoản chi và xem báo cáo chi tiêu của mình.",
                "Quản lý tài chính cá nhân bắt đầu từ việc hiểu rõ thu nhập và chi tiêu của bạn. Hãy dành thời gian để ghi lại mọi khoản chi tiêu, dù nhỏ, để có cái nhìn tổng quan về tình hình tài chính của mình.",
                "Một phương pháp phổ biến để quản lý tài chính là quy tắc 50/30/20: dùng 50% thu nhập cho nhu cầu thiết yếu, 30% cho mong muốn cá nhân, và 20% để tiết kiệm hoặc đầu tư.",
                "Để cải thiện tình hình tài chính, việc tạo quỹ khẩn cấp là rất quan trọng. Hãy cố gắng tiết kiệm đủ chi phí sinh hoạt trong 3-6 tháng để phòng những tình huống bất ngờ."
            };
            return getRandomResponse(financeResponses);
        }
        // Giữ nguyên các trường hợp khác
        else if (isWorried) {
            String[] worriedResponses = {
                "Lo lắng là điều tự nhiên khi chúng ta đối mặt với thách thức. Hãy thử tập trung vào những điều bạn có thể kiểm soát và để qua những điều ngoài tầm kiểm soát.",
                "Tôi hiểu cảm giác lo lắng có thể rất áp đảo. Thử thực hiện một số bài tập thở sâu có thể giúp bạn cảm thấy bình tĩnh hơn: hít vào trong 4 giây, giữ 4 giây, và thở ra trong 6 giây.",
                "Khi lo lắng, đôi khi viết ra những điều bạn đang lo có thể giúp bạn nhìn nhận chúng rõ ràng hơn. Bạn đã thử phương pháp này chưa?",
                "Lo lắng thường bắt nguồn từ việc không chắc chắn về tương lai. Hãy nhớ rằng, dù có chuyện gì xảy ra, bạn đều có đủ sức mạnh để vượt qua."
            };
            return getRandomResponse(worriedResponses);
        }
        // Trường hợp người dùng mệt mỏi
        else if (isTired) {
            String[] tiredResponses = {
                "Cảm giác mệt mỏi có thể ảnh hưởng lớn đến tinh thần và thể chất. Hãy dành thời gian nghỉ ngơi và phục hồi năng lượng. Một giấc ngủ ngắn hoặc một bữa ăn nhẹ lành mạnh có thể giúp ích.",
                "Sự mệt mỏi thường là dấu hiệu cho thấy cơ thể và tâm trí cần được nghỉ ngơi. Đừng quá khắt khe với bản thân, hãy cho phép mình có thời gian thư giãn.",
                "Khi cảm thấy kiệt sức, hãy thử áp dụng quy tắc 'thiền 5 phút': chỉ cần ngồi yên lặng và tập trung vào hơi thở trong 5 phút. Điều này có thể giúp bạn phục hồi tinh thần.",
                "Mệt mỏi thường đến từ việc cố gắng quá sức. Hãy nhớ rằng việc chăm sóc bản thân cũng quan trọng như việc hoàn thành công việc. Bạn xứng đáng được nghỉ ngơi."
            };
            return getRandomResponse(tiredResponses);
        }
        // Trường hợp người dùng vui vẻ
        else if (isHappy) {
            String[] happyResponses = {
                "Thật vui khi nghe bạn đang có tâm trạng tốt! Những cảm xúc tích cực như vậy rất đáng trân trọng. Hãy tiếp tục duy trì năng lượng tích cực này nhé!",
                "Niềm vui của bạn thực sự truyền cảm hứng! Khi chúng ta vui vẻ, mọi thứ xung quanh dường như cũng trở nên tươi sáng hơn. Hãy chia sẻ niềm vui này với những người xung quanh bạn!",
                "Tuyệt vời! Những khoảnh khắc hạnh phúc là nguồn năng lượng quý giá giúp chúng ta vượt qua những thời điểm khó khăn. Hãy trân trọng và ghi nhớ cảm giác này!",
                "Thật tuyệt khi biết bạn đang cảm thấy vui vẻ! Hạnh phúc là điều chúng ta đều hướng tới, và tôi rất vui khi bạn đang cảm nhận được điều đó!"
            };
            return getRandomResponse(happyResponses);
        }
        // Trường hợp người dùng đặt câu hỏi
        else if (asksQuestion) {
            String[] questionResponses = {
                "Đó là câu hỏi rất hay. Tôi nghĩ việc quan trọng là bạn cần thời gian để suy ngẫm và tìm ra giải pháp phù hợp với bản thân. Tôi luôn ở đây để lắng nghe và hỗ trợ bạn.",
                "Câu hỏi của bạn rất thú vị. Đôi khi, việc đặt câu hỏi là bước đầu tiên để tìm ra giải pháp. Bạn đã thử nghĩ về vấn đề này từ góc độ nào khác chưa?",
                "Tôi hiểu câu hỏi của bạn. Đôi khi, việc chia sẻ những suy nghĩ và cảm xúc của mình có thể giúp bạn tìm ra câu trả lời. Hãy kể thêm về điều đang khiến bạn quan tâm."
            };
            return getRandomResponse(questionResponses);
        } 
        // Tìm chủ đề trong tin nhắn của người dùng
        else if (containsAny(message, "chi tiêu", "chi tieu", "tiêu tiền", "tieu tien", "mua sắm", "mua sam")) {
            return "Việc quản lý chi tiêu là rất quan trọng. Bạn có thể sử dụng ứng dụng này để theo dõi chi tiêu hàng ngày, qua đó có cái nhìn rõ ràng hơn về những khoản tiền đã sử dụng và lập kế hoạch tốt hơn cho tương lai.";
        } else {
            // Trả lời đa dạng hơn cho các tin nhắn khác
            String[] generalResponses = {
                "Cảm ơn bạn đã chia sẻ. Tôi luôn ở đây để lắng nghe và trò chuyện với bạn về bất cứ điều gì bạn muốn.",
                "Điều bạn nói thật thú vị. Có điều gì khác về tài chính cá nhân mà bạn muốn trao đổi không?",
                "Tôi đánh giá cao việc bạn chia sẻ những suy nghĩ của mình. Bạn có cần tư vấn gì về quản lý chi tiêu hoặc ngân sách không?",
                "Thật thú vị khi nghe bạn nói về điều này. Bạn có muốn chia sẻ thêm về mục tiêu tài chính của mình không?",
                "Tôi hiểu điều bạn đang nói. Nếu bạn muốn tìm hiểu thêm về cách quản lý tài chính hiệu quả, tôi có thể chia sẻ một số mẹo hữu ích.",
                "Mỗi cuộc trò chuyện đều giúp tôi hiểu bạn hơn. Hãy cho tôi biết nếu bạn cần hỗ trợ về lập kế hoạch tài chính hoặc tiết kiệm."
            };
            return getRandomResponse(generalResponses);
        }
    }

    /**
     * Tạo phản hồi giúp giảm stress dựa trên tin nhắn của người dùng
     * @param userId ID của người dùng
     * @param message Tin nhắn của người dùng
     * @return Câu trả lời giúp giảm stress
     */
    private String getStressReliefResponse(int userId, String message) {
        String[] supportMessages = {
            "Tôi hiểu rằng bạn đang cảm thấy căng thẳng. Hãy thử thực hiện phương pháp thở sâu: hít vào trong 4 giây, giữ 4 giây, và thở ra trong 6 giây. Lặp lại 5-10 lần, bạn sẽ cảm thấy bình tĩnh hơn.",
            
            "Áp lực cuộc sống đôi khi có thể rất nặng nề. Nhưng hãy nhớ rằng, bạn không cô đơn. Tôi luôn ở đây để lắng nghe và đồng hành cùng bạn. Hãy dành vài phút để nghỉ ngơi và chăm sóc bản thân.",
            
            "Khi cảm thấy lo lắng, hãy thử phương pháp 5-4-3-2-1: Nhận biết 5 thứ bạn nhìn thấy, 4 thứ bạn có thể chạm vào, 3 thứ bạn nghe được, 2 thứ bạn ngửi được, và 1 thứ bạn nếm được. Điều này sẽ giúp bạn kéo tâm trí về hiện tại.",
            
            "Đôi khi, việc viết ra những điều khiến bạn lo lắng có thể giúp giảm bớt gánh nặng trong tâm trí. Hãy thử ghi lại những suy nghĩ của bạn, sau đó gấp tờ giấy lại và cất đi. Điều này tượng trưng cho việc bạn đang tạm gác lại những lo lắng.",
            
            "Một cách để giảm căng thẳng là thực hành lòng biết ơn. Hãy nghĩ về 3 điều bạn biết ơn ngày hôm nay, dù là những điều nhỏ nhất. Điều này có thể giúp chuyển hướng suy nghĩ của bạn từ tiêu cực sang tích cực.",
            
            "Khi cảm thấy quá tải, hãy nhớ rằng bạn không cần phải giải quyết mọi thứ cùng một lúc. Hãy chia nhỏ vấn đề và giải quyết từng bước một. Mỗi bước nhỏ đều đáng được ghi nhận.",
            
            "Hãy dành thời gian cho những hoạt động bạn yêu thích, dù chỉ là 10-15 phút mỗi ngày. Đó có thể là đọc sách, nghe nhạc, đi dạo, hoặc bất cứ điều gì mang lại niềm vui và sự thư giãn cho bạn.",
            
            "Áp lực tài chính là điều mà hầu hết mọi người đều gặp phải. Hãy nhớ rằng, quản lý tài chính là một quá trình và bạn đang đi đúng hướng khi sử dụng ứng dụng này để theo dõi chi tiêu."
        };
        
        Random random = new Random();
        return supportMessages[random.nextInt(supportMessages.length)];
    }

    private String getRandomResponse(String[] responses) {
        Random random = new Random();
        return responses[random.nextInt(responses.length)];
    }

    private String getSpendingAdvice(int userId) {
        try {
            // Kiểm tra xem có dữ liệu chi tiêu không
            Calendar cal = Calendar.getInstance();
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH) + 1;
            List<Transaction> transactions = transactionRepository.getTransactionsForMonth(userId, year, month);
            
            String[] advices = {
                "Tôi hiểu cảm giác chi tiêu nhiều có thể gây áp lực. Một cách giúp kiểm soát là lập danh sách ưu tiên trước khi mua sắm, phân biệt giữa 'muốn' và 'cần'.",
                "Chi tiêu nhiều đôi khi là điều không thể tránh khỏi, nhưng bạn có thể áp dụng quy tắc 24 giờ: Chờ ít nhất 24 giờ trước khi mua bất kỳ thứ gì không cần thiết.",
                "Nếu bạn cảm thấy đang tiêu nhiều, hãy thử phương pháp '30-ngày không mua sắm' cho các khoản không thiết yếu. Sau một tháng, bạn sẽ nhận ra những gì thực sự cần thiết.",
                "Lập ngân sách chi tiêu theo phương pháp phong bì có thể giúp kiểm soát chi tiêu. Phân bổ tiền mặt vào các 'phong bì' cho từng mục đích chi tiêu và chỉ dùng số tiền đó."
            };
            
            String advice = getRandomResponse(advices);
            
            // Nếu có dữ liệu chi tiêu, thêm phân tích
            if (!transactions.isEmpty()) {
                // Tính tổng chi tiêu
                double totalExpense = 0;
                for (Transaction t : transactions) {
                    if ("expense".equals(t.getType())) {
                        totalExpense += t.getAmount();
                    }
                }
                
                // Tìm danh mục chi tiêu nhiều nhất
                String topCategory = findTopExpenseCategory(transactions);
                
                if (!topCategory.isEmpty()) {
                    advice += "\n\nTôi nhận thấy danh mục '" + topCategory + "' chiếm tỷ lệ chi tiêu cao nhất của bạn trong tháng này, với tổng chi tiêu là " + 
                    currencyFormatter.format(totalExpense) + ". Hãy xem xét liệu bạn có thể cắt giảm một số chi phí trong danh mục này không.";
                }
            }
            
            return advice;
        } catch (Exception e) {
            // Trong trường hợp lỗi, trả về lời khuyên chung
            return "Khi cảm thấy mình tiêu quá nhiều, bạn nên dành thời gian xem lại các khoản chi và xác định đâu là chi phí thiết yếu, đâu là không cần thiết. Sau đó, tạo ngân sách thực tế và theo dõi chi tiêu hàng ngày bằng ứng dụng này để duy trì kiểm soát tài chính.";
        }
    }

    private String getBudgetStatus(int userId) {
        try {
            // Lấy ngân sách tháng hiện tại
            Budget budget = budgetRepository.getBudgetForCurrentMonth(userId);
            
            if (budget == null || budget.getLimit() <= 0) {
                return "Bạn chưa thiết lập ngân sách cho tháng này. Hãy thiết lập ngân sách để quản lý tài chính hiệu quả hơn.";
            }
            
            double percentUsed = budget.getPercentageUsed();
            double remainingAmount = budget.getRemainingAmount();
            
            StringBuilder response = new StringBuilder();
            response.append("Tình hình ngân sách tháng này của bạn:\n\n");
            response.append("- Tổng ngân sách: ").append(currencyFormatter.format(budget.getLimit())).append("\n");
            response.append("- Đã sử dụng: ").append(String.format("%.1f", percentUsed)).append("%\n");
            response.append("- Còn lại: ").append(currencyFormatter.format(remainingAmount)).append("\n\n");
            
            if (percentUsed > 90) {
                response.append("Bạn đã sử dụng gần hết ngân sách tháng này. Hãy cẩn thận với các khoản chi tiêu trong thời gian tới.");
            } else if (percentUsed > 70) {
                response.append("Bạn đã sử dụng một phần lớn ngân sách. Hãy xem xét các khoản chi tiêu không cần thiết.");
            } else {
                response.append("Bạn đang quản lý ngân sách rất tốt. Hãy tiếp tục duy trì!");
            }
            
            return response.toString();
        } catch (Exception e) {
            return "Xin lỗi, tôi gặp sự cố khi kiểm tra ngân sách của bạn. Vui lòng thử lại sau.";
        }
    }

    private String getFinancialSummary(int userId) {
        try {
            // Lấy ngân sách và giao dịch
            Budget budget = budgetRepository.getBudgetForCurrentMonth(userId);
            Calendar cal = Calendar.getInstance();
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH) + 1;
            List<Transaction> transactions = transactionRepository.getTransactionsForMonth(userId, year, month);
            
            if (transactions.isEmpty()) {
                return "Tôi không tìm thấy dữ liệu giao dịch nào trong tháng này để tạo báo cáo.";
            }
            
            // Tính tổng thu nhập và chi tiêu
            double totalIncome = 0;
            double totalExpense = 0;
            for (Transaction t : transactions) {
                if ("income".equals(t.getType())) {
                    totalIncome += t.getAmount();
                } else if ("expense".equals(t.getType())) {
                    totalExpense += t.getAmount();
                }
            }
            
            // Tạo báo cáo
            StringBuilder report = new StringBuilder();
            report.append("📊 BÁO CÁO TÀI CHÍNH THÁNG ").append(month).append("/").append(year).append("\n\n");
            report.append("💰 Tổng thu nhập: ").append(currencyFormatter.format(totalIncome)).append("\n");
            report.append("💸 Tổng chi tiêu: ").append(currencyFormatter.format(totalExpense)).append("\n");
            
            double balance = totalIncome - totalExpense;
            report.append("🏦 Số dư: ").append(currencyFormatter.format(balance)).append("\n\n");
            
            if (budget != null && budget.getLimit() > 0) {
                double percentUsed = budget.getPercentageUsed();
                report.append("📝 Ngân sách: ").append(currencyFormatter.format(budget.getLimit())).append("\n");
                report.append("📌 Đã sử dụng: ").append(String.format("%.1f", percentUsed)).append("% ngân sách\n\n");
            }
            
            if (balance < 0) {
                report.append("⚠️ Lưu ý: Chi tiêu của bạn đang vượt quá thu nhập. Hãy xem xét cắt giảm các khoản chi tiêu không cần thiết.");
            } else if (balance > 0) {
                report.append("✅ Tuyệt vời! Bạn đang có số dư dương. Hãy cân nhắc đưa một phần vào tiết kiệm hoặc đầu tư.");
            } else {
                report.append("⚖️ Thu chi của bạn đang cân bằng. Hãy cố gắng tăng thu nhập hoặc giảm chi phí để có thêm tiền tiết kiệm.");
            }
            
            return report.toString();
        } catch (Exception e) {
            return "Xin lỗi, tôi gặp sự cố khi tạo báo cáo tài chính. Vui lòng thử lại sau.";
        }
    }

    private String findTopExpenseCategory(List<Transaction> transactions) {
        try {
            // Tạo map để lưu tổng chi tiêu theo danh mục
            java.util.Map<String, Double> categoryExpenses = new java.util.HashMap<>();
            
            // Tính tổng chi tiêu cho mỗi danh mục
            for (Transaction t : transactions) {
                if ("expense".equals(t.getType()) && t.getCategoryName() != null && !t.getCategoryName().isEmpty()) {
                    double currentAmount = categoryExpenses.getOrDefault(t.getCategoryName(), 0.0);
                    categoryExpenses.put(t.getCategoryName(), currentAmount + t.getAmount());
                }
            }
            
            // Tìm danh mục có chi tiêu cao nhất
            String topCategory = "";
            double maxExpense = 0;
            
            for (java.util.Map.Entry<String, Double> entry : categoryExpenses.entrySet()) {
                if (entry.getValue() > maxExpense) {
                    maxExpense = entry.getValue();
                    topCategory = entry.getKey();
                }
            }
            
            return topCategory;
        } catch (Exception e) {
            return "";
        }
    }

    private String getSavingAdvice() {
        String[] advices = {
            "Một mẹo tiết kiệm hiệu quả là quy tắc 24 giờ: Khi muốn mua một thứ gì đó không cần thiết, hãy chờ 24 giờ trước khi quyết định. Điều này giúp tránh mua sắm bốc đồng.",
            "Bạn có thể áp dụng phương pháp tiết kiệm 50/30/20: 50% thu nhập cho nhu cầu thiết yếu, 30% cho nhu cầu cá nhân, và 20% để tiết kiệm.",
            "Một cách tiết kiệm hiệu quả là tự nấu ăn tại nhà thay vì đi ăn ngoài. Bạn có thể tiết kiệm được từ 20-30% chi phí ăn uống.",
            "Hãy đặt ra mục tiêu tiết kiệm cụ thể và thực tế. Đặt mục tiêu tiết kiệm 10-20% thu nhập mỗi tháng là khởi đầu tốt.",
            "Tạo một quỹ khẩn cấp tương đương 3-6 tháng chi tiêu. Điều này sẽ giúp bạn không phải lo lắng khi có sự cố tài chính bất ngờ.",
            "Theo dõi chi tiêu bằng ứng dụng này sẽ giúp bạn nhận ra các khoản chi không cần thiết và có thể cắt giảm để tiết kiệm hơn."
        };
        
        Random random = new Random();
        return advices[random.nextInt(advices.length)];
    }

    private String getRandomFinancialTip() {
        Random random = new Random();
        return financialTips.get(random.nextInt(financialTips.size()));
    }

    private boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            if (source.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAnyPattern(String input, String[] patterns) {
        for (String pattern : patterns) {
            if (input.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private int calculateReplyDelay(String reply) {
        // Tính độ trễ dựa trên độ dài của tin nhắn
        // 1000ms cơ bản + 10ms cho mỗi ký tự
        int baseDelay = 1000;
        int charDelay = Math.min(reply.length() * 10, 3000); // Tối đa 3 giây
        return baseDelay + charDelay;
    }

    /**
     * Cung cấp giải pháp chi tiết cho vấn đề chi tiêu quá nhiều
     */
    private String getDetailedSpendingSolution(int userId) {
        StringBuilder solution = new StringBuilder();
        solution.append("Để giải quyết vấn đề chi tiêu quá nhiều, bạn có thể thực hiện các bước sau:\n\n");
        solution.append("1️⃣ Theo dõi chi tiêu: Ghi lại tất cả các khoản chi tiêu của bạn trong ít nhất 2 tuần. Ứng dụng này có thể giúp bạn làm điều đó dễ dàng.\n\n");
        solution.append("2️⃣ Phân loại chi tiêu: Chia chi tiêu thành 'cần thiết' (ví dụ: thực phẩm, tiền thuê nhà) và 'không cần thiết' (ví dụ: ăn ngoài, giải trí).\n\n");
        solution.append("3️⃣ Tạo ngân sách: Thiết lập ngân sách hàng tháng cho mỗi danh mục và cố gắng tuân thủ.\n\n");
        solution.append("4️⃣ Cắt giảm chi tiêu không cần thiết: Xác định các lĩnh vực bạn có thể cắt giảm, như ăn uống bên ngoài ít hơn, hạn chế mua sắm không cần thiết.\n\n");
        solution.append("5️⃣ Áp dụng quy tắc 24 giờ: Trước khi mua bất cứ thứ gì không cần thiết, hãy đợi 24 giờ để xem bạn có thực sự cần nó không.\n\n");
        solution.append("6️⃣ Tạo mục tiêu tài chính: Đặt mục tiêu cụ thể để tiết kiệm tiền và theo dõi tiến độ của bạn.\n\n");
        
        // Nếu có dữ liệu chi tiêu, thêm phân tích cụ thể
        try {
            Calendar cal = Calendar.getInstance();
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH) + 1;
            List<Transaction> transactions = transactionRepository.getTransactionsForMonth(userId, year, month);
            
            if (!transactions.isEmpty()) {
                // Tìm danh mục chi tiêu nhiều nhất
                String topCategory = findTopExpenseCategory(transactions);
                
                if (!topCategory.isEmpty()) {
                    solution.append("Theo dữ liệu của bạn, danh mục '").append(topCategory).append("' có mức chi tiêu cao nhất. ");
                    solution.append("Bạn có thể xem xét các cách để giảm chi phí trong danh mục này, ví dụ:\n\n");
                    
                    if (topCategory.toLowerCase().contains("ăn uống") || topCategory.toLowerCase().contains("an uong") || topCategory.toLowerCase().contains("food")) {
                        solution.append("• Lập kế hoạch bữa ăn trước và nấu ăn tại nhà nhiều hơn\n");
                        solution.append("• Mang theo đồ ăn trưa thay vì ăn ngoài\n");
                        solution.append("• Giảm số lần ăn tại nhà hàng hoặc đặt đồ ăn\n");
                    } else if (topCategory.toLowerCase().contains("mua sắm") || topCategory.toLowerCase().contains("shopping")) {
                        solution.append("• Lập danh sách mua sắm và chỉ mua những gì cần thiết\n");
                        solution.append("• Tìm kiếm khuyến mãi, giảm giá trước khi mua\n");
                        solution.append("• Xem xét mua đồ đã qua sử dụng có chất lượng tốt\n");
                    } else if (topCategory.toLowerCase().contains("giải trí") || topCategory.toLowerCase().contains("entertainment")) {
                        solution.append("• Tìm kiếm các hoạt động giải trí miễn phí hoặc chi phí thấp\n");
                        solution.append("• Tận dụng ưu đãi và khuyến mãi cho các sự kiện\n");
                        solution.append("• Hạn chế số lần đi chơi đắt tiền mỗi tháng\n");
                    } else {
                        solution.append("• Nghiên cứu các lựa chọn rẻ hơn nhưng vẫn đáp ứng nhu cầu\n");
                        solution.append("• Xác định đâu là chi tiêu cần thiết và đâu là có thể cắt giảm\n");
                        solution.append("• Lên kế hoạch chi tiêu trước để tránh mua sắm bốc đồng\n");
                    }
                }
            }
        } catch (Exception e) {
            // Bỏ qua lỗi nếu có
        }
        
        return solution.toString();
    }

    /**
     * Cung cấp giải pháp chi tiết cho vấn đề ngân sách
     */
    private String getDetailedBudgetSolution(int userId) {
        StringBuilder solution = new StringBuilder();
        solution.append("Để quản lý ngân sách hiệu quả hơn, đây là các bước cụ thể bạn có thể thực hiện:\n\n");
        solution.append("1️⃣ Đánh giá tình hình hiện tại: Xem xét thu nhập và chi tiêu hiện tại của bạn để hiểu được bức tranh tài chính tổng thể.\n\n");
        solution.append("2️⃣ Thiết lập ngân sách thực tế: Tạo ngân sách dựa trên thu nhập thực tế của bạn, theo tỷ lệ 50/30/20:\n");
        solution.append("   • 50% cho nhu cầu thiết yếu (nhà ở, thực phẩm, hóa đơn)\n");
        solution.append("   • 30% cho mong muốn (giải trí, mua sắm, ăn ngoài)\n");
        solution.append("   • 20% cho tiết kiệm và trả nợ\n\n");
        solution.append("3️⃣ Theo dõi chi tiêu hàng ngày: Sử dụng ứng dụng này để ghi lại mọi khoản chi tiêu, dù nhỏ.\n\n");
        solution.append("4️⃣ Đánh giá định kỳ: Vào cuối mỗi tuần, kiểm tra xem bạn có tuân thủ ngân sách không và điều chỉnh nếu cần.\n\n");
        solution.append("5️⃣ Tạo quỹ khẩn cấp: Dành một phần ngân sách để xây dựng quỹ khẩn cấp tương đương 3-6 tháng chi tiêu.\n\n");
        
        // Nếu có dữ liệu ngân sách, thêm phân tích cụ thể
        try {
            Budget budget = budgetRepository.getBudgetForCurrentMonth(userId);
            
            if (budget != null && budget.getLimit() > 0) {
                double percentUsed = budget.getPercentageUsed();
                double remainingAmount = budget.getRemainingAmount();
                
                solution.append("Dựa vào dữ liệu ngân sách của bạn:\n\n");
                
                if (percentUsed > 90) {
                    solution.append("Bạn đã sử dụng ").append(String.format("%.1f", percentUsed)).append("% ngân sách tháng này, chỉ còn lại ").append(currencyFormatter.format(remainingAmount)).append(". Để cải thiện:\n\n");
                    solution.append("• Xác định các chi tiêu khẩn cấp còn lại trong tháng và ưu tiên chúng\n");
                    solution.append("• Tạm hoãn mọi chi tiêu không cần thiết đến tháng sau\n");
                    solution.append("• Xem xét tăng ngân sách tháng sau nếu ngân sách hiện tại không thực tế\n");
                } else if (percentUsed > 70) {
                    solution.append("Bạn đã sử dụng ").append(String.format("%.1f", percentUsed)).append("% ngân sách tháng này, còn lại ").append(currencyFormatter.format(remainingAmount)).append(". Để quản lý tốt hơn:\n\n");
                    solution.append("• Lên kế hoạch chi tiêu cẩn thận cho phần còn lại của tháng\n");
                    solution.append("• Ưu tiên các chi phí thiết yếu trước\n");
                    solution.append("• Hạn chế các khoản chi không cần thiết\n");
                } else {
                    solution.append("Bạn đã sử dụng ").append(String.format("%.1f", percentUsed)).append("% ngân sách tháng này, còn lại ").append(currencyFormatter.format(remainingAmount)).append(". Bạn đang quản lý tốt! Để tiếp tục:\n\n");
                    solution.append("• Duy trì thói quen chi tiêu hiện tại\n");
                    solution.append("• Xem xét chuyển một phần tiền còn dư vào tài khoản tiết kiệm\n");
                    solution.append("• Theo dõi các mục chi tiêu để xác định những điểm có thể tối ưu hóa thêm\n");
                }
            }
        } catch (Exception e) {
            // Bỏ qua lỗi nếu có
        }
        
        return solution.toString();
    }

    /**
     * Cung cấp giải pháp chi tiết cho vấn đề tiết kiệm
     */
    private String getDetailedSavingSolution() {
        StringBuilder solution = new StringBuilder();
        solution.append("Để tiết kiệm tiền hiệu quả, hãy thực hiện kế hoạch cụ thể sau:\n\n");
        solution.append("1️⃣ Xác định mục tiêu tiết kiệm: Đặt mục tiêu cụ thể và thực tế (ví dụ: tiết kiệm 20% thu nhập mỗi tháng hoặc 30 triệu đồng trong 1 năm).\n\n");
        solution.append("2️⃣ Tạo ngân sách tiết kiệm: Áp dụng nguyên tắc 'Trả cho bản thân trước' - nghĩa là để riêng tiền tiết kiệm ngay khi nhận lương.\n\n");
        solution.append("3️⃣ Tự động hóa tiết kiệm: Thiết lập chuyển khoản tự động từ tài khoản chính sang tài khoản tiết kiệm vào ngày nhận lương.\n\n");
        solution.append("4️⃣ Cắt giảm chi tiêu không cần thiết:\n");
        solution.append("   • Giảm chi phí ăn uống bên ngoài bằng cách nấu ăn tại nhà nhiều hơn\n");
        solution.append("   • Hủy các dịch vụ đăng ký bạn ít sử dụng (Netflix, Spotify, v.v.)\n");
        solution.append("   • Đợi khuyến mãi trước khi mua sắm\n");
        solution.append("   • Sử dụng phương tiện công cộng hoặc đi bộ thay vì taxi/xe ôm\n\n");
        solution.append("5️⃣ Tăng thu nhập:\n");
        solution.append("   • Tìm công việc bán thời gian hoặc freelance\n");
        solution.append("   • Bán những đồ không cần thiết trong nhà\n");
        solution.append("   • Đầu tư một phần tiền tiết kiệm vào các kênh an toàn\n\n");
        solution.append("6️⃣ Theo dõi tiến độ: Kiểm tra tài khoản tiết kiệm của bạn hàng tháng để xem bạn đã đạt được bao nhiêu % mục tiêu.\n\n");
        solution.append("7️⃣ Áp dụng quy tắc 30 ngày: Chờ 30 ngày trước khi mua bất kỳ thứ gì không cần thiết. Sau 30 ngày, nếu bạn vẫn muốn nó, hãy xem xét việc mua.\n\n");
        solution.append("8️⃣ Tạo quỹ khẩn cấp: Dành ít nhất 3-6 tháng chi tiêu vào quỹ khẩn cấp trước khi tiết kiệm cho các mục tiêu khác.\n\n");
        solution.append("Nhớ rằng, tiết kiệm là một cuộc chạy đường dài, không phải chạy nước rút. Hãy kiên nhẫn và nhất quán với kế hoạch của bạn.");
        
        return solution.toString();
    }

    public interface ChatCallback {
        void onMessageReceived(ChatMessage message);
    }
} 