package com.github.goomon.jpa

import com.github.goomon.jpa.common.AbstractTest
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PostPersist
import jakarta.persistence.PostUpdate
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import org.junit.jupiter.api.Test
import org.junit.platform.commons.logging.LoggerFactory

class EntityListenerTest : AbstractTest() {
    @Test
    fun entityListenerTest() {
        doInJPA { entityManager ->
            entityManager.persist(
                Post(title = "entity listener test")
            )
        }
    }

    /**
     * 네이티브 쿼리를 사용할 경우 당연히 `EntityListener`가 작동하지 않는다.
     */
    @Test
    fun entityListenerTestWithNativeQuery() {
        doInJPA { entityManager ->
            entityManager.createNativeQuery(
                """
                    INSERT INTO post(id, title) VALUES (1, "entity listener test")
                """.trimIndent()
            ).executeUpdate()
        }
    }

    @Test
    fun test() {
        doInJPA { entityManager ->
            entityManager.persist(
                Post(title = "entity listener test")
            )

            entityManager
                .find(Post::class.java, 1)
                .copy(title = "entity listener test 2")
                .run(entityManager::merge)
        }
    }

    @Entity(name = "Post")
    @Table(name = "post")
    @EntityListeners(value = [PostListener::class])
    data class Post(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private var id: Long = 0,

        @Column(name = "title")
        private val title: String,
    )

    class PostListener {
        private val LOGGER = LoggerFactory.getLogger(javaClass)

        @PrePersist
        fun prePersist(post: Post) {
            LOGGER.info { "[PostListener] pre-persist | $post" }
        }

        @PostPersist
        fun postPersist(post: Post) {
            LOGGER.info { "[PostListener] post-persist | $post"}
        }

        @PreUpdate
        fun preUpdate(post: Post) {
            LOGGER.info { "[PostListener] pre-update | $post"}
        }

        @PostUpdate
        fun postUpdate(post: Post) {
            LOGGER.info { "[PostListener] post-update | $post"}
        }
    }
}