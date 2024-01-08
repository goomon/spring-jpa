package com.github.goomon.jpa

import com.github.goomon.jpa.common.AbstractTest
import io.kotest.matchers.shouldBe
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.Session
import org.hibernate.jpa.HibernateHints
import org.junit.jupiter.api.Test
import org.springframework.data.jpa.repository.support.QueryHints

class DirtyCheckTest : AbstractTest() {
    @Test
    fun dirtyCheckingTest() {
        doInJPA { entityManager ->
            for (i in 0 until 100) {
                entityManager.persist(Post(
                    title = "dirty check test $i",
                    creator = "creator $i",
                ))
            }
        }

        doInJPA { entityManager ->
            entityManager.createQuery(
                """
                    select p
                    from Post p
                """.trimIndent()
            ).resultList.size shouldBe 100
        }
    }

    @Test
    fun dirtyCheckingWithReadOnlyHintTest() {
        doInJPA { entityManager ->
            for (i in 1 .. 2) {
                entityManager.persist(Post(
                    id = i.toLong(),
                    title = "dirty check test $i",
                    creator = "creator $i",
                ))
            }
        }

        doInJPA { entityManager ->
            val session = entityManager.unwrap(Session::class.java).apply {
                isDefaultReadOnly = true
            }

            session.createQuery(
                """
                    select p
                    from Post p
                """.trimIndent(),
                Post::class.java,
            ).resultList.size shouldBe 2

            session.flush()
        }
    }

    @Entity(name = "Post")
    @Table(name = "post")
    class Post(
        @Id
        var id: Long = 0,

        @Column(name = "title")
        var title: String,

        @Column(name = "creator")
        var creator: String,
    )
}