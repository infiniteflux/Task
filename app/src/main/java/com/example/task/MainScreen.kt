package com.example.task

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    var isStreaming by remember { mutableStateOf(false) }
    var streamUrl by remember { mutableStateOf<String?>(null) }
    var showPermissionRationale by remember { mutableStateOf(false) }

    // Update streaming state
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(500)
            isStreaming = ScreenStreamingService.isStreaming
            streamUrl = ScreenStreamingService.streamUrl
        }
    }

    // Permissions
    val permissions = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val permissionsState = rememberMultiplePermissionsState(permissions)

    // Media projection launcher
    val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                startStreamingService(context, result.resultCode, data)
            }
        } else {
            Toast.makeText(context, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Title
        Text(
            text = "Task",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Stream your screen to your laptop",
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isStreaming)
                    Color(0xFF4CAF50).copy(alpha = 0.1f)
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isStreaming) "🟢 Streaming" else "⚫ Not Streaming",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isStreaming) Color(0xFF4CAF50) else Color.Gray
                )

                if (isStreaming && streamUrl != null) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Open this URL in your laptop's media player:",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        SelectionContainer {
                            Text(
                                text = streamUrl!!,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Use VLC Media Player or ffplay:\nffplay -rtsp_transport tcp $streamUrl",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Control Button
        Button(
            onClick = {
                if (!isStreaming) {
                    if (permissionsState.allPermissionsGranted) {
                        // Request screen capture
                        val intent = mediaProjectionManager.createScreenCaptureIntent()
                        mediaProjectionLauncher.launch(intent)
                    } else {
                        permissionsState.launchMultiplePermissionRequest()
                        showPermissionRationale = true
                    }
                } else {
                    stopStreamingService(context)
                }
            },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isStreaming)
                    Color(0xFFE53935)
                else
                    MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text(
                text = if (isStreaming) "Stop Streaming" else "Start Streaming",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Info Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Instructions:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "1. Connect both devices to same WiFi\n" +
                            "2. Grant all required permissions\n" +
                            "3. Start streaming\n" +
                            "4. Copy the URL and open in VLC/ffplay",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                    lineHeight = 20.sp
                )
            }
        }

        // Permission rationale dialog
        if (showPermissionRationale && !permissionsState.allPermissionsGranted) {
            AlertDialog(
                onDismissRequest = { showPermissionRationale = false },
                title = { Text("Permissions Required") },
                text = {
                    Text(
                        "This app needs the following permissions:\n\n" +
                                "• Audio Recording: To stream your device audio\n" +
                                "• Notifications: To show streaming status\n" +
                                "• Screen Capture: To capture your screen"
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            permissionsState.launchMultiplePermissionRequest()
                            showPermissionRationale = false
                        }
                    ) {
                        Text("Grant Permissions")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPermissionRationale = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

private fun startStreamingService(context: Context, resultCode: Int, data: Intent) {
    val intent = Intent(context, ScreenStreamingService::class.java).apply {
        action = ScreenStreamingService.ACTION_START
        putExtra(ScreenStreamingService.EXTRA_RESULT_CODE, resultCode)
        putExtra(ScreenStreamingService.EXTRA_DATA, data)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

private fun stopStreamingService(context: Context) {
    val intent = Intent(context, ScreenStreamingService::class.java).apply {
        action = ScreenStreamingService.ACTION_STOP
    }
    context.startService(intent)
}