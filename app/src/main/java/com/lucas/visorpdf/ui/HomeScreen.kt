package com.lucas.visorpdf.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.lucas.visorpdf.model.Pdfs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {

    val context = LocalContext.current
    // Obtener los valores del object
    val radioOptions = Pdfs.list
    // Tecnica para manejar seleccion de opciones
    val (selectedOption, onOptionSelected) = remember { mutableStateOf(radioOptions[0]) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PdfVisor", color = Color.White, fontSize = 25.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Blue),
                actions = {
                    IconButton(onClick = {navController.navigate("PdfScreen/${selectedOption.name}")}) {
                        Icon(imageVector = Icons.Filled.Done, "Entrar al visor", tint = Color.White)
                    }
                    IconButton(onClick = {(context as Activity).finish()}) {
                        Icon(imageVector = Icons.Filled.Close, "Salir de la aplicacion", tint = Color.White)
                    }
                }
            )
        }, content = { paddingValues ->
            LazyColumn(modifier = Modifier
                .selectableGroup()
                .padding(paddingValues)
                .fillMaxSize()
                .background(Color.White)) {
                items(radioOptions.toList()) { option ->

                    Spacer(modifier = Modifier.padding(10.dp))

                    Row(
                        modifier = Modifier
                            .height(30.dp)
                            .selectable(
                                selected = (option == selectedOption),
                                onClick = { onOptionSelected(option) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp)
                    ) {
                        RadioButton(
                            selected = (option == selectedOption),
                            onClick = null
                        )
                        Text(
                            text = option.name.replaceFirstChar { it.uppercase() },
                            color = Color.Black
                        )
                    }
                }
            }
        }
    )
}