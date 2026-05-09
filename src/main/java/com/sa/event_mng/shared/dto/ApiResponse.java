package com.sa.event_mng.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
  // Mã trả về chung, có thể dùng để phân biệt success/error.
  @Builder.Default int code = 1000;
  // Thông điệp mô tả kết quả hoặc lỗi.
  String message;
  // Dữ liệu trả về thực tế.
  T result;
}
