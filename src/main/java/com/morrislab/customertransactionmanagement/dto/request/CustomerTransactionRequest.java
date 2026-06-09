package com.morrislab.customertransactionmanagement.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class CustomerTransactionRequest {

    @NotBlank(message = "Customer identifier is required")
    private String customerId;

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotBlank(message = "Transaction reference is required")
    private String transactionReference;

    @NotNull(message = "Transaction amount is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Transaction amount cannot be negative")
    private BigDecimal transactionAmount;

    @NotNull(message = "Current balance is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Current balance cannot be negative")
    private BigDecimal currentBalance;
}
