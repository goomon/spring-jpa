package com.github.goomon.jpa.cache

import com.github.goomon.jpa.common.AbstractTest
import io.kotest.matchers.shouldBe
import org.hibernate.annotations.Immutable
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

class ImmutableEntityTest : AbstractTest() {
    @DisplayName("@Immutable 엔터티는 업데이트 쿼리가 실행되지 않는다.")
    @Test
    fun immutableEntityTest() {
        doInJPA { entityManager ->
            entityManager.persist(Account(balance = 0))
            entityManager.persist(AccountImmutable(balance = 0))
        }

        doInJPA { entityManager ->
            val account = entityManager.find(Account::class.java, 1L)
            account.balance = 10
            entityManager.merge(account)

            val accountImmutable = entityManager.find(AccountImmutable::class.java, 1L)
            accountImmutable.balance = 10
        }

        doInJPA { entityManager ->
            val account = entityManager.find(Account::class.java, 1L)
            account.balance shouldBe 10

            val accountImmutable = entityManager.find(AccountImmutable::class.java, 1L)
            accountImmutable.balance shouldBe 0
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

    @Entity(name = "AccountImmutable")
    @Table(name = "account_immutable")
    @Immutable
    class AccountImmutable(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long = 0,

        @Column(name = "balance")
        var balance: Long
    )
}
