package com.cleber.baixatrix.data.resolver

import com.cleber.baixatrix.data.model.ResolvedMedia

/** Contrato de quem sabe transformar um link de site em arquivo de vídeo. */
interface PlatformResolver {
    suspend fun resolve(url: String): ResolvedMedia
}
