package com.lucas.visorpdf

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.lucas.visorpdf.navigaton.Navigation
import com.lucas.visorpdf.ui.PdfRender
import com.lucas.visorpdf.ui.theme.VisorPDFTheme
import com.lucas.visorpdf.viewModel.PdfViewModel

class MainActivity : ComponentActivity() {
    @SuppressLint("StateFlowValueCalledInComposition")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VisorPDFTheme {
                val navController = rememberNavController()
                val pdfViewModel: PdfViewModel = viewModel()

                // Se llama a la pantalla de carga si no estan cargados
                if (pdfViewModel.renderedPdfs.value.isEmpty()) {
                    // Pantalla de carga mientras se renderizan los PDFs
                    PdfRender(viewModel = pdfViewModel) {
                    }
                } else {
                    // Mostrar los PDFs renderizados
                    Navigation(
                        renderedPdfs = pdfViewModel.renderedPdfs.value,
                        navController = navController,
                        viewModel = pdfViewModel
                    )
                }
            }
        }
    }
}