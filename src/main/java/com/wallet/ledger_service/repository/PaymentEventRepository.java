package com.wallet.ledger_service.repository;

import com.wallet.ledger_service.Domain.PaymentEvent;
import com.wallet.ledger_service.Domain.PaymentIntent;

import java.util.List;
import java.util.Optional;

public interface PaymentEventRepository {

    PaymentEvent save(PaymentEvent paymentEvent);

    Optional<PaymentEvent> findById(String id);

    List<PaymentEvent> findByProviderEventId(String ProviderEventId);

    List<PaymentEvent> findAll();

    Optional<PaymentEvent> findByProviderAndProviderEventId(PaymentIntent.Provider provider, String ProviderEventId);

}
