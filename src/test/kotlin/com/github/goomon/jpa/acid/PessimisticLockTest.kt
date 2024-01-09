package com.github.goomon.jpa.acid

import com.github.goomon.jpa.common.AbstractTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.LockModeType
import jakarta.persistence.LockTimeoutException
import jakarta.persistence.Table
import org.hibernate.LockOptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PessimisticLockTest : AbstractTest() {
    @BeforeEach
    fun init() {
        doInJPA { entityManager ->
            for (i in 1..10) {
                entityManager.persist(Account(balance = 0))
            }
        }
    }

    @DisplayName("LockModeType.PESSIMISTIC_WRITE의 경우 FOR UPDATE가 추가된다.")
    @Test
    fun pessimisticWriteLockTest() {
        doInJPA { entityManager ->
            entityManager.createQuery(
                """
                    select a
                    from Account a
                """.trimIndent()
            ).setLockMode(LockModeType.PESSIMISTIC_WRITE).resultList
        }
    }

    @DisplayName("LockModeType.PESSIMISTIC_READ일 경우 FOR SHARE이 추가된다.")
    @Test
    fun pessimisticReadLockTest() {
        doInJPA { entityManager ->
            entityManager.createQuery(
                """
                    select a
                    from Account a
                """.trimIndent()
            ).setLockMode(LockModeType.PESSIMISTIC_READ).resultList
        }
    }

    @DisplayName("LockOptions.NO_WAIT는 바로 LockTimeoutException예외를 던진다.")
    @Test
    fun lockOptionNoWaitTest() {
        doInJPA { entityManager ->
            entityManager.createQuery(
                """
                    select a
                    from Account a
                """.trimIndent()
            ).setLockMode(LockModeType.PESSIMISTIC_WRITE).resultList

            shouldThrow<LockTimeoutException> {
                doInJPA { _entityManager ->
                    _entityManager.createQuery(
                        """
                            select a
                            from Account a
                        """.trimIndent()
                    ).setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .setHint("jakarta.persistence.lock.timeout", LockOptions.NO_WAIT)
                        .resultList
                }
            }
        }
    }

    @DisplayName("LockOptions.NO_WAIT는 바로 LockTimeoutException예외를 던진다.")
    @Test
    fun lockOptionSkipLockTest() {
        doInJPA { entityManager ->
            entityManager.createQuery(
                """
                    select a
                    from Account a
                    where a.id between :minId and :maxId
                """.trimIndent()
            ).setParameter("minId", 1)
                .setParameter("maxId", 5)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE).resultList.size shouldBe 5

            doInJPA { _entityManager ->
                _entityManager.createQuery(
                    """
                        select a
                        from Account a
                    """.trimIndent()
                ).setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .setHint("jakarta.persistence.lock.timeout", LockOptions.SKIP_LOCKED)
                    .resultList.size shouldBe 5
            }
        }
    }

    @Entity(name = "Account")
    @Table(name = "account")
    class Account(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long = 0,

        @Column(name = "balance")
        var balance: Long
    )
}
