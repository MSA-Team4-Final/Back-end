package com.project.korex.user.repository.jpa;


import com.project.korex.user.entity.CalendarEvent;
import com.project.korex.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CalendarEventJpaRepository extends JpaRepository<CalendarEvent, Long> {

    List<CalendarEvent> findByUserAndDate(Users user, LocalDate date);
    List<CalendarEvent> findByUser(Users user);
    List<CalendarEvent> findByUserAndDateBetween(Users user, LocalDate startDate, LocalDate endDate);
}
