@file:OptIn(ExperimentalMaterial3Api::class)

package com.cleber.baixatrix.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.cleber.baixatrix.BuildConfig
import com.cleber.baixatrix.data.BackendSettingsRepository

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val backendSettings = remember { BackendSettingsRepository(context) }
    val savedSettings = remember { backendSettings.load() }
    var backendUrl by remember { mutableStateOf(savedSettings.baseUrl) }
    var apiKey by remember { mutableStateOf(savedSettings.apiKey) }
    var backendMessage by remember { mutableStateOf<String?>(null) }
    var backendMessageIsError by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Configurações", style = MaterialTheme.typography.titleLarge) },
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
                title = "Backend pessoal do YouTube",
                description = "O link do YouTube será enviado ao backend HTTPS que você configurar. " +
                    "Use somente um servidor seu ou de confiança. A chave, se usada, fica nas " +
                    "preferências privadas do BaixaTrix."
            )

            OutlinedTextField(
                value = backendUrl,
                onValueChange = {
                    backendUrl = it
                    backendMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("URL HTTPS do backend") },
                placeholder = { Text("https://meu-servidor.example") },
                singleLine = true
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = {
                    apiKey = it
                    backendMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Chave da API (opcional)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

            Button(
                onClick = {
                    if (backendUrl.isBlank()) {
                        backendSettings.save("", "")
                        apiKey = ""
                        backendMessage = "Configuração removida."
                        backendMessageIsError = false
                    } else {
                        val normalizedUrl = BackendSettingsRepository.normalizeBaseUrl(backendUrl)
                        if (normalizedUrl == null) {
                            backendMessage = "Informe uma URL HTTPS válida, sem usuário, senha ou parâmetros."
                            backendMessageIsError = true
                        } else {
                            backendSettings.save(normalizedUrl, apiKey)
                            backendUrl = normalizedUrl
                            backendMessage = "Backend salvo."
                            backendMessageIsError = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salvar backend")
            }

            backendMessage?.let { message ->
                Text(
                    text = message,
                    color = if (backendMessageIsError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            SettingsCard(
                title = "Armazenamento",
                description = "Os vídeos são salvos em Filmes/BaixaTrix, na memória interna. " +
                    "Eles aparecem em \"Meus vídeos\" e na galeria do aparelho."
            )

            SettingsCard(
                title = "Uso responsável",
                description = "Baixe apenas vídeos seus ou que você tenha autorização para usar. " +
                    "Respeite os direitos autorais e os Termos de Uso das plataformas."
            )

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
                    text = "BaixaTrix · versão ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SettingsCard(title: String, description: String) {
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
        }
    }
}
