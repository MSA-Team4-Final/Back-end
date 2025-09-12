package com.project.korex.ForeignTransfer.service;

import com.project.korex.ForeignTransfer.dto.request.RecipientRequest;
import com.project.korex.ForeignTransfer.dto.response.RecipientResponse;
import com.project.korex.ForeignTransfer.entity.Recipient;
import com.project.korex.ForeignTransfer.exception.CurrencyNotFoundException;
import com.project.korex.ForeignTransfer.exception.RecipientNotFoundException;
import com.project.korex.ForeignTransfer.repository.RecipientRepository;
import com.project.korex.common.code.ErrorCode;
import com.project.korex.common.exception.UserNotFoundException;
import com.project.korex.transaction.entity.Currency;
import com.project.korex.transaction.repository.CurrencyRepository;
import com.project.korex.user.entity.Users;
import com.project.korex.user.repository.jpa.UserJpaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecipientService {

    private final RecipientRepository recipientRepository;
    private final UserJpaRepository userRepository;
    private final CurrencyRepository currencyRepository;

    private Currency getCurrencyByCode(String currencyCode) {
        return currencyRepository.findByCode(currencyCode)
                .orElseThrow(() -> new CurrencyNotFoundException(ErrorCode.CURRENCY_NOT_FOUND));
    }

    @Transactional
    public RecipientResponse createRecipient(String loginId, RecipientRequest request) {
        Users user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND));

        Recipient recipient = new Recipient();
        recipient.setUser(user);
        recipient.setName(request.getName());
        recipient.setBankName(request.getBankName());
        recipient.setAccountNumber(request.getAccountNumber());
        recipient.setCountryNumber(request.getCountryNumber());
        recipient.setCountry(request.getCountry());
        recipient.setPhoneNumber(request.getPhoneNumber());
        recipient.setEmail(request.getEmail());
        recipient.setEngAddress(request.getEngAddress());
        recipient.setCurrency(getCurrencyByCode(request.getCurrencyCode()));
        recipient.setIsActive(true);

        Recipient savedRecipient = recipientRepository.save(recipient);
        return convertToResponse(savedRecipient);
    }

    @Transactional
    public RecipientResponse getRecipientById(String loginId, Long recipientId) {
        Recipient recipient = recipientRepository
                .findByIdAndUser_LoginId(recipientId, loginId)
                .orElseThrow(() -> new RecipientNotFoundException(ErrorCode.RECIPIENT_NOT_FOUND));

        return convertToResponse(recipient);
    }

    @Transactional
    public List<RecipientResponse> getRecipientsByLoginId(String loginId) {
        List<Recipient> recipients = recipientRepository.findByUser_LoginId(loginId);
        return recipients.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public RecipientResponse updateRecipient(String loginId, Long recipientId, RecipientRequest request) {
        Recipient recipient = recipientRepository.findByIdAndUser_LoginId(recipientId, loginId)
                .orElseThrow(() -> new RecipientNotFoundException(ErrorCode.RECIPIENT_NOT_FOUND));

        recipient.setName(request.getName());
        recipient.setBankName(request.getBankName());
        recipient.setAccountNumber(request.getAccountNumber());
        recipient.setCountryNumber(request.getCountryNumber());
        recipient.setCountry(request.getCountry());
        recipient.setPhoneNumber(request.getPhoneNumber());
        recipient.setEmail(request.getEmail());
        recipient.setEngAddress(request.getEngAddress());
        recipient.setCurrency(getCurrencyByCode(request.getCurrencyCode()));

        return convertToResponse(recipient);
    }

    @Transactional
    public void deactivateRecipient(Long recipientId) {
        Recipient recipient = recipientRepository.findById(recipientId)
                .orElseThrow(() -> new RuntimeException("Recipient not found"));
        recipient.setIsActive(false);
        recipientRepository.save(recipient);
    }

    @Transactional
    public List<RecipientResponse> getActiveRecipientsByLoginId(String loginId) {
        List<Recipient> recipients = recipientRepository.findByUser_LoginIdAndIsActiveTrue(loginId);
        return recipients.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // 🔹 Response 변환
    private RecipientResponse convertToResponse(Recipient recipient) {
        RecipientResponse response = new RecipientResponse();

        response.setRecipientId(recipient.getId());
        response.setName(recipient.getName());
        response.setBankName(recipient.getBankName());
        response.setAccountNumber(recipient.getAccountNumber());
        response.setCountryNumber(recipient.getCountryNumber());
        response.setCountry(recipient.getCountry());
        response.setPhoneNumber(recipient.getPhoneNumber());
        response.setEmail(recipient.getEmail());
        response.setEngAddress(recipient.getEngAddress());
        response.setIsActive(recipient.getIsActive());

        if (recipient.getCurrency() != null) {
            response.setCurrencyCode(recipient.getCurrency().getCode());
            response.setCurrencyName(recipient.getCurrency().getCurrencyName());
        }

        return response;
    }
}
