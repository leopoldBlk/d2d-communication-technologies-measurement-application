package com.leopoldbialek.measurementapp.Utils

import android.os.Environment
import java.io.File
import java.io.FileWriter
import java.time.LocalDateTime

class PersistenceController(
    private val technology: String,
    private val testType: String,
    private val role: String
) {
    private lateinit var fileWriter: FileWriter
    private lateinit var measurementFile: File

    private var alreadyPersistedBatch: Boolean = false

    private val measurementFilesDir =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            .toString() + "/Measurements/"

    fun persistData(dataToPersist: String) {
        if(!alreadyPersistedBatch){
            if (!File(measurementFilesDir).exists()) {
                File(measurementFilesDir).mkdir()
            }

            measurementFile = File(
                measurementFilesDir + technology + "_" + testType + "_" + role + "_" + LocalDateTime.now()
                    .toString()
                    .replace("T", "_")
                    .replace(":", "-") + ".txt"
            )

            fileWriter = FileWriter(measurementFile)
            fileWriter.write(dataToPersist)
            fileWriter.close()

            alreadyPersistedBatch = true
        }
        else{
            fileWriter= FileWriter(measurementFile, true)
            fileWriter.write(dataToPersist)
            fileWriter.close()
        }
    }
}