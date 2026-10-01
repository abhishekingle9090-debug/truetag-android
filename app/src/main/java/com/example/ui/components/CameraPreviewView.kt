package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import java.util.concurrent.Executors

private const val TAG = "TrueTagOCR"

/**
 * Controller to trigger still camera capture and torch control from Compose UI.
 */
class CameraCaptureController {
    var captureImage: ((onSuccess: (Bitmap, Int) -> Unit, onError: (Exception) -> Unit) -> Unit)? = null
    var setTorchEnabled: ((Boolean) -> Unit)? = null

    fun takePicture(onSuccess: (Bitmap, Int) -> Unit, onError: (Exception) -> Unit) {
        captureImage?.invoke(onSuccess, onError)
    }

    fun toggleTorch(enabled: Boolean) {
        setTorchEnabled?.invoke(enabled)
    }
}

@Composable
fun rememberCameraCaptureController(): CameraCaptureController {
    return remember { CameraCaptureController() }
}

/**
 * CameraX Live Viewfinder with on-demand ImageCapture still photo capabilities.
 * NO continuous background scanning. Scans are strictly user-triggered via controller.takePicture().
 */
@SuppressLint("UnsafeOptInUsageError")
@Composable
fun CameraPreviewView(
    controller: CameraCaptureController,
    isTorchOn: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var cameraRef = remember { arrayOfNulls<Camera>(1) }

    LaunchedEffect(isTorchOn) {
        cameraRef[0]?.cameraControl?.enableTorch(isTorchOn)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            controller.captureImage = null
            controller.setTorchEnabled = null
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        cameraProvider.unbindAll()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        // Use CameraX ImageCapture for still photo capture, NOT ImageAnalysis
                        val imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()

                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture
                        )
                        cameraRef[0] = camera

                        // Wire up torch control
                        camera.cameraControl.enableTorch(isTorchOn)
                        controller.setTorchEnabled = { enabled ->
                            try {
                                camera.cameraControl.enableTorch(enabled)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to toggle torch: ${e.message}")
                            }
                        }

                        // Wire up on-demand single-frame capture
                        controller.captureImage = { onSuccess, onError ->
                            imageCapture.takePicture(
                                cameraExecutor,
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                        try {
                                            val rotation = imageProxy.imageInfo.rotationDegrees
                                            val bitmap = imageProxy.toBitmap()
                                            imageProxy.close()
                                            onSuccess(bitmap, rotation)
                                        } catch (e: Exception) {
                                            imageProxy.close()
                                            Log.e(TAG, "Error converting imageProxy to bitmap", e)
                                            onError(e)
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        Log.e(TAG, "CameraX capture error: ${exception.message}", exception)
                                        onError(exception)
                                    }
                                }
                            )
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to bind camera use cases", e)
                    }
                }, ctx.mainExecutor)

                previewView
            }
        )
    }
}
