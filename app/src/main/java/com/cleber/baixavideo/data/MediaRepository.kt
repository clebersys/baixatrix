package com.cleber.baixavideo.data

import com.cleber.baixavideo.data.model.ResolveException
import com.cleber.baixavideo.data.model.ResolvedMedia
import com.cleber.baixavideo.data.resolver.InstagramResolver
import com.cleber.baixavideo.data.resolver.KwaiResolver
import com.cleber.baixavideo.data.resolver.PlatformResolver
import com.cleber.baixavideo.data.resolver.RemoteResolver
import com.cleber.baixavideo.util.LinkParser
import com.cleber.baixavideo.util.Platform

/**
 * Orquestra a extração: tenta o extrator nativo da plataforma e, se o usuário
 * configurou um servidor próprio, usa esse servidor como alternativa.
 */
class MediaRepository(private val settings: SettingsStore) {

    suspend fun resolve(rawInput: String): ResolvedMedia {
        val link = LinkParser.firstUrl(rawInput)
            ?: throw ResolveException("Cole um link do Instagram ou do Kwai para começar.")

        val platform = LinkParser.platform(link)
        val config = settings.current()
        val errors = mutableListOf<String>()
        var friendly: String? = null

        val nativeResolvers: List<PlatformResolver> = when (platform) {
            Platform.INSTAGRAM -> listOf(InstagramResolver(config.instagramCookie.ifBlank { null }))
            Platform.KWAI -> listOf(KwaiResolver())
            Platform.OTHER -> emptyList()
        }

        for (resolver in nativeResolvers) {
            val result = runCatching { resolver.resolve(link) }
            result.getOrNull()?.let { return it }
            val error = result.exceptionOrNull() ?: continue
            if (error is ResolveException) {
                if (friendly == null) friendly = error.message
                error.details?.takeIf { it.isNotBlank() }?.let { errors += it }
            } else if (!error.message.isNullOrBlank()) {
                errors += error.message.orEmpty()
            }
        }

        if (config.resolverUrl.isNotBlank()) {
            val remote = runCatching { RemoteResolver(config.resolverUrl).resolve(link) }
            remote.getOrNull()?.let { return it }
            val error = remote.exceptionOrNull()
            if (error is ResolveException && friendly == null) friendly = error.message
            error?.message?.takeIf { it.isNotBlank() && it != friendly }?.let { errors += it }
        }

        if (platform == Platform.OTHER && config.resolverUrl.isBlank()) {
            throw ResolveException(
                "Este link não é do Instagram nem do Kwai. " +
                    "Para baixar de outros sites, configure um servidor de extração em Ajustes."
            )
        }

        throw ResolveException(
            message = friendly ?: "Não foi possível encontrar o vídeo deste link.",
            details = errors.distinct().joinToString(" | ").takeIf { it.isNotBlank() }
        )
    }
}
