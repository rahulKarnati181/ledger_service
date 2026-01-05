package com.wallet.ledger_service.repository;

import com.wallet.ledger_service.Domain.PaymentEvent;
import com.wallet.ledger_service.Domain.PaymentIntent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentEventJpaRepository extends JpaRepository<PaymentEvent,String> {

    Optional<PaymentEvent> findByProviderAndProviderEventId(PaymentIntent.Provider provider, String ProviderEventId);

    List<PaymentEvent>  findByProviderEventId(String ProviderEventId);
}
