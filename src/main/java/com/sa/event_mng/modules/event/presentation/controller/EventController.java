package com.sa.event_mng.modules.event.presentation.controller;

import com.sa.event_mng.modules.event.application.dto.request.EventFilterRequest;
import com.sa.event_mng.modules.event.application.dto.request.EventRequest;
import com.sa.event_mng.modules.event.application.dto.response.EventResponse;
import com.sa.event_mng.modules.event.application.service.EventService;
import com.sa.event_mng.modules.event.domain.model.EventStatus;
import com.sa.event_mng.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Sự kiện", description = "Quản lý và xem danh sách sự kiện")
@SecurityRequirement(name = "bearerAuth")
public class EventController {

        EventService eventService;

        @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @Operation(summary = "Tạo sự kiện mới (ORGANIZER)")
        public ApiResponse<EventResponse> create(@ModelAttribute @Valid EventRequest request) {
                return ApiResponse.<EventResponse>builder()
                                .result(eventService.create(request))
                                .build();
        }

        @GetMapping("/search")
        @Operation(summary = "Lấy danh sách sự kiện đã đăng / Tìm kiếm sự kiện")
        public ApiResponse<Page<EventResponse>> getAllPublished(@ModelAttribute EventFilterRequest filter) {
                PageRequest pageRequest = PageRequest.of(
                                filter.getPage() - 1, filter.getSize(),
                                Sort.by("createdAt").descending());
                return ApiResponse.<Page<EventResponse>>builder()
                                .result(eventService.getAllPublished(filter, pageRequest))
                                .build();
        }

        @GetMapping("/{id}")
        @Operation(summary = "Xem thông tin chi tiết sự kiện")
        public ApiResponse<EventResponse> getById(@PathVariable Long id) {
                return ApiResponse.<EventResponse>builder()
                                .result(eventService.getById(id))
                                .build();
        }

        @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @Operation(summary = "Cập nhật sự kiện (Chủ sự kiện/ADMIN)")
        public ApiResponse<EventResponse> update(@PathVariable Long id, @ModelAttribute @Valid EventRequest request) {
                return ApiResponse.<EventResponse>builder()
                                .result(eventService.update(id, request))
                                .build();
        }

        @GetMapping("/admin/all")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Lấy tất cả sự kiện (ADMIN)")
        public ApiResponse<Page<EventResponse>> getAllForAdmin(
                        @RequestParam(defaultValue = "1") int page,
                        @RequestParam(defaultValue = "12") int size,
                        @RequestParam(defaultValue = "") String search,
                        @RequestParam(required = false) String status) {
                PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
                return ApiResponse.<Page<EventResponse>>builder()
                                .result(eventService.getAllForAdmin(search, status, pageRequest))
                                .build();
        }

        @PatchMapping("/{id}/status")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Cập nhật trạng thái sự kiện (ADMIN)")
        public ApiResponse<EventResponse> updateStatus(
                        @PathVariable Long id,
                        @RequestParam EventStatus status) {
                return ApiResponse.<EventResponse>builder()
                                .result(eventService.updateStatus(id, status))
                                .build();
        }

        @GetMapping("/organizer/my-events")
        @PreAuthorize("hasRole('ORGANIZER')")
        @Operation(summary = "Lấy danh sách sự kiện cá nhân (ORGANIZER)")
        public ApiResponse<Page<EventResponse>> getMyEvents(
                        @RequestParam(defaultValue = "1") int page,
                        @RequestParam(defaultValue = "10") int size) {
                PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
                return ApiResponse.<Page<EventResponse>>builder()
                                .result(eventService.getMyEvents(pageRequest))
                                .build();
        }

//        @GetMapping("/organizer/stats")
//        @PreAuthorize("hasRole('ORGANIZER')")
//        @Operation(summary = "Lấy thống kê doanh thu (ORGANIZER)")
//        public ApiResponse<OrganizerStatsResponse> getStats() {
//                return ApiResponse.<OrganizerStatsResponse>builder()
//                                .result(eventService.getOrganizerStats())
//                                .build();
//        }

//        @GetMapping("/blog-news")
//        @Operation(summary = "Lấy dữ liệu đồng bộ cho trang Blog tin tức")
//        public ApiResponse<Page<com.sa.event_mng.modules.event.application.dto.response.BlogEventResponse>> getBlogNews(
//                @RequestParam(defaultValue = "0") int page,
//                @RequestParam(defaultValue = "10") int size
//        ) {
//                return ApiResponse.<Page<com.sa.event_mng.modules.event.application.dto.response.BlogEventResponse>>builder()
//                                .result(eventService.getBlogNews(page, size))
//                                .build();
//        }
}
