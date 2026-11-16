package com.cleber.baixatrix.ui

import android.app.Application
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cleber.baixatrix.data.DownloadRepository
import com.cleber.baixatrix.data.MediaRepository
import com.cleber.baixatrix.data.model.ResolveException
import com.cleber.baixatrix.data.model.ResolvedMedia
import com.cleber.baixatrix.data.processing.VideoSplitter
import com.cleber.baixatrix.util.LinkParser
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

data class HomeUiState(
    val url: String = "",
    val isResolving: Boolean = false,
    val media: ResolvedMedia? = null,
    val selectedVariant: Int = 0,
    val isDownloading: Boolean = false,
    /** 0..100, ou -1 enquanto o tamanho total é desconhecido. */
    val downloadProgress: Int? = null,
    val savedPath: String? = null,
    val savedUri: Uri? = null,
    val isSplitting: Boolean = false,
    val splitProgress: Float? = null,
    val splitFiles: List<File> = emptyList(),
    val splitMimeType: String? = null,
    val splitError: String? = null,
    val error: String? = null,
    val errorDetails: String? = null,
    val message: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val mediaRepository = MediaRepository()
    private val downloadRepository = DownloadRepository(application)

    private val _state = MutableStateFlow(HomeUiState())
    val state = _state.asStateFlow()

    private var resolveJob: Job? = null
    private var downloadJob: Job? = null
    private var lastCompletedDownloadId: Long? = null
    private var lastCompletedSourceName: String = "video"

    fun onUrlChange(value: String) {
        _state.update {
            it.copy(
                url = value,
                error = null,
                errorDetails = null,
                media = null,
                savedPath = null,
                savedUri = null,
                isSplitting = false,
                splitProgress = null,
                splitFiles = emptyList(),
                splitMimeType = null,
                splitError = null,
                downloadProgress = null
            )
        }
    }

    /** Chamado pelo botão "Colar" e ao receber um link compartilhado. */
    fun submitUrl(text: String) {
        val link = LinkParser.firstUrl(text) ?: text.trim()
        if (link.isBlank()) {
            showMessage("Copie o link do vídeo primeiro.")
            return
        }
        _state.update { it.copy(url = link) }
        resolve()
    }

    fun resolve() {
        val input = _state.value.url.trim()
        if (input.isBlank()) {
            showMessage("Cole um link para continuar.")
            return
        }

        resolveJob?.cancel()
        resolveJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    isResolving = true,
                    error = null,
                    errorDetails = null,
                    media = null,
                    savedPath = null,
                    savedUri = null,
                    isSplitting = false,
                    splitProgress = null,
                    splitFiles = emptyList(),
                    splitMimeType = null,
                    splitError = null,
                    downloadProgress = null
                )
            }

            runCatching { mediaRepository.resolve(input) }
                .onSuccess { media ->
                    _state.update {
                        it.copy(
                            isResolving = false,
                            media = media,
                            selectedVariant = 0,
                            url = media.sourceUrl.ifBlank { it.url }
                        )
                    }
                }
                .onFailure { error ->
                    val friendly = (error as? ResolveException)?.message
                        ?: "Não consegui ler esse link. Verifique sua conexão e tente novamente."
                    _state.update {
                        it.copy(
                            isResolving = false,
                            error = friendly,
                            errorDetails = (error as? ResolveException)?.details
                                ?: error.message?.takeIf { message -> message != friendly }
                        )
                    }
                }
        }
    }

    fun selectVariant(index: Int) {
        _state.update {
            it.copy(
                selectedVariant = index,
                savedPath = null,
                savedUri = null,
                splitFiles = emptyList(),
                splitMimeType = null,
                splitError = null
            )
        }
    }

    fun download(splitAfterDownload: Boolean = false) {
        val current = _state.value
        val media = current.media ?: return
        if (current.isDownloading) return
        val variant = media.variants.getOrNull(current.selectedVariant) ?: media.bestVariant

        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    isDownloading = true,
                    downloadProgress = -1,
                    error = null,
                    savedPath = null,
                    savedUri = null,
                    isSplitting = false,
                    splitProgress = null,
                    splitFiles = emptyList(),
                    splitMimeType = null,
                    splitError = null
                )
            }

            downloadRepository.download(media, variant).collect { downloadState ->
                when (downloadState) {
                    is DownloadRepository.State.Running -> _state.update {
                        it.copy(downloadProgress = downloadState.progress)
                    }

                    is DownloadRepository.State.Completed -> {
                        lastCompletedDownloadId = downloadState.downloadId
                        lastCompletedSourceName = media.displayTitle
                        _state.update {
                            it.copy(
                                isDownloading = false,
                                downloadProgress = null,
                                savedPath = downloadState.path,
                                savedUri = downloadState.uri,
                                message = "Vídeo salvo em Filmes/BaixaTrix"
                            )
                        }
                        if (splitAfterDownload) {
                            splitDownloadedVideo(downloadState.downloadId, media.displayTitle)
                        }
                    }

                    is DownloadRepository.State.Failed -> _state.update {
                        it.copy(
                            isDownloading = false,
                            downloadProgress = null,
                            error = downloadState.reason
                        )
                    }
                }
            }
        }
    }

    fun splitAgain() {
        val downloadId = lastCompletedDownloadId ?: return
        if (_state.value.isSplitting) return
        viewModelScope.launch { splitDownloadedVideo(downloadId, lastCompletedSourceName) }
    }

    private suspend fun splitDownloadedVideo(downloadId: Long, sourceName: String) {
        _state.update { it.copy(isSplitting = true, splitProgress = 0f, splitError = null) }
        try {
            val result = withContext(Dispatchers.IO) {
                val root = getApplication<Application>()
                    .getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                    ?: File(getApplication<Application>().filesDir, "videos")
                val safeName = sourceName
                    .replace(Regex("[^A-Za-z0-9À-ÿ _-]"), " ")
                    .trim()
                    .replace(Regex("\\s+"), "_")
                    .take(40)
                    .ifBlank { "video" }
                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val partsRoot = File(root, "BaixaTrix/partes")
                // Mantém somente o conjunto mais recente, como no Divide30s,
                // para não acumular arquivos grandes no armazenamento do app.
                partsRoot.listFiles()?.forEach { old ->
                    runCatching { old.deleteRecursively() }
                }
                val outputDir = File(partsRoot, "${safeName}_$stamp")
                val descriptor = downloadRepository.openDownloadedFile(downloadId)
                VideoSplitter.split(
                    descriptor = descriptor,
                    outputDir = outputDir,
                    segmentUs = VideoSplitter.DEFAULT_SEGMENT_US,
                    onProgress = { progress ->
                        _state.update { it.copy(splitProgress = progress) }
                    },
                    checkActive = { coroutineContext.ensureActive() }
                )
            }
            val mimeType = if (result.files.first().extension.equals("webm", true)) {
                "video/webm"
            } else {
                "video/mp4"
            }
            _state.update {
                it.copy(
                    isSplitting = false,
                    splitProgress = 1f,
                    splitFiles = result.files,
                    splitMimeType = mimeType,
                    splitError = null,
                    message = "Vídeo dividido em ${result.files.size} parte(s) de até 30 segundos"
                )
            }
        } catch (cancelled: CancellationException) {
            _state.update { it.copy(isSplitting = false, splitProgress = null) }
            throw cancelled
        } catch (error: Exception) {
            _state.update {
                it.copy(
                    isSplitting = false,
                    splitProgress = null,
                    splitError = error.message ?: "Não foi possível dividir o vídeo."
                )
            }
        }
    }

    fun showMessage(text: String) {
        _state.update { it.copy(message = text) }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }
}
