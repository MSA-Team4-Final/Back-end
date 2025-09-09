package com.project.korex.transaction.controller;


import com.project.korex.common.code.ErrorCode;
import com.project.korex.common.exception.InsufficientBalanceException;
import com.project.korex.common.security.jwt.JwtProvider;
import com.project.korex.exchangeRate.service.ExchangeRateCrawlerService;
import com.project.korex.exchangeRate.service.ExchangeRateService;
import com.project.korex.transaction.dto.request.ExchangeRequestDto;
import com.project.korex.transaction.dto.request.ExchangeSimulationRequestDto;
import com.project.korex.transaction.dto.response.ExchangeExecutionResponseDto;
import com.project.korex.transaction.dto.response.ExchangeResultDto;
import com.project.korex.transaction.dto.response.ExchangeSimulationDto;
import com.project.korex.transaction.dto.response.ExchangeSimulationResponseDto;
import com.project.korex.transaction.service.ExchangeService;
import com.project.korex.user.entity.Users;
import com.project.korex.user.repository.jpa.UserJpaRepository;
import jakarta.security.auth.message.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exchange")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ExchangeController {

    @Autowired
    private ExchangeService exchangeService;

    @Autowired
    private ExchangeRateCrawlerService crawlerService;

    private final JwtProvider jwtProvider;
    private final UserJpaRepository userJpaRepository;

    /**
     * 환전 시뮬레이션 (계산만)
     */
    @PostMapping("/simulate")
    public ResponseEntity<ExchangeSimulationResponseDto> simulateExchange(
            @Valid @RequestBody ExchangeSimulationRequestDto request) {

        try {
            ExchangeSimulationDto simulation = exchangeService.simulateExchange(
                    request.getFromCurrency(),
                    request.getToCurrency(),
                    request.getAmount()
            );

            ExchangeSimulationResponseDto response = ExchangeSimulationResponseDto.builder()
                    .success(true)
                    .fromAmount(simulation.getFromAmount())
                    .toAmount(simulation.getToAmount())
                    .exchangeRate(simulation.getExchangeRate())
                    .fee(simulation.getFee())
                    .totalDeductedAmount(simulation.getTotalDeductedAmount())
                    .rateUpdateTime(String.valueOf(simulation.getRateUpdateTime()))
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("환전 시뮬레이션 오류: {}", e.getMessage());
            ExchangeSimulationResponseDto errorResponse = ExchangeSimulationResponseDto.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 즉시 환전 실행
     */
    @PostMapping("/execute")
    public ResponseEntity<ExchangeExecutionResponseDto> executeExchange(
            @Valid @RequestBody ExchangeRequestDto request,
            HttpServletRequest httpRequest) throws AuthException {

            Long userId = getUserIdFromSession(httpRequest);

            ExchangeResultDto result = exchangeService.executeExchange(
                    userId,
                    request.getFromCurrency(),
                    request.getToCurrency(),
                    request.getAmount(),
                    request.getTransactionPassword()
            );

            ExchangeExecutionResponseDto response = ExchangeExecutionResponseDto.builder()
                    .success(result.isSuccess())
                    .transactionId(result.getExchangeId())
                    .total_deducted_amount(result.getTotal_deducted_amount())
                    .appliedRate(result.getAppliedRate())
                    .fee(result.getFee())
                    .message("환전이 완료되었습니다.")
                    .build();

            return ResponseEntity.ok(response);
    }

    /**
     * JWT 토큰에서 사용자 ID 추출
     */
    private Long getUserIdFromSession(HttpServletRequest request) throws AuthException {

            // 1. Header에서 JWT 토큰 추출
            String token = resolveToken(request);

            if (token == null) {
                throw new AuthException(String.valueOf(ErrorCode.TOKEN_NOT_FOUND));
            }

            // 2. JWT 토큰 유효성 검증 (기존 JwtProvider 사용)
            if (!jwtProvider.validateToken(token)) {
                throw new AuthException(String.valueOf(ErrorCode.INVALID_TOKEN));
            }

            // 3. JWT 토큰에서 loginId 추출 (기존 JwtProvider 사용)
            String loginId = jwtProvider.getLoginId(token);
            if (loginId == null) {
                throw new AuthException(String.valueOf(ErrorCode.AUTHENTICATION_FAILED));
            }

            // 4. loginId로 사용자 조회하여 userId 추출
            Users user = userJpaRepository.findByLoginId(loginId)
                    .orElseThrow(() -> new AuthException(String.valueOf(ErrorCode.USER_NOT_FOUND)));

            return user.getId();

        }


    /**
     * Request Header에서 JWT 토큰 추출
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}