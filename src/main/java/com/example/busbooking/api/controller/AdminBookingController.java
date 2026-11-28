package com.example.busbooking.api.controller;

import com.example.busbooking.api.dto.PageResponse;
import com.example.busbooking.api.dto.booking.BookingResponse;
import com.example.busbooking.api.mapper.BookingDtoMapper;
import com.example.busbooking.business.service.BookingService;
import com.example.busbooking.domain.enums.BookingStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/bookings")
@SecurityRequirement(name = "bearerAuth")
public class AdminBookingController {
    private final BookingService service;
    private final BookingDtoMapper mapper;

    public AdminBookingController(BookingService service, BookingDtoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "List bookings across all users with optional filters")
    public PageResponse<BookingResponse> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long tripId,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit) {
        return PageResponse.from(service.listForAdmin(userId, tripId, status, PageResponse.request(page, limit))
                .map(mapper::toResponse));
    }
}
