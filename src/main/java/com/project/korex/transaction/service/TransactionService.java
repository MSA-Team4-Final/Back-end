package com.project.korex.transaction.service;

import com.project.korex.externalAccount.dto.response.DepositWithdrawHistoryDto;
import com.project.korex.externalAccount.dto.response.DepositWithdrawSummaryDto;
import com.project.korex.externalAccount.entity.Bank;
import com.project.korex.externalAccount.entity.ExternalAccount;
import com.project.korex.externalAccount.entity.TransactionExternalAccount;
import com.project.korex.externalAccount.enums.AccountRole;
import com.project.korex.externalAccount.repository.BankRepository;
import com.project.korex.externalAccount.repository.ExternalAccountRepository;
import com.project.korex.externalAccount.repository.TransactionExternalAccountRepository;
import com.project.korex.transaction.dto.response.TransactionResponseDto;
import com.project.korex.transaction.dto.response.MonthlyStatsDto;
import com.project.korex.transaction.entity.Balance;
import com.project.korex.transaction.entity.Currency;
import com.project.korex.transaction.entity.Transaction;
import com.project.korex.transaction.enums.TransactionType;
import com.project.korex.transaction.repository.BalanceRepository;
import com.project.korex.transaction.repository.CurrencyRepository;
import com.project.korex.transaction.repository.TransactionRepository;
import com.project.korex.user.entity.Users;
import com.project.korex.user.repository.jpa.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserJpaRepository userRepository;
    private final CurrencyRepository currencyRepository;
    private final ExternalAccountRepository externalAccountRepository;
    private final BankRepository bankRepository;
    private final BalanceRepository balanceRepository;
    private final TransactionExternalAccountRepository transactionExternalAccountRepository;

    public Transaction createTransaction(Users fromUser, Users toUser,
                                         String fromCurrencyCode, String toCurrencyCode,
                                         BigDecimal sendAmount, BigDecimal receiveAmount,
                                         BigDecimal feeAmount, TransactionType transactionType) {

        Currency fromCurrency = currencyRepository.findById(fromCurrencyCode)
                .orElseThrow(() -> new RuntimeException("유효하지 않은 송금 통화 코드: " + fromCurrencyCode));

        Currency toCurrency = currencyRepository.findById(toCurrencyCode)
                .orElseThrow(() -> new RuntimeException("유효하지 않은 수취 통화 코드: " + toCurrencyCode));

        Transaction transaction = new Transaction();
        transaction.setFromUser(fromUser);
        transaction.setToUser(toUser);
        transaction.setFromCurrencyCode(fromCurrency);
        transaction.setToCurrencyCode(toCurrency);
        transaction.setSendAmount(sendAmount);
        transaction.setReceiveAmount(receiveAmount);
        transaction.setExchangeRateApplied(BigDecimal.ONE);
        transaction.setFeeAmount(feeAmount);
        transaction.setTotalDeductedAmount(sendAmount.add(feeAmount));
        transaction.setTransactionType(transactionType);
        transaction.setStatus("COMPLETED");

        return transactionRepository.save(transaction);
    }

    @Transactional(readOnly = true)
    public LocalDateTime getLastTransferDate(Long fromUserId, Long toUserId) {
        return transactionRepository
                .findLastTransferDate(fromUserId, toUserId, TransactionType.TRANSFER)
                .orElse(null);
    }

    /**
     * 기본 거래 내역 조회 - 필터링 없음 (다른 곳에서 사용)
     */
    @Transactional(readOnly = true)
    public List<TransactionResponseDto> getUserTransactions(Long userId) {
        List<Transaction> transactions = transactionRepository
                .findByUserIdOrderByCreatedAtDesc(userId);

        return transactions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 필터링이 적용된 거래 내역 조회 (Controller에서 사용)
     */
    @Transactional(readOnly = true)
    public List<TransactionResponseDto> getUserTransactions(Long userId, String currencyCode,
                                                            String period, String type, String sortBy) {

        List<Transaction> transactions;

        if (currencyCode != null && !currencyCode.isEmpty() && !"all".equals(currencyCode)) {
            Currency currency = currencyRepository.findById(currencyCode)
                    .orElseThrow(() -> new RuntimeException("유효하지 않은 통화 코드: " + currencyCode));

            transactions = transactionRepository.findByUserIdAndCurrency(userId, currency);
        } else {
            transactions = transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }

        transactions = filterByPeriod(transactions, period);
        transactions = filterByType(transactions, type, userId);
        transactions = sortTransactions(transactions, sortBy);

        return transactions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }


    /**
     * 특정 통화의 거래 내역만 조회 (환전 등에서 사용)
     */
    @Transactional(readOnly = true)
    public List<TransactionResponseDto> getUserTransactionsByCurrency(Long userId, String currencyCode) {
        Currency currency = currencyRepository.findById(currencyCode)
                .orElseThrow(() -> new RuntimeException("유효하지 않은 통화 코드: " + currencyCode));

        List<Transaction> transactions = transactionRepository.findByUserIdAndCurrency(userId, currency);

        return transactions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 특정 통화의 월간 통계 조회
     */
    @Transactional(readOnly = true)
    public MonthlyStatsDto getMonthlyStats(Long userId, String currencyCode) {
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        Currency currency = currencyRepository.findById(currencyCode)
                .orElseThrow(() -> new RuntimeException("유효하지 않은 통화 코드: " + currencyCode));

        List<Transaction> monthlyTransactions = transactionRepository
                .findMonthlyTransactionsByCurrency(userId, currency, startOfMonth, endOfMonth);

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        long count = monthlyTransactions.size();

        for (Transaction transaction : monthlyTransactions) {
            if (transaction.getToUser() != null && transaction.getToUser().getId().equals(userId)) {
                income = income.add(transaction.getReceiveAmount() != null ? transaction.getReceiveAmount() : BigDecimal.ZERO);
            } else if (transaction.getFromUser() != null && transaction.getFromUser().getId().equals(userId)) {
                expense = expense.add(transaction.getSendAmount() != null ? transaction.getSendAmount() : BigDecimal.ZERO);
            }
        }

        return MonthlyStatsDto.builder()
                .income(income)
                .expense(expense)
                .count(count)
                .build();
    }

    /**
     * 특정 기간의 거래 내역 조회
     */
    @Transactional(readOnly = true)
    public List<TransactionResponseDto> getTransactionsByPeriod(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Transaction> transactions = transactionRepository
                .findTransactionsByUserIdAndPeriod(userId, startDate, endDate);

        return transactions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 특정 거래 타입의 거래 내역 조회
     */
    @Transactional(readOnly = true)
    public List<TransactionResponseDto> getTransactionsByType(Long userId, TransactionType transactionType) {
        List<Transaction> transactions = transactionRepository
                .findByUserIdAndTransactionType(userId, transactionType);

        return transactions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private List<Transaction> filterByPeriod(List<Transaction> transactions, String period) {
        if (period == null || "all".equals(period)) {
            return transactions;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime filterDate;

        switch (period) {
            case "week":
                filterDate = now.minusWeeks(1);
                break;
            case "month":
                filterDate = now.minusMonths(1);
                break;
            case "3months":
                filterDate = now.minusMonths(3);
                break;
            case "6months":
                filterDate = now.minusMonths(6);
                break;
            case "year":
                filterDate = now.minusYears(1);
                break;
            default:
                return transactions;
        }

        return transactions.stream()
                .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(filterDate))
                .collect(Collectors.toList());
    }

    private List<Transaction> filterByType(List<Transaction> transactions, String type, Long userId) {
        if (type == null || "all".equals(type)) {
            return transactions;
        }

        return transactions.stream()
                .filter(t -> {
                    if ("income".equals(type)) {
                        return t.getToUser() != null && t.getToUser().getId().equals(userId);
                    } else if ("expense".equals(type)) {
                        return t.getFromUser() != null && t.getFromUser().getId().equals(userId);
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    private List<Transaction> sortTransactions(List<Transaction> transactions, String sortBy) {
        if (sortBy == null || "date".equals(sortBy)) {
            return transactions.stream()
                    .sorted((a, b) -> {
                        if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                        if (a.getCreatedAt() == null) return 1;
                        if (b.getCreatedAt() == null) return -1;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    })
                    .collect(Collectors.toList());
        } else if ("amount".equals(sortBy)) {
            return transactions.stream()
                    .sorted((a, b) -> {
                        BigDecimal amountA = a.getSendAmount() != null ? a.getSendAmount() : BigDecimal.ZERO;
                        BigDecimal amountB = b.getSendAmount() != null ? b.getSendAmount() : BigDecimal.ZERO;
                        return amountB.compareTo(amountA);
                    })
                    .collect(Collectors.toList());
        }
        return transactions;
    }

    /**
     * 충전 거래 생성 (외부계좌 매핑 포함)
     */
    public Transaction createDepositTransaction(Users user,
                                                ExternalAccount fromExternalAccount,
                                                String toCurrencyCode,
                                                BigDecimal amount,
                                                BigDecimal feeAmount) {

        Currency toCurrency = currencyRepository.findById(toCurrencyCode)
                .orElseThrow(() -> new RuntimeException("유효하지 않은 통화 코드: " + toCurrencyCode));

        // 1. 기본 거래 생성
        Transaction transaction = new Transaction();
        transaction.setFromUser(user);
        transaction.setToUser(user);
        transaction.setFromCurrencyCode(toCurrency); // 임시로 같은 통화 사용
        transaction.setToCurrencyCode(toCurrency);
        transaction.setSendAmount(amount);
        transaction.setReceiveAmount(amount);
        transaction.setExchangeRateApplied(BigDecimal.ONE);
        transaction.setFeeAmount(feeAmount);
        transaction.setTotalDeductedAmount(amount.add(feeAmount));
        transaction.setTransactionType(TransactionType.DEPOSIT);
        transaction.setStatus("COMPLETED");

        Transaction savedTransaction = transactionRepository.save(transaction);

        // 2. 외부계좌 매핑 정보 저장
        TransactionExternalAccount mapping = new TransactionExternalAccount();
        mapping.setTransaction(savedTransaction);
        mapping.setExternalAccount(fromExternalAccount);
        mapping.setAccountRole(AccountRole.FROM);

        transactionExternalAccountRepository.save(mapping);

        log.info("충전 거래 및 외부계좌 매핑 저장 완료 - transactionId: {}, externalAccountId: {}",
                savedTransaction.getId(), fromExternalAccount.getId());

        return savedTransaction;
    }

    /**
     * 출금 거래 생성 (외부계좌 매핑 포함)
     */
    public Transaction createWithdrawalTransaction(Users user,
                                                   String fromCurrencyCode,
                                                   ExternalAccount toExternalAccount,
                                                   BigDecimal amount,
                                                   BigDecimal feeAmount) {

        Currency fromCurrency = currencyRepository.findById(fromCurrencyCode)
                .orElseThrow(() -> new RuntimeException("유효하지 않은 통화 코드: " + fromCurrencyCode));

        // 1. 기본 거래 생성
        Transaction transaction = new Transaction();
        transaction.setFromUser(user);
        transaction.setToUser(user);
        transaction.setFromCurrencyCode(fromCurrency);
        transaction.setToCurrencyCode(fromCurrency);
        transaction.setSendAmount(amount);
        transaction.setReceiveAmount(amount.subtract(feeAmount));
        transaction.setExchangeRateApplied(BigDecimal.ONE);
        transaction.setFeeAmount(feeAmount);
        transaction.setTotalDeductedAmount(amount.add(feeAmount));
        transaction.setTransactionType(TransactionType.WITHDRAW);
        transaction.setStatus("COMPLETED");

        Transaction savedTransaction = transactionRepository.save(transaction);

        // 2. 외부계좌 매핑 정보 저장
        TransactionExternalAccount mapping = new TransactionExternalAccount();
        mapping.setTransaction(savedTransaction);
        mapping.setExternalAccount(toExternalAccount);
        mapping.setAccountRole(AccountRole.TO);

        transactionExternalAccountRepository.save(mapping);

        log.info("출금 거래 및 외부계좌 매핑 저장 완료 - transactionId: {}, externalAccountId: {}",
                savedTransaction.getId(), toExternalAccount.getId());

        return savedTransaction;
    }


    /**
     * 충전/출금 내역 조회 (매핑 테이블 버전)
     */
    @Transactional(readOnly = true)
    public List<DepositWithdrawHistoryDto> getDepositwithdrawHistory(Long userId,
                                                                         String transactionType,
                                                                         LocalDateTime startDate,
                                                                         LocalDateTime endDate) {

        log.info("충전/출금 내역 조회 - userId: {}, type: {}, startDate: {}, endDate: {}",
                userId, transactionType, startDate, endDate);

        // 1. 기본 거래 데이터 조회
        List<Transaction> transactions = getFilteredTransactions(userId, transactionType, startDate, endDate);

        if (transactions.isEmpty()) {
            log.info("조회된 거래가 없습니다.");
            return Collections.emptyList();
        }

        // 2. 거래 ID 목록 추출
        List<Long> transactionIds = transactions.stream()
                .map(Transaction::getId)
                .collect(Collectors.toList());

        // 3. 외부계좌 매핑 정보 한번에 조회
        List<TransactionExternalAccount> externalMappings =
                transactionExternalAccountRepository.findByTransactionIdInWithExternalAccount(transactionIds);

        log.info("외부계좌 매핑 정보 조회 완료 - 건수: {}", externalMappings.size());

        // 4. 거래별 외부계좌 정보 매핑 (거래ID -> (역할 -> 외부계좌))
        Map<Long, Map<AccountRole, ExternalAccount>> externalAccountMap =
                externalMappings.stream()
                        .collect(Collectors.groupingBy(
                                tea -> tea.getTransaction().getId(),
                                Collectors.toMap(
                                        TransactionExternalAccount::getAccountRole,
                                        TransactionExternalAccount::getExternalAccount,
                                        (existing, replacement) -> existing // 중복 키가 있으면 기존 값 유지
                                )
                        ));

        // 5. DTO 변환
        List<DepositWithdrawHistoryDto> result = transactions.stream()
                .map(transaction -> {
                    Map<AccountRole, ExternalAccount> accountMap =
                            externalAccountMap.getOrDefault(transaction.getId(), new HashMap<>());
                    return convertToDepositwithdrawDto(transaction, userId, accountMap);
                })
                .collect(Collectors.toList());

        log.info("DTO 변환 완료 - 건수: {}", result.size());
        return result;
    }

    /**
     * 필터 조건에 따른 거래 조회
     */
    private List<Transaction> getFilteredTransactions(Long userId, String transactionType,
                                                      LocalDateTime startDate, LocalDateTime endDate) {

        // 거래 타입 설정
        List<TransactionType> types = new ArrayList<>();
        if (transactionType == null || transactionType.isEmpty()) {
            types.add(TransactionType.DEPOSIT);
            types.add(TransactionType.WITHDRAW);
        } else if ("DEPOSIT".equals(transactionType)) {
            types.add(TransactionType.DEPOSIT);
        } else if ("withdraw".equals(transactionType)) {
            types.add(TransactionType.WITHDRAW);
        }

        // 날짜 조건에 따라 쿼리 분기
        if (startDate != null && endDate != null) {
            return transactionRepository.findDepositWithdrawTransactionsByDateRange(userId, types, startDate, endDate);
        } else {
            return transactionRepository.findDepositWithdrawTransactions(userId, types);
        }
    }

    /**
     * Transaction을 DTO로 변환 (외부계좌 매핑 포함)
     */
    private DepositWithdrawHistoryDto convertToDepositwithdrawDto(Transaction transaction,
                                                                      Long userId,
                                                                      Map<AccountRole, ExternalAccount> externalAccounts) {

        String currencyCode = getCurrencyCodeForBalance(transaction, userId);
        BigDecimal balanceBefore = calculateBalanceBefore(transaction, userId, currencyCode);
        BigDecimal balanceAfter = calculateBalanceAfter(transaction, userId, currencyCode, balanceBefore);

        // 외부계좌 정보를 포함한 계좌 표시명 생성
        String fromAccount = getFromAccountDisplay(transaction, externalAccounts.get(AccountRole.FROM));
        String toAccount = getToAccountDisplay(transaction, externalAccounts.get(AccountRole.TO));

        return DepositWithdrawHistoryDto.builder()
                .transactionId(transaction.getId())
                .transactionType(transaction.getTransactionType())
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(transaction.getSendAmount())
                .feeAmount(transaction.getFeeAmount() != null ? transaction.getFeeAmount() : BigDecimal.ZERO)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    /**
     * 출금 계좌 표시명 생성 (외부계좌 정보 포함)
     */
    private String getFromAccountDisplay(Transaction transaction, ExternalAccount fromExternalAccount) {
        try {
            if (transaction.getTransactionType() == TransactionType.DEPOSIT) {
                // 충전: 외부계좌에서 내 계좌로
                if (fromExternalAccount != null) {
                    String bankName = getBankName(fromExternalAccount.getBankCode().getBankName());
                    return bankName + " " + maskAccountNumber(fromExternalAccount.getAccountNumber());
                } else {
                    return "외부계좌 (정보 없음)";
                }
            } else {
                // 출금: 내 계좌에서
                return transaction.getFromCurrencyCode().getCurrencyName() + " 계좌";
            }

        } catch (Exception e) {
            log.error("출금 계좌 표시명 생성 중 오류: {}", e.getMessage(), e);
            return "알 수 없는 계좌";
        }
    }

    /**
     * 입금 계좌 표시명 생성 (외부계좌 정보 포함)
     */
    private String getToAccountDisplay(Transaction transaction, ExternalAccount toExternalAccount) {
        try {
            if (transaction.getTransactionType() == TransactionType.DEPOSIT) {
                // 충전: 내 계좌로
                return transaction.getToCurrencyCode().getCurrencyName() + " 계좌";
            } else {
                // 출금: 외부계좌로
                if (toExternalAccount != null) {
                    String bankName = getBankName(toExternalAccount.getBankCode().getBankName());
                    return bankName + " " + maskAccountNumber(toExternalAccount.getAccountNumber());
                } else {
                    return "외부계좌 (정보 없음)";
                }
            }

        } catch (Exception e) {
            log.error("입금 계좌 표시명 생성 중 오류: {}", e.getMessage(), e);
            return "알 수 없는 계좌";
        }
    }

    /**
     * 은행명 조회
     */
    private String getBankName(String bankCode) {
        try {
            return bankRepository.findById(Long.valueOf(bankCode))
                    .map(Bank::getBankName)
                    .orElse("은행-" + bankCode);
        } catch (Exception e) {
            log.error("은행명 조회 중 오류: {}", e.getMessage(), e);
            return "은행-" + bankCode;
        }
    }

    /**
     * 계좌번호 마스킹 (앞 3자리, 뒤 4자리 노출)
     */
    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            return accountNumber;
        }

        // 숫자만 추출
        String numbersOnly = accountNumber.replaceAll("[^0-9]", "");
        int length = numbersOnly.length();

        // 4자리 이하는 마스킹하지 않음
        if (length <= 4) {
            return numbersOnly;
        }

        // 7자리 이하: 앞 2자리 + 마스킹 + 뒤 2자리
        if (length <= 7) {
            String prefix = numbersOnly.substring(0, 2);
            String suffix = numbersOnly.substring(length - 2);
            String masked = "*".repeat(length - 4);
            return prefix + masked + suffix;
        }

        // 8자리 이상: 앞 3자리 + 마스킹 + 뒤 4자리
        String prefix = numbersOnly.substring(0, 3);
        String suffix = numbersOnly.substring(length - 4);
        String masked = "*".repeat(length - 7);

        return prefix + masked + suffix;
    }

    /**
     * 충전/출금 요약 정보 조회
     */
    @Transactional(readOnly = true)
    public DepositWithdrawSummaryDto getDepositwithdrawSummary(Long userId,
                                                                   String transactionType,
                                                                   LocalDateTime startDate,
                                                                   LocalDateTime endDate) {

        List<DepositWithdrawHistoryDto> history = getDepositwithdrawHistory(userId, transactionType, startDate, endDate);

        BigDecimal totalDeposit = history.stream()
                .filter(h -> h.getTransactionType() == TransactionType.DEPOSIT)
                .map(DepositWithdrawHistoryDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalWithdraw = history.stream()
                .filter(h -> h.getTransactionType() == TransactionType.WITHDRAW)
                .map(DepositWithdrawHistoryDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DepositWithdrawSummaryDto.builder()
                .totalDeposit(totalDeposit)
                .totalWithdraw(totalWithdraw)
                .netAmount(totalDeposit.subtract(totalWithdraw))
                .build();
    }

    /**
     * 거래 전 잔액 계산
     */
    private BigDecimal calculateBalanceBefore(Transaction transaction, Long userId, String currencyCode) {
        try {
            // 현재 잔액 조회
            Currency currency = currencyRepository.findById(currencyCode)
                    .orElseThrow(() -> new RuntimeException("유효하지 않은 통화 코드: " + currencyCode));

            Balance currentBalance = balanceRepository.findByUserIdAndCurrency(userId, currency)
                    .orElse(createZeroBalance(userId, currency));

            // 해당 거래 이후 발생한 거래들의 합계를 구해서 역산
            BigDecimal laterTransactionsSum = transactionRepository
                    .sumTransactionsAfterTransaction(transaction.getId(), userId, currencyCode);

            // 현재 잔액에서 이후 거래들을 빼면 해당 거래 직후 잔액
            BigDecimal balanceAfterThisTransaction = currentBalance.getAvailableAmount().subtract(laterTransactionsSum);

            // 해당 거래 금액을 고려해서 거래 전 잔액 계산
            BigDecimal transactionAmount = getTransactionImpact(transaction, userId);

            return balanceAfterThisTransaction.subtract(transactionAmount);

        } catch (Exception e) {
            log.error("거래 전 잔액 계산 중 오류: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * 거래 후 잔액 계산
     */
    private BigDecimal calculateBalanceAfter(Transaction transaction, Long userId, String currencyCode, BigDecimal balanceBefore) {
        BigDecimal transactionImpact = getTransactionImpact(transaction, userId);
        return balanceBefore.add(transactionImpact);
    }

    /**
     * 거래가 잔액에 미치는 영향 계산
     */
    private BigDecimal getTransactionImpact(Transaction transaction, Long userId) {
        if (transaction.getTransactionType() == TransactionType.DEPOSIT &&
                transaction.getToUser().getId().equals(userId)) {
            // 충전: 받는 금액
            return transaction.getReceiveAmount() != null ? transaction.getReceiveAmount() : BigDecimal.ZERO;
        } else if (transaction.getTransactionType() == TransactionType.WITHDRAW &&
                transaction.getFromUser().getId().equals(userId)) {
            // 출금: 보낸 금액 + 수수료 (마이너스)
            BigDecimal sendAmount = transaction.getSendAmount() != null ? transaction.getSendAmount() : BigDecimal.ZERO;
            BigDecimal feeAmount = transaction.getFeeAmount() != null ? transaction.getFeeAmount() : BigDecimal.ZERO;
            return sendAmount.add(feeAmount).negate();
        }

        return BigDecimal.ZERO;
    }

    /**
     * 0원 잔액 객체 생성
     */
    private Balance createZeroBalance(Long userId, Currency currency) {
        Balance balance = new Balance();
        balance.setAvailableAmount(BigDecimal.ZERO);
        balance.setCurrency(currency);
        return balance;
    }

    /**
     * 잔액 계산에 사용할 통화 코드 결정
     */
    private String getCurrencyCodeForBalance(Transaction transaction, Long userId) {
        if (transaction.getTransactionType() == TransactionType.DEPOSIT) {
            // 충전: 받는 통화 (내 계좌로 들어오는 통화)
            return transaction.getToCurrencyCode() != null ? transaction.getToCurrencyCode().getCode() : null;
        } else if (transaction.getTransactionType() == TransactionType.WITHDRAW) {
            // 출금: 보내는 통화 (내 계좌에서 나가는 통화)
            return transaction.getFromCurrencyCode() != null ? transaction.getFromCurrencyCode().getCode() : null;
        }
        return null;
    }

    private TransactionResponseDto convertToDto(Transaction transaction) {
        return TransactionResponseDto.builder()
                .id(transaction.getId())
                .fromUserId(transaction.getFromUser() != null ? transaction.getFromUser().getId() : null)
                .fromUserName(transaction.getFromUser() != null ? transaction.getFromUser().getName() : null)
                .toUserId(transaction.getToUser() != null ? transaction.getToUser().getId() : null)
                .toUserName(transaction.getToUser() != null ? transaction.getToUser().getName() : null)
                .fromCurrencyCode(transaction.getFromCurrencyCode() != null ? transaction.getFromCurrencyCode().getCode() : null)
                .fromCurrencyName(transaction.getFromCurrencyCode() != null ? transaction.getFromCurrencyCode().getCurrencyName() : null)
                .toCurrencyCode(transaction.getToCurrencyCode() != null ? transaction.getToCurrencyCode().getCode() : null)
                .toCurrencyName(transaction.getToCurrencyCode() != null ? transaction.getToCurrencyCode().getCurrencyName() : null)
                .sendAmount(transaction.getSendAmount())
                .receiveAmount(transaction.getReceiveAmount())
                .feeAmount(transaction.getFeeAmount())
                .totalDeductedAmount(transaction.getTotalDeductedAmount())
                .transactionType(transaction.getTransactionType())
                .status(transaction.getStatus())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
