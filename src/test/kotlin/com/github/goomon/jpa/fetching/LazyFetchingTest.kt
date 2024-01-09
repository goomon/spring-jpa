package com.github.goomon.jpa.fetching

import com.github.goomon.jpa.common.AbstractTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.LazyInitializationException
import org.hibernate.loader.MultipleBagFetchException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.lang.IllegalArgumentException
import java.util.Properties

class LazyFetchingTest : AbstractTest() {
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

    @DisplayName("FetchType.LAZY인 상황에서 N+1이 발생할 수 있다.")
    @Test
    fun lazyFetchingTest() {
        doInJPA { entityManager ->
            val comments = entityManager.createQuery(
                """
                    select pc
                    from PostComment pc
                    where pc.review = :review
                """.trimIndent(),
                PostComment::class.java
            ).setParameter("review", "Good")
                .resultList

            for (comment in comments) {
                println(comment.post?.title)
            }
        }
    }

    @DisplayName("JOIN FETCH는 FetchType.LAZY를 무시하고 연관 정보를 가져온다.")
    @Test
    fun lazyFetchingWithJpqlTest() {
        doInJPA { entityManager ->
            entityManager.createQuery(
                """
                    select pc
                    from PostComment pc
                    join fetch pc.post
                """.trimIndent()
            ).resultList
        }
    }

    @DisplayName("두 개 이상의 리스트의 연관 데이터를 불러올 경우 MultipleBagFetchException가 발생한다.")
    @Test
    fun fetchingMultipleListShouldFail() {
        doInJPA { entityManager ->
            val exception = shouldThrow<IllegalArgumentException> {
                entityManager.createQuery(
                    """
                        select p
                        from Post p
                        left join fetch p.comments
                        lfet join fetch p.tags
                    """.trimIndent(),
                    Post::class.java
                ).resultList
            }
            exception.cause shouldBe beInstanceOf<MultipleBagFetchException>()
        }
    }

    @DisplayName("트랜잭션 외부에서 프록시에 접근할 경우 LazyInitializationException예외가 발생한다.")
    @Test
    fun accessProxyOutOfTransactionShouldFail() {
        val comment = doInJPA { entityManager ->
            entityManager.find(PostComment::class.java, 1L)
        }

        shouldThrow<LazyInitializationException> {
            comment.post?.title
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
            fetch = FetchType.LAZY,
            cascade = [CascadeType.ALL],
            orphanRemoval = true
        )
        var comments: MutableList<PostComment> = mutableListOf(),

        @OneToMany(
            mappedBy = "post",
            fetch = FetchType.LAZY,
            cascade = [CascadeType.ALL],
            orphanRemoval = true
        )
        var tags: MutableList<Tag> = mutableListOf()
    )

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    class PostComment(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long = 0,

        @Column(name = "review")
        var review: String,

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "post_id")
        open var post: Post? = null
    )

    @Entity(name = "Tag")
    @Table(name = "tag")
    class Tag(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long = 0,

        @Column(name = "name")
        var name: String,

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "post_id")
        var post: Post? = null
    )
}
