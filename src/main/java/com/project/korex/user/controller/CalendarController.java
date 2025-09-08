package com.project.korex.user.controller;

import com.project.korex.common.security.user.CustomUserPrincipal;
import com.project.korex.user.dto.CalendarEventDto;
import com.project.korex.user.entity.Users;
import com.project.korex.user.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/user/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService service;

    @GetMapping
    public ResponseEntity<List<CalendarEventDto>> getAllEvents(@AuthenticationPrincipal CustomUserPrincipal customUserPrincipal) {
        Users user = customUserPrincipal.getUser();
        return ResponseEntity.ok(service.getEventsByUser(user));
    }

    @GetMapping("/range")
    public ResponseEntity<List<CalendarEventDto>> getEventsByRange(
            @AuthenticationPrincipal CustomUserPrincipal customUserPrincipal,
            @RequestParam String start, @RequestParam String end) {
        Users user = customUserPrincipal.getUser();
        LocalDate startDate = LocalDate.parse(start);
        LocalDate endDate = LocalDate.parse(end);
        List<CalendarEventDto> events = service.getEventsByUserAndDateRange(user, startDate, endDate);
        return ResponseEntity.ok(events);
    }

    @PostMapping
    public ResponseEntity<CalendarEventDto> addEvent(@AuthenticationPrincipal CustomUserPrincipal customUserPrincipal,
                                                     @RequestBody CalendarEventDto dto) {
        Users user = customUserPrincipal.getUser();
        return ResponseEntity.ok(service.addEvent(dto, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@AuthenticationPrincipal CustomUserPrincipal customUserPrincipal,
                                            @PathVariable Long id) {
        Users user = customUserPrincipal.getUser();
        service.deleteEvent(id, user);
        return ResponseEntity.noContent().build();
    }
}
