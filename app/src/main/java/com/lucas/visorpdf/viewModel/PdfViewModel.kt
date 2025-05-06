package com.lucas.visorpdf.viewModel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import android.util.LruCache
import androidx.compose.runtime.mutableStateOf
import androidx.core.graphics.createBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lucas.visorpdf.model.Pdf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class PdfViewModel : ViewModel() {

    // Mapa con el nombre de pdfs y la lista de rutas a las imagenes
    val renderedPdfs = mutableStateOf<Map<String, List<String>>>(emptyMap())

    // Memoria cache
    val memoryCache =
        object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 1024 / 6).toInt()) {
            override fun sizeOf(key: String, value: Bitmap): Int {
                return value.byteCount / 1024
            }
        }

    // Carga de primeras paginas
    fun loadInitialPages(context: Context, pdf: Pdf, onFinished: (Pair<Map<String, List<String>>, Int>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val renderedMap = mutableMapOf<String, List<String>>()
            var totalPages = 0

            try {
                clearPdf(context, pdf.name)

                val file = File(context.filesDir, "${pdf.name}.pdf").apply {
                    parentFile?.mkdirs()
                }

                context.resources.openRawResource(pdf.resId).use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }

                val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(descriptor)
                totalPages = renderer.pageCount

                val renderedPages = renderPages(context, renderer, 0, minOf(5, totalPages), pdf.name)
                renderedMap[pdf.name] = renderedPages

                renderer.close()
                descriptor.close()

            } catch (e: Exception) {
                e.printStackTrace()
            }

            renderedPdfs.value = renderedMap
            onFinished(Pair(renderedMap, totalPages))
        }
    }


    // Llamada para cargar otras 5 paginas
    fun loadMorePages(context: Context, name: String, onFinished: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentRendered = renderedPdfs.value[name] ?: emptyList()
                val currentCount = currentRendered.size

                // Necesitamos volver a abrir el archivo y el renderer para cargar más páginas
                val file = File(context.filesDir, "${name}.pdf")
                val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(descriptor)

                val totalPages = renderer.pageCount

                if (currentCount >= totalPages) {
                    renderer.close()
                    descriptor.close()
                    onFinished()
                    return@launch
                }

                val pagesToLoad = minOf(5, totalPages - currentCount)
                val newPages = renderPages(context, renderer, currentCount, pagesToLoad, name)

                // Actualizamos el estado
                val updatedMap = renderedPdfs.value.toMutableMap()
                updatedMap[name] = currentRendered + newPages
                renderedPdfs.value = updatedMap

                // Cerrar recursos inmediatamente después de renderizar
                renderer.close()
                descriptor.close()

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                onFinished()
            }
        }
    }

    // Renderiza las paginas de pdfs
    fun renderPages(
        context: Context,
        renderer: PdfRenderer,
        start: Int,
        count: Int,
        pdfName: String
    ): List<String> {
        val renderedPaths = mutableListOf<String>()
        val end = minOf(start + count, renderer.pageCount)

        for (i in start until end) {
            val filePath = File(context.cacheDir, "pdf_bitmaps/$pdfName/page_$i.png")
            if (filePath.exists()) {
                renderedPaths.add(filePath.absolutePath)
                continue
            }

            val page = renderer.openPage(i)
            try {
                val targetWidth = 1920
                val scale = targetWidth.toFloat() / page.width
                val targetHeight = (page.height * scale).toInt()

                val bitmap = createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                // Guardar la imagen renderizada en disco
                filePath.parentFile?.mkdirs()
                filePath.outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 60, it)
                }
                renderedPaths.add(filePath.absolutePath)

                putBitmapInCache(filePath.absolutePath, bitmap)

            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
                System.gc()
                break
            } finally {
                page.close()
            }
        }
        return renderedPaths
    }

    // Elimina todos los datos del PDF
    fun clearPdf(context: Context, name: String) {
        // Limpiar el mapa de PDFs renderizados
        renderedPdfs.value = renderedPdfs.value.toMutableMap().apply {
            remove(name)
        }

        // Limpiar la caché de memoria
        memoryCache.evictAll()

        // Limpiar archivos en caché de disco
        val dir = File(context.cacheDir, "pdf_bitmaps/$name")
        if (dir.exists()) {
            dir.deleteRecursively()
        }
    }

    // Calculo de total de paginas
    fun getTotalPages(context: Context, name: String): Int {
        return try {
            val file = File(context.filesDir, "${name}.pdf")
            if (file.exists()) {
                val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(descriptor)
                val count = renderer.pageCount
                renderer.close()
                descriptor.close()
                count
            } else {
                Log.d("No existe", "No existe")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("Error", "Error")
        }
    }

    // Agrega los bitmaps en cache
    fun getBitmapFromCache(key: String): Bitmap? {
        return memoryCache.get(key)
    }

    // Elimina los bitmaps de cache
    fun putBitmapInCache(key: String, bitmap: Bitmap) {
        memoryCache.put(key, bitmap)
    }
}