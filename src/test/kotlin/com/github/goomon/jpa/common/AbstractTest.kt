package com.github.goomon.jpa.common

import com.p6spy.engine.spy.P6SpyDriver
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import javax.persistence.Entity
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.EntityTransaction
import org.hibernate.SessionFactory
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor
import org.junit.platform.commons.logging.LoggerFactory
import org.junit.platform.commons.util.AnnotationUtils
import org.springframework.jdbc.datasource.SimpleDriverDataSource
import java.util.Properties
import javax.sql.DataSource

abstract class AbstractTest {

    private val LOGGER = LoggerFactory.getLogger(javaClass)
    private var entityManagerFactory: EntityManagerFactory? = null

    private fun newEntityManagerFactory(name: String): EntityManagerFactory {
        if (entityManagerFactory != null) return entityManagerFactory!!

        val persistenceUnitInfo = PersistenceUnitInfoImpl(
            persistenceUnitName = name,
            managedClassNames = entities(),
            properties = properties()
        )
        val config = mutableMapOf<String, Any>()
        val entityManagerFactoryBuilder = EntityManagerFactoryBuilderImpl(
            PersistenceUnitInfoDescriptor(persistenceUnitInfo),
            config
        )
        entityManagerFactory = entityManagerFactoryBuilder.build()
        return entityManagerFactory!!
    }

    protected fun<T> doInJPA(function: (EntityManager) -> T): T {
        LOGGER.info { "\n\n\n Transaction start \n\n\n" }
        var entityManager: EntityManager? = null
        var transaction: EntityTransaction? = null
        return try {
            entityManager = newEntityManagerFactory(javaClass.simpleName).createEntityManager()
            transaction = entityManager.transaction
            transaction.begin()
            val result = function.invoke(entityManager)
            transaction.commit()
            result
        } catch (e: Exception) {
            LOGGER.error(e) { "Transaction failure" }
            try {
                transaction?.rollback()
            } catch (e: Exception) {
                LOGGER.error(e) { "Rollback failure" }
            }
            throw e
        } finally {
            entityManager?.close()
            LOGGER.info { "\n\n\n Transaction exit \n\n\n" }
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
        props["hibernate.dialect"] = "org.hibernate.dialect.MySQL8Dialect"
        props["hibernate.connection.datasource"] = dataSource()
        props["hibernate.generate_statistics"] = true
        return props
    }

    protected fun printCacheRegionStatisticsEntries(regionName: String) {
        val sessionFactory = requireNotNull(entityManagerFactory?.unwrap(SessionFactory::class.java))
        val statistics = sessionFactory.statistics
        if (sessionFactory.sessionFactoryOptions.isQueryCacheEnabled) {
            ReflectionUtils.invokeMethod<Unit>(
                statistics,
                "getQueryRegionStats",
                "default-query-results-region"
            )
        }
        statistics.getDomainDataRegionStatistics(regionName)
    }
}
