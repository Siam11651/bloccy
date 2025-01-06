package com.siam11651.bloccy

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.contract.ActivityResultContract

class RoleRequestActivityContract : ActivityResultContract<Intent, Unit>() {
    override fun createIntent(context: Context, input: Intent): Intent {
        return input
    }

    override fun parseResult(resultCode: Int, intent: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            Log.i("start", "ok")
        } else {
            Log.i("start", "not ok")
        }
    }
}