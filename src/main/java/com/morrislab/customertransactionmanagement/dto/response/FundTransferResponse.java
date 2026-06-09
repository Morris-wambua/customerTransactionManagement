package com.morrislab.customertransactionmanagement.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FundTransferResponse {

    private Long id;
    private String transferReference;
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private BigDecimal amount;
    private BigDecimal sourceBalance;
    private BigDecimal destinationBalance;
    private String status;
    private String message;
    private LocalDateTime createdAt;
}
