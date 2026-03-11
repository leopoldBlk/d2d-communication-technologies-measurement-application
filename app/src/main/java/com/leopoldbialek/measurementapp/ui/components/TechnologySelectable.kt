package com.leopoldbialek.measurementapp.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.leopoldbialek.measurementapp.Enums.Technology

@Composable
fun TechnologySelectable(
    technology: Technology,
    selectedTechnology: Technology,
    changeSelectedTechnology: (Technology) -> Unit,
    activeMeasurement: Boolean
) {
    Row() {
        RadioButton(
            technology == selectedTechnology,
            { changeSelectedTechnology(technology) },
            enabled = !activeMeasurement
        )

        Text(
            technology.toString(),
            Modifier.align(Alignment.CenterVertically)
        )
    }
}