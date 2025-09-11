package com.project.korex.transaction.service;

import com.project.korex.common.code.ErrorCode;
import com.project.korex.common.exception.InsufficientBalanceException;
import com.project.korex.common.exception.UserNotFoundException;
import com.project.korex.exchangeRate.service.ExchangeRateCrawlerService;
import com.project.korex.transaction.dto.response.ExchangeCalculationDto;
import com.project.korex.transaction.dto.response.ExchangeResultDto;
import com.project.korex.transaction.dto.response.ExchangeSimulationDto;
import com.project.korex.transaction.entity.Balance;
import com.project.korex.transaction.entity.Currency;
import com.project.korex.transaction.entity.Transaction;
import com.project.korex.transaction.enums.TransactionType;
import com.project.korex.transaction.exception.ExchangeException;
import com.project.korex.transaction.repository.BalanceRepository;
import com.project.korex.transaction.repository.CurrencyRepository;
import com.project.korex.transaction.repository.TransactionRepository;
import com.project.korex.user.entity.Users;
import com.project.korex.user.repository.jpa.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.security.auth.login.AccountNotFoundException;
import java.awt.print.Pageable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ExchangeService {

    private final CurrencyRepository currencyRepository;
    private final BalanceService balanceService; // 기존 BalanceService 활용
    private final TransactionRepository transactionRepository;
    private final UserJpaRepository userRepository;
    // Redis 환율 조회는 RestTemplate으로 기존 API 호출
    private final RestTemplate restTemplate;
    private final PasswordEncoder passwordEncoder;


    /**
     * 환전 시뮬레이션
     */
    public ExchangeSimulationDto simulateExchange(String fromCurrency, String toCurrency, BigDecimal amount) {
        // 1. 기존 ExchangeRateController API로 현재 환율 조회
        List<Map<String, String>> currentRate = getCurrentExchangeRateFromAPI(fromCurrency, toCurrency);
        if (currentRate == null || currentRate.isEmpty()) {
            throw new ExchangeException(ErrorCode.EXCHANGE_RATE_NOT_FOUND);
        }

        // 2. 환전 계산
        ExchangeCalculationDto calculation = calculateExchange(fromCurrency, toCurrency, amount, currentRate.get(0));

        return ExchangeSimulationDto.builder()
                .fromAmount(amount)
                .toAmount(calculation.getConvertedAmount())
                .exchangeRate(calculation.getExchangeRate())
                .fee(calculation.getFee())
                .totalDeductedAmount(calculation.getTotalDeductedAmount())
                .rateUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
//                .rateUpdateTime(currentRate.get(0).get("crawl_time"))
                .build();
    }

    /**
     * 환전 실행
     */
    public ExchangeResultDto executeExchange(Long userId, String fromCurrency, String toCurrency, BigDecimal amount, String transactionPassword) {
        // 1. 사용자 존재 확인
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND));

        // 2. Currency 객체 조회
        Currency fromCurrencyEntity = currencyRepository.findById(fromCurrency)
                .orElseThrow(() -> new ExchangeException(ErrorCode.INVALID_FROM_CURRENCY));

        Currency toCurrencyEntity = currencyRepository.findById(toCurrency)
                .orElseThrow(() -> new ExchangeException(ErrorCode.INVALID_TO_CURRENCY));

        // 2. 기존 API로 현재 환율 조회
        List<Map<String, String>> currentRate = getCurrentExchangeRateFromAPI(fromCurrency, toCurrency);
        if (currentRate == null || currentRate.isEmpty()) {
            throw new ExchangeException(ErrorCode.EXCHANGE_RATE_NOT_FOUND);
        }

        // 3. 환전 계산
        ExchangeCalculationDto calculation = calculateExchange(fromCurrency, toCurrency, amount, currentRate.get(0));

        // 4. 잔액 확인 (기존 BalanceService 활용)
        if (!balanceService.hasEnoughBalance(userId, fromCurrency, amount)) {
            throw new InsufficientBalanceException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        if (!passwordEncoder.matches(transactionPassword, user.getTransactionPassword())) {
            throw new RuntimeException("거래 비밀번호가 일치하지 않습니다");
        }
        // 5. 환전 실행 (기존 BalanceService 활용)
        balanceService.deductBalance(userId, fromCurrency, amount);
        balanceService.addBalance(userId, toCurrency, calculation.getConvertedAmount());

        // 6. 거래 기록 저장
        Transaction transaction = Transaction.builder()
                .fromUser(user)
                .toUser(user)
                .fromCurrencyCode(fromCurrencyEntity)
                .toCurrencyCode(toCurrencyEntity)
                .sendAmount(amount)
                .receiveAmount(calculation.getConvertedAmount())
                .feePercentage(new BigDecimal("0.005"))
                .exchangeRateApplied(calculation.getExchangeRate())
                .feeAmount(calculation.getFee())
                .totalDeductedAmount(amount)
                .transactionType(TransactionType.EXCHANGE)
//                .createdAt(LocalDateTime.now())
                .build();

        transactionRepository.save(transaction);

        return ExchangeResultDto.builder()
                .success(true)
                .exchangeId(transaction.getId())
                .total_deducted_amount(calculation.getTotalDeductedAmount())
                .appliedRate(calculation.getExchangeRate())
                .fee(calculation.getFee())
                .build();
    }

    /**
     * 기존 ExchangeRateController API 호출로 환율 조회
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, String>> getCurrentExchangeRateFromAPI(String fromCurrency, String toCurrency) {
        try {
            String targetCurrency = "KRW".equals(fromCurrency) ? toCurrency : fromCurrency;

            String url = "http://localhost:8080/api/exchange/realtime/" + targetCurrency;
            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);

            return response.getBody();
        } catch (Exception e) {
            log.error("환율 API 호출 오류: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 환전 계산
     */
    private ExchangeCalculationDto calculateExchange(String fromCurrency, String toCurrency,
                                                     BigDecimal amount, Map<String, String> currentRate) {

        BigDecimal baseRate = new BigDecimal(currentRate.get("base_rate").toString().replace(",", ""));
        BigDecimal feeRate = new BigDecimal("0.005");

        // 통화별 환율 단위 설정
        BigDecimal exchangeUnit = getExchangeUnit(fromCurrency, toCurrency);
        BigDecimal adjustedRate = baseRate.divide(exchangeUnit, 6, RoundingMode.HALF_UP);

        BigDecimal fee = BigDecimal.ZERO;
        BigDecimal totalDeductedAmount = BigDecimal.ZERO;
        BigDecimal convertedAmount = BigDecimal.ZERO;


        if ("KRW".equals(fromCurrency)) {
            // KRW → 외화
            fee = amount.multiply(feeRate);
            totalDeductedAmount = fee;

            BigDecimal amountAfterFee = amount.subtract(fee);
            convertedAmount = amountAfterFee.divide(adjustedRate, 2, RoundingMode.HALF_UP);

        } else if ("KRW".equals(toCurrency)) {
            // 외화 → KRW
            fee = amount.multiply(feeRate);
            totalDeductedAmount = fee;

            BigDecimal amountAfterFee = amount.subtract(fee);
            convertedAmount = amountAfterFee.multiply(adjustedRate);
        }

        return ExchangeCalculationDto.builder()
                .exchangeRate(baseRate) // 원본 환율 표시용
                .beforeFeeAmount(amount.multiply(adjustedRate))
                .convertedAmount(convertedAmount)
                .fee(fee)
                .totalDeductedAmount(totalDeductedAmount)
                .build();
    }

    /**
     * 통화별 환율 단위 반환
     */
    private BigDecimal getExchangeUnit(String fromCurrency, String toCurrency) {

        String targetCurrency = "KRW".equals(fromCurrency) ? toCurrency : fromCurrency;

        return switch (targetCurrency) {
            case "JPY", "CNY" -> new BigDecimal("100");
            default -> BigDecimal.ONE;
        };
    }

    /**
     * 환전 내역 조회
     */
    public List<Transaction> getExchangeHistory(Long userId, int page, int size) {
//        Pageable pageable = (Pageable) PageRequest.of(page, size);
//        return transactionRepository.findByUserIdAndTransactionTypeOrderByCreatedAtDesc(
//                userId, TransactionType.EXCHANGE, pageable).getContent();
        return null;
    }

    public boolean canExchange(String fromCurrency, String toCurrency) {
        return "KRW".equals(fromCurrency) || "KRW".equals(toCurrency);
    }
}



