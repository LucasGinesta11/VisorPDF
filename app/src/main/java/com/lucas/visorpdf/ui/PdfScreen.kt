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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.lucas.visorpdf.model.Pdf
import com.lucas.visorpdf.viewModel.PdfViewModel
import kotlinx.coroutines.Dispatchers
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
    var isLoadingMore by remember { mutableStateOf(false) }
    var isInitialLoad by remember { mutableStateOf(true) }
    var pdfResolution by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var totalPages by remember { mutableIntStateOf(0) }

    // Cargar más páginas cuando nos acercamos al final
    LaunchedEffect(listState, imagePaths, isLoadingMore, totalPages) {
        snapshotFlow {
            Pair(
                listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0,
                imagePaths.size
            )
        }.collect { (lastVisibleIndex, pathCount) ->
            // Anticipacion de 5 paginas
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

    // Cargar el PDF cuando se entra a la pantalla
    LaunchedEffect(option.name) {
        isInitialLoad = true
        if (!renderedPdfs.containsKey(option.name)) {
            viewModel.loadInitialPages(context, option) { (_, pagesCount) ->
                totalPages = pagesCount
                isInitialLoad = false
            }
        } else {
            // Si ya estaba cargado, obtenemos el total de páginas
            totalPages = viewModel.getTotalPages(context, option.name)
            isInitialLoad = false
        }
    }

    // Limpiar cuando se sale de la pantalla
//    DisposableEffect(Unit) {
//        onDispose {
//            viewModel.clearPdf(context, option.name)
//        }
//    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isInitialLoad) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                items(imagePaths, key = { it }) { path ->
                    val bitmap = remember(path) { mutableStateOf<Bitmap?>(null) }

                    LaunchedEffect(path) {
                        withContext(Dispatchers.IO) {
                            val cached = viewModel.getBitmapFromCache(path)
                            val bmp = cached ?: BitmapFactory.decodeFile(path)?.also {
                                viewModel.putBitmapInCache(path, it)
                            }

                            if (bmp != null) {
                                bitmap.value = bmp
                                if (pdfResolution == null) {
                                    pdfResolution = Pair(bmp.width, bmp.height)
                                }
                            }
                        }
                    }

                    // Muestra los bitmaps con Image cuando esten cargados
                    if (bitmap.value != null) {
                        Image(
                            bitmap = bitmap.value!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Pequeña carga de las paginas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                // Carga de mas paginas
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
        }

        // Resolucion de las paginas
        pdfResolution?.let { (width, height) ->
            Text(
                text = "Resolución: $width x $height",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp),
                color = Color.Black
            )
        }

        // Boton para volver a la lista de pdfs
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

        // Total de paginas
        Text(
            text = "${listState.firstVisibleItemIndex + 1} de $totalPages",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp),
            color = Color.Black
        )
    }
}
