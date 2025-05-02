package com.lucas.visorpdf.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lucas.visorpdf.viewModel.PdfViewModel

@SuppressLint("UseKtx")
@Composable
fun PdfRender(viewModel: PdfViewModel, onFinished: (Map<String, List<Bitmap>>) -> Unit) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }

    // Llama al viewModel para cargar las primeras 10 paginas de los pdfs
    LaunchedEffect(Unit) {
        viewModel.loadInitialPages(context) {
            isLoading = false
        }
    }

    // Carga inicial de las primeras 10 paginas de la lista de pdfs
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
            Text("Renderizando PDFs...", modifier = Modifier.padding(top = 60.dp))
        }
    }
}
