package com.morrislab.customertransactionmanagement.service;

import com.morrislab.customertransactionmanagement.dto.request.CustomerTransactionRequest;
import com.morrislab.customertransactionmanagement.dto.response.CustomerBalanceResponse;
import com.morrislab.customertransactionmanagement.dto.response.CustomerTransactionResponse;

public interface CustomerTransactionService {

    CustomerTransactionResponse saveCustomerTransactionDetails(
            CustomerTransactionRequest request,
            String idempotencyKey);

    CustomerBalanceResponse getCustomerBalance(String accountNumber);
}
