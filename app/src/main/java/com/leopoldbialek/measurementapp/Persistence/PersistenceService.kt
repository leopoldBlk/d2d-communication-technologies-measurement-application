package com.leopoldbialek.measurementapp.Persistence

import android.app.Service
import android.content.Intent
import android.os.Environment
import android.os.Environment.DIRECTORY_DOCUMENTS
import android.os.IBinder
import java.io.File
import java.io.FileWriter
import java.time.LocalDateTime

class PersistenceService : Service() {
    companion object {
        private lateinit var fileWriter: FileWriter
        private lateinit var technology: String
        private lateinit var testType: String
        private lateinit var role: String

        fun persistData(dataToPersist: String) {
            fileWriter.write(dataToPersist)
            fileWriter.close()
        }
    }

    private lateinit var measurementFile: File

    private val measurementFilesDir =
        Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS)
            .toString() + "/Measurements/"


    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent != null){
            technology = intent.getStringExtra("Technology").toString()
            testType = intent.getStringExtra("TestType").toString()
            role = intent.getStringExtra("Role").toString()
        }

        if (!File(measurementFilesDir).exists()) {
            File(measurementFilesDir).mkdir()
        }

        measurementFile = File(
            measurementFilesDir + technology + "_" + testType + "_" + role + "_" + LocalDateTime.now().toString()
                .replace("T", "_")
                .replace(":", "-") + ".txt"
        )
        fileWriter = FileWriter(measurementFile)

        return super.onStartCommand(intent, flags, startId)
    }
}