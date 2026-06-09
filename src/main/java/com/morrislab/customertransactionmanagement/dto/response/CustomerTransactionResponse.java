package com.morrislab.customertransactionmanagement.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerTransactionResponse {

    private Long id;
    private String customerId;
    private String customerName;
    private String accountNumber;
    private String transactionReference;
    private BigDecimal transactionAmount;
    private BigDecimal currentBalance;
    private String message;
    private LocalDateTime createdAt;
}
