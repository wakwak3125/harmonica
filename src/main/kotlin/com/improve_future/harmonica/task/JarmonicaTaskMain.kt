package com.improve_future.harmonica.task

import com.improve_future.harmonica.core.AbstractMigration
import com.improve_future.harmonica.core.Connection
import com.improve_future.harmonica.core.DbConfig
import com.improve_future.harmonica.service.VersionService
import org.reflections.Reflections

open class JarmonicaTaskMain {
    protected val migrationTableName: String = "harmonica_migration"
    protected val versionService: VersionService
    protected var classLoader: ClassLoader

    init {
        versionService = VersionService(migrationTableName)
        classLoader = ClassLoader.getSystemClassLoader()
    }

    protected fun createConnection(packageName: String): Connection {
        return Connection(loadDbConfig(packageName))
    }

    protected fun findMigrationClassList(packageName: String): List<Class<out AbstractMigration>> {
        val reflections = Reflections(packageName)
        val classList = reflections.getSubTypesOf(AbstractMigration::class.java)
        return classList.toList().sortedBy { it.name }
    }

    private fun loadDbConfig(packageName: String, env: String = "Default"): DbConfig {
        val reflections = Reflections(packageName)
        val classList = reflections.getSubTypesOf(DbConfig::class.java)
        classList.forEach {
            if (it.simpleName == env) return it.newInstance()
        }
        throw Exception("no config was found.")
    }
}