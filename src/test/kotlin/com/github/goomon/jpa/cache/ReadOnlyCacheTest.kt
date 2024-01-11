package com.github.goomon.jpa.cache

import com.github.goomon.jpa.common.AbstractTest
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.cache.ehcache.internal.EhcacheRegionFactory
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.lang.UnsupportedOperationException
import java.util.Properties
import javax.persistence.Cacheable
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

class ReadOnlyCacheTest : AbstractTest() {
    override fun properties(): Properties {
        val props = super.properties()
        props["hibernate.cache.use_second_level_cache"] = true
        props["hibernate.cache.region.factory_class"] = EhcacheRegionFactory::class.java.name
        return props
    }

    @DisplayName("READ_ONLY 캐시 기본 동작 테스트")
    @Test
    fun readOnlyCacheTest() {
        // initial state
        doInJPA { entityManager -> entityManager.persist(Account(id = 1L, balance = 0)) }
        assertSoftly {
            getCacheRegionStatisticsEntries(Account::class.java.name).apply {
                hitCount shouldBe 0
                missCount shouldBe 0
                putCount shouldBe 1
            }
        }

        // cache hit
        doInJPA { entityManager -> entityManager.find(Account::class.java, 1L) }
        assertSoftly {
            getCacheRegionStatisticsEntries(Account::class.java.name).apply {
                hitCount shouldBe 1
                missCount shouldBe 0
                putCount shouldBe 1
            }
        }

        // cache hit
        doInJPA { entityManager -> entityManager.find(Account::class.java, 1L) }
        assertSoftly {
            getCacheRegionStatisticsEntries(Account::class.java.name).apply {
                hitCount shouldBe 2
                missCount shouldBe 0
                putCount shouldBe 1
            }
        }
    }

    @DisplayName("IDENTITY인 경우 READ_ONLY 캐시 기본 동작 테스트")
    @Test
    fun readOnlyCacheWithIdentityGenerationTypeTest() {
        // initial state
        doInJPA { entityManager -> entityManager.persist(AccountIdentityGenerationType(balance = 0)) }
        assertSoftly {
            getCacheRegionStatisticsEntries(AccountIdentityGenerationType::class.java.name).apply {
                hitCount shouldBe 0
                missCount shouldBe 0
                putCount shouldBe 0
            }
        }

        // cache miss
        doInJPA { entityManager -> entityManager.find(AccountIdentityGenerationType::class.java, 1L) }
        assertSoftly {
            getCacheRegionStatisticsEntries(AccountIdentityGenerationType::class.java.name).apply {
                hitCount shouldBe 0
                missCount shouldBe 1
                putCount shouldBe 1
            }
        }

        // cache hit
        doInJPA { entityManager -> entityManager.find(AccountIdentityGenerationType::class.java, 1L) }
        assertSoftly {
            getCacheRegionStatisticsEntries(AccountIdentityGenerationType::class.java.name).apply {
                hitCount shouldBe 1
                missCount shouldBe 1
                putCount shouldBe 1
            }
        }
    }

    @DisplayName("READ_ONLY 캐시에서 엔터티를 수정하면 예외가 발생한다.")
    @Test
    fun readOnlyCacheUpdateTest() {
        doInJPA { entityManager -> entityManager.persist(Account(balance = 0)) }
        doInJPA { entityManager ->
            val account = entityManager.find(Account::class.java, 1L)
            account.balance += 10

            shouldThrow<UnsupportedOperationException> {
                entityManager.flush()
            }
        }
    }

    @DisplayName("1차 캐시에서 hit이 발생한 경우 2차 캐시의 hitCount는 올라가지 않는다.")
    @Test
    fun cacheHitInFirstCacheTest() {
        doInJPA { entityManager -> entityManager.persist(Account(id = 1L, balance = 0)) }
        doInJPA { entityManager ->
            repeat(5) {
                entityManager.find(Account::class.java, 1L)
            }
        }
        assertSoftly {
            getCacheRegionStatisticsEntries(Account::class.java.name).apply {
                hitCount shouldBe 1
                missCount shouldBe 0
                putCount shouldBe 1
            }
        }
    }

    @Entity(name = "Account")
    @Table(name = "account")
    @Cacheable
    @Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
    class Account(
        @Id
        var id: Long = 0,

        @Column(name = "balance")
        var balance: Long
    )

    @Entity(name = "AccountIdentityGenerationType")
    @Table(name = "account_identity_generation_type")
    @Cacheable
    @Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
    class AccountIdentityGenerationType(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long = 0,

        @Column(name = "balance")
        var balance: Long
    )
}
