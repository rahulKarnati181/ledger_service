package com.wallet.ledger_service.dto;

import lombok.Data;

@Data
public class ErrorResponse {
    private String errorCode;
    private String message;
}
