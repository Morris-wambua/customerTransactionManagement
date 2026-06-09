package com.morrislab.customertransactionmanagement.service.impl;

import com.morrislab.customertransactionmanagement.dto.request.CustomerTransactionRequest;
import com.morrislab.customertransactionmanagement.dto.response.CustomerTransactionResponse;
import com.morrislab.customertransactionmanagement.entity.CustomerTransactionDetail;
import com.morrislab.customertransactionmanagement.exception.ConflictException;
import com.morrislab.customertransactionmanagement.exception.ValidationException;
import com.morrislab.customertransactionmanagement.repository.CustomerTransactionDetailRepository;
import com.morrislab.customertransactionmanagement.service.CustomerTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerTransactionServiceImpl implements CustomerTransactionService {

    private final CustomerTransactionDetailRepository customerTransactionDetailRepository;

    @Override
    @Transactional
    public CustomerTransactionResponse saveCustomerTransactionDetails(
            CustomerTransactionRequest request,
            String idempotencyKey) {
        validateRequest(request, idempotencyKey);

        return customerTransactionDetailRepository.findByIdempotencyKey(idempotencyKey)
                .map(existingTransaction -> toResponse(
                        existingTransaction,
                        "Customer transaction details already processed for this idempotency key"))
                .orElseGet(() -> saveNewCustomerTransaction(request, idempotencyKey));
    }

    private CustomerTransactionResponse saveNewCustomerTransaction(
            CustomerTransactionRequest request,
            String idempotencyKey) {
        if (customerTransactionDetailRepository.existsByAccountNumber(request.getAccountNumber())) {
            throw new ConflictException("ACCOUNT_ALREADY_EXISTS", "Account number already exists");
        }

        if (customerTransactionDetailRepository.existsByTransactionReference(request.getTransactionReference())) {
            throw new ConflictException("TRANSACTION_REFERENCE_ALREADY_EXISTS", "Transaction reference already exists");
        }

        log.info("Saving customer transaction details for reference {}", request.getTransactionReference());

        CustomerTransactionDetail transactionDetail = CustomerTransactionDetail.builder()
                .customerId(request.getCustomerId())
                .customerName(request.getCustomerName())
                .accountNumber(request.getAccountNumber())
                .transactionReference(request.getTransactionReference())
                .idempotencyKey(idempotencyKey)
                .transactionAmount(request.getTransactionAmount())
                .currentBalance(request.getCurrentBalance())
                .build();

        CustomerTransactionDetail savedTransactionDetail =
                customerTransactionDetailRepository.save(transactionDetail);

        return toResponse(savedTransactionDetail, "Customer transaction details saved successfully");
    }

    private void validateRequest(CustomerTransactionRequest request, String idempotencyKey) {
        if (StringUtils.isBlank(idempotencyKey)) {
            throw new ValidationException("IDEMPOTENCY_KEY_REQUIRED", "Idempotency-Key header is required");
        }

        if (request == null) {
            throw new ValidationException("INVALID_REQUEST", "Customer transaction request is required");
        }

        if (StringUtils.isAnyBlank(
                request.getCustomerId(),
                request.getCustomerName(),
                request.getAccountNumber(),
                request.getTransactionReference())) {
            throw new ValidationException("INVALID_REQUEST", "Customer, account, and transaction fields are required");
        }
    }

    private CustomerTransactionResponse toResponse(CustomerTransactionDetail transactionDetail, String message) {
        return CustomerTransactionResponse.builder()
                .id(transactionDetail.getId())
                .customerId(transactionDetail.getCustomerId())
                .customerName(transactionDetail.getCustomerName())
                .accountNumber(transactionDetail.getAccountNumber())
                .transactionReference(transactionDetail.getTransactionReference())
                .transactionAmount(transactionDetail.getTransactionAmount())
                .currentBalance(transactionDetail.getCurrentBalance())
                .message(message)
                .createdAt(transactionDetail.getCreatedAt())
                .build();
    }
}
