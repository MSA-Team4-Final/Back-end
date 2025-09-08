package com.project.korex.user.entity;

import com.project.korex.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    // 일정명
    @Column(nullable = false, length = 100)
    private String title;

    // 일정 설명
    @Column(length = 500)
    private String description;

    // 일정 날짜
    @Column(nullable = false)
    private LocalDate date;

    // 시작 시간
    private LocalTime startTime;

    // 종료 시간
    private LocalTime endTime;
}

