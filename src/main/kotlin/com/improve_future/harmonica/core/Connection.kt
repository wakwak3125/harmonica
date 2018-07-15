package com.improve_future.harmonica.core

import com.improve_future.harmonica.config.PluginConfig
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.DEFAULT_ISOLATION_LEVEL
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.io.Closeable
import java.sql.*

open class Connection(
    override val config: DbConfig
) : Closeable, ConnectionInterface {
    private lateinit var coreConnection: java.sql.Connection

    private fun connect(config: DbConfig) {
        coreConnection = object : java.sql.Connection by DriverManager.getConnection(
            buildConnectionUriFromDbConfig(config),
            config.user,
            config.password
        ) {
            override fun setTransactionIsolation(level: Int) {}
        }
        database =
                if (PluginConfig.hasExposed())
                    Database.connect({ coreConnection })
                else null
        coreConnection.autoCommit = false
    }

    private val javaConnection: java.sql.Connection
        get () {
            if (isClosed) connect(config)

            return if (PluginConfig.hasExposed())
                coreConnection
//            TransactionManager.current().connection
            else
                coreConnection
        }

    private var database: Database? = null

    override fun close() {
        if (!isClosed) javaConnection.close()
//        DriverManager.getDrivers().iterator().forEach {
//            it.takeUnless {  }
//        }
//        DriverManager.deregisterDriver(DriverManager.getDriver(
//                buildConnectionUriFromDbConfig(config)))
    }

    private val isClosed: Boolean
        get() {
            return coreConnection.isClosed
        }

    init {
//        val ds = InitialContext().lookup(
//                config.toConnectionUrlString()) as DataSource
//        javaConnection = object : java.sql.Connection by ds.connection {
//            override fun setTransactionIsolation(level: Int) {}
//        }
//        DriverManager.registerDriver(
//                DriverManager.getDriver(buildConnectionUriFromDbConfig(config)))
        //coreConnection =
        connect(config)
        when (config.dbms) {
            Dbms.Oracle ->
                execute("SELECT 1 FROM DUAL;")
            else ->
                execute("SELECT 1;")
        }
    }

    companion object {
        fun create(block: DbConfig.() -> Unit): Connection {
            return Connection(DbConfig.create(block))
        }

        private fun buildConnectionUriFromDbConfig(dbConfig: DbConfig): String {
            return dbConfig.run {
                when (dbms) {
                    Dbms.PostgreSQL ->
                        "jdbc:postgresql://$host:$port/$dbName?autoReconnect=true"
                    Dbms.MySQL ->
                        "jdbc:mysql://$host:$port/$dbName?autoReconnect=true"
                    Dbms.SQLite ->
                        ""
                    Dbms.Oracle ->
                        ""
                }
            }
        }
    }

    override fun transaction(block: Connection.() -> Unit) {
        javaConnection.autoCommit = false
        if (PluginConfig.hasExposed()) {
            TransactionManager.currentOrNew(DEFAULT_ISOLATION_LEVEL).let {
                try {
                    block()
                    it.commit()
                    it.close()
                } catch (e: Exception) {
                    it.rollback()
                    it.close()
                    throw e
                }
            }
        } else {
            try {
                block()
                javaConnection.commit()
            } catch (e: Exception) {
                javaConnection.rollback()
                javaConnection.close()
                throw e
            }
        }
    }

//    fun executeSelect(sql: String) {
//        val statement = createStatement()
//        val rs = statement.executeQuery(sql)
//        while (rs.next()) {
//            for (i in 0 until rs.metaData.columnCount - 1) {
//                when (rs.metaData.getColumnType(i)) {
//                    Types.DATE -> {}
//                    Types.BIGINT -> {}
//                    Types.BINARY -> {}
//                    Types.BIT -> {}
//                }
//            }
//        }
//        rs.close()
//    }

    /**
     * Execute SQL
     */
    override fun execute(sql: String): Boolean {
        return if (PluginConfig.hasExposed()) {
            val tr = TransactionManager.currentOrNull()
            if (tr == null) {
                val newTr = TransactionManager.currentOrNew(DEFAULT_ISOLATION_LEVEL)
                newTr.exec(sql)
                newTr.close()
            } else {
                tr.exec(sql)
            }
            // ToDo: return correct status
            true
        } else {
            val statement = createStatement()
            val result: Boolean
            result = statement.execute(sql)
            statement.close()
            result
        }
    }

    override fun doesTableExist(tableName: String): Boolean {
        val resultSet = javaConnection.metaData.getTables(
            null, null, tableName, null
        )
        val result = resultSet.next()
        resultSet.close()
        return result
    }

    override fun createStatement(): Statement {
        return javaConnection.createStatement()
    }
}