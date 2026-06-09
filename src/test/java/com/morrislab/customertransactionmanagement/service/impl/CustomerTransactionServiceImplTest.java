package com.morrislab.customertransactionmanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.morrislab.customertransactionmanagement.dto.request.CustomerTransactionRequest;
import com.morrislab.customertransactionmanagement.dto.response.CustomerBalanceResponse;
import com.morrislab.customertransactionmanagement.dto.response.CustomerTransactionResponse;
import com.morrislab.customertransactionmanagement.entity.CustomerTransactionDetail;
import com.morrislab.customertransactionmanagement.exception.ConflictException;
import com.morrislab.customertransactionmanagement.exception.ResourceNotFoundException;
import com.morrislab.customertransactionmanagement.exception.ValidationException;
import com.morrislab.customertransactionmanagement.repository.CustomerTransactionDetailRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerTransactionServiceImplTest {

    private static final String IDEMPOTENCY_KEY = "req-123";

    @Mock
    private CustomerTransactionDetailRepository repository;

    private CustomerTransactionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CustomerTransactionServiceImpl(repository);
    }

    @Test
    void shouldSaveCustomerTransactionDetailsSuccessfully() {
        CustomerTransactionRequest request = validRequest();
        CustomerTransactionDetail savedTransaction = transactionDetail();

        when(repository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(repository.existsByAccountNumber(request.getAccountNumber())).thenReturn(false);
        when(repository.existsByTransactionReference(request.getTransactionReference())).thenReturn(false);
        when(repository.save(any(CustomerTransactionDetail.class))).thenReturn(savedTransaction);

        CustomerTransactionResponse response =
                service.saveCustomerTransactionDetails(request, IDEMPOTENCY_KEY);

        assertEquals("CUST001", response.getCustomerId());
        assertEquals("ACC001", response.getAccountNumber());
        assertEquals("Customer transaction details saved successfully", response.getMessage());
        verify(repository).save(any(CustomerTransactionDetail.class));
    }

    @Test
    void shouldReturnExistingTransactionWhenIdempotencyKeyAlreadyProcessed() {
        CustomerTransactionDetail existingTransaction = transactionDetail();

        when(repository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.of(existingTransaction));

        CustomerTransactionResponse response =
                service.saveCustomerTransactionDetails(validRequest(), IDEMPOTENCY_KEY);

        assertEquals("Customer transaction details already processed for this idempotency key", response.getMessage());
        verify(repository, never()).save(any(CustomerTransactionDetail.class));
    }

    @Test
    void shouldThrowWhenIdempotencyKeyIsBlank() {
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.saveCustomerTransactionDetails(validRequest(), " "));

        assertEquals("IDEMPOTENCY_KEY_REQUIRED", exception.getCode());
    }

    @Test
    void shouldThrowWhenAccountNumberAlreadyExists() {
        CustomerTransactionRequest request = validRequest();

        when(repository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(repository.existsByAccountNumber(request.getAccountNumber())).thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> service.saveCustomerTransactionDetails(request, IDEMPOTENCY_KEY));

        assertEquals("ACCOUNT_ALREADY_EXISTS", exception.getCode());
        verify(repository, never()).save(any(CustomerTransactionDetail.class));
    }

    @Test
    void shouldRetrieveCustomerBalanceSuccessfully() {
        CustomerTransactionDetail transactionDetail = transactionDetail();

        when(repository.findByAccountNumber("ACC001")).thenReturn(Optional.of(transactionDetail));

        CustomerBalanceResponse response = service.getCustomerBalance("ACC001");

        assertEquals("CUST001", response.getCustomerId());
        assertEquals("ACC001", response.getAccountNumber());
        assertEquals(new BigDecimal("1500.00"), response.getCurrentBalance());
        assertEquals("Current account balance retrieved successfully", response.getMessage());
    }

    @Test
    void shouldThrowWhenBalanceAccountNotFound() {
        when(repository.findByAccountNumber("UNKNOWN")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.getCustomerBalance("UNKNOWN"));

        assertEquals("ACCOUNT_NOT_FOUND", exception.getCode());
    }

    @Test
    void shouldThrowWhenBalanceAccountNumberIsBlank() {
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.getCustomerBalance(" "));

        assertEquals("ACCOUNT_NUMBER_REQUIRED", exception.getCode());
    }

    private CustomerTransactionRequest validRequest() {
        CustomerTransactionRequest request = new CustomerTransactionRequest();
        request.setCustomerId("CUST001");
        request.setCustomerName("Jane Doe");
        request.setAccountNumber("ACC001");
        request.setTransactionReference("TXN001");
        request.setTransactionAmount(new BigDecimal("500.00"));
        request.setCurrentBalance(new BigDecimal("1500.00"));
        return request;
    }

    private CustomerTransactionDetail transactionDetail() {
        CustomerTransactionDetail transactionDetail = CustomerTransactionDetail.builder()
                .customerId("CUST001")
                .customerName("Jane Doe")
                .accountNumber("ACC001")
                .transactionReference("TXN001")
                .idempotencyKey(IDEMPOTENCY_KEY)
                .transactionAmount(new BigDecimal("500.00"))
                .currentBalance(new BigDecimal("1500.00"))
                .build();
        transactionDetail.setId(1L);
        return transactionDetail;
    }
}
