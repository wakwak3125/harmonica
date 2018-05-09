package com.improve_future.harmonica.core

class DbConfig {
    lateinit var dbms: Dbms
    lateinit var host: String
    var port: Int = -1
    lateinit var dbName: String
    lateinit var user: String
    lateinit var password: String

    companion object {
        fun create(block: DbConfig.() -> Unit): DbConfig {
            return DbConfig().apply {
                this.block()

                if (port == -1) {
                    port = when (dbms) {
                        Dbms.PostgreSQL -> 5432
                        Dbms.MySQL -> 3396
                        Dbms.SQLite -> 0
                    }
                }
            }
        }
    }

    fun toConnectionUrlString(): String {
        return when (dbms) {
            Dbms.PostgreSQL ->
                    "postgresql://$user:$password@$host:$port/$dbName"
            Dbms.MySQL ->
                    "jdbc:mysql://$host:$port/$dbName"
            Dbms.SQLite ->
                    ""
        }
    }
}

operator fun DbConfig.invoke(block: DbConfig.() -> Unit): DbConfig {
    this.block()
    return this
}