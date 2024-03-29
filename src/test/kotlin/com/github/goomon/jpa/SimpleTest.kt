package com.github.goomon.jpa

import com.github.goomon.jpa.common.AbstractTest
import org.junit.jupiter.api.Test
import java.util.Properties
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

class SimpleTest : AbstractTest() {

    override fun properties(): Properties {
        val props = super.properties()
        props["hibernate.connection.autocommit"] = false
        props["hibernate.jdbc.batch_size"] = 50
        props["hibernate.connection.provider_disables_autocommit"] = false
        return props
    }

    @Test
    fun test() {
        doInJPA { entityManager ->
            for (i in 1..100) {
                entityManager.persist(
                    Post(
                        title = "test",
                        creator = "test"
                    )
                )
            }
        }
    }

    @Entity
    @Table(name = "post")
    data class Post(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long = 0,

        @Column(name = "title")
        val title: String,

        @Column(name = "creator")
        val creator: String
    )
}
