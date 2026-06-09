package com.morrislab.customertransactionmanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.morrislab.customertransactionmanagement.dto.request.FundTransferRequest;
import com.morrislab.customertransactionmanagement.dto.response.FundTransferResponse;
import com.morrislab.customertransactionmanagement.entity.CustomerTransactionDetail;
import com.morrislab.customertransactionmanagement.entity.FundTransfer;
import com.morrislab.customertransactionmanagement.entity.TransferStatus;
import com.morrislab.customertransactionmanagement.exception.BusinessException;
import com.morrislab.customertransactionmanagement.exception.ConflictException;
import com.morrislab.customertransactionmanagement.exception.ValidationException;
import com.morrislab.customertransactionmanagement.repository.CustomerTransactionDetailRepository;
import com.morrislab.customertransactionmanagement.repository.FundTransferRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FundTransferServiceImplTest {

    private static final String IDEMPOTENCY_KEY = "transfer-key-001";

    @Mock
    private CustomerTransactionDetailRepository customerTransactionDetailRepository;

    @Mock
    private FundTransferRepository fundTransferRepository;

    private FundTransferServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FundTransferServiceImpl(customerTransactionDetailRepository, fundTransferRepository);
    }

    @Test
    void shouldTransferFundsSuccessfully() {
        FundTransferRequest request = validRequest();
        CustomerTransactionDetail sourceAccount = account("ACC001", "1000.00");
        CustomerTransactionDetail destinationAccount = account("ACC002", "250.00");

        when(fundTransferRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(fundTransferRepository.existsByTransferReference(request.getTransferReference())).thenReturn(false);
        when(customerTransactionDetailRepository.findByAccountNumberForUpdate("ACC001"))
                .thenReturn(Optional.of(sourceAccount));
        when(customerTransactionDetailRepository.findByAccountNumberForUpdate("ACC002"))
                .thenReturn(Optional.of(destinationAccount));
        when(fundTransferRepository.save(any(FundTransfer.class))).thenAnswer(invocation -> {
            FundTransfer transfer = invocation.getArgument(0);
            transfer.setId(1L);
            return transfer;
        });

        FundTransferResponse response = service.transferFunds(request, IDEMPOTENCY_KEY);

        assertEquals(new BigDecimal("900.00"), sourceAccount.getCurrentBalance());
        assertEquals(new BigDecimal("350.00"), destinationAccount.getCurrentBalance());
        assertEquals("Funds transferred successfully", response.getMessage());
        assertEquals("SUCCESSFUL", response.getStatus());
        verify(fundTransferRepository).save(any(FundTransfer.class));
    }

    @Test
    void shouldReturnExistingTransferWhenIdempotencyKeyAlreadyProcessed() {
        FundTransfer existingTransfer = transfer();

        when(fundTransferRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.of(existingTransfer));

        FundTransferResponse response = service.transferFunds(validRequest(), IDEMPOTENCY_KEY);

        assertEquals("Funds transfer already processed for this idempotency key", response.getMessage());
        verify(customerTransactionDetailRepository, never()).findByAccountNumberForUpdate(any());
        verify(fundTransferRepository, never()).save(any(FundTransfer.class));
    }

    @Test
    void shouldRejectReusedIdempotencyKeyForDifferentRequest() {
        FundTransfer existingTransfer = transfer();
        FundTransferRequest request = validRequest();
        request.setAmount(new BigDecimal("200.00"));

        when(fundTransferRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.of(existingTransfer));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> service.transferFunds(request, IDEMPOTENCY_KEY));

        assertEquals("IDEMPOTENCY_KEY_REUSED", exception.getCode());
    }

    @Test
    void shouldRejectSameSourceAndDestinationAccount() {
        FundTransferRequest request = validRequest();
        request.setDestinationAccountNumber("ACC001");

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.transferFunds(request, IDEMPOTENCY_KEY));

        assertEquals("SAME_SOURCE_AND_DESTINATION_ACCOUNT", exception.getCode());
        verify(fundTransferRepository, never()).save(any(FundTransfer.class));
    }

    @Test
    void shouldRejectInsufficientFunds() {
        FundTransferRequest request = validRequest();
        CustomerTransactionDetail sourceAccount = account("ACC001", "50.00");
        CustomerTransactionDetail destinationAccount = account("ACC002", "250.00");

        when(fundTransferRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(fundTransferRepository.existsByTransferReference(request.getTransferReference())).thenReturn(false);
        when(customerTransactionDetailRepository.findByAccountNumberForUpdate("ACC001"))
                .thenReturn(Optional.of(sourceAccount));
        when(customerTransactionDetailRepository.findByAccountNumberForUpdate("ACC002"))
                .thenReturn(Optional.of(destinationAccount));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.transferFunds(request, IDEMPOTENCY_KEY));

        assertEquals("INSUFFICIENT_FUNDS", exception.getCode());
        assertEquals(new BigDecimal("50.00"), sourceAccount.getCurrentBalance());
        assertEquals(new BigDecimal("250.00"), destinationAccount.getCurrentBalance());
        verify(fundTransferRepository, never()).save(any(FundTransfer.class));
    }

    private FundTransferRequest validRequest() {
        FundTransferRequest request = new FundTransferRequest();
        request.setSourceAccountNumber("ACC001");
        request.setDestinationAccountNumber("ACC002");
        request.setTransferReference("TRF001");
        request.setAmount(new BigDecimal("100.00"));
        return request;
    }

    private CustomerTransactionDetail account(String accountNumber, String balance) {
        return CustomerTransactionDetail.builder()
                .customerId("CUST-" + accountNumber)
                .customerName("Customer " + accountNumber)
                .accountNumber(accountNumber)
                .transactionReference("TXN-" + accountNumber)
                .idempotencyKey("KEY-" + accountNumber)
                .transactionAmount(BigDecimal.ZERO)
                .currentBalance(new BigDecimal(balance))
                .build();
    }

    private FundTransfer transfer() {
        FundTransfer transfer = FundTransfer.builder()
                .transferReference("TRF001")
                .idempotencyKey(IDEMPOTENCY_KEY)
                .sourceAccountNumber("ACC001")
                .destinationAccountNumber("ACC002")
                .amount(new BigDecimal("100.00"))
                .sourceBalanceAfterTransfer(new BigDecimal("900.00"))
                .destinationBalanceAfterTransfer(new BigDecimal("350.00"))
                .status(TransferStatus.SUCCESSFUL)
                .build();
        transfer.setId(1L);
        return transfer;
    }
}
