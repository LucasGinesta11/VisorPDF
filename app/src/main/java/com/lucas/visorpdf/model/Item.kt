package com.lucas.visorpdf.model

import com.lucas.visorpdf.R

data class Pdf(val name: String, val resId: Int)

// Lista de pdfs con nombre y ruta
object Pdfs {
    val list = listOf(
        Pdf("Animales", R.raw.animales),
        Pdf("Corazon", R.raw.corazon),
        Pdf("Android", R.raw.android),
        Pdf("Mecanica", R.raw.mecanica),
        Pdf("Orbys", R.raw.orbys)
    )

    // Metodo para buscar PDFs por nombre
    fun getByName(name: String): Pdf = list.firstOrNull { it.name == name } ?: list.first()
}
