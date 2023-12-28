package com.github.goomon.jpa.common

import com.p6spy.engine.spy.P6SpyDriver
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

abstract class AbstractTest {

    val LOGGER = LoggerFactory.getLogger(javaClass)

    private fun newEntityManagerFactory(name: String): EntityManagerFactory {
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
        return entityManagerFactoryBuilder.build()
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
        return HikariDataSource().apply {
            driverClassName = P6SpyDriver::class.java.name
            jdbcUrl = "jdbc:p6spy:mysql://localhost:3306/study?serverTimezone=Asia/Seoul"
            username = "root"
        }
    }

    protected open fun properties(): Properties {
        val props = Properties()
        props["hibernate.hbm2ddl.auto"] = "create-drop"
        props["hibernate.dialect"] = "org.hibernate.dialect.MySQLDialect"
        props["hibernate.connection.datasource"] = dataSource()
        props["hibernate.generate_statistics"] = true
        return props
    }
}
