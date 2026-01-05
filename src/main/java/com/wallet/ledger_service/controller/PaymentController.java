package com.wallet.ledger_service.controller;

import com.wallet.ledger_service.Domain.PaymentIntent;
import com.wallet.ledger_service.dto.CheckoutRequestDto;
import com.wallet.ledger_service.dto.CheckoutResponseDto;
import com.wallet.ledger_service.dto.ErrorResponse;
import com.wallet.ledger_service.exception.ConflictException;
import com.wallet.ledger_service.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentService paymentService;
    private static final Logger log =
            LoggerFactory.getLogger(PaymentController.class);


    public PaymentController(PaymentService paymentService){
        this.paymentService=paymentService;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse HandleExp(IllegalArgumentException e){
        ErrorResponse er=new ErrorResponse();
        er.setErrorCode("BAD_REQUEST");
        er.setMessage(e.getMessage());
        return(er);
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse HandleCon(ConflictException e){
        ErrorResponse er = new ErrorResponse();
        er.setErrorCode("CONFLICT");
        er.setMessage(e.getMessage());
        return er;
    }



    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponseDto> checkout(@RequestBody CheckoutRequestDto checkoutRequestDto,
                                         @RequestHeader(value = "Idempotency-Key", required = false)
                                              String idempotencyKey){

        if(idempotencyKey == null ||idempotencyKey.isBlank()){
            throw new IllegalArgumentException("Missing Idempotency Key");
        }

        CheckoutResponseDto checkoutResponseDto=paymentService.checkoutflow(checkoutRequestDto.getAccountId(),checkoutRequestDto.getAmount(),
                checkoutRequestDto.getCurrency(),checkoutRequestDto.getProvider(),idempotencyKey);
        if(checkoutResponseDto.isIdempotencyHit()){
            return ResponseEntity.ok(checkoutResponseDto);
        }else{
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(checkoutResponseDto);
        }
    }

    @PostMapping("/webhook/{provider}")
    public ResponseEntity<Void> webhook(@PathVariable PaymentIntent.Provider provider,
                                        @RequestBody String payload){
        try {
            paymentService.handleWebhook(provider, payload);
        }catch (IllegalArgumentException e){
            throw e;
        } catch (RuntimeException e) {
            log.error(
                    "Webhook processing failed but acknowledged. provider={}, payload={}",
                    provider,
                    payload,
                    e
            );
        }

        return ResponseEntity.ok().build();
    }


}
