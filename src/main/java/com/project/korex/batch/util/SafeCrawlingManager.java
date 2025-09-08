package com.project.korex.batch.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class SafeCrawlingManager {

    private final AtomicInteger requestCount = new AtomicInteger(0);
    private long sessionStartTime = System.currentTimeMillis();

    @Value("${batch.safety.max-requests-per-hour:30}")
    private int maxRequestsPerHour;

    public boolean canMakeRequest() {
        long currentTime = System.currentTimeMillis();

        // 1시간마다 카운터 리셋
        if (currentTime - sessionStartTime > 3600000) {
            requestCount.set(0);
            sessionStartTime = currentTime;
            log.info("요청 카운터 리셋 - 새로운 시간 윈도우 시작");
        }

        // 시간당 요청 수 제한
        if (requestCount.get() >= maxRequestsPerHour) {
            log.warn("시간당 요청 한도 초과: {}/{}", requestCount.get(), maxRequestsPerHour);
            return false;
        }

        return true;
    }

    public void recordRequest() {
        int count = requestCount.incrementAndGet();
        log.debug("요청 기록: {}/{}", count, maxRequestsPerHour);
    }

    public long getRecommendedDelay() {
        int currentRequests = requestCount.get();

        // 요청 횟수에 따른 동적 지연 (안전한 시간으로 설정)
        if (currentRequests < 5) {
            return 8000L;   // 8초
        } else if (currentRequests < 15) {
            return 12000L;  // 12초
        } else {
            return 20000L;  // 20초
        }
    }
}
