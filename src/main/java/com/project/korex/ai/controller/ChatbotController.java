package com.project.korex.ai.controller;

import com.project.korex.ai.dto.ChatbotRequestDto;
import com.project.korex.ai.dto.ChatbotResponseDto;
import com.project.korex.ai.service.ChatbotService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;
    private static final String CHATBOT_SESSION_ID = "CHATBOT_SESSION_ID";

    /**
     * ChatbotRequestDto로 요청을 받아 세션을 관리하고,
     * 비동기 처리 후 ChatbotResponseDto로 응답합니다.
     */
    @PostMapping("/ask")
    public Mono<ChatbotResponseDto> askToFastApi(@RequestBody ChatbotRequestDto requestDto, HttpSession session) {

        // 1. HttpSession에서 세션 ID를 가져옵니다.
        String sessionId = (String) session.getAttribute(CHATBOT_SESSION_ID);

        // 2. 세션 ID가 없으면 새로 생성하여 세션에 저장합니다.
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
            session.setAttribute(CHATBOT_SESSION_ID, sessionId);
        }

        // 3. DTO에서 사용자의 질문(prompt)을 추출합니다.
        String prompt = requestDto.getPrompt();

        // 4. Service를 호출하여 비동기 응답(Mono<String>)을 받습니다.
        Mono<String> responseMono = chatbotService.getChatbotResponse(prompt, sessionId);

        // 5. Mono 스트림을 변환하여 최종 응답 DTO(Mono<ChatbotResponseDto>) 형태로 가공 후 반환합니다.
        return responseMono.map(ChatbotResponseDto::new); // response 문자열로 ChatbotResponseDto 객체 생성
    }
}