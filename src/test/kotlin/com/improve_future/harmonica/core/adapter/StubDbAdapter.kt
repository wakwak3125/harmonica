package com.improve_future.harmonica.core.adapter

import com.improve_future.harmonica.core.table.TableBuilder
import com.improve_future.harmonica.core.table.column.AbstractColumn
import com.improve_future.harmonica.core.table.column.AddingColumnOption
import com.improve_future.harmonica.stub.core.StubConnection

class StubDbAdapter : DbAdapter(StubConnection()) {
    val addingColumnList = mutableListOf<AddingColumn>()

    data class AddingColumn(
        val tableName: String,
        val column: AbstractColumn,
        val option: AddingColumnOption)

    override fun createTable(tableName: String, tableBuilder: TableBuilder) {
    }

    override fun createIndex(tableName: String, columnName: String, unique: Boolean) {
    }

    override fun addColumn(tableName: String, column: AbstractColumn, option: AddingColumnOption) {
        addingColumnList.add(AddingColumn(tableName, column, option))
    }
}