package com.wallet.ledger_service.Domain;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;


@Entity
@Table(name = "payment_intents")
@Getter
@Setter
public class PaymentIntent {
    public enum PaymentStatus{
        CREATED, PROCESSING, SUCCEEDED, FAILED, CANCELLED;

        public boolean canTransitionTo(PaymentStatus target){
            if(this==CREATED){
                return target==PROCESSING||target==CANCELLED;
            }
            if(this==PROCESSING){
                return target==SUCCEEDED||target==FAILED;
            }
            return false;
        }
    }
    public enum Provider{
        STRIPE, RAZORPAY, MOCK
    }


    @Id
    private String id;
    private String accountId;
    private BigDecimal amount;
    private String currency;
    @Version
    private long version;

    @Enumerated(EnumType.STRING)
    @Setter(AccessLevel.NONE)
    private PaymentStatus status=PaymentStatus.CREATED;

    @Enumerated(EnumType.STRING)
    private Provider provider;
    private String providerPaymentId;
    private String idempotencyKey;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    private String walletOperationId;

    public void transitionTo(PaymentStatus newstatus){
        if(status==null){
            throw new IllegalArgumentException("Payment status not initialized");
        }
        if (newstatus == null) {
            throw new IllegalArgumentException("New payment status cannot be null");
        }

        if(!status.canTransitionTo(newstatus)){
            throw new IllegalStateException( "Invalid payment status transition: " + status + " → " + newstatus );
        }
        this.status=newstatus;
    }

}
