package com.cleber.baixatrix.data

import com.cleber.baixatrix.data.model.ResolveException
import com.cleber.baixatrix.data.model.ResolvedMedia
import com.cleber.baixatrix.data.resolver.YoutubeResolver
import com.cleber.baixatrix.data.resolver.InstagramResolver
import com.cleber.baixatrix.data.resolver.KwaiResolver
import com.cleber.baixatrix.util.LinkParser
import com.cleber.baixatrix.util.Platform

/** Resolve links das plataformas suportadas pelo BaixaTrix. */
class MediaRepository(youtubeSettings: BackendSettingsRepository? = null) {

    private val youtubeResolver = youtubeSettings?.let(::YoutubeResolver)

    suspend fun resolve(rawInput: String): ResolvedMedia {
        val link = LinkParser.firstUrl(rawInput)
            ?: throw ResolveException("Cole um link de vídeo do Instagram, Kwai ou YouTube para começar.")

        return when (LinkParser.platform(link)) {
            Platform.INSTAGRAM -> InstagramResolver().resolve(link)
            Platform.KWAI -> KwaiResolver().resolve(link)
            Platform.YOUTUBE -> youtubeResolver?.resolve(link) ?: throw ResolveException(
                "Configure o backend YouTube em Configurações para continuar."
            )
            Platform.OTHER -> throw ResolveException(
                "Este link não é do Instagram, Kwai nem YouTube."
            )
        }
    }
}
