package com.checkin.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.checkin.app.ui.navigation.CheckInNavGraph
import com.checkin.app.ui.theme.CheckInTheme
import com.checkin.app.util.PermissionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var permissionManager: PermissionManager

    private val requiredPermissions = mutableListOf(
        Manifest.permission.SEND_SMS,
        Manifest.permission.CALL_PHONE
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.SCHEDULE_EXACT_ALARM)
        }
    }.toTypedArray()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        showPermissionDeniedDialog = !requiredPermissions.all { permission ->
            checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    private var showPermissionDeniedDialog by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        permissionLauncher.launch(requiredPermissions)

        setContent {
            CheckInTheme {
                if (showPermissionDeniedDialog) {
                    PermissionDeniedDialog(
                        onDismiss = { showPermissionDeniedDialog = false },
                        onRetry = {
                            showPermissionDeniedDialog = false
                            permissionLauncher.launch(requiredPermissions)
                        }
                    )
                }
                CheckInNavGraph()
            }
        }
    }
}

@Composable
private fun PermissionDeniedDialog(
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Permissions Required", color = TextPrimary)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "This app needs the following permissions to function properly:",
                    color = TextSecondary
                )
                Spacer(Modifier.height(8.dp))
                Text("• Notifications - For emergency alerts", color = TextSecondary)
                Text("• SMS - To send emergency text messages", color = TextSecondary)
                Text("• Phone - To initiate emergency calls", color = TextSecondary)
                Text("• Exact Alarm - For precise check-in timing", color = TextSecondary)
            }
        },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text("Grant Permissions")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Maybe Later")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
