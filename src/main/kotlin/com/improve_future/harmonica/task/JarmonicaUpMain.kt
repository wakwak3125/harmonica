package com.improve_future.harmonica.task

import com.improve_future.harmonica.core.AbstractMigration
import com.improve_future.harmonica.core.Connection
import com.improve_future.harmonica.core.DbConfig
import com.improve_future.harmonica.service.VersionService
import org.reflections.Reflections

object JarmonicaUpMain : JarmonicaTaskMain() {
    @JvmStatic
    fun main(vararg args: String) {
        println("start main method")
        val migrationPackage = args[0]

//        (classLoader as URLClassLoader).urLs.forEach {
//            println(it)
//        }

        val connection = createConnection(migrationPackage)
        try {
            connection.transaction {
                versionService.setupHarmonicaMigrationTable(connection)
            }
            for (clazz in findMigrationClassList(migrationPackage)) {
                val migrationVersion: String = clazz.name.substring(clazz.name.lastIndexOf('_') + 1)
                if (versionService.doesVersionMigrated(connection, migrationVersion)) continue

                println("== [Start] Migrate up $migrationVersion ==")
                connection.transaction {
                    val migration = clazz.newInstance()
                    migration.connection = connection
                    migration.up()
                    versionService.saveVersion(connection, migrationVersion)
                }
                println("== [End] Migrate up $migrationVersion ==")
            }
            connection.close()
        } catch (e: Exception) {
            connection.close()
            throw e
        }
    }
}