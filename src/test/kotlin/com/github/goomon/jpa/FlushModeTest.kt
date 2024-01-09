package com.github.goomon.jpa

import com.github.goomon.jpa.common.AbstractTest
import io.kotest.matchers.shouldBe
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.FlushMode
import org.hibernate.Session
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class FlushModeTest : AbstractTest() {
    @Test
    fun flushModeDefault() {
        doInJPA { entityManager ->
            val session = entityManager.unwrap(Session::class.java)

            session.createQuery(
                """
                    select count(p)
                    from Post p
                """.trimIndent(),
                Int::class.java
            ).singleResult shouldBe 0

            session.persist(
                Post(
                    id = 1L,
                    title = "JPA FlushMode test"
                )
            )

            session.createQuery(
                """
                    select count(p)
                    from Post p
                """.trimIndent(),
                Int::class.java
            ).singleResult shouldBe 1
        }
    }

    @Test
    fun flushModeManualWithoutFlushCall() {
        doInJPA { entityManager ->
            val session = entityManager.unwrap(Session::class.java).apply {
                hibernateFlushMode = FlushMode.MANUAL
            }

            session.createQuery(
                """
                    select count(p)
                    from Post p
                """.trimIndent(),
                Int::class.java
            ).singleResult shouldBe 0

            /**
             * `FlushMode.MANUAL`이기 때문에 `flush()`가 자동으로 호출되지 않는다.
             */
            session.persist(
                Post(
                    id = 1L,
                    title = "JPA FlushMode test"
                )
            )

            session.createQuery(
                """
                    select count(p)
                    from Post p
                """.trimIndent(),
                Int::class.java
            ).singleResult shouldBe 0
        }
    }

    @DisplayName("FlushModeType.MANUAL은 수동으로 flush()를 호출해야 한다.")
    @Test
    fun flushModeManualWithFlushCall() {
        doInJPA { entityManager ->
            val session = entityManager.unwrap(Session::class.java).apply {
                hibernateFlushMode = FlushMode.MANUAL
            }

            session.createQuery(
                """
                    select count(p)
                    from Post p
                """.trimIndent(),
                Int::class.java
            ).singleResult shouldBe 0

            session.persist(
                Post(
                    id = 1L,
                    title = "JPA FlushMode test"
                )
            )

            /**
             * INSERT 쿼리가 실행된다.
             */
            session.flush()

            session.createQuery(
                """
                    select count(p)
                    from Post p
                """.trimIndent(),
                Int::class.java
            ).singleResult shouldBe 1
        }
    }

    @DisplayName("FlushModeType.COMMIT은 트랜잭션 종료 이후에 flush()를 수행한다.")
    @Test
    fun flushModeCommit() {
        doInJPA { entityManager ->
            val session = entityManager.unwrap(Session::class.java).apply {
                hibernateFlushMode = FlushMode.COMMIT
            }

            session.createQuery(
                """
                    select count(p)
                    from Post p
                """.trimIndent(),
                Int::class.java
            ).singleResult shouldBe 0

            session.persist(
                Post(
                    id = 1L,
                    title = "JPA FlushMode test"
                )
            )

            session.createQuery(
                """
                    select count(p)
                    from Post p
                """.trimIndent(),
                Int::class.java
            ).singleResult shouldBe 0
        }

        doInJPA { entityManager ->
            entityManager.createQuery(
                """
                    select count(p)
                    from Post p
                """.trimIndent(),
                Int::class.java
            ).singleResult shouldBe 1
        }
    }

    @DisplayName("FlushModeType.ALWAYS는 모든 쿼리 실행에 대해서 flush()를 수행한다.")
    @Test
    fun flushModeAlways() {
        doInJPA { entityManager ->
            val session = entityManager.unwrap(Session::class.java).apply {
                hibernateFlushMode = FlushMode.ALWAYS
            }

            session.createQuery(
                """
                    select count(p)
                    from Post p
                """.trimIndent(),
                Int::class.java
            ).singleResult shouldBe 0

            session.persist(
                Post(
                    id = 1L,
                    title = "JPA FlushMode test"
                )
            )

            session.createQuery(
                """
                    select count(p)
                    from Post p
                """.trimIndent(),
                Int::class.java
            ).singleResult shouldBe 1
        }
    }

    @Entity(name = "Post")
    @Table(name = "post")
    data class Post(
        @Id
        private var id: Long = 0,

        @Column(name = "title")
        private val title: String
    )
}
