package com.github.goomon.jpa

import com.github.goomon.jpa.common.AbstractTest
import com.vladmihalcea.flexypool.FlexyPoolDataSource
import com.vladmihalcea.flexypool.adaptor.DataSourcePoolAdapter
import com.vladmihalcea.flexypool.config.Configuration
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.Properties
import java.util.concurrent.TimeUnit
import javax.sql.DataSource
import org.junit.jupiter.api.Test

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
                        creator = "test",
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
        val creator: String,
    )
}
