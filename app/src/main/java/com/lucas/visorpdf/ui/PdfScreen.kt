package com.lucas.visorpdf.ui

import android.graphics.Bitmap
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.lucas.visorpdf.model.Pdf
import kotlinx.coroutines.launch

// Pantalla con la lista de imagenes Bitmap mediante LazyColumn
@Composable
fun PdfScreen(
    option: Pdf,
    renderedPdfs: Map<String, List<Bitmap>>,
    navController: NavController
) {
    val bitmaps = renderedPdfs[option.name] ?: emptyList()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val currentPage by remember { derivedStateOf { listState.firstVisibleItemIndex + 1 } }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(bitmaps) { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }
        }

        bitmaps.firstOrNull()?.let { firstBitmap ->
            Text(
                text = "Resolución: ${firstBitmap.width} x ${firstBitmap.height}",
                modifier = Modifier.align(Alignment.TopEnd).padding(24.dp),
                color = Color.Red
            )
        }

        FloatingActionButton(
            onClick = { navController.navigate("HomeScreen") },
            modifier = Modifier.align(Alignment.BottomStart).padding(24.dp),
            containerColor = Color.Blue,
            contentColor = Color.White
        ) {
            Icon(Icons.Filled.Home, "Volver a Home")
        }

        FloatingActionButton(
            onClick = { coroutineScope.launch { listState.animateScrollToItem(bitmaps.size - 1) } },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = Color.Blue,
            contentColor = Color.White
        ) {
            Icon(Icons.Filled.KeyboardArrowDown, "Ir al final")
        }

        Text(
            text = "$currentPage de ${bitmaps.size}",
            modifier = Modifier.align(Alignment.TopStart).padding(24.dp),
            color = Color.Black
        )
    }
}
