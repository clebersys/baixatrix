@file:OptIn(ExperimentalMaterial3Api::class)

package com.cleber.baixavideo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cleber.baixavideo.BaixaVideoApplication
import com.cleber.baixavideo.BuildConfig
import com.cleber.baixavideo.data.SettingsStore
import com.cleber.baixavideo.ui.components.GradientButton
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as BaixaVideoApplication
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val saved by app.settings.settings.collectAsStateWithLifecycle(
        initialValue = SettingsStore.Settings()
    )

    var resolverUrl by remember(saved.resolverUrl) { mutableStateOf(saved.resolverUrl) }
    var cookie by remember(saved.instagramCookie) { mutableStateOf(saved.instagramCookie) }
    var showCookie by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Ajustes", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
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
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsCard(
                title = "Extração de vídeos",
                description = "O app tenta extrair o arquivo direto do Instagram e do Kwai. " +
                    "Se as plataformas mudarem o layout, use um servidor próprio como alternativa."
            ) {
                OutlinedTextField(
                    value = resolverUrl,
                    onValueChange = { resolverUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Servidor de extração (opcional)") },
                    placeholder = { Text("https://meuservidor.com/resolve") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    )
                )
                Text(
                    text = "O servidor recebe ?url=<link> e devolve um JSON com a chave \"url\" " +
                        "apontando para o arquivo de vídeo. Um exemplo pronto está na pasta backend do projeto.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsCard(
                title = "Instagram",
                description = "Opcional. Se o Instagram pedir login ao analisar um link, cole aqui o " +
                    "valor do cookie sessionid da sua conta."
            ) {
                OutlinedTextField(
                    value = cookie,
                    onValueChange = { cookie = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Cookie sessionid") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    visualTransformation = if (showCookie) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    )
                )
                TextButton(onClick = { showCookie = !showCookie }) {
                    Text(if (showCookie) "Ocultar cookie" else "Mostrar cookie")
                }
                Text(
                    text = "Use somente com uma conta sua. O cookie fica salvo apenas neste aparelho.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            GradientButton(
                text = "Salvar ajustes",
                onClick = {
                    scope.launch {
                        app.settings.setResolverUrl(resolverUrl)
                        app.settings.setInstagramCookie(cookie)
                        snackbarHostState.showSnackbar("Ajustes salvos.")
                    }
                }
            )

            SettingsCard(
                title = "Armazenamento",
                description = "Os vídeos são salvos em Filmes/BaixaVideo, na memória interna. " +
                    "Eles aparecem em \"Meus vídeos\" e na galeria do aparelho."
            ) {}

            SettingsCard(
                title = "Uso responsável",
                description = "Baixe apenas vídeos seus ou que você tenha autorização para usar. " +
                    "Respeite os direitos autorais e os Termos de Uso das plataformas."
            ) {}

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Desenvolvido por Cleber Reis",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "BaixaVídeo · versão ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}
