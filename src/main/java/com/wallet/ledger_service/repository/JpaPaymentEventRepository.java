package com.wallet.ledger_service.repository;

import com.wallet.ledger_service.Domain.PaymentEvent;
import com.wallet.ledger_service.Domain.PaymentIntent;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaPaymentEventRepository implements PaymentEventRepository {

    private final PaymentEventJpaRepository paymentEventJpaRepository;
    JpaPaymentEventRepository (PaymentEventJpaRepository paymentEventJpaRepository){
        this.paymentEventJpaRepository=paymentEventJpaRepository;
    }

    @Override
    public PaymentEvent save(PaymentEvent paymentEvent){
        return(paymentEventJpaRepository.save(paymentEvent));
    }

    @Override
    public Optional<PaymentEvent> findById(String id){
        return(paymentEventJpaRepository.findById(id));
    }
    @Override
    public List<PaymentEvent> findAll(){
        return (paymentEventJpaRepository.findAll());
    }

    @Override
    public List<PaymentEvent>  findByProviderEventId(String ProviderEventId){
        return(paymentEventJpaRepository. findByProviderEventId( ProviderEventId));
    }



    @Override
    public Optional<PaymentEvent> findByProviderAndProviderEventId(PaymentIntent.Provider provider, String ProviderEventId){
        return(paymentEventJpaRepository.findByProviderAndProviderEventId(provider,ProviderEventId));
    }


}
