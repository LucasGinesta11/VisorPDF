package com.lucas.visorpdf.model

import com.lucas.visorpdf.R

data class Pdf(val name: String, val resId: Int)

object Pdfs {
    val list = listOf(
        Pdf("Tartarín de Tarascón", R.raw.libro),
        Pdf("Animales", R.raw.animales),
        Pdf("Deporte", R.raw.deporte),
        Pdf("Mecanica", R.raw.mecanica),
        Pdf("Orbys", R.raw.orbys),
        Pdf("Corazon", R.raw.corazon)
    )

    fun getByName(name: String): Pdf = list.firstOrNull { it.name == name } ?: list.first()
}
