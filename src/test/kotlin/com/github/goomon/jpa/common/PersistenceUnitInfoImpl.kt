package com.github.goomon.jpa.common

import org.hibernate.jpa.HibernatePersistenceProvider
import java.net.URL
import java.util.Properties
import javax.persistence.SharedCacheMode
import javax.persistence.ValidationMode
import javax.persistence.spi.ClassTransformer
import javax.persistence.spi.PersistenceUnitInfo
import javax.persistence.spi.PersistenceUnitTransactionType
import javax.sql.DataSource

class PersistenceUnitInfoImpl(
    private val persistenceUnitName: String,
    private val managedClassNames: List<String>,
    private val properties: Properties
) : PersistenceUnitInfo {
    private var transactionType = PersistenceUnitTransactionType.RESOURCE_LOCAL
    private val mappingFileNames: List<String> = mutableListOf()
    private var jtaDataSource: DataSource? = null
    private var nonJtaDataSource: DataSource? = null
    override fun getPersistenceUnitName(): String = persistenceUnitName

    override fun getPersistenceProviderClassName(): String = HibernatePersistenceProvider::class.java.name

    override fun getTransactionType(): PersistenceUnitTransactionType = transactionType

    override fun getJtaDataSource(): DataSource? = jtaDataSource

    fun setJtaDataSource(jtaDataSource: DataSource?): PersistenceUnitInfoImpl {
        this.jtaDataSource = jtaDataSource
        nonJtaDataSource = null
        transactionType = PersistenceUnitTransactionType.JTA
        return this
    }

    override fun getNonJtaDataSource(): DataSource? = nonJtaDataSource

    fun setNonJtaDataSource(nonJtaDataSource: DataSource?): PersistenceUnitInfoImpl {
        this.nonJtaDataSource = nonJtaDataSource
        jtaDataSource = null
        transactionType = PersistenceUnitTransactionType.RESOURCE_LOCAL
        return this
    }

    override fun getMappingFileNames(): List<String> = mappingFileNames

    override fun getJarFileUrls(): List<URL> = emptyList()

    override fun getPersistenceUnitRootUrl(): URL? = null

    override fun getManagedClassNames(): List<String> = managedClassNames

    override fun excludeUnlistedClasses(): Boolean = false

    override fun getSharedCacheMode(): SharedCacheMode = SharedCacheMode.UNSPECIFIED

    override fun getValidationMode(): ValidationMode = ValidationMode.AUTO

    override fun getProperties(): Properties = properties

    override fun getPersistenceXMLSchemaVersion(): String = JPA_VERSION

    override fun getClassLoader(): ClassLoader = Thread.currentThread().contextClassLoader

    override fun addTransformer(transformer: ClassTransformer) {}
    override fun getNewTempClassLoader(): ClassLoader? = null

    companion object {
        const val JPA_VERSION = "3.2"
    }
}
