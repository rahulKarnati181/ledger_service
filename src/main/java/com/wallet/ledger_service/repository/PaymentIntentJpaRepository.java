package com.wallet.ledger_service.repository;

import com.wallet.ledger_service.Domain.PaymentIntent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentIntentJpaRepository extends JpaRepository<PaymentIntent,String> {

    Optional<PaymentIntent> findByAccountIdAndIdempotencyKey(String accountId, String idempotencyKey);

    Optional<PaymentIntent> findByProviderAndProviderPaymentId(PaymentIntent.Provider provider,String providerPaymentId);
}
