package com.cleber.baixavideo.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cleber.baixavideo.BaixaVideoApplication
import com.cleber.baixavideo.data.DownloadRepository
import com.cleber.baixavideo.data.MediaRepository
import com.cleber.baixavideo.data.SettingsStore
import com.cleber.baixavideo.data.model.ResolveException
import com.cleber.baixavideo.data.model.ResolvedMedia
import com.cleber.baixavideo.util.LinkParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val error: String? = null,
    val errorDetails: String? = null,
    val message: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BaixaVideoApplication
    private val mediaRepository = MediaRepository(app.settings)
    private val downloadRepository = DownloadRepository(application)
    private val settings: SettingsStore = app.settings

    private val _state = MutableStateFlow(HomeUiState())
    val state = _state.asStateFlow()

    private var resolveJob: Job? = null
    private var downloadJob: Job? = null

    fun onUrlChange(value: String) {
        _state.update {
            it.copy(
                url = value,
                error = null,
                errorDetails = null,
                media = null,
                savedPath = null,
                savedUri = null,
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
        _state.update { it.copy(selectedVariant = index, savedPath = null, savedUri = null) }
    }

    fun download() {
        val current = _state.value
        val media = current.media ?: return
        if (current.isDownloading) return
        val variant = media.variants.getOrNull(current.selectedVariant) ?: media.bestVariant

        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            _state.update {
                it.copy(isDownloading = true, downloadProgress = -1, error = null, savedPath = null, savedUri = null)
            }

            val cookie = runCatching { settings.current().instagramCookie }.getOrDefault("")
            downloadRepository.download(media, variant, cookie.ifBlank { null }).collect { downloadState ->
                when (downloadState) {
                    is DownloadRepository.State.Running -> _state.update {
                        it.copy(downloadProgress = downloadState.progress)
                    }

                    is DownloadRepository.State.Completed -> _state.update {
                        it.copy(
                            isDownloading = false,
                            downloadProgress = null,
                            savedPath = downloadState.path,
                            savedUri = downloadState.uri,
                            message = "Vídeo salvo em Filmes/BaixaVideo"
                        )
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

    fun showMessage(text: String) {
        _state.update { it.copy(message = text) }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }
}
