package com.github.goomon.jpa

import com.github.goomon.jpa.common.AbstractTest
import io.kotest.matchers.shouldBe
import org.hibernate.Session
import org.junit.jupiter.api.Test
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

class DirtyCheckTest : AbstractTest() {
    @Test
    fun dirtyCheckingTest() {
        doInJPA { entityManager ->
            for (i in 0 until 100) {
                entityManager.persist(
                    Post(
                        title = "dirty check test $i",
                        creator = "creator $i"
                    )
                )
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
            for (i in 1..2) {
                entityManager.persist(
                    Post(
                        title = "dirty check test $i",
                        creator = "creator $i"
                    )
                )
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
                Post::class.java
            ).resultList.size shouldBe 2

            session.flush()
        }
    }

    @Entity(name = "Post")
    @Table(name = "post")
    class Post(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long = 0,

        @Column(name = "title")
        var title: String,

        @Column(name = "creator")
        var creator: String
    )
}
