package com.github.goomon.jpa

import com.github.goomon.jpa.common.AbstractTest
import io.kotest.matchers.shouldBe
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.junit.jupiter.api.Test

class FlushTest : AbstractTest() {
    @Test
    fun flush() {
        doInJPA { entityManager ->
            (
                entityManager.createQuery(
                    """
                    select count(p)
                    from Post p
                    """.trimIndent()
                ).singleResult as Number
                ).toInt() shouldBe 0

            entityManager.persist(
                Post(
                    id = 1L,
                    title = "JPA flush test"
                )
            )

            /**
             * Break point
             *
             * 조회하는 과정에서 `flush()` 발생
             */
            val postCount = (
                entityManager.createQuery(
                    """
                    select count(p)
                    from Post p
                    """.trimIndent()
                ).singleResult as Number
                ).toInt()

            postCount shouldBe 1
        }
    }

    @Test
    fun flushCauseBySpaceOverlap() {
        doInJPA { entityManager ->
            (
                entityManager.createQuery(
                    """
                    select count(p)
                    from Post p
                    """.trimIndent()
                ).singleResult as Number
                ).toInt() shouldBe 0

            entityManager.persist(
                Post(
                    id = 1L,
                    title = "JPA flush test"
                )
            )

            /**
             * Break point
             *
             * fetch join을 하는 과정에서 `flush()` 발생
             */
            entityManager.createQuery(
                """
                    select pd
                    from PostDetails pd
                    join fetch pd.post
                """.trimIndent()
            ).resultList

            (
                entityManager.createQuery(
                    """
                    select count(p)
                    from Post p
                    """.trimIndent()
                ).singleResult as Number
                ).toInt() shouldBe 1
        }
    }

    @Test
    fun flushWithNativeQuery() {
        doInJPA { entityManager ->
            (
                entityManager.createNativeQuery(
                    """
                    SELECT COUNT(*)
                    FROM post
                    """.trimIndent()
                ).singleResult as Number
                ).toInt() shouldBe 0

            entityManager.persist(
                Post(
                    id = 1L,
                    title = "JPA flush test"
                )
            )

            /**
             * Break point
             *
             * 네이티브 쿼리를 사용하는 경우 정확한 DB를 알 수 없기 때문에
             * 사용할 필요가 없어도 `flush()`가 호출된다.
             */
            (
                entityManager.createNativeQuery(
                    """
                    SELECT COUNT(*)
                    FROM post_details
                    """.trimIndent()
                ).singleResult as Number
                ).toInt() shouldBe 0
        }
    }

    @Entity(name = "Post")
    @Table(name = "post")
    data class Post(
        @Id
        private var id: Long = 0,

        @Column(name = "title")
        private val title: String,

        @OneToOne(
            mappedBy = "post",
            cascade = [CascadeType.ALL],
            orphanRemoval = true,
            fetch = FetchType.LAZY
        )
        private val postDetails: PostDetails? = null
    )

    @Entity(name = "PostDetails")
    @Table(name = "post_details")
    data class PostDetails(
        @Id
        private var id: Long = 0,

        @OneToOne(fetch = FetchType.LAZY)
        private val post: Post? = null
    )
}
