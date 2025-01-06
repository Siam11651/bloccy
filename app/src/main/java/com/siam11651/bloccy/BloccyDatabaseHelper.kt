package com.siam11651.bloccy

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

private const val DB_NAME = "bloccy-db"
private const val DB_VERSION = 1

class BloccyDatabaseHelper(context: Context) :
  SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
  private val createCallTableSql =
    """create table if not exists calls (
      | number text,
      | time integer
      |);
    """.trimMargin()

  override fun onCreate(db: SQLiteDatabase?) {
    if (db == null) {
      return
    }

    db.execSQL(createCallTableSql)
  }

  override fun onUpgrade(db: SQLiteDatabase?, p1: Int, p2: Int) {

  }
}