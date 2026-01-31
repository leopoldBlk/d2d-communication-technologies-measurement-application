package com.leopoldbialek.measurementapp

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leopoldbialek.measurementapp.ui.theme.MeasurementAppTheme
import java.time.Duration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeasurementAppTheme {
                Column(
                    Modifier
                        .padding(start = 25.dp),
                    Arrangement.spacedBy(50.dp)
                ) {
                    Spacer(
                        Modifier
                            .height(75.dp)
                    )
                    TechnologySelection(listOf("Bluetooth LE", "Wi-Fi Aware"))
                    RoleSelection(listOf("Sender", "Receiver"))
                    ControlButtons(applicationContext)
                }
            }
        }
    }
}

@Composable
fun TechnologySelection(technologies: List<String>) {
    Column(
        Modifier,
        Arrangement.spacedBy(7.dp)
    ) {
        Text("Technology for the Measurement", fontSize = 20.sp)

        Spacer(Modifier.height(3.dp))

        technologies.forEach { technology ->
            Row() {
                RadioButton(
                    false,
                    null
                )
                Text(technology)
            }
        }
    }
}

@Composable
fun RoleSelection(roles: List<String>) {
    Column(
        Modifier,
        Arrangement.spacedBy(7.dp)
    ) {
        Text("Role for the Measurement", fontSize = 20.sp)

        Spacer(Modifier.height(3.dp))

        roles.forEach { role ->
            Row() {
                RadioButton(
                    false,
                    null
                )
                Text(role)
            }
        }
    }
}

@Composable
fun ControlButtons(context: Context) {
    Column(
        Modifier.padding(end=25.dp),
        Arrangement.spacedBy(10.dp)
    ) {
        Button(
            {
                Toast.makeText(context, "Measurement Started", Toast.LENGTH_SHORT).show()
            },
            Modifier.fillMaxWidth()
        ) {
            Text("Start")
        }
        Button(
            {
                Toast.makeText(context, "Measurement Stopped", Toast.LENGTH_SHORT).show()
            },
            Modifier.fillMaxWidth()
        ) {
            Text("Stop")
        }
    }
}