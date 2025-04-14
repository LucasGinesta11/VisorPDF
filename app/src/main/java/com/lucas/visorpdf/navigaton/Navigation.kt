package com.lucas.visorpdf.navigaton

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.lucas.visorpdf.model.Pdfs
import com.lucas.visorpdf.ui.HomeScreen
import com.lucas.visorpdf.ui.PdfScreen

@Composable
fun Navigation(renderedPdfs: Map<String, List<Bitmap>>, navController: NavHostController) {
    // Pantalla principal y a la que se navegara al clickar el boton Home
    NavHost(navController = navController, startDestination = "HomeScreen") {
        composable("HomeScreen") {
            HomeScreen(navController)
        }
        // Pdf al que se navegara una vez seleccionado por su nombre, donde estaran los bitmaps
        // que se han generado en el render y han sido guardados en el viewModel
        composable("PdfScreen/{option}") { backStackEntry ->
            val optionString = backStackEntry.arguments?.getString("option") ?: ""
            val option = Pdfs.getByName(optionString)
            PdfScreen(option, renderedPdfs, navController)
        }
    }
}