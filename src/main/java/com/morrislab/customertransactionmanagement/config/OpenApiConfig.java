package com.morrislab.customertransactionmanagement.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Customer Transaction Management API",
                version = "v1",
                description = "APIs for saving customer transaction details, transferring funds, and checking balances"))
public class OpenApiConfig {
}
