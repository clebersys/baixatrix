package com.cleber.baixatrix.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkParserTest {

    @Test
    fun `extrai link do instagram de texto compartilhado`() {
        val text = "Olha esse reel! https://www.instagram.com/reel/C8xYz12AbCd/?igsh=MW9vZg== mais texto"
        assertEquals("https://www.instagram.com/reel/C8xYz12AbCd/?igsh=MW9vZg==", LinkParser.firstUrl(text))
    }

    @Test
    fun `reconhece post, reel e igtv`() {
        val post = "https://www.instagram.com/p/ABC123/"
        val reel = "https://www.instagram.com/reels/XYZ789/?utm_source=ig_web"
        val tv = "https://instagram.com/tv/TV1234/"

        assertEquals(Platform.INSTAGRAM, LinkParser.platform(post))
        assertEquals(Platform.INSTAGRAM, LinkParser.platform(reel))
        assertEquals(Platform.INSTAGRAM, LinkParser.platform(tv))

        assertEquals("ABC123", LinkParser.instagramShortcode(post))
        assertEquals("XYZ789", LinkParser.instagramShortcode(reel))
        assertEquals("TV1234", LinkParser.instagramShortcode(tv))
    }

    @Test
    fun `normaliza reels para reel`() {
        assertEquals("reel", LinkParser.instagramKind("https://www.instagram.com/reels/ABC/"))
        assertEquals("p", LinkParser.instagramKind("https://www.instagram.com/p/ABC/"))
    }

    @Test
    fun `reconhece kwai e link curto`() {
        assertEquals(Platform.KWAI, LinkParser.platform("https://www.kwai.com/@user/video/123456"))
        assertEquals(Platform.KWAI, LinkParser.platform("https://k.kwai.com/s/abcDEF"))
        assertEquals(Platform.KWAI, LinkParser.platform("https://kwai.app/video/123"))
    }

    @Test
    fun `link de outro site nao e suportado`() {
        val url = "https://www.youtube.com/watch?v=abc"
        assertEquals(Platform.OTHER, LinkParser.platform(url))
        assertFalse(LinkParser.supports(url))
    }

    @Test
    fun `limpa parametros de rastreio`() {
        assertEquals(
            "https://www.instagram.com/reel/ABC",
            LinkParser.clean("https://www.instagram.com/reel/ABC/?igsh=xyz")
        )
    }

    @Test
    fun `texto sem link retorna nulo`() {
        assertNull(LinkParser.firstUrl("nenhum link aqui"))
        assertFalse(LinkParser.supports("nenhum link aqui"))
        assertTrue(LinkParser.supports("veja https://www.instagram.com/reel/ABC/"))
    }
}
