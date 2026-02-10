package com.leopoldbialek.measurementapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.leopoldbialek.measurementapp.Enums.Role

@Composable
fun SendingReceivingIndicator(
    role: Role,
    isSendingOrReceiving: Boolean,
    activeMeasurement: Boolean,
) {
    Box(
        Modifier
            .padding(end = 25.dp)
            .background(
                if (!activeMeasurement) {
                    Color(
                        223, 223, 224
                    )
                } else if (!isSendingOrReceiving) {
                    Color(
                        230, 0, 0
                    )
                } else {
                    Color(
                        0, 230, 0
                    )
                }
            )
            .fillMaxWidth(),
        Alignment.Center
    ) {
        when (role) {
            Role.SENDER -> {
                Text("Sending Data")
            }

            Role.RECEIVER -> {
                Text("Receiving Data")
            }
        }
    }
}