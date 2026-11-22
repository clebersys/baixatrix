package com.cleber.baixatrix.data

import com.cleber.baixatrix.data.model.ResolveException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaRepositoryTest {

    @Test
    fun `orienta configurar backend quando link youtube nao tem servidor configurado`() {
        val error = runCatching {
            runBlocking {
                MediaRepository().resolve("https://youtu.be/dQw4w9WgXcQ")
            }
        }.exceptionOrNull()

        assertTrue(error is ResolveException)
        assertEquals(
            "Configure o backend YouTube em Configurações para continuar.",
            error?.message
        )
    }
}
