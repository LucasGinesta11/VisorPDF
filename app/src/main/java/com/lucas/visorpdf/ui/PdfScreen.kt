package com.lucas.visorpdf.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.lucas.visorpdf.model.Pdf
import java.io.File

@SuppressLint("UseKtx")
@Composable
fun PdfScreen(option: Pdf, navController: NavController) {
    // Contexto para acceder a archivos raw
    val context = LocalContext.current

    // Lista de cada pagina con imagen Bitmap
    var bitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }

    val listState = rememberLazyListState()
    val currentPage by remember {
        derivedStateOf {
            // Primera pagina visible
            val index = listState.firstVisibleItemIndex
            // Cuantos pixeles ocultos
            val offset = listState.firstVisibleItemScrollOffset
            // +2 por si el pdf empieza ocupando 100px de la segunda
            if (offset > 100) index + 2 else index + 1
        }
    }

    // Copia pdf a un archivo temporal en cache porque PdfRenderer no puede
    // trabajar directamente con raw
    val file = remember(option) {
        // Abre la carpeta donde se encuentra el pdf
        val inputStream = context.resources.openRawResource(option.resId)
        // Ruta temporal en el cache de la app
        val outputFile = File(context.cacheDir, "${option.name}.pdf")
        inputStream.use { input ->
            outputFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        outputFile
    }

    // Se ejecuta una vez por cada pdf
    LaunchedEffect(file) {
        // Abre el pdf con ParcelFileDirector en solo la lectura
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        // Crea un PdfRenderer con ese descriptor
        val renderer = PdfRenderer(descriptor)

        // Convierte cada pagina en una imagen (Bitmap) para mostrarlas por pantalla
        val tempBitMaps = mutableListOf<Bitmap>()
        for (i in 0 until renderer.pageCount) {
            val page = renderer.openPage(i)
            val bitMap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            page.render(bitMap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            tempBitMaps.add(bitMap)
            page.close()
        }

        renderer.close()
        descriptor.close()

        // Guarda la lista
        bitmaps = tempBitMaps
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Lista vertical de elementos
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            // BitMap mediante Image, con asImageBitmap para que pueda ser dibujado por Compose
            items(bitmaps) { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Pagina del PDF",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }
        }

        FloatingActionButton(
            onClick = { navController.navigate("HomeScreen") },
            containerColor = Color.Blue,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
        ) {
            Icon(imageVector = Icons.Filled.Home, contentDescription = "Volver a Home")
        }

        Text(text = "$currentPage de ${bitmaps.size}",
            modifier = Modifier
                .align (Alignment.BottomEnd)
                .padding(24.dp), color = Color.Black)
    }
}