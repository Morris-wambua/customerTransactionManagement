package com.morrislab.customertransactionmanagement.controller;

import com.morrislab.customertransactionmanagement.dto.request.CustomerTransactionRequest;
import com.morrislab.customertransactionmanagement.dto.response.CustomerTransactionResponse;
import com.morrislab.customertransactionmanagement.service.CustomerTransactionService;
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
@RequestMapping("/api/v1/customer-transactions")
@Tag(name = "Customer Transactions", description = "APIs for customer transaction management")
public class CustomerTransactionController {

    private final CustomerTransactionService customerTransactionService;

    @PostMapping
    @Operation(summary = "Save customer transaction details")
    public ResponseEntity<CustomerTransactionResponse> saveCustomerTransactionDetails(
            @Parameter(description = "Unique request key used to safely retry the same request")
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CustomerTransactionRequest request) {
        log.info("Received request to save customer transaction details for reference {}",
                request.getTransactionReference());

        CustomerTransactionResponse response =
                customerTransactionService.saveCustomerTransactionDetails(request, idempotencyKey);

        HttpStatus status = response.getMessage().contains("already processed") ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(response);
    }
}
