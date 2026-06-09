package com.morrislab.customertransactionmanagement.controller;

import com.morrislab.customertransactionmanagement.dto.request.FundTransferRequest;
import com.morrislab.customertransactionmanagement.dto.response.FundTransferResponse;
import com.morrislab.customertransactionmanagement.service.FundTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/transfers")
@Tag(name = "Fund Transfers", description = "APIs for moving funds between customer accounts")
public class FundTransferController {

    private final FundTransferService fundTransferService;

    @PostMapping
    @Operation(summary = "Transfer funds between two persisted customer accounts")
    public ResponseEntity<FundTransferResponse> transferFunds(
            @Parameter(description = "Unique request key used to safely retry the same transfer")
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody FundTransferRequest request) {
        log.info("Received fund transfer request for reference {}", request.getTransferReference());

        FundTransferResponse response = fundTransferService.transferFunds(request, idempotencyKey);
        HttpStatus status = response.getMessage().contains("already processed") ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(response);
    }
}
