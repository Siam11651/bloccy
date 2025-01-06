package com.siam11651.bloccy

import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.siam11651.bloccy.ui.theme.BloccyTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class CallItem(var number: String, var time: Long)

class MainActivity : ComponentActivity() {
  private lateinit var receiver: BroadcastReceiver
  private var callScreenRunning = mutableStateOf(false)
  private var calls = mutableStateListOf<CallItem>()

  private fun requestRole(activityResultLauncher: ActivityResultLauncher<Intent>) {
    val roleManager = getSystemService(ROLE_SERVICE) as RoleManager
    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)

    activityResultLauncher.launch(intent)
  }

  private fun convertUnixTimestampToDateString(unixTimestamp: Long): String {
    val instant = Instant.ofEpochMilli(unixTimestamp)
    val formatter = DateTimeFormatter.ofPattern("hh:mma dd-MM-yyyy")
      .withZone(ZoneId.systemDefault())

    return formatter.format(instant)
  }

  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    receiver = object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
        val number = intent.getStringExtra("number")
        val time = intent.getLongExtra("time", -1)

        if (number != null && time != (-1).toLong()) {
          calls.add(CallItem(number, time))
        } else {
          Toast
            .makeText(
              this@MainActivity,
              "Invalid number/time \uD83D\uDE2D",
              Toast.LENGTH_LONG
            )
            .show()
        }
      }
    }

    registerReceiver(receiver, IntentFilter("com.siam11651.blocked"), RECEIVER_EXPORTED)

    val activityResultLauncher =
      registerForActivityResult(RoleRequestActivityContract(), RoleRequestActivityCallback())
    val sharedPreferences = this.getSharedPreferences("metadata", Context.MODE_PRIVATE)

    if (!sharedPreferences.contains("pattern")) {
      sharedPreferences.edit()
        .putString("pattern", ".^")
        .apply()
    }

    if (!sharedPreferences.contains("running")) {
      sharedPreferences.edit()
        .putString("running", "false")
        .apply()
    }

    callScreenRunning.value =
      (sharedPreferences.getString("running", null) ?: "false").equals("true")

    requestRole(activityResultLauncher)

    val db = BloccyDatabaseHelper(this@MainActivity).readableDatabase
    val cursor = db.rawQuery(
      """
      select *
      from calls
      order by time desc;
    """.trimIndent(),
      null
    )

    if (cursor.moveToFirst()) {
      do {
        val number = cursor.getString(cursor.getColumnIndexOrThrow("number"))
        val time = cursor.getLong(cursor.getColumnIndexOrThrow("time"))

        calls.add(
          CallItem(number, time)
        )
      } while (cursor.moveToNext())
    }

    cursor.close()
    enableEdgeToEdge()
    setContent {
      BloccyTheme {
        Scaffold(
          topBar = {
            TopAppBar(
              title = {
                Text(
                  "Bloccy",
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 32.sp
                )
              }
            )
          },
          modifier = Modifier
            .fillMaxSize()
        ) { innerPadding ->
          Home(
            sharedPreferences = sharedPreferences,
            modifier = Modifier.padding(innerPadding)
          )
        }
      }
    }
  }

  override fun onDestroy() {
    unregisterReceiver(receiver)
    super.onDestroy()
  }

  @Composable
  private fun Home(sharedPreferences: SharedPreferences, modifier: Modifier) {
    val patternState = remember {
      mutableStateOf(sharedPreferences.getString("pattern", null) ?: ".^")
    }

    Column(
      modifier = modifier
        .fillMaxWidth()
        .fillMaxHeight()
        .padding(16.dp)
    ) {
      OutlinedTextField(
        value = patternState.value,
        onValueChange = { newValue ->
          patternState.value = newValue

          sharedPreferences
            .edit()
            .putString("pattern", newValue)
            .apply()
        },
        label = {
          Text("Pattern")
        },
        modifier = Modifier.fillMaxWidth()
      )

      Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Screen Calls")
        Switch(
          callScreenRunning.value,
          onCheckedChange = { newValue ->
            callScreenRunning.value = newValue

            if (newValue) {
              sharedPreferences.edit()
                .putString("running", "true")
                .apply()
            } else {
              sharedPreferences.edit()
                .putString("running", "false")
                .apply()
            }
          }
        )
      }

      Text(
        "Blocked Calls",
        style = TextStyle(
          fontWeight = FontWeight.SemiBold,
          fontSize = 24.sp
        )
      )

      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
      ) {
        calls.forEach { callItem ->
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp)
          ) {
            Column(
              horizontalAlignment = Alignment.Start,
              modifier = Modifier
                .padding(8.dp)
            ) {
              Text(
                callItem.number,
                fontSize = 24.sp
              )
              Text(
                convertUnixTimestampToDateString(callItem.time),
                fontSize = 12.sp,
                color = Color.Gray
              )
            }
          }
        }
      }
    }
  }
}