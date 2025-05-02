package com.lucas.visorpdf.viewModel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.runtime.mutableStateOf
import androidx.core.graphics.createBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lucas.visorpdf.model.Pdfs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class PdfViewModel : ViewModel() {

    // Mapa con el nombre de pdfs y la lista de rutas a las imagenes
    val renderedPdfs = mutableStateOf<Map<String, List<String>>>(emptyMap())

    // PdfRenderer que extraen las paginas como bitmaps
    val renderers = mutableMapOf<String, PdfRenderer>()

    // Archivos abiertos en .pdf para crear PdfRenderer
    val descriptors = mutableMapOf<String, ParcelFileDescriptor>()

    // Paginas renderizadas
    val pagesRendered = mutableMapOf<String, Int>()

    // Pdfs renderizados
    val fullLoadedPdfs = mutableSetOf<String>()


    // Memoria cache
    val memoryCache =
        object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()) {
            override fun sizeOf(key: String, value: Bitmap): Int {
                return value.byteCount / 1024
            }
        }

    // Carga de primeras paginas
    fun loadInitialPages(context: Context, onFinished: (Map<String, List<String>>) -> Unit) {
        // Corrutinas hasta que se destruya el viewModel
        viewModelScope.launch(Dispatchers.IO) {
            val renderedMap = mutableMapOf<String, List<String>>()

            // Procesamos cada PDF secuencialmente para evitar sobrecarga de memoria
            Pdfs.list.forEach { pdf ->
                try {
                    if (renderedPdfs.value.containsKey(pdf.name)) return@forEach

                    // Copiar Pdf al almacenamiento interno
                    val file = File(context.filesDir, "${pdf.name}.pdf").apply {
                        parentFile?.mkdirs()
                    }

                    context.resources.openRawResource(pdf.resId).use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }

                    // Renderizador
                    val descriptor =
                        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    descriptors[pdf.name] = descriptor
                    val renderer = PdfRenderer(descriptor)
                    storeRenderer(pdf.name, renderer)

                    // Renderizar las primeras 10 paginas
                    val renderedPages = renderPages(context, renderer, 0, 10, pdf.name)
                    renderedMap[pdf.name] = renderedPages
                    pagesRendered[pdf.name] = renderedPages.size

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Actualizamos el estado una sola vez al final
            renderedPdfs.value = renderedMap
            onFinished(renderedMap)
        }
    }

    // Llamada para cargar otras 10 paginas
    fun loadMorePages(context: Context, name: String, onFinished: () -> Unit) {
        if (fullLoadedPdfs.contains(name)) {
            onFinished()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val renderer = renderers[name] ?: run {
                onFinished()
                return@launch
            }

            val currentRendered = renderedPdfs.value[name] ?: emptyList()
            val currentCount = pagesRendered[name] ?: 0
            val totalPages = renderer.pageCount

            if (currentCount >= totalPages) {
                fullLoadedPdfs.add(name)
                onFinished()
                return@launch
            }

            try {
                val pagesToLoad = minOf(10, totalPages - currentCount)

                // Renderizamos las nuevas paginas
                val newPages = renderPages(context, renderer, currentCount, pagesToLoad, name)

                // Actualizamos el estado
                val updatedMap = renderedPdfs.value.toMutableMap()
                updatedMap[name] = currentRendered + newPages
                renderedPdfs.value = updatedMap
                pagesRendered[name] = currentCount + newPages.size

                // Verificamos si hemos terminado
                if (currentCount + pagesToLoad >= totalPages) {
                    fullLoadedPdfs.add(name)

                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                onFinished()
            }
        }
    }

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
                val targetWidth = 3840
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

    fun storeRenderer(name: String, renderer: PdfRenderer) {
        renderers[name] = renderer
        pagesRendered[name] = 0
    }

    fun getTotalPages(name: String): Int = renderers[name]?.pageCount ?: 0

    override fun onCleared() {
        super.onCleared()
        renderers.values.forEach { it.close() }
        descriptors.values.forEach { it.close() }
    }

    fun getBitmapFromCache(key: String): Bitmap? {
        return memoryCache.get(key)
    }

    fun putBitmapInCache(key: String, bitmap: Bitmap) {
        memoryCache.put(key, bitmap)
    }
}
