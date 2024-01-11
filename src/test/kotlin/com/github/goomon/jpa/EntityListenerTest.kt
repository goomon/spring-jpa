package com.github.goomon.jpa

import com.github.goomon.jpa.common.AbstractTest
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.PostPersist
import javax.persistence.PostUpdate
import javax.persistence.PrePersist
import javax.persistence.PreUpdate
import javax.persistence.Table
import org.junit.jupiter.api.DisplayName
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
    fun entityMergeEventListenerTest() {
        doInJPA { entityManager ->
            entityManager.persist(
                Post(id = 1L, title = "entity listener test")
            )

            val post = entityManager.find(Post::class.java, 1L)
            post.title = "entity listener test2"
            entityManager.merge(post)
        }
    }

    @DisplayName("같은 아이디에 대해서는 ActionQueue가 아닌 선언한 순서가 중요하다.")
    @Test
    fun entityActionQueueTest() {
        doInJPA { entityManager ->
            val post = Post(id = 1L, title = "test")
            entityManager.persist(post)

            val tag1 = Tag(id = 1L, name = "tag1", post = post)
            val tag2 = Tag(id = 2L, name = "tag2", post = post)
            entityManager.persist(tag1)
            entityManager.persist(tag2)
        }

        /**
         * 로그 순서
         * delete from post where id=1
         * insert into post (title,id) values ('test2',1)
         */
        doInJPA { entityManager ->
            val post = entityManager.find(Post::class.java, 1L)
            entityManager.remove(post)
            entityManager.persist(Post(id = 1L, title = "test2"))
        }
    }

    @DisplayName("ActionQueue는 INSERT > DELETE 순서로 동작한다.")
    @Test
    fun entityActionQueueTest2() {
        doInJPA { entityManager ->
            val post = Post(id = 1L, title = "test")
            entityManager.persist(post)

            val tag1 = Tag(id = 1L, name = "tag1", post = post)
            val tag2 = Tag(id = 2L, name = "tag2", post = post)
            entityManager.persist(tag1)
            entityManager.persist(tag2)
        }

        /**
         * 로그 순서
         * insert into post (title,id) values ('test2',2)
         * delete from post where id=1
         */
        doInJPA { entityManager ->
            val post = entityManager.find(Post::class.java, 1L)
            entityManager.remove(post)
            entityManager.persist(Post(id = 2L, title = "test2"))
        }
    }

    @Entity(name = "Post")
    @Table(name = "post")
    @EntityListeners(value = [PostListener::class])
    class Post(
        //        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Id
        var id: Long = 0,

        @Column(name = "title")
        var title: String,

        @OneToMany(
            fetch = FetchType.LAZY,
            mappedBy = "post",
            cascade = [CascadeType.ALL],
            orphanRemoval = true
        )
        val tags: MutableList<Tag> = mutableListOf()
    )

    @Entity(name = "Tag")
    @Table(name = "tag")
    class Tag(
        //        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Id
        var id: Long = 0,

        @Column
        val name: String,

        @ManyToOne
        @JoinColumn(name = "post_id")
        var post: Post?
    )

    class PostListener {
        private val LOGGER = LoggerFactory.getLogger(javaClass)

        @PrePersist
        fun prePersist(post: Post) {
            LOGGER.info { "[PostListener] pre-persist | $post" }
        }

        @PostPersist
        fun postPersist(post: Post) {
            LOGGER.info { "[PostListener] post-persist | $post" }
        }

        @PreUpdate
        fun preUpdate(post: Post) {
            LOGGER.info { "[PostListener] pre-update | $post" }
        }

        @PostUpdate
        fun postUpdate(post: Post) {
            LOGGER.info { "[PostListener] post-update | $post" }
        }
    }
}
