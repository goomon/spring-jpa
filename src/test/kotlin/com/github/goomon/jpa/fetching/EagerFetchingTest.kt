package com.github.goomon.jpa.fetching

import com.github.goomon.jpa.common.AbstractTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Properties
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table

class EagerFetchingTest : AbstractTest() {
    override fun properties(): Properties {
        val props = super.properties()
        props["hibernate.format_sql"] = true
        return props
    }

    @BeforeEach
    fun init() {
        doInJPA { entityManager ->
            for (i in 1..3) {
                val post = Post(title = "Post no. $i")
                for (j in 1..3) {
                    post.comments.add(
                        PostComment(review = "Good", post = post)
                    )
                    post.tags.add(
                        Tag(name = "Tag no. $i", post = post)
                    )
                }
                entityManager.persist(post)
            }
        }
    }

    @Test
    fun collectionEagerFetchingTest() {
        // directly fetching - Cartesian product
        doInJPA { entityManager ->
            entityManager.find(Post::class.java, 1L)
        }

        // fetching by JPQL - explicitly fetching
        doInJPA { entityManager ->
            entityManager.createQuery(
                """
                    select p
                    from Post p
                    where p.id = 1L
                """.trimIndent()
            ).resultList
        }
    }

    /**
     * 골 때리는 상황이 발생한다.
     * * `PostComment` -> `Post`: eager fetching
     * * `Post` -> `PostComment`, `Tag`: eager fetching
     *
     * 연관 관계를 사용하다 보니 생각지도 못하게 연관된 엔터티가 자동으로 fetching이 발생한다.
     */
    @Test
    fun eagerFetchingTest() {
        doInJPA { entityManager ->
            entityManager.find(PostComment::class.java, 1L)
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

        @OneToMany(
            mappedBy = "post",
            fetch = FetchType.EAGER,
            cascade = [CascadeType.ALL],
            orphanRemoval = true
        )
        val comments: MutableSet<PostComment> = mutableSetOf(),

        @OneToMany(
            mappedBy = "post",
            fetch = FetchType.EAGER,
            cascade = [CascadeType.ALL],
            orphanRemoval = true
        )
        val tags: MutableSet<Tag> = mutableSetOf()
    )

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    class PostComment(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long = 0,

        @Column(name = "review")
        var review: String,

        @ManyToOne
        @JoinColumn(name = "post_id")
        var post: Post? = null
    )

    @Entity(name = "Tag")
    @Table(name = "tag")
    class Tag(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long = 0,

        @Column(name = "name")
        var name: String,

        @ManyToOne
        @JoinColumn(name = "post_id")
        var post: Post? = null
    )
}
