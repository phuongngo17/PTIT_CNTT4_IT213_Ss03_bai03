package com.crm.bai03;

/**
 * Java Record dùng để nhận kết quả trích xuất thông tin đặt phòng từ LLM.
 * Được sử dụng kết hợp với BeanOutputConverter<BookingExtraction> của Spring AI.
 *
 * Fields:
 *   guestName      - Tên khách hàng
 *   checkInDate    - Ngày check-in (format dd/MM/yyyy)
 *   durationNights - Số đêm lưu trú
 *   roomType       - Loại phòng
 */
public record BookingExtraction(
        String guestName,
        String checkInDate,
        int durationNights,
        String roomType
) {}
