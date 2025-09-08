package com.project.korex.externalAccount.service;

import com.project.korex.externalAccount.dto.request.AddExternalAccountRequestDto;
import com.project.korex.externalAccount.dto.response.ExternalAccountResponseDto;
import com.project.korex.externalAccount.entity.Bank;
import com.project.korex.externalAccount.entity.ExternalAccount;
import com.project.korex.externalAccount.repository.BankRepository;
import com.project.korex.externalAccount.repository.ExternalAccountRepository;
import com.project.korex.externalAccount.repository.TransactionExternalAccountRepository;
import com.project.korex.user.entity.Users;
import com.project.korex.user.repository.jpa.UserJpaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ExternalAccountService {

    private final ExternalAccountRepository externalAccountRepository;
    private final BankRepository bankRepository;
    private final UserJpaRepository usersRepository;
    private final TransactionExternalAccountRepository transactionExternalAccountRepository;

    @Transactional(readOnly = true)
    public List<ExternalAccountResponseDto> getUserAccounts(Long userId) {
        Users user = getUserEntity(userId);
        List<ExternalAccount> accounts = externalAccountRepository
                .findByUserAndDeletedFalseOrderByIsPrimaryDescCreatedAtAsc(user);

        return accounts.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ExternalAccountResponseDto addAccount(Long userId, AddExternalAccountRequestDto request) {
        Users user = getUserEntity(userId);
        Bank bank = getBankEntity(request.getBankCode());

        // 최대 계좌 수 체크
        long accountCount = externalAccountRepository.countByUserAndDeletedFalse(user);
        if (accountCount >= 3) {
            throw new IllegalArgumentException("최대 3개의 외부 계좌만 등록할 수 있습니다.");
        }

        // 중복 계좌 체크
        boolean isDuplicate = externalAccountRepository
                .existsByUserAndBankCodeAndAccountNumberAndDeletedFalse(
                        user, bank, request.getAccountNumber());

        if (isDuplicate) {
            throw new IllegalArgumentException("이미 등록된 계좌입니다.");
        }

        // 첫 번째 계좌는 자동으로 주계좌
        boolean isFirstAccount = accountCount == 0;

        // 주계좌 설정 시 기존 주계좌 해제
        if (request.getIsPrimary() || isFirstAccount) {
            externalAccountRepository.updateAllToPrimaryFalseAndDeletedFalse(user);
        }

        ExternalAccount account = new ExternalAccount();
        account.setUser(user);  // 객체 설정
        account.setBankCode(bank);  // 객체 설정
        account.setAccountNumber(request.getAccountNumber());
        account.setAccountHolder(request.getAccountHolder());
        account.setSimulationBalance(request.getBalance());
        account.setIsPrimary(request.getIsPrimary() || isFirstAccount);

        ExternalAccount savedAccount = externalAccountRepository.save(account);
        return toResponse(savedAccount);
    }

    public ExternalAccountResponseDto setPrimaryAccount(Long userId, Long externalAccountId) {
        Users user = getUserEntity(userId);
        ExternalAccount account = externalAccountRepository
                .findByIdAndUser(externalAccountId, user)
                .orElseThrow(() -> new EntityNotFoundException("계좌를 찾을 수 없습니다."));

        // 모든 계좌의 주계좌 설정 해제
        externalAccountRepository.updateAllToPrimaryFalseAndDeletedFalse(user);

        // 선택한 계좌를 주계좌로 설정
        account.setIsPrimary(true);
        ExternalAccount savedAccount = externalAccountRepository.save(account);

        return toResponse(savedAccount);
    }

    public void deleteAccount(Long userId, Long externalAccountId) {
        Users user = getUserEntity(userId);
        ExternalAccount account = externalAccountRepository
                .findByIdAndUser(externalAccountId, user)
                .orElseThrow(() -> new EntityNotFoundException("계좌를 찾을 수 없습니다."));

        // 계좌가 2개 이상이고 삭제하려는 계좌가 주계좌인 경우 삭제 불가
        long accountCount = externalAccountRepository.countByUserAndDeletedFalse(user);
        if (account.getIsPrimary() && accountCount > 1) {
            throw new IllegalArgumentException("주계좌를 삭제하려면 다른 계좌를 주계좌로 변경한 후 삭제해주세요.");
        }

        // 소프트 삭제
        account.setDeleted(true);
        account.setDeletedAt(LocalDateTime.now());
        externalAccountRepository.save(account);

        // 삭제된 계좌가 주계좌이고 다른 계좌가 있다면 첫 번째 계좌를 주계좌로 설정
        if (account.getIsPrimary() && accountCount > 1) {
            List<ExternalAccount> remainingAccounts = externalAccountRepository
                    .findByUserAndDeletedFalseOrderByIsPrimaryDescCreatedAtAsc(user);

            if (!remainingAccounts.isEmpty()) {
                ExternalAccount firstAccount = remainingAccounts.get(0);
                firstAccount.setIsPrimary(true);
                externalAccountRepository.save(firstAccount);
            }
        }
    }

    // 유틸리티 메서드들
    private Users getUserEntity(Long userId) {
        return usersRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
    }

    private Bank getBankEntity(String bankCode) {
        return bankRepository.findByBankCode(bankCode)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 은행입니다."));
    }

    private ExternalAccountResponseDto toResponse(ExternalAccount account) {
        return ExternalAccountResponseDto.builder()
                .accountId(account.getId())
                .bankName(account.getBankCode().getBankName())  // 객체에서 직접 조회
                .accountNumber(account.getAccountNumber())
                .accountHolder(account.getAccountHolder())
                .balance(account.getSimulationBalance())
                .isPrimary(account.getIsPrimary())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
