package com.lucas.visorpdf.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import com.lucas.visorpdf.viewModel.PdfViewModel
import kotlinx.coroutines.launch

@Composable
fun PdfScreen(
    option: Pdf,
    renderedPdfs: Map<String, List<String>>,
    navController: NavController,
    viewModel: PdfViewModel
) {
    // Bitmaps de los pdfs divididos por nombre
    val bitmaps = renderedPdfs[option.name] ?: emptyList()

    val imagePaths = renderedPdfs[option.name] ?: emptyList()

    // Logico de conteo de paginas
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val currentPage by remember { derivedStateOf { listState.firstVisibleItemIndex + 1 } }

    // Total de paginas
    val totalPages = viewModel.getTotalPages(option.name)
    var isLoadingMore by remember { mutableStateOf(false) }

    // Resolucion de la pantalla
    val context = LocalContext.current
    val displayMetrics = context.resources.displayMetrics

    val screenWidthPx = displayMetrics.widthPixels
    val screenHeightPx = displayMetrics.heightPixels

    // Renderiza mas paginas cuando llegue a la penultima cargada
    val endReached by remember {
        derivedStateOf {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val renderedCount = viewModel.renderedPdfs.value[option.name]?.size ?: 0
            lastVisibleIndex >= renderedCount - 1
        }
    }

    // Cuando llegue al final de los bitmaps cargados muestre mas
    LaunchedEffect(endReached) {
        if (endReached && bitmaps.size < totalPages && !isLoadingMore) {
            isLoadingMore = true
            viewModel.loadMorePages(context, option.name)
            isLoadingMore = false
        }
    }

    // Box del visor de pdfs
    Box(modifier = Modifier.fillMaxSize()) {
        // LazyColumn con las imagenes de bitmaps y que ademas ahorra memoria
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(imagePaths) { path ->
                val bitmap = remember(path) {
                    android.graphics.BitmapFactory.decodeFile(path.toString())
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null
                    )
                }
            }
        }

        // Resolucion de la pantalla
        Text(
            text = "Resolucion: $screenWidthPx x $screenHeightPx",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp),
            color = Color.Red
        )


        // Boton flotante para volver a la lista de pdfs
        FloatingActionButton(
            onClick = { navController.navigate("HomeScreen") },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp),
            containerColor = Color.Blue,
            contentColor = Color.White
        ) {
            Icon(Icons.Filled.Home, "Volver a Home")
        }

        // Boton flotante para ir a la ultima pagina renderizada
        FloatingActionButton(
            onClick = {
                coroutineScope.launch {
                    listState.animateScrollToItem(bitmaps.lastIndex)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = Color.Blue,
            contentColor = Color.White
        ) {
            Icon(Icons.Filled.KeyboardArrowDown, "Ir al final")
        }

        // Conteo de paginas de la actual a la ultima ultima del pdf
        Text(
            text = "$currentPage de $totalPages",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp),
            color = Color.Black
        )
    }
}
