package com.lucas.visorpdf

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.lucas.visorpdf.navigaton.Navigation
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

                // Navigation ya se encarga de decidir qué pantalla mostrar
                Navigation(
                    renderedPdfs = pdfViewModel.renderedPdfs.value,
                    navController = navController,
                    viewModel = pdfViewModel
                )
            }
        }
    }
}
