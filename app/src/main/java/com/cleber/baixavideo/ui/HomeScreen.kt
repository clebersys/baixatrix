@file:OptIn(ExperimentalMaterial3Api::class)

package com.cleber.baixavideo.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cleber.baixavideo.R
import com.cleber.baixavideo.data.model.ResolvedMedia
import com.cleber.baixavideo.ui.components.GradientButton
import com.cleber.baixavideo.ui.components.LogoMark
import com.cleber.baixavideo.ui.components.PlatformBadge
import com.cleber.baixavideo.ui.components.VideoPreview
import com.cleber.baixavideo.ui.theme.BrandGradient
import com.cleber.baixavideo.util.Formatters
import com.cleber.baixavideo.util.LinkParser
import com.cleber.baixavideo.util.MediaIntents

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.download()
        } else {
            viewModel.showMessage("Sem essa permissão o app não consegue salvar o vídeo.")
        }
    }

    val startDownload: () -> Unit = {
        val needsPermission = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED

        if (needsPermission) {
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            viewModel.download()
        }
    }

    val pasteFromClipboard: () -> Unit = {
        val text = clipboard.getText()?.text.orEmpty().trim()
        val link = LinkParser.firstUrl(text)
        when {
            text.isBlank() -> viewModel.showMessage("A área de transferência está vazia.")
            link == null -> viewModel.showMessage("Copie o link do vídeo primeiro.")
            else -> viewModel.submitUrl(link)
        }
    }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LogoMark()
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "BaixaVídeo",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenLibrary) {
                        Icon(
                            painter = painterResource(R.drawable.ic_movie),
                            contentDescription = "Meus vídeos",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Ajustes",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            Hero()
            LinkCard(
                url = state.url,
                isResolving = state.isResolving,
                onUrlChange = viewModel::onUrlChange,
                onPaste = pasteFromClipboard,
                onAnalyze = viewModel::resolve
            )

            if (state.isResolving) {
                ResolvingCard()
            }

            state.error?.let { message ->
                ErrorCard(
                    message = message,
                    details = state.errorDetails,
                    onOpenSettings = onOpenSettings
                )
            }

            state.media?.let { media ->
                MediaCard(
                    media = media,
                    selectedVariant = state.selectedVariant,
                    isDownloading = state.isDownloading,
                    progress = state.downloadProgress,
                    savedPath = state.savedPath,
                    savedUri = state.savedUri,
                    onSelectVariant = viewModel::selectVariant,
                    onDownload = startDownload,
                    onOpen = { path, uri ->
                        val opened = if (uri != null) {
                            MediaIntents.openUri(context, uri)
                        } else {
                            MediaIntents.openLocalVideo(context, path)
                        }
                        if (!opened) {
                            viewModel.showMessage("Nenhum app encontrado para abrir o vídeo.")
                        }
                    },
                    onShare = { path, uri ->
                        val shared = if (uri != null) {
                            MediaIntents.shareUri(context, uri)
                        } else {
                            MediaIntents.shareLocalVideo(context, path)
                        }
                        if (!shared) {
                            viewModel.showMessage("Não foi possível compartilhar o vídeo.")
                        }
                    }
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Desenvolvido por Cleber Reis",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun Hero() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Salve o vídeo.\nSem complicação.",
            style = TextStyle(
                brush = BrandGradient,
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.8).sp
            )
        )
        Text(
            text = "Cole o link do Instagram ou do Kwai, assista à prévia e baixe em um toque.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LinkCard(
    url: String,
    isResolving: Boolean,
    onUrlChange: (String) -> Unit,
    onPaste: () -> Unit,
    onAnalyze: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Link do vídeo",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("https://www.instagram.com/reel/...") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_link),
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    IconButton(onClick = onPaste) {
                        Icon(
                            painter = painterResource(R.drawable.ic_paste),
                            contentDescription = "Colar link",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(onGo = { onAnalyze() })
            )
            GradientButton(
                text = "Analisar link",
                onClick = onAnalyze,
                enabled = url.isNotBlank() && !isResolving,
                loading = isResolving,
                icon = painterResource(R.drawable.ic_link)
            )
            Text(
                text = "Posts e Reels públicos do Instagram e vídeos do Kwai.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ResolvingCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            strokeWidth = 2.5.dp
        )
        Column {
            Text("Lendo o link...", style = MaterialTheme.typography.titleMedium)
            Text(
                "Buscando a melhor qualidade disponível.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorCard(message: String, details: String?, onOpenSettings: () -> Unit) {
    var expanded by remember(message) { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Não deu certo",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            if (!details.isNullOrBlank()) {
                if (expanded) {
                    Text(
                        text = details,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.75f)
                    )
                }
                TextButton(
                    onClick = { expanded = !expanded },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text(if (expanded) "Ocultar detalhes" else "Ver detalhes técnicos")
                }
            }
            TextButton(
                onClick = onOpenSettings,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text("Abrir ajustes de extração")
            }
        }
    }
}

@Composable
private fun MediaCard(
    media: ResolvedMedia,
    selectedVariant: Int,
    isDownloading: Boolean,
    progress: Int?,
    savedPath: String?,
    savedUri: Uri?,
    onSelectVariant: (Int) -> Unit,
    onDownload: () -> Unit,
    onOpen: (String, Uri?) -> Unit,
    onShare: (String, Uri?) -> Unit
) {
    val variant = media.variants.getOrNull(selectedVariant) ?: media.bestVariant
    val headers = remember(media) { media.requestHeaders() }
    val duration = Formatters.duration(media.durationMillis)

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(Modifier.fillMaxWidth()) {
                VideoPreview(
                    url = variant.url,
                    headers = headers,
                    thumbnailUrl = media.thumbnailUrl,
                    isVertical = media.isVertical
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlatformBadge(media.platform.label)
                    if (duration != null) {
                        Text(
                            text = duration,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.55f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = media.displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    media.displayAuthor?.let { author ->
                        Text(
                            text = author,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (media.variants.size > 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Qualidade",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            media.variants.forEachIndexed { index, item ->
                                FilterChip(
                                    selected = index == selectedVariant,
                                    onClick = { onSelectVariant(index) },
                                    label = { Text(item.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }
                }

                DownloadSection(
                    isDownloading = isDownloading,
                    progress = progress,
                    savedPath = savedPath,
                    savedUri = savedUri,
                    onDownload = onDownload,
                    onOpen = onOpen,
                    onShare = onShare
                )
            }
        }
    }
}

@Composable
private fun DownloadSection(
    isDownloading: Boolean,
    progress: Int?,
    savedPath: String?,
    savedUri: Uri?,
    onDownload: () -> Unit,
    onOpen: (String, Uri?) -> Unit,
    onShare: (String, Uri?) -> Unit
) {
    when {
        savedPath != null -> {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Salvo em Filmes/BaixaVideo",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(text = "Abrir", onClick = { onOpen(savedPath, savedUri) })
                    SecondaryButton(text = "Compartilhar", onClick = { onShare(savedPath, savedUri) })
                }
                TextButton(onClick = onDownload) {
                    Text("Baixar novamente")
                }
            }
        }

        isDownloading -> {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val label = if (progress == null || progress < 0) {
                    "Baixando..."
                } else {
                    "Baixando... $progress%"
                }
                GradientButton(text = label, onClick = {}, enabled = false, loading = true)
                val percent = progress?.coerceIn(0, 100)
                if (percent != null) {
                    LinearProgressIndicator(
                        progress = { percent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                    )
                }
                Text(
                    text = "Você pode continuar usando o app — o download aparece na notificação.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        else -> {
            GradientButton(
                text = "Baixar vídeo",
                onClick = onDownload,
                icon = painterResource(R.drawable.ic_download)
            )
            Text(
                text = "O arquivo é salvo em Filmes/BaixaVideo, sem marca d'água adicional.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RowScope.SecondaryButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.weight(1f)
    ) {
        Text(text)
    }
}
