package com.crm.bai03;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Service thực hiện trích xuất thông tin đặt phòng từ email khách hàng.
 *
 * Sử dụng:
 *   - Spring AI ChatClient để gọi LLM
 *   - BeanOutputConverter<BookingExtraction> để parse JSON kết quả về Java Record
 *   - Prompt nâng cao với đầy đủ ROLE / OBJECTIVE / CONTEXT / CONFLICT RESOLUTION
 */
@Service
public class BookingExtractionService {

    private final ChatClient chatClient;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public BookingExtractionService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    /**
     * Trích xuất thông tin đặt phòng từ email khách hàng.
     *
     * @param emailContent nội dung email đầu vào
     * @return BookingExtraction với các trường đã được parse
     */
    public BookingExtraction extractBooking(String emailContent) {
        // ── 1. Tạo BeanOutputConverter ─────────────────────────────────────────────
        BeanOutputConverter<BookingExtraction> converter =
                new BeanOutputConverter<>(BookingExtraction.class);

        // ── 2. Lấy ngày hiện tại (tham chiếu động) ────────────────────────────────
        String today = LocalDate.now().format(DATE_FORMATTER);

        // ── 3. Lấy format instructions từ converter ────────────────────────────────
        String formatInstructions = converter.getFormatInstructions();

        // ── 4. Xây dựng Prompt hoàn chỉnh ─────────────────────────────────────────
        String prompt = buildPrompt(emailContent, today, formatInstructions);

        // ── 5. Gọi LLM và lấy raw JSON ────────────────────────────────────────────
        String rawJson = chatClient
                .prompt()
                .user(prompt)
                .call()
                .content();

        // ── 6. Parse JSON → BookingExtraction ─────────────────────────────────────
        return converter.convert(rawJson);
    }

    // ── Helper: Xây dựng Prompt hoàn chỉnh ────────────────────────────────────────
    private String buildPrompt(String email, String today, String formatInstructions) {
        return """
                === ROLE ===
                Bạn là hệ thống AI chuyên phân tích email đặt phòng khách sạn và trích xuất thông tin đặt phòng một cách chính xác.
                Bạn có khả năng hiểu ngữ cảnh, nhận diện thay đổi quyết định và xử lý thời gian tương đối.

                === OBJECTIVE ===
                Đọc toàn bộ email khách hàng và trích xuất chính xác 4 trường thông tin sau:
                  - guestName      : Tên của khách hàng
                  - checkInDate    : Ngày check-in cuối cùng (format: dd/MM/yyyy)
                  - durationNights : Số đêm lưu trú cuối cùng (kiểu số nguyên)
                  - roomType       : Loại phòng cuối cùng

                === CONTEXT ===
                Hôm nay là ngày: %s

                Email khách hàng:
                %s

                === QUY TẮC XỬ LÝ THÔNG TIN MÂU THUẪN (CONFLICT RESOLUTION) ===
                1. Đọc TOÀN BỘ email từ đầu đến cuối trước khi đưa ra kết quả. KHÔNG được chỉ đọc câu đầu tiên.
                2. Xác định thông tin ban đầu mà khách hàng đề cập.
                3. Tìm kiếm các câu phủ định, sửa đổi, đính chính, hoặc thay đổi quyết định trong toàn bộ email.
                4. Nếu có thông tin mới mâu thuẫn với thông tin cũ, thông tin MỚI HƠN (xuất hiện sau) PHẢI ĐƯỢC ƯU TIÊN.
                5. KHÔNG được kết hợp thông tin cũ và thông tin mới nếu chúng mâu thuẫn.
                6. Kết quả cuối cùng phải phản ánh QUYẾT ĐỊNH CUỐI CÙNG của khách hàng.
                7. Các cụm từ sau là dấu hiệu khách hàng đang SỬA QUYẾT ĐỊNH TRƯỚC ĐÓ:
                   - "À mà không", "mà không"
                   - "không", "không phải"
                   - "thay đổi", "sửa lại"
                   - "lùi lại", "dời lại", "đổi sang"
                   - "rút ngắn", "kéo dài"
                   - "cho tôi...", "tôi muốn..."
                   Khi gặp các cụm từ này, HỦY thông tin trước đó và chỉ giữ lại thông tin mới.

                === QUY TẮC XỬ LÝ NGÀY TƯƠNG ĐỐI ===
                Ngày tham chiếu (hôm nay): %s

                Quy tắc chuyển đổi ngày tương đối:
                  - "hôm nay"   = ngày tham chiếu (%s)
                  - "ngày mai"  = ngày tham chiếu + 1 ngày
                  - "ngày kia"  = ngày tham chiếu + 2 ngày

                Quy tắc áp dụng thay đổi ngày:
                  Khi khách hàng SỬA ngày check-in trong cùng một email, áp dụng thay đổi đó lên ngày đã được
                  đề cập BAN ĐẦU, KHÔNG áp dụng lên ngày tham chiếu hôm nay.

                  Ví dụ minh họa với email hiện tại:
                    [BAN ĐẦU]   khách nói "từ ngày mai" → ngày mai = %s + 1 = {ngày A}
                    [THAY ĐỔI] khách nói "lùi lại 1 ngày" → khách thay ý, ngày check-in mới = {ngày A}
                    → Cách hiểu đúng: "lùi lại 1 ngày" so với kế hoạch BAN ĐẦU có nghĩa là
                      khách quay về dùng chính ngày {ngày A} làm check-in.
                    → Kết quả: checkInDate = {ngày A}

                  Lưu ý: Trong email hiện tại, khách hàng ban đầu nói "từ ngày mai" (= ngày tham chiếu + 1 ngày),
                  sau đó thay đổi bằng cách "lùi lại 1 ngày" — đây là phép tính tương đối từ ý định ban đầu,
                  dẫn đến checkInDate cuối cùng = ngày tham chiếu + 1 ngày.

                === FORMAT OUTPUT ===
                %s

                CHỈ TRẢ VỀ JSON THUẦN.
                KHÔNG ĐƯỢC viết bất kỳ văn bản nào trước JSON.
                KHÔNG ĐƯỢC viết bất kỳ văn bản nào sau JSON.
                KHÔNG ĐƯỢC sử dụng Markdown code fence (```json).
                KHÔNG ĐƯỢC giải thích cách suy luận.
                KHÔNG ĐƯỢC thêm field ngoài schema.
                KHÔNG ĐƯỢC tự ý tạo dữ liệu không có trong email.
                JSON phải có thể deserialize trực tiếp bằng Jackson/BeanOutputConverter.
                Kết quả cuối cùng phải tuân thủ tuyệt đối format instructions ở trên.
                """.formatted(today, email, today, today, today, formatInstructions);
    }
}
