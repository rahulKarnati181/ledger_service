package com.wallet.ledger_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.ledger_service.Domain.LedgerEntry;
import com.wallet.ledger_service.Domain.PaymentEvent;
import com.wallet.ledger_service.Domain.PaymentIntent;
import com.wallet.ledger_service.dto.CheckoutRequestDto;
import com.wallet.ledger_service.dto.CheckoutResponseDto;
import com.wallet.ledger_service.exception.ConflictException;
import com.wallet.ledger_service.provider.MockPaymentProvider;
import com.wallet.ledger_service.repository.JpaPaymentEventRepository;
import com.wallet.ledger_service.repository.JpaPaymentIntentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PaymentServiceTest {

    @Autowired
    private JpaPaymentIntentRepository jpaPaymentIntentRepository;

    @Autowired
    private JpaPaymentEventRepository jpaPaymentEventRepository;

    @Autowired
    private WalletService walletService; // real or minimal in-memory

    private PaymentService paymentService;
    private MockPaymentProvider paymentProvider;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        paymentProvider = new MockPaymentProvider(objectMapper);


        paymentService = new PaymentService(
                jpaPaymentIntentRepository,
                jpaPaymentEventRepository,
                walletService,
                paymentProvider,
                objectMapper
        );
    }
    @Test
    @Transactional
    void createIntent(){
        CheckoutRequestDto checkoutRequestDto=new CheckoutRequestDto();
        checkoutRequestDto.setAccountId("9839b7d3-331a-42c5-9c18-a32b47938a85");
        checkoutRequestDto.setCurrency("INR");
        checkoutRequestDto.setAmount(BigDecimal.valueOf(500));
        checkoutRequestDto.setProvider(PaymentIntent.Provider.MOCK);
        String idempotencyKey= "K1";
        CheckoutResponseDto checkoutResponseDto=paymentService.checkoutflow(checkoutRequestDto.getAccountId(),checkoutRequestDto.getAmount(),
                checkoutRequestDto.getCurrency(),checkoutRequestDto.getProvider(),idempotencyKey);

        Optional<PaymentIntent> intentFromDb = jpaPaymentIntentRepository
                .findByAccountIdAndIdempotencyKey(checkoutRequestDto.getAccountId(), idempotencyKey);
        assertTrue(intentFromDb.isPresent(), "PaymentIntent row should exist in DB");
        PaymentIntent paymentIntent = intentFromDb.get();

        assertEquals(PaymentIntent.PaymentStatus.PROCESSING, paymentIntent.getStatus());
        assertNotNull(paymentIntent.getProviderPaymentId());
        assertFalse(paymentIntent.getProviderPaymentId().isBlank());

        assertFalse(checkoutResponseDto.getProviderPaymentId().isBlank());
        assertFalse(checkoutResponseDto.getPaymentIntentId().isBlank());
        assertEquals(PaymentIntent.PaymentStatus.PROCESSING, checkoutResponseDto.getStatus());

        CheckoutResponseDto checkoutResponseDto1=paymentService.checkoutflow(checkoutRequestDto.getAccountId(),checkoutRequestDto.getAmount(),
                checkoutRequestDto.getCurrency(),checkoutRequestDto.getProvider(),idempotencyKey);
        assertFalse(checkoutResponseDto1.getProviderPaymentId().isBlank());
        assertFalse(checkoutResponseDto1.getPaymentIntentId().isBlank());
        assertEquals(PaymentIntent.PaymentStatus.PROCESSING, checkoutResponseDto1.getStatus());



    }
    @Test
    @Transactional
    void idempotencyKeyReuseWithDifferentDetailsThrowsConflict() {

        String accountId = "9839b7d3-331a-42c5-9c18-a32b47938a85";
        String idempotencyKey = "K2";

        // --- First checkout (valid) ---
        CheckoutResponseDto first =
                paymentService.checkoutflow(
                        accountId,
                        BigDecimal.valueOf(100),
                        "INR",
                        PaymentIntent.Provider.MOCK,
                        idempotencyKey
                );

        assertNotNull(first.getPaymentIntentId());
        assertEquals(PaymentIntent.PaymentStatus.PROCESSING, first.getStatus());

        // --- Second checkout with SAME key but DIFFERENT amount ---
        ConflictException ex = assertThrows(
                ConflictException.class,
                () -> paymentService.checkoutflow(
                        accountId,
                        BigDecimal.valueOf(200),   // changed
                        "INR",
                        PaymentIntent.Provider.MOCK,
                        idempotencyKey
                )
        );

        assertTrue(
                ex.getMessage().contains("Idempotency key already used"),
                "Expected idempotency conflict message"
        );

        // --- DB must remain unchanged ---
        Optional<PaymentIntent> persisted =
                jpaPaymentIntentRepository
                        .findByAccountIdAndIdempotencyKey(accountId, idempotencyKey);

        assertTrue(persisted.isPresent(), "Original intent must still exist");

        PaymentIntent intent = persisted.get();

        assertEquals(BigDecimal.valueOf(100), intent.getAmount()); // NOT 200
        assertEquals("INR", intent.getCurrency());
        assertEquals(PaymentIntent.Provider.MOCK, intent.getProvider());
    }

    @Test
    @Transactional
    void webhookSucceededCreditsWalletExactlyOnce() {

        String accountId = "9839b7d3-331a-42c5-9c18-a32b47938a85";
        BigDecimal amount = BigDecimal.valueOf(500);
        String idempotencyKey = "K3";

        // --- Initial balance ---
        BigDecimal balanceBefore = walletService.getBalance(accountId).getCurrentBalance();

        // --- Checkout ---
        CheckoutResponseDto checkout =
                paymentService.checkoutflow(
                        accountId,
                        amount,
                        "INR",
                        PaymentIntent.Provider.MOCK,
                        idempotencyKey
                );

        String providerPaymentId = checkout.getProviderPaymentId();
        String paymentIntentId = checkout.getPaymentIntentId();

        assertNotNull(providerPaymentId);
        assertNotNull(paymentIntentId);

        // --- Build webhook payload (MOCK provider) ---
        String webhookPayload =
                """
                {
                  "id": "evt_1",
                  "type": "payment_intent.succeeded",
                  "data": {
                    "object": {
                      "id": "%s",
                      "status": "succeeded"
                    }
                  }
                }
                """.formatted(providerPaymentId);

        // --- Call webhook ---
        paymentService.handleWebhook(PaymentIntent.Provider.MOCK, webhookPayload);

        // --- Reload intent ---
        PaymentIntent intent =
                jpaPaymentIntentRepository
                        .findByAccountIdAndIdempotencyKey(accountId, idempotencyKey)
                        .orElseThrow();

        assertEquals(PaymentIntent.PaymentStatus.SUCCEEDED, intent.getStatus());
        assertNotNull(intent.getWalletOperationId());

        // --- Balance must increase exactly once ---
        BigDecimal balanceAfter = walletService.getBalance(accountId).getCurrentBalance();
        assertEquals(balanceBefore.add(amount), balanceAfter);

        // --- Ledger entry must exist for this payment intent ---
        Optional<LedgerEntry> creditEntry =
                walletService.findCreditForPaymentIntent(accountId, paymentIntentId);

        assertTrue(creditEntry.isPresent(), "Ledger credit must exist");

        LedgerEntry entry = creditEntry.get();
        assertEquals(amount, entry.getAmount());
        assertEquals(LedgerEntry.moneyDirection.CREDIT, entry.getDirection());
        assertEquals("PAYMENT_INTENT", entry.getReferenceType());
        assertEquals(paymentIntentId, entry.getReferenceId());
    }







}
