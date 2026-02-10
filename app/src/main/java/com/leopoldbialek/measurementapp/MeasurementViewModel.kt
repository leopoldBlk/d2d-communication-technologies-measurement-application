package com.leopoldbialek.measurementapp

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.leopoldbialek.measurementapp.Enums.BluetoothLeReceiverPhy
import com.leopoldbialek.measurementapp.Enums.BluetoothLeSenderPhy
import com.leopoldbialek.measurementapp.Enums.Role
import com.leopoldbialek.measurementapp.Enums.Technology
import com.leopoldbialek.measurementapp.Enums.TestType
import java.util.UUID

class MeasurementViewModel : ViewModel() {
    val deviceUuid: UUID = UUID.randomUUID()

    var activeMeasurement: MutableState<Boolean> = mutableStateOf(false)
        private set

    var selectedTechnology: MutableState<Technology> = mutableStateOf(Technology.BLUETOOTH_LE)
        private set

    var selectedBluetoothLeSenderPrimaryPhy: MutableState<BluetoothLeSenderPhy> = mutableStateOf(BluetoothLeSenderPhy.PHY_1M)
        private set

    var selectedBluetoothLeSenderSecondaryPhy: MutableState<BluetoothLeSenderPhy> = mutableStateOf(BluetoothLeSenderPhy.PHY_1M)
        private set

    var selectedBluetoothLeReceiverPhy: MutableState<BluetoothLeReceiverPhy> = mutableStateOf(BluetoothLeReceiverPhy.PHY_1M)
        private set

    var selectedRole: MutableState<Role> = mutableStateOf(Role.SENDER)
        private set

    var selectedTestType: MutableState<TestType> = mutableStateOf(TestType.DISTANCE)
        private set

    fun changeMeasurementState() {
        activeMeasurement.value = if (activeMeasurement.value) {
            false
        } else {
            true
        }
    }

    fun changeSelectedTechnology(technology: Technology) {
        selectedTechnology.value = technology
    }

    fun changeSelectedBluetoothLeSenderPrimaryPhy(bluetoothLeSenderPrimaryPhy: BluetoothLeSenderPhy) {
        selectedBluetoothLeSenderPrimaryPhy.value = bluetoothLeSenderPrimaryPhy
    }

    fun changeSelectedBluetoothLeSenderSecondaryPhy(bluetoothLeSenderSecondaryPhy: BluetoothLeSenderPhy) {
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
}