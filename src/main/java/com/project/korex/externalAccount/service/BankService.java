package com.project.korex.externalAccount.service;

import com.project.korex.externalAccount.dto.response.BankResponseDto;
import com.project.korex.externalAccount.entity.Bank;
import com.project.korex.externalAccount.repository.BankRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class BankService {

    private final BankRepository bankRepository;

    public List<BankResponseDto> getAllBanks() {
        List<Bank> banks = bankRepository.findAllByOrderByBankName();

        return banks.stream()
                .map(bank -> BankResponseDto.builder()
                        .code(bank.getBankCode())
                        .name(bank.getBankName())
                        .build())
                .collect(Collectors.toList());
    }

    public List<BankResponseDto> getUserActiveBanks(Long userId) {
        // 사용자의 삭제되지 않은 계좌들과 연관된 은행들만 조회
        List<Bank> banks = bankRepository.findBanksByUserActiveAccounts(userId);

        return banks.stream()
                .map(this::toBankResponse)
                .collect(Collectors.toList());
    }



    /**
     * 특정 은행 코드로 은행 정보 조회
     */
    public BankResponseDto getBankByCode(String bankCode) {
        Bank bank = bankRepository.findByBankCode(bankCode)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 은행입니다: " + bankCode));

        return toBankResponse(bank);
    }

    /**
     * 은행명으로 검색
     */
    public List<BankResponseDto> searchBanksByName(String bankName) {
        List<Bank> banks = bankRepository.findByBankNameContainingOrderByBankName(bankName);

        return banks.stream()
                .map(this::toBankResponse)
                .collect(Collectors.toList());
    }

    /**
     * 은행 코드 유효성 검증
     */
    public boolean isValidBankCode(String bankCode) {
        return bankRepository.existsByBankCode(bankCode);
    }

    /**
     * Bank Entity를 BankResponseDto로 변환
     */
    private BankResponseDto toBankResponse(Bank bank) {
        return BankResponseDto.builder()
                .code(bank.getBankCode())
                .name(bank.getBankName())
                .build();
    }
}

