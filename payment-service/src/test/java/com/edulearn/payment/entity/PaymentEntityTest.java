package com.edulearn.payment.entity;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class PaymentEntityTest {

    @Test
    void walletPreUpdate_SetsUpdatedAt() {
        Wallet wallet = new Wallet();

        ReflectionTestUtils.invokeMethod(wallet, "onUpdate");

        assertNotNull(wallet.getUpdatedAt());
    }

    @Test
    void walletTransactionPrePersist_SetsTimestamp() {
        WalletTransaction transaction = new WalletTransaction();

        ReflectionTestUtils.invokeMethod(transaction, "onCreate");

        assertNotNull(transaction.getTimestamp());
    }
}
