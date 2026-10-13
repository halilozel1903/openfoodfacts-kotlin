package io.github.halilozel1903.openfoodfacts.sample

import android.content.Context
import android.content.ContextWrapper
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.awaitCancellation
import java.util.concurrent.Executors

/**
 * A live camera preview that reads retail barcodes (EAN-13, EAN-8, UPC-A, UPC-E) with ML Kit's bundled model and
 * calls [onBarcode] with each code it reads. CameraX binds the camera to the activity's lifecycle and releases it
 * when this leaves the composition. The caller asks for the camera permission first.
 */
@OptIn(ExperimentalGetImage::class)
@Composable
fun BarcodeScanner(onBarcode: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = remember(context) { context.lifecycleOwner() }
    val currentOnBarcode by rememberUpdatedState(onBarcode)
    val previewView = remember(context) {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }

    LaunchedEffect(lifecycleOwner, previewView) {
        val owner = lifecycleOwner ?: return@LaunchedEffect
        val provider = ProcessCameraProvider.awaitInstance(context)
        val executor = Executors.newSingleThreadExecutor()
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E)
                .build(),
        )
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        analysis.setAnalyzer(executor) { proxy ->
            val image = proxy.image
            if (image == null) {
                proxy.close()
                return@setAnalyzer
            }
            scanner.process(InputImage.fromMediaImage(image, proxy.imageInfo.rotationDegrees))
                .addOnSuccessListener(mainExecutor) { codes ->
                    codes.firstNotNullOfOrNull { it.rawValue?.takeIf { value -> value.all(Char::isDigit) } }
                        ?.let { currentOnBarcode(it) }
                }
                .addOnCompleteListener { proxy.close() }
        }
        try {
            provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            awaitCancellation()
        } finally {
            provider.unbind(preview, analysis)
            analysis.clearAnalyzer()
            scanner.close()
            executor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

private tailrec fun Context.lifecycleOwner(): LifecycleOwner? = when (this) {
    is LifecycleOwner -> this
    is ContextWrapper -> baseContext.lifecycleOwner()
    else -> null
}
