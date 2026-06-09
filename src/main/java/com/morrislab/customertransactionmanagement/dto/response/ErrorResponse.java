package com.morrislab.customertransactionmanagement.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorResponse {

    private String code;
    private String message;
    private LocalDateTime timestamp;
}
