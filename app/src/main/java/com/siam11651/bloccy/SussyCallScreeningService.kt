package com.siam11651.bloccy

import android.content.Context
import android.content.Intent
import android.telecom.Call
import android.telecom.CallScreeningService
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread

class SussyCallScreeningService : CallScreeningService() {
  override fun onScreenCall(callDetails: Call.Details) {
    if (callDetails.callDirection == Call.Details.DIRECTION_OUTGOING) {
      return
    }

    val number = callDetails.handle.schemeSpecificPart
    val sharedPreferences = this.getSharedPreferences("metadata", Context.MODE_PRIVATE)
    val regex = Regex(sharedPreferences.getString("pattern", null) ?: ".^")
    val running = (sharedPreferences.getString("running", null) ?: "false").equals("true")

    if (running && regex.matches(number)) {
      respondToCall(
        callDetails,
        CallResponse
          .Builder()
          .setDisallowCall(true)
          .build()
      )

      val intent = Intent("com.siam11651.blocked")

      intent.putExtra("number", number)
      intent.putExtra("time", System.currentTimeMillis())
      sendBroadcast(intent)

      runBlocking {
        launch {
          val db = BloccyDatabaseHelper(this@SussyCallScreeningService).writableDatabase

          db.execSQL(
            """insert into calls(number, time)
              |values($1, $2);
            """.trimMargin(),
            arrayOf(number, System.currentTimeMillis())
          )
        }
      }
    }
  }
}