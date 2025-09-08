package com.project.korex.ai.service;

import com.project.korex.ai.dto.ChatbotRequestDto;
import com.project.korex.ai.dto.ChatbotResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ChatbotService {

    // 1. WebClient를 서비스 클래스 내에서 직접 생성합니다.
    // 인스턴스를 재사용하기 위해 static final로 선언하는 것이 효율적입니다.
    private static final WebClient webClient = WebClient.create();

    // 2. 호출할 API의 전체 URL을 상수로 정의합니다.
    private static final String FASTAPI_URL = "http://127.0.0.1:8000/api/chatbot";

    /**
     * WebClient를 사용해 FastAPI 챗봇 API를 비동기적으로 호출합니다.
     * @param userPrompt 사용자가 입력한 메시지
     * @param sessionId 대화 연속성을 위한 세션 ID
     * @return 챗봇 응답을 담은 Mono<String> 객체
     */
    public Mono<String> getChatbotResponse(String userPrompt, String sessionId) {
        // FastAPI에 보낼 요청 객체 생성
        ChatbotRequestDto requestDto = new ChatbotRequestDto(userPrompt, sessionId);

        // 3. WebClient 호출 시 .uri() 메소드에 전체 URL을 전달합니다.
        return webClient.post()
                .uri(FASTAPI_URL) // 설정된 전체 URL 사용
                .bodyValue(requestDto) // 요청 본문 설정
                .retrieve() // 응답 수신
                .bodyToMono(ChatbotResponseDto.class) // 응답 본문을 ChatbotResponseDto로 변환
                .map(ChatbotResponseDto::getResponse) // DTO에서 실제 응답 문자열만 추출
                .onErrorReturn("챗봇 서버에 오류가 발생했습니다."); // 에러 발생 시 기본 메시지 반환
    }
}