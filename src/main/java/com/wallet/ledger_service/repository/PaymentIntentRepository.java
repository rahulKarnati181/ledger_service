package com.wallet.ledger_service.repository;



import com.wallet.ledger_service.Domain.PaymentIntent;

import java.util.Optional;

public interface PaymentIntentRepository {
    PaymentIntent save(PaymentIntent paymentIntent);

    Optional<PaymentIntent> findById(String paymentIntentId);

    Optional<PaymentIntent> findByAccountIdAndIdempotencyKey(String accountId,String idempotencyKey);



    Optional<PaymentIntent> findByProviderAndProviderPaymentId(PaymentIntent.Provider provider,String providerPaymentId);

}
