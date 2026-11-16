package com.cleber.baixatrix.data

import com.cleber.baixatrix.data.model.ResolveException
import com.cleber.baixatrix.data.model.ResolvedMedia
import com.cleber.baixatrix.data.resolver.InstagramResolver
import com.cleber.baixatrix.data.resolver.KwaiResolver
import com.cleber.baixatrix.util.LinkParser
import com.cleber.baixatrix.util.Platform

/** Resolve links do Instagram e do Kwai diretamente no aparelho. */
class MediaRepository {

    suspend fun resolve(rawInput: String): ResolvedMedia {
        val link = LinkParser.firstUrl(rawInput)
            ?: throw ResolveException("Cole um link do Instagram ou do Kwai para começar.")

        return when (LinkParser.platform(link)) {
            Platform.INSTAGRAM -> InstagramResolver().resolve(link)
            Platform.KWAI -> KwaiResolver().resolve(link)
            Platform.OTHER -> throw ResolveException(
                "O BaixaTrix aceita links do Instagram e do Kwai."
            )
        }
    }
}
