package com.leopoldbialek.measurementapp

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.leopoldbialek.measurementapp.Enums.BluetoothLeReceiverPhy
import com.leopoldbialek.measurementapp.Enums.BluetoothLeSenderPrimaryPhy
import com.leopoldbialek.measurementapp.Enums.BluetoothLeSenderSecondaryPhy
import com.leopoldbialek.measurementapp.Enums.Role
import com.leopoldbialek.measurementapp.Enums.Technology
import com.leopoldbialek.measurementapp.Enums.TestType
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class MeasurementViewModel : ViewModel() {
    var activeMeasurement: MutableState<Boolean> = mutableStateOf(false)
        private set

    var activeDataTransmission: MutableState<Boolean> = mutableStateOf(false)
        private set

    private lateinit var activeDataTransmissionLastSet: LocalDateTime

    var selectedTechnology: MutableState<Technology> = mutableStateOf(Technology.BLUETOOTH_LE)
        private set

    var selectedBluetoothLeSenderPrimaryPhy: MutableState<BluetoothLeSenderPrimaryPhy> = mutableStateOf(BluetoothLeSenderPrimaryPhy.PHY_1M)
        private set

    var selectedBluetoothLeSenderSecondaryPhy: MutableState<BluetoothLeSenderSecondaryPhy> = mutableStateOf(
        BluetoothLeSenderSecondaryPhy.PHY_1M)
        private set

    var selectedBluetoothLeReceiverPhy: MutableState<BluetoothLeReceiverPhy> = mutableStateOf(BluetoothLeReceiverPhy.PHY_1M)
        private set

    var selectedRole: MutableState<Role> = mutableStateOf(Role.SENDER)
        private set

    var selectedTestType: MutableState<TestType> = mutableStateOf(TestType.RANGE)
        private set

    fun changeMeasurementState() {
        activeMeasurement.value = if (activeMeasurement.value) {
            false
        } else {
            true
        }
    }

    fun changeActiveDataTransmission(isActiveDataTransmission: Boolean){
        activeDataTransmission.value = isActiveDataTransmission
        activeDataTransmissionLastSet = LocalDateTime.now()
        if(!checkActiveDataTransmissionThread.isAlive){
            checkActiveDataTransmissionThread.start()
        }
    }

    fun changeSelectedTechnology(technology: Technology) {
        selectedTechnology.value = technology
    }

    fun changeSelectedBluetoothLeSenderPrimaryPhy(bluetoothLeSenderPrimaryPhy: BluetoothLeSenderPrimaryPhy) {
        selectedBluetoothLeSenderPrimaryPhy.value = bluetoothLeSenderPrimaryPhy
    }

    fun changeSelectedBluetoothLeSenderSecondaryPhy(bluetoothLeSenderSecondaryPhy: BluetoothLeSenderSecondaryPhy) {
        selectedBluetoothLeSenderSecondaryPhy.value = bluetoothLeSenderSecondaryPhy
    }

    fun changeSelectedBluetoothLeReceiverPhy(bluetoothLeReceiverPhy: BluetoothLeReceiverPhy) {
        selectedBluetoothLeReceiverPhy.value = bluetoothLeReceiverPhy
    }

    fun changeSelectedRole(role: Role){
        selectedRole.value = role
    }

    fun changeSelectedTestType(testType: TestType){
        selectedTestType.value = testType
    }

    val checkActiveDataTransmissionThread: Thread = Thread {
        val waitDurationForCheckMillis: Long = 5000

        while (!Thread.currentThread().isInterrupted){
            Thread.sleep(waitDurationForCheckMillis)

            if (waitDurationForCheckMillis < ChronoUnit.MILLIS.between(activeDataTransmissionLastSet, LocalDateTime.now())){
                activeDataTransmission.value = false
            }
        }
    }
}