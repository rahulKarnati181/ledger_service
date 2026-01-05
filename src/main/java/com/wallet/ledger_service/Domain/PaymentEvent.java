package com.wallet.ledger_service.Domain;


import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Data
@Entity
@Table(name = "payment_events")
public class PaymentEvent {


    @Id
    private String id;
    @Enumerated(EnumType.STRING)
    private PaymentIntent.Provider provider;
    private String providerEventId;
    private String providerPaymentId;
    private String eventType;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String,Object> payload;

    @CreationTimestamp
    @Column(nullable = false,updatable = false)
    private Instant createdAt;

}
