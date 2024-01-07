package com.github.goomon.jpa.common

import com.p6spy.engine.spy.P6SpyDriver
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import jakarta.persistence.Entity
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.EntityTransaction
import java.util.Properties
import javax.sql.DataSource
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor
import org.junit.platform.commons.logging.LoggerFactory
import org.junit.platform.commons.util.AnnotationUtils
import org.springframework.jdbc.datasource.SimpleDriverDataSource

abstract class AbstractTest {

    private val LOGGER = LoggerFactory.getLogger(javaClass)
    private var entityManagerFactory: EntityManagerFactory? = null

    private fun newEntityManagerFactory(name: String): EntityManagerFactory {
        if (entityManagerFactory != null) return entityManagerFactory!!

        val persistenceUnitInfo = PersistenceUnitInfoImpl(
            persistenceUnitName = name,
            managedClassNames = entities(),
            properties = properties(),
        )
        val config = mutableMapOf<String, Any>()
        val entityManagerFactoryBuilder = EntityManagerFactoryBuilderImpl(
            PersistenceUnitInfoDescriptor(persistenceUnitInfo),
            config,
        )
        entityManagerFactory = entityManagerFactoryBuilder.build()
        return entityManagerFactory!!
    }

    protected fun doInJPA(function: (EntityManager) -> Unit) {
        var entityManager: EntityManager? = null
        var transaction: EntityTransaction? = null
        try {
            entityManager = newEntityManagerFactory(javaClass.simpleName).createEntityManager()
            transaction = entityManager.transaction
            transaction.begin()
            function.invoke(entityManager)
            transaction.commit()
        } catch (e: Exception) {
            LOGGER.error(e) { "Transaction failure" }
            try {
                transaction?.rollback()
            } catch (e: Exception) {
                LOGGER.error(e) { "Rollback failure" }
            }
        } finally {
            entityManager?.close()
        }
    }

    private fun entities(): List<String> {
        return this::class.nestedClasses
            .filter { AnnotationUtils.isAnnotated(it.java, Entity::class.java) }
            .map { it.java.name }
    }

    protected open fun dataSource(): DataSource {
        val datasource = SimpleDriverDataSource()
        datasource.driver = P6SpyDriver()
        datasource.url = "jdbc:p6spy:mysql://localhost:3306/study?serverTimezone=Asia/Seoul"
        datasource.username = "root"

        val hikariConfig = HikariConfig()
        val cpuCores = Runtime.getRuntime().availableProcessors()
        hikariConfig.maximumPoolSize = cpuCores * 4
        hikariConfig.dataSource = datasource

        return HikariDataSource(hikariConfig)
    }

    protected open fun properties(): Properties {
        val props = Properties()
        props["hibernate.hbm2ddl.auto"] = "create"
        props["hibernate.dialect"] = "org.hibernate.dialect.MySQLDialect"
        props["hibernate.connection.datasource"] = dataSource()
        props["hibernate.generate_statistics"] = true
        return props
    }
}
