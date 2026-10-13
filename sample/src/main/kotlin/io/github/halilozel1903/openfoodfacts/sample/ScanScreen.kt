package io.github.halilozel1903.openfoodfacts.sample

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.github.halilozel1903.openfoodfacts.Barcode
import io.github.halilozel1903.openfoodfacts.images.ImageSize
import io.github.halilozel1903.openfoodfacts.model.Product

/**
 * Scan with the camera, or type a barcode. Below, real products to open without a camera, looked up live by their
 * barcodes. [cameraAvailable] is false in screenshot scenes, which never start the camera.
 */
@Composable
fun ScanScreen(
    repository: FoodRepository,
    initialBarcode: String,
    cameraAvailable: Boolean,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var reload by remember { mutableIntStateOf(0) }
    val examples by produceState<Load<List<Product>>>(Load.Loading, reload) {
        value = Load.Loading
        value = load { repository.examples() }
    }
    ReportLoad(examples)

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text("Scan a product", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "Nutri-Score, NOVA, nutrition and allergens from Open Food Facts",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { CameraCard(cameraAvailable = cameraAvailable, onBarcode = onOpen) }
        item { BarcodeEntry(initial = initialBarcode, onSubmit = onOpen) }
        item {
            Text(
                "Try these products",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        when (val state = examples) {
            Load.Loading -> item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            is Load.Failed -> item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { reload++ }) { Text("Try again") }
                }
            }
            is Load.Ready -> items(state.value, key = { it.code }) { product ->
                ProductRow(product, onClick = { onOpen(product.code) })
            }
        }
    }
}

@Composable
private fun CameraCard(cameraAvailable: Boolean, onBarcode: (String) -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var scanning by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        scanning = ok
    }
    // A scan opens the product once; the camera stops while the product is shown.
    var lastCode by remember { mutableStateOf<String?>(null) }

    Box(
        Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1D241D)),
        contentAlignment = Alignment.Center,
    ) {
        if (cameraAvailable && scanning && granted) {
            BarcodeScanner(
                onBarcode = { code ->
                    if (code != lastCode) {
                        lastCode = code
                        scanning = false
                        onBarcode(code)
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
            ViewfinderIcon(Color.White, Modifier.size(width = 220.dp, height = 120.dp))
        } else {
            ViewfinderIcon(Color.White.copy(alpha = 0.5f), Modifier.size(width = 220.dp, height = 120.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BarcodeIcon(Color.White, Modifier.size(44.dp))
                FilledTonalButton(
                    onClick = {
                        lastCode = null
                        if (granted) scanning = true else permission.launch(Manifest.permission.CAMERA)
                    },
                    enabled = cameraAvailable,
                ) { Text("Scan with camera") }
            }
        }
    }
}

@Composable
private fun BarcodeEntry(initial: String, onSubmit: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    val code = Barcode.normalize(text)
    val submit = { code?.let(onSubmit) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.take(24) },
            label = { Text("Barcode") },
            singleLine = true,
            supportingText = {
                Text(
                    when {
                        text.isBlank() -> "EAN-13, EAN-8 or UPC"
                        code == null -> "Digits only"
                        Barcode.isValidGtin(code) -> "Valid check digit"
                        else -> "Not a GTIN, still searchable"
                    },
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submit() }),
            modifier = Modifier.weight(1f),
        )
        Button(onClick = { submit() }, enabled = code != null, modifier = Modifier.padding(bottom = 18.dp)) { Text("Look up") }
    }
}

/** A product in a list: photo, name, brand and quantity, Nutri-Score. */
@Composable
fun ProductRow(product: Product, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProductImage(
            url = product.imageUrl(size = ImageSize.Small, languages = listOf("en")),
            description = product.displayName,
            modifier = Modifier.size(64.dp),
            corner = 10.dp,
            padding = 4.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(product.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val details = listOfNotNull(product.brand, product.quantity?.takeIf { it.isNotBlank() }).joinToString(" · ")
            if (details.isNotEmpty()) {
                Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(product.code, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
        Spacer(Modifier.width(8.dp))
        NutriScoreChip(product.nutriScore)
    }
}
