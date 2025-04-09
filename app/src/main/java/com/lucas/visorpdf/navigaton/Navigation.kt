package com.lucas.visorpdf.navigaton

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lucas.visorpdf.model.Pdfs
import com.lucas.visorpdf.ui.HomeScreen
import com.lucas.visorpdf.ui.PdfScreen

@Composable
fun Navigation() {
    val navController: NavHostController = rememberNavController()

    NavHost(navController = navController, startDestination = "HomeScreen") {
        composable("HomeScreen") {
            HomeScreen(navController)
        }
        composable("PdfScreen/{option}") { backStackEntry ->
            val optionString = backStackEntry.arguments?.getString("option") ?: ""
            val option = Pdfs.getByName(optionString)
            PdfScreen(option, navController)
        }
    }
}