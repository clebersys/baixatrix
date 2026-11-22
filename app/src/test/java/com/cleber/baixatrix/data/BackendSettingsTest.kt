package com.cleber.baixatrix.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackendSettingsTest {

    @Test
    fun `normaliza a URL HTTPS do backend`() {
        assertEquals(
            "https://baixatrix.example/api",
            BackendSettingsRepository.normalizeBaseUrl("  https://BaixaTrix.example/api/  ")
        )
    }

    @Test
    fun `recusa HTTP, credenciais, query e fragmento`() {
        listOf(
            "http://baixatrix.example",
            "https://user:password@baixatrix.example",
            "https://baixatrix.example?token=secret",
            "https://baixatrix.example#section",
            "not a URL"
        ).forEach { value ->
            assertNull(BackendSettingsRepository.normalizeBaseUrl(value))
        }
    }
}
