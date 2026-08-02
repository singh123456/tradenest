package com.aakash.tradenest;

import com.aakash.tradenest.user.entity.User;
import com.aakash.tradenest.user.repository.UserRepository;
import com.aakash.tradenest.wallet.entity.Wallet;
import com.aakash.tradenest.wallet.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ActiveProfiles("test")
public class WalletConcurrencyTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    void concurrent_secondSaveFailsWithOptimisticLockException(){

        User user = userRepository.save(User.builder()
                .name("Concurrency Test")
                .email("concurrent@example.com")
                .passwordHash("hash")
                .build());

        Wallet original = walletRepository.save(Wallet.builder()
                .user(user)
                .balance(BigDecimal.valueOf(1000))
                .build());

        Long walletId = original.getId();

        entityManager.clear();

        Wallet managed = walletRepository.findById(walletId).orElseThrow();

        entityManager.getEntityManager()
                .createNativeQuery("UPDATE wallets SET version = version + 1, balance = balance + 100 WHERE id = :id")
                .setParameter("id", walletId)
                .executeUpdate();

        managed.setBalance(managed.getBalance().subtract(BigDecimal.valueOf(50)));


//        Wallet w1 = walletRepository.findById(walletId).orElseThrow();
//
//        entityManager.detach(w1);
//
//        Wallet w2 = walletRepository.findById(walletId).orElseThrow();
//
//
//        w1.setBalance(w1.getBalance().add(BigDecimal.valueOf(100)));
//        walletRepository.saveAndFlush(w1);
//
//        entityManager.clear();
//
//        w2.setBalance(w2.getBalance().subtract(BigDecimal.valueOf(50)));

        assertThrows(ObjectOptimisticLockingFailureException.class,()->{
            walletRepository.saveAndFlush(managed);
        });
    }
}
