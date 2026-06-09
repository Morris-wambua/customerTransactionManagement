package com.morrislab.customertransactionmanagement.service;

import com.morrislab.customertransactionmanagement.dto.request.FundTransferRequest;
import com.morrislab.customertransactionmanagement.dto.response.FundTransferResponse;

public interface FundTransferService {

    FundTransferResponse transferFunds(FundTransferRequest request, String idempotencyKey);
}
