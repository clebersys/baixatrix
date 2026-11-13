package com.cleber.baixavideo.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** Compartilha todas as partes em um único ACTION_SEND_MULTIPLE. */
object BatchShareUtils {
    private val whatsappPackages = listOf("com.whatsapp", "com.whatsapp.w4b")

    fun shareToWhatsApp(context: Context, files: List<File>) {
        require(files.isNotEmpty()) { "Nenhuma parte para compartilhar." }
        // Enviar como documento evita que o WhatsApp recomprima as partes.
        val base = buildIntent(context, files, "application/octet-stream")
        val installed = whatsappPackages.firstOrNull { packageName ->
            runCatching { context.packageManager.getPackageInfo(packageName, 0) }.isSuccess
        }
        if (installed != null) {
            try {
                context.startActivity(Intent(base).setPackage(installed))
                return
            } catch (_: ActivityNotFoundException) {
                // Usa o seletor padrão se o WhatsApp não aceitar o intent.
            } catch (_: SecurityException) {
                // Alguns aparelhos bloqueiam abertura direta.
            }
        }
        shareToAnyApp(context, files)
    }

    fun shareToAnyApp(context: Context, files: List<File>) {
        require(files.isNotEmpty()) { "Nenhuma parte para compartilhar." }
        context.startActivity(
            Intent.createChooser(buildIntent(context, files, "video/mp4"), "Compartilhar partes")
        )
    }

    private fun buildIntent(context: Context, files: List<File>, mimeType: String): Intent {
        val uris = files.map { file ->
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
        }
        val clip = ClipData.newRawUri("partes do vídeo", uris.first())
        uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }

        return Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mimeType
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList<Uri>(uris))
            clipData = clip
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
