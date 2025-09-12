package com.project.korex.ForeignTransfer.service;

import com.project.korex.ForeignTransfer.entity.TransferFeeAdmin;
import com.project.korex.ForeignTransfer.repository.TransferFeeAdminRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class    TransferFeeAdminService {

    private final TransferFeeAdminRepository repository;

    public TransferFeeAdminService(TransferFeeAdminRepository repository) {
        this.repository = repository;
    }

    public List<TransferFeeAdmin> getAllPolicies() {
        return repository.findAll();
    }

    public Optional<TransferFeeAdmin> getPolicyByCurrency(String currency) {
        return repository.findById(currency);
    }

    public TransferFeeAdmin saveOrUpdatePolicy(TransferFeeAdmin policy) {
        return repository.save(policy);
    }
}
