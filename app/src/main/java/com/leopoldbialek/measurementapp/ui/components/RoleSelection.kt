package com.leopoldbialek.measurementapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leopoldbialek.measurementapp.Enums.Role

@Composable
fun RoleSelection(
    selectedRole: Role,
    changedSelectedRole: (Role) -> Unit,
    activeMeasurement: Boolean
) {
    Column() {
        Text("Role for the Measurement", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(5.dp))

        Role.entries.forEach { role ->
            Row() {
                RadioButton(
                    role == selectedRole,
                    { changedSelectedRole(role) },
                    enabled = !activeMeasurement
                )
                Text(
                    role.toString(), Modifier.align(Alignment.CenterVertically)
                )
            }
        }
    }
}