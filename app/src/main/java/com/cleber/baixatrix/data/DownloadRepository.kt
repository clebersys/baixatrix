package com.cleber.baixatrix.data

import android.app.DownloadManager
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import com.cleber.baixatrix.data.model.ResolvedMedia
import com.cleber.baixatrix.data.model.VideoVariant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.coroutines.resume
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Baixa o vídeo usando o DownloadManager do Android: o download continua em
 * segundo plano, aparece na notificação do sistema e é retomado se a conexão
 * cair. O arquivo vai para Filmes/BaixaTrix.
 */
class DownloadRepository(private val context: Context) {

    sealed interface State {
        /** progresso em 0..100, ou -1 quando o tamanho ainda é desconhecido. */
        data class Running(val progress: Int) : State
        data class Completed(val path: String, val uri: Uri?, val downloadId: Long) : State
        data class Failed(val reason: String) : State
    }

    fun download(
        media: ResolvedMedia,
        variant: VideoVariant,
        cookie: String?
    ): Flow<State> = flow {
        val fileName = fileNameFor(media)
        val relativePath = "$FOLDER_NAME/$fileName"

        val request = DownloadManager.Request(Uri.parse(variant.url))
            .setTitle(media.displayTitle)
            .setDescription("BaixaTrix · ${media.platform.label}")
            .setMimeType("video/mp4")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, relativePath)

        media.requestHeaders(cookie).forEach { (key, value) ->
            runCatching { request.addRequestHeader(key, value) }
        }

        val manager = context.getSystemService(DownloadManager::class.java)
        if (manager == null) {
            emit(State.Failed("Serviço de download indisponível neste aparelho."))
            return@flow
        }

        val downloadId = try {
            manager.enqueue(request)
        } catch (error: SecurityException) {
            emit(State.Failed("O app precisa de permissão para salvar vídeos no aparelho."))
            return@flow
        } catch (error: Exception) {
            emit(State.Failed("Não foi possível iniciar o download: ${error.message.orEmpty()}"))
            return@flow
        }

        emit(State.Running(-1))

        while (true) {
            val progress = query(manager, downloadId)
            if (progress == null) {
                emit(State.Failed("O download foi removido do sistema."))
                return@flow
            }

            when (progress.status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    val path = progress.localUri?.path
                        ?: File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                            relativePath
                        ).absolutePath
                    val uri = scanMedia(path)
                    emit(State.Completed(path, uri, downloadId))
                    return@flow
                }

                DownloadManager.STATUS_FAILED -> {
                    emit(State.Failed(reasonMessage(progress.reason)))
                    return@flow
                }

                else -> {
                    val percent = if (progress.totalBytes > 0) {
                        ((progress.downloadedBytes * 100) / progress.totalBytes).toInt()
                    } else {
                        -1
                    }
                    emit(State.Running(percent))
                }
            }
            delay(400)
        }
    }

    /** Opens the completed DownloadManager file without requiring media-library access. */
    fun openDownloadedFile(downloadId: Long): ParcelFileDescriptor {
        val manager = context.getSystemService(DownloadManager::class.java)
            ?: error("Serviço de download indisponível neste aparelho.")
        return manager.openDownloadedFile(downloadId)
    }

    /**
     * Registra o arquivo na galeria e devolve a URI do MediaStore. Compartilhar
     * por essa URI funciona mesmo com scoped storage, onde o arquivo pertence
     * ao DownloadManager e não pode ser lido direto pelo app.
     */
    private suspend fun scanMedia(path: String): Uri? = withTimeoutOrNull(8_000) {
        suspendCancellableCoroutine { continuation ->
            MediaScannerConnection.scanFile(
                context, arrayOf(path), arrayOf("video/mp4")
            ) { _, uri ->
                if (continuation.isActive) continuation.resume(uri)
            }
        }
    }

    private fun query(manager: DownloadManager, id: Long): Progress? {
        val cursor = manager.query(DownloadManager.Query().setFilterById(id)) ?: return null
        cursor.use {
            if (!it.moveToFirst()) return null
            val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val downloaded = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val reason = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
            val localUri = it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                ?.let { uri -> runCatching { Uri.parse(uri) }.getOrNull() }
            return Progress(status, downloaded, total, reason, localUri)
        }
    }

    private fun reasonMessage(reason: Int): String = when (reason) {
        DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Espaço insuficiente no aparelho."
        DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "Já existe um arquivo com esse nome."
        DownloadManager.ERROR_FILE_ERROR -> "Não foi possível gravar o arquivo."
        DownloadManager.ERROR_HTTP_DATA_ERROR -> "Falha de rede durante o download."
        DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "O servidor redirecionou demais o link do vídeo."
        DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "O servidor do vídeo recusou o download."
        DownloadManager.ERROR_CANNOT_RESUME -> "Não foi possível retomar o download."
        else -> "O download falhou (código $reason)."
    }

    private fun fileNameFor(media: ResolvedMedia): String {
        val base = (media.author ?: media.title ?: "video")
            .replace(Regex("[^A-Za-z0-9À-ÿ _-]"), " ")
            .trim()
            .replace(Regex("\\s+"), "_")
            .take(40)
            .ifBlank { "video" }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "baixatrix_${base}_$stamp.mp4"
    }

    private data class Progress(
        val status: Int,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val reason: Int,
        val localUri: Uri?
    )

    companion object {
        const val FOLDER_NAME = "BaixaTrix"
    }
}
