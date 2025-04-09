package com.lucas.visorpdf.model

import com.lucas.visorpdf.R

data class Pdf(val name: String, val resId: Int)

object Pdfs {
    val list = listOf(
        Pdf("android", R.raw.android),
        Pdf("animales", R.raw.animales),
        Pdf("deporte", R.raw.deporte),
        Pdf("mecanica", R.raw.mecanica),
        Pdf("orbys", R.raw.orbys)
    )

    fun getByName(name: String): Pdf = list.firstOrNull { it.name == name } ?: list.first()
}
