package com.example.dynamicmoduledemo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dynamicmoduledemo.ui.theme.DynamicModuleDemoTheme
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus

class MainActivity : ComponentActivity() {
    private lateinit var splitInstallManager: SplitInstallManager
    private var mySessionId: Int? = null
    private val REQUEST_CODE: Int = 222

    private var isDownloading by mutableStateOf(false)
    private var downloadProgress by mutableFloatStateOf(0f)
    private var statusMessage by mutableStateOf("")

    // Define the listener as a property to manage its lifecycle correctly
    private val splitInstallStateUpdatedListener = SplitInstallStateUpdatedListener { state ->
        // LOGGING: Crucial for debugging status changes
        Log.d("MainActivity", "Status: ${state.status()} for modules: ${state.moduleNames()}")

        // FIX: Match session ID OR check if the module name matches if sessionId isn't set yet
        val isOurSession = state.sessionId() == mySessionId ||
                (mySessionId == null && state.moduleNames().contains("ondemand"))


        if (isOurSession) {
            // Update our local sessionId if we didn't have it yet
            if (mySessionId == null) mySessionId = state.sessionId()

            when (state.status()) {
                SplitInstallSessionStatus.DOWNLOADING -> {
                    isDownloading = true
                    statusMessage = "Downloading..."
                    val totalBytes = state.totalBytesToDownload()
                    if (totalBytes > 0) {
                        downloadProgress = state.bytesDownloaded().toFloat() / totalBytes
                    }
                }

                SplitInstallSessionStatus.INSTALLING -> {
                    isDownloading = true
                    statusMessage = "Installing..."
                }

                SplitInstallSessionStatus.INSTALLED -> {
                    isDownloading = false
                    statusMessage = "Installed"
                    Toast.makeText(this, "Module installed successfully", Toast.LENGTH_SHORT).show()
                    navigateToOnDemand()
                }

                SplitInstallSessionStatus.FAILED -> {
                    isDownloading = false
                    statusMessage = "Failed: ${state.errorCode()}"
                    Log.d("MainActivity", "Error in downloading: ${state.errorCode()}")
                }

                SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION -> {
                    splitInstallManager.startConfirmationDialogForResult(state, this, REQUEST_CODE)
                }

                SplitInstallSessionStatus.CANCELED -> {
                    isDownloading = false
                    statusMessage = "Cancelled"
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
                }

                SplitInstallSessionStatus.CANCELING -> {
                    statusMessage = "Cancelling..."
                }

                SplitInstallSessionStatus.PENDING -> {
                    isDownloading = true
                    statusMessage = "Pending..."
                }

                SplitInstallSessionStatus.DOWNLOADED -> {
                    isDownloading = true
                    statusMessage = "Downloaded"
                }

                SplitInstallSessionStatus.UNKNOWN -> {
                    isDownloading = true
                    statusMessage = "Unknown"
                }
            }
        }
//        else{
//            isDownloading = false
//            statusMessage = "Not Downloaded"
//            Log.d("MainActivity", "StatusListener: Session not match")
//        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        splitInstallManager = SplitInstallManagerFactory.create(this)
        splitInstallManager.registerListener(splitInstallStateUpdatedListener)

        enableEdgeToEdge()
        setContent {
            DynamicModuleDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        modifier = Modifier.padding(innerPadding),
                        isDownloading = isDownloading,
                        progress = downloadProgress,
                        statusMessage=statusMessage,
                        onInstallClick = {
                            val intent = Intent()
                            intent.setClassName(
                                BuildConfig.APPLICATION_ID,
                                "com.example.oninstall.MainActivity"
                            )
                            startActivity(intent)
                        },
                        onDemandClick = {
                            startDownloading()
                        }
                    )
                }
            }
        }
    }

    private fun startDownloading() {

        // If already installed, skip download and just navigate
//        if (splitInstallManager.installedModules.contains("ondemand")) {
//            navigateToOnDemand()
//            return
//        }

        val splitInstallRequest: SplitInstallRequest =
            SplitInstallRequest.newBuilder().addModule("ondemand").build()

        isDownloading = true
        statusMessage = "Requesting..."

        splitInstallManager.startInstall(splitInstallRequest).addOnFailureListener({ e ->
            Log.d("MainActivity", "startDownloading: Exception: ${e.message}")
            isDownloading = false
            statusMessage = "Error: ${e.message}"
        }).addOnSuccessListener { sessionId ->
            mySessionId = sessionId
            Log.d("MainActivity", "startDownloading: Success")
        }

    }

    private fun navigateToOnDemand() {
        val intent = Intent().setClassName(
            BuildConfig.APPLICATION_ID,
            "com.example.ondemand.MainActivity"
        )
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        splitInstallManager.unregisterListener(splitInstallStateUpdatedListener)
    }
}

@Composable
fun Greeting(
    modifier: Modifier = Modifier,
    isDownloading: Boolean = false,
    progress: Float = 0f,
    statusMessage: String = "",
    onInstallClick: () -> Unit,
    onDemandClick: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isDownloading) {
            Text(text = statusMessage)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress, modifier = Modifier.width(200.dp))
            Spacer(modifier = Modifier.height(20.dp))
        }
        Button(onClick = onInstallClick, modifier = Modifier.width(150.dp), enabled = !isDownloading) {
            Text(text = "On Install")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = onDemandClick, modifier = Modifier.width(150.dp), enabled = !isDownloading) {
            Text("On Demand")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DynamicModuleDemoTheme {
        Greeting(onInstallClick = {}, onDemandClick = {})
    }
}
