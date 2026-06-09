package com.morrislab.customertransactionmanagement.service.impl;

import com.morrislab.customertransactionmanagement.dto.request.FundTransferRequest;
import com.morrislab.customertransactionmanagement.dto.response.FundTransferResponse;
import com.morrislab.customertransactionmanagement.entity.CustomerTransactionDetail;
import com.morrislab.customertransactionmanagement.entity.FundTransfer;
import com.morrislab.customertransactionmanagement.entity.TransferStatus;
import com.morrislab.customertransactionmanagement.exception.BusinessException;
import com.morrislab.customertransactionmanagement.exception.ConflictException;
import com.morrislab.customertransactionmanagement.exception.ResourceNotFoundException;
import com.morrislab.customertransactionmanagement.exception.ValidationException;
import com.morrislab.customertransactionmanagement.repository.CustomerTransactionDetailRepository;
import com.morrislab.customertransactionmanagement.repository.FundTransferRepository;
import com.morrislab.customertransactionmanagement.service.FundTransferService;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FundTransferServiceImpl implements FundTransferService {

    private final CustomerTransactionDetailRepository customerTransactionDetailRepository;
    private final FundTransferRepository fundTransferRepository;

    @Override
    @Transactional
    public FundTransferResponse transferFunds(FundTransferRequest request, String idempotencyKey) {
        validateRequest(request, idempotencyKey);

        return fundTransferRepository.findByIdempotencyKey(idempotencyKey)
                .map(existingTransfer -> handleIdempotentReplay(existingTransfer, request))
                .orElseGet(() -> processNewTransfer(request, idempotencyKey));
    }

    private FundTransferResponse processNewTransfer(FundTransferRequest request, String idempotencyKey) {
        if (fundTransferRepository.existsByTransferReference(request.getTransferReference())) {
            throw new ConflictException("TRANSFER_REFERENCE_ALREADY_EXISTS", "Transfer reference already exists");
        }

        log.info("Processing fund transfer reference {}", request.getTransferReference());

        LockedAccounts lockedAccounts = lockAccounts(request.getSourceAccountNumber(), request.getDestinationAccountNumber());
        CustomerTransactionDetail sourceAccount = lockedAccounts.sourceAccount();
        CustomerTransactionDetail destinationAccount = lockedAccounts.destinationAccount();

        if (sourceAccount.getCurrentBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessException("INSUFFICIENT_FUNDS", "Source account has insufficient balance");
        }

        BigDecimal sourceBalanceAfterTransfer = sourceAccount.getCurrentBalance().subtract(request.getAmount());
        BigDecimal destinationBalanceAfterTransfer = destinationAccount.getCurrentBalance().add(request.getAmount());

        sourceAccount.setCurrentBalance(sourceBalanceAfterTransfer);
        destinationAccount.setCurrentBalance(destinationBalanceAfterTransfer);

        FundTransfer fundTransfer = FundTransfer.builder()
                .transferReference(request.getTransferReference())
                .idempotencyKey(idempotencyKey)
                .sourceAccountNumber(request.getSourceAccountNumber())
                .destinationAccountNumber(request.getDestinationAccountNumber())
                .amount(request.getAmount())
                .sourceBalanceAfterTransfer(sourceBalanceAfterTransfer)
                .destinationBalanceAfterTransfer(destinationBalanceAfterTransfer)
                .status(TransferStatus.SUCCESSFUL)
                .build();

        FundTransfer savedTransfer = fundTransferRepository.save(fundTransfer);
        log.info("Fund transfer reference {} completed successfully", request.getTransferReference());

        return toResponse(savedTransfer, "Funds transferred successfully");
    }

    private LockedAccounts lockAccounts(String sourceAccountNumber, String destinationAccountNumber) {
        if (sourceAccountNumber.compareTo(destinationAccountNumber) <= 0) {
            CustomerTransactionDetail sourceAccount = findAccountForUpdate(sourceAccountNumber, "SOURCE_ACCOUNT_NOT_FOUND");
            CustomerTransactionDetail destinationAccount =
                    findAccountForUpdate(destinationAccountNumber, "DESTINATION_ACCOUNT_NOT_FOUND");
            return new LockedAccounts(sourceAccount, destinationAccount);
        }

        CustomerTransactionDetail destinationAccount =
                findAccountForUpdate(destinationAccountNumber, "DESTINATION_ACCOUNT_NOT_FOUND");
        CustomerTransactionDetail sourceAccount = findAccountForUpdate(sourceAccountNumber, "SOURCE_ACCOUNT_NOT_FOUND");
        return new LockedAccounts(sourceAccount, destinationAccount);
    }

    private CustomerTransactionDetail findAccountForUpdate(String accountNumber, String errorCode) {
        return customerTransactionDetailRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(errorCode, "Account number does not exist: " + accountNumber));
    }

    private FundTransferResponse handleIdempotentReplay(FundTransfer existingTransfer, FundTransferRequest request) {
        boolean sameRequest = StringUtils.equals(existingTransfer.getSourceAccountNumber(), request.getSourceAccountNumber())
                && StringUtils.equals(existingTransfer.getDestinationAccountNumber(), request.getDestinationAccountNumber())
                && StringUtils.equals(existingTransfer.getTransferReference(), request.getTransferReference())
                && existingTransfer.getAmount().compareTo(request.getAmount()) == 0;

        if (!sameRequest) {
            throw new ConflictException(
                    "IDEMPOTENCY_KEY_REUSED",
                    "Idempotency key was already used for a different transfer request");
        }

        return toResponse(existingTransfer, "Funds transfer already processed for this idempotency key");
    }

    private void validateRequest(FundTransferRequest request, String idempotencyKey) {
        if (StringUtils.isBlank(idempotencyKey)) {
            throw new ValidationException("IDEMPOTENCY_KEY_REQUIRED", "Idempotency-Key header is required");
        }

        if (Objects.isNull(request)) {
            throw new ValidationException("INVALID_TRANSFER_REQUEST", "Fund transfer request is required");
        }

        if (StringUtils.isAnyBlank(
                request.getSourceAccountNumber(),
                request.getDestinationAccountNumber(),
                request.getTransferReference())) {
            throw new ValidationException(
                    "INVALID_TRANSFER_REQUEST",
                    "Source account, destination account, and transfer reference are required");
        }

        if (StringUtils.equals(request.getSourceAccountNumber(), request.getDestinationAccountNumber())) {
            throw new ValidationException(
                    "SAME_SOURCE_AND_DESTINATION_ACCOUNT",
                    "Source and destination accounts cannot be the same");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("INVALID_TRANSFER_AMOUNT", "Transfer amount must be greater than zero");
        }
    }

    private FundTransferResponse toResponse(FundTransfer fundTransfer, String message) {
        return FundTransferResponse.builder()
                .id(fundTransfer.getId())
                .transferReference(fundTransfer.getTransferReference())
                .sourceAccountNumber(fundTransfer.getSourceAccountNumber())
                .destinationAccountNumber(fundTransfer.getDestinationAccountNumber())
                .amount(fundTransfer.getAmount())
                .sourceBalance(fundTransfer.getSourceBalanceAfterTransfer())
                .destinationBalance(fundTransfer.getDestinationBalanceAfterTransfer())
                .status(fundTransfer.getStatus().name())
                .message(message)
                .createdAt(fundTransfer.getCreatedAt())
                .build();
    }

    private record LockedAccounts(
            CustomerTransactionDetail sourceAccount,
            CustomerTransactionDetail destinationAccount) {
    }
}
