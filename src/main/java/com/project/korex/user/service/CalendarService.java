package com.project.korex.user.service;

import com.project.korex.user.dto.CalendarEventDto;
import com.project.korex.user.entity.CalendarEvent;
import com.project.korex.user.entity.Users;
import com.project.korex.user.repository.jpa.CalendarEventJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final CalendarEventJpaRepository repository;

    public List<CalendarEventDto> getEventsByUser(Users user) {
        return repository.findByUser(user)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<CalendarEventDto> getEventsByUserAndDateRange(Users user, LocalDate startDate, LocalDate endDate) {
        return repository.findByUserAndDateBetween(user, startDate, endDate)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public CalendarEventDto addEvent(CalendarEventDto dto, Users user) {
        CalendarEvent event = CalendarEvent.builder()
                .user(user)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .date(dto.getDate())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .build();
        CalendarEvent saved = repository.save(event);
        return toDto(saved);
    }

    public void deleteEvent(Long id, Users user) {
        CalendarEvent event = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        if (!event.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("No permission to delete this event");
        }
        repository.delete(event);
    }

    private CalendarEventDto toDto(CalendarEvent event) {
        return CalendarEventDto.builder()
                .id(event.getId())
                .userId(event.getUser().getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .date(event.getDate())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .build();
    }
}