package com.leopoldbialek.measurementapp.Utils

import android.util.Log

class LogController {
    companion object {
        public var activeLogging: Boolean = false

        public fun log(level: Int, tag: String, message: String) {
            if (activeLogging) {
                when (level) {
                    Log.DEBUG -> Log.d(tag, message)
                    Log.ERROR -> Log.e(tag, message)
                    Log.INFO -> Log.i(tag, message)
                    Log.VERBOSE -> Log.v(tag, message)
                    Log.WARN -> Log.w(tag, message)
                }
            }
        }
    }
}