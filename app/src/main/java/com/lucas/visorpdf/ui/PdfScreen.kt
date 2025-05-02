package com.lucas.visorpdf.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.lucas.visorpdf.model.Pdf
import com.lucas.visorpdf.viewModel.PdfViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("FrequentlyChangedStateReadInComposition")
@Composable
fun PdfScreen(
    option: Pdf,
    renderedPdfs: Map<String, List<String>>,
    navController: NavController,
    viewModel: PdfViewModel
) {
    val context = LocalContext.current
    val imagePaths by remember(option.name, renderedPdfs) {
        derivedStateOf { renderedPdfs[option.name] ?: emptyList() }
    }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val totalPages = viewModel.getTotalPages(option.name)
    var isLoadingMore by remember { mutableStateOf(false) }
    var isInitialLoad by remember { mutableStateOf(true) }

    var pdfResolution by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // Cargar mas paginas cuando nos acercamos al final
    LaunchedEffect(listState, imagePaths, isLoadingMore) {
        snapshotFlow {
            Pair(
                listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0,
                imagePaths.size
            )
        }.collect { (lastVisibleIndex, pathCount) ->
            // Cargamos mas cuando queden 5
            if (lastVisibleIndex >= pathCount - 5 &&
                pathCount < totalPages &&
                !isLoadingMore &&
                !isInitialLoad
            ) {
                isLoadingMore = true
                viewModel.loadMorePages(context, option.name) {
                    isLoadingMore = false
                }
            }
        }
    }

    // Precarga las imágenes en cache cuando cambia el PDF
    LaunchedEffect(option.name) {
        isInitialLoad = true
        imagePaths.forEach { path ->
            if (viewModel.getBitmapFromCache(path) == null) {
                val bitmap = withContext(Dispatchers.IO) {
                    // Si no esta en el cache se carga desde el archivo
                    BitmapFactory.decodeFile(path)
                }
                // Y ya se guarda en el cache
                bitmap?.let { viewModel.putBitmapInCache(path, it) }
            }
        }
        isInitialLoad = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(imagePaths, key = { it }) { path ->

                // Usamos un estado derivado para cada bitmap
                val bitmap = remember(path) { mutableStateOf<Bitmap?>(null) }

                // Recicla el bitmap cuando ya no esta visible
                DisposableEffect(path) {
                    onDispose {
                        bitmap.value?.recycle()
                        bitmap.value = null
                    }
                }

                LaunchedEffect(path) {
                    withContext(Dispatchers.IO) {
                        val cached = viewModel.getBitmapFromCache(path)
                        val bmp = cached ?: BitmapFactory.decodeFile(path)?.also {
                            viewModel.putBitmapInCache(path, it)
                        }

                        if (bmp != null) {
                            bitmap.value = bmp

                            // Solo actualiza la resolución si aún no se ha definido
                            if (pdfResolution == null) {
                                pdfResolution = Pair(bmp.width, bmp.height)
                            }
                        }
                    }
                }

                if (bitmap.value != null) {
                    Image(
                        bitmap = bitmap.value!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Hasta que se decodifican los bitmaps
                        CircularProgressIndicator()
                    }
                }
            }

            // Mostrar un indicador cuando se están cargando más páginas
            if (isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }

        pdfResolution?.let { (width, height) ->
            Text(
                text = "Resolucion: $width x $height",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp),
                color = Color.Black
            )
        }

        // Navegar al seleccionador de pdfs
        FloatingActionButton(
            onClick = { navController.navigate("HomeScreen") },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp),
            containerColor = Color.Blue,
            contentColor = Color.White
        ) {
            Icon(Icons.Filled.Home, contentDescription = "Volver a Home")
        }

        // Bajar a la ultima pagina cargada
        FloatingActionButton(
            onClick = {
                coroutineScope.launch {
                    listState.animateScrollToItem(imagePaths.lastIndex)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = Color.Blue,
            contentColor = Color.White
        ) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Ir al final")
        }

        Text(
            text = "${listState.firstVisibleItemIndex + 1} de $totalPages",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp),
            color = Color.Black
        )
    }

}