package com.morrislab.customertransactionmanagement.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerBalanceResponse {

    private String customerId;
    private String customerName;
    private String accountNumber;
    private BigDecimal currentBalance;
    private String message;
    private LocalDateTime asOf;
}
