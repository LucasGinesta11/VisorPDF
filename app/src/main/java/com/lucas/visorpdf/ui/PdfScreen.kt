package com.lucas.visorpdf.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.lucas.visorpdf.model.Pdf
import kotlinx.coroutines.launch
import java.io.File

@SuppressLint("UseKtx")
@Composable
fun PdfScreen(option: Pdf, navController: NavController) {
    val context = LocalContext.current
    var bitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    val listState = rememberLazyListState()
    val currentPage by remember { derivedStateOf { listState.firstVisibleItemIndex + 1 } }
    val coroutineScope = rememberCoroutineScope()

    // Archivo temporal
    val file = remember(option) {
        val inputStream = context.resources.openRawResource(option.resId)
        val outputFile = File(context.cacheDir, "${option.name}.pdf")
        inputStream.use { input ->
            outputFile.outputStream().use { output -> input.copyTo(output) }
        }
        outputFile
    }

    LaunchedEffect(file) {
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(descriptor)
        val tempBitMaps = mutableListOf<Bitmap>()

        for (i in 0 until renderer.pageCount) {
            val page = renderer.openPage(i)

            // Aumentamos la resolucion lo maximo posible
            val scale = 2f
            val width = (page.width * scale).toInt()
            val height = (page.height * scale).toInt()

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            tempBitMaps.add(bitmap)
        }

        renderer.close()
        descriptor.close()
        bitmaps = tempBitMaps
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(bitmaps) { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Página del PDF",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
                )
            }
        }

        FloatingActionButton(
            onClick = { navController.navigate("HomeScreen") },
            contentColor = Color.White,
            containerColor = Color.Blue,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
        ) {
            Icon(Icons.Filled.Home, "Volver a Home")
        }

        FloatingActionButton(
            onClick = { coroutineScope.launch { listState.animateScrollToItem(bitmaps.size - 1) } },
            contentColor = Color.White,
            containerColor = Color.Blue,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(Icons.Filled.KeyboardArrowDown, "Ir al final")
        }

        Text(
            text = "$currentPage de ${bitmaps.size}",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp),
            color = Color.Black
        )
    }
}