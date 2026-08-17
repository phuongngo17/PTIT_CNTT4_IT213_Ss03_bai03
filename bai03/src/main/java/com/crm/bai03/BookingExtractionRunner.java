package com.crm.bai03;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Runner thực thi demo trích xuất thông tin đặt phòng khi khởi động ứng dụng.
 *
 * Email mẫu sử dụng trong Bài 3 — Edge Case: thông tin mâu thuẫn trong cùng một email.
 */
@Component
public class BookingExtractionRunner implements CommandLineRunner {

    private final BookingExtractionService service;

    public BookingExtractionRunner(BookingExtractionService service) {
        this.service = service;
    }

    // Email mẫu đề bài yêu cầu
    private static final String SAMPLE_EMAIL = """
            Chào lễ tân, tôi tên là Minh. Tôi định đặt phòng Suite cho 3 ngày bắt đầu từ ngày mai.
            À mà không, mai tôi bận đột xuất nên cho tôi check-in lùi lại 1 ngày nhé,
            và tôi rút ngắn chuyến đi xuống còn 2 ngày thôi. Có gì liên hệ lại tôi.
            """;

    @Override
    public void run(String... args) {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("  BÀI 3: Tối ưu Prompt — Email đặt phòng mâu thuẫn (Edge Case)");
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println();
        System.out.println("── Email đầu vào ──────────────────────────────────────────────");
        System.out.println(SAMPLE_EMAIL);

        System.out.println("── Đang gọi LLM để trích xuất thông tin đặt phòng... ──────────");
        BookingExtraction result = service.extractBooking(SAMPLE_EMAIL);

        System.out.println();
        System.out.println("── Kết quả trích xuất ─────────────────────────────────────────");
        System.out.printf("  guestName      : %s%n", result.guestName());
        System.out.printf("  checkInDate    : %s%n", result.checkInDate());
        System.out.printf("  durationNights : %d%n", result.durationNights());
        System.out.printf("  roomType       : %s%n", result.roomType());
        System.out.println();

        System.out.println("── Đối chiếu Expected vs Actual ───────────────────────────────");
        printVerification("guestName",      "Minh",       result.guestName());
        printVerification("checkInDate",    "18/07/2026", result.checkInDate());
        printVerification("durationNights", "2",          String.valueOf(result.durationNights()));
        printVerification("roomType",       "Suite",      result.roomType());
        System.out.println("═══════════════════════════════════════════════════════════════");
    }

    private void printVerification(String field, String expected, String actual) {
        boolean pass = expected.equals(actual);
        System.out.printf("  %-18s | Expected: %-12s | Actual: %-12s | %s%n",
                field, expected, actual, pass ? "✅ PASS" : "❌ FAIL");
    }
}
