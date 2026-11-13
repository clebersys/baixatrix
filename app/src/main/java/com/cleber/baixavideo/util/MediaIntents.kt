package com.cleber.baixavideo.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** Abre e compartilha vídeos salvos, usando FileProvider para arquivos locais. */
object MediaIntents {

    fun openLocalVideo(context: Context, path: String): Boolean = runCatching {
        val uri = fileProviderUri(context, path)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "video/mp4")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
        true
    }.getOrDefault(false)

    fun shareLocalVideo(context: Context, path: String): Boolean = runCatching {
        val uri = fileProviderUri(context, path)
        shareUri(context, uri)
    }.getOrDefault(false)

    fun openUri(context: Context, uri: Uri, mimeType: String = "video/mp4"): Boolean = runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, mimeType)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        )
        true
    }.getOrDefault(false)

    fun shareUri(context: Context, uri: Uri, mimeType: String = "video/mp4"): Boolean = runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar vídeo"))
        true
    }.getOrDefault(false)

    private fun fileProviderUri(context: Context, path: String): Uri =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            File(path)
        )
}
