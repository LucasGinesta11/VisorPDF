package com.lucas.visorpdf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lucas.visorpdf.navigaton.Navigation
import com.lucas.visorpdf.ui.theme.VisorPDFTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VisorPDFTheme {
                Navigation()
            }
        }
    }
}
