package com.project.korex.transaction.service;

import com.project.korex.transaction.dto.request.TransferRequestDto;
import com.project.korex.transaction.dto.response.TransferResponseDto;
import com.project.korex.transaction.entity.Transaction;
import com.project.korex.transaction.enums.TransactionType;
import com.project.korex.user.entity.Users;
import com.project.korex.user.repository.jpa.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TransferService {

    private final BalanceService balanceService;
    private final TransactionService transactionService;
    private final UserJpaRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TransferResponseDto executeTransfer(Long fromUserId, TransferRequestDto request) {
        try {
            // 1. User 객체들을 먼저 조회
            Users fromUser = userRepository.findById(fromUserId)
                    .orElseThrow(() -> new RuntimeException("송금자를 찾을 수 없습니다"));

            Users toUser = userRepository.findByPhone(request.getRecipientPhone())
                    .orElseThrow(() -> new RuntimeException("수취인을 찾을 수 없습니다"));

            if (!passwordEncoder.matches(request.getTransactionPassword(), fromUser.getTransactionPassword())) {
                throw new RuntimeException("거래 비밀번호가 일치하지 않습니다");
            }
//            if (!request.getTransactionPassword().equals(fromUser.getTransactionPassword())) {
//                throw new RuntimeException("거래 비밀번호가 일치하지 않습니다");
//            }

            if (!toUser.getName().equals(request.getRecipientName())) {
                throw new RuntimeException("수취인 이름과 전화번호가 일치하지 않습니다");
            }

            // 자기 자신에게 송금 방지
            if (fromUser.getId().equals(toUser.getId())) {
                throw new RuntimeException("본인에게는 송금할 수 없습니다");
            }
//            // 숫자만 허용 (필요한 경우)
//            if (!request.getTransactionPassword().matches("\\d{4}")) {
//                throw new RuntimeException("거래 비밀번호는 4자리 숫자여야 합니다");
//            }

            // 3. 잔액 확인
            if (!balanceService.hasEnoughBalance(fromUser.getId(), request.getFromCurrencyCode(), request.getSendAmount())) {
                throw new RuntimeException("잔액이 부족합니다");
            }

            // 4. 수수료 계산 (같은 통화이므로 무료)
            BigDecimal feeAmount = BigDecimal.ZERO;

            // 5. 잔액 차감 및 추가
            balanceService.deductBalance(fromUser.getId(), request.getFromCurrencyCode(), request.getSendAmount());
            balanceService.addBalance(toUser.getId(), request.getToCurrencyCode(), request.getSendAmount());

            // 6. 거래 기록 생성
            Transaction transaction = transactionService.createTransaction(
                    fromUser,
                    toUser,
                    request.getFromCurrencyCode(),
                    request.getToCurrencyCode(),
                    request.getSendAmount(),
                    request.getSendAmount(), // 같은 통화이므로 동일
                    feeAmount,
                    TransactionType.TRANSFER
            );

            // 7. 응답 생성
            TransferResponseDto.TransferDataDto data = new TransferResponseDto.TransferDataDto();
            data.setTransactionId("TX" + transaction.getId());
            data.setStatus("COMPLETED");
            data.setTransferTime(LocalDateTime.now().toString());
            data.setMessage("송금이 완료되었습니다");

            return new TransferResponseDto(true, "송금 성공", data);

        } catch (Exception e) {
                log.error("송금 실패: {}", e.getMessage());
            return new TransferResponseDto(false, e.getMessage(), null);
        }
    }
}
