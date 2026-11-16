package com.cleber.baixatrix.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlTest {

    @Test
    fun `le meta tags em qualquer ordem de atributos`() {
        val html = """
            <html><head>
            <meta property="og:title" content="Fulano (@fulano) • Instagram photos and videos">
            <meta content="https://cdn.exemplo.com/capa.jpg" property="og:image">
            </head></html>
        """.trimIndent()

        assertEquals("https://cdn.exemplo.com/capa.jpg", Html.meta(html, "og:image"))
        assertEquals("Fulano (@fulano) • Instagram photos and videos", Html.meta(html, "og:title"))
        assertNull(Html.meta(html, "og:video"))
    }

    @Test
    fun `extrai video_url com escapes de json`() {
        val json = """{"video_url":"https:\/\/cdn.exemplo.com\/video.mp4?x=1\u0026y=2","width":1080}"""
        val urls = Html.jsonStrings(json, "video_url")
        assertEquals(listOf("https://cdn.exemplo.com/video.mp4?x=1&y=2"), urls)
    }

    @Test
    fun `extrai video_url com escapes duplos da pagina de embed`() {
        val html =
            """<script>window.__data = "{\"video_url\":\"https:\\/\\/cdn.exemplo.com\\/v.mp4\\u0026token=abc\"}";</script>"""
        val urls = Html.jsonStrings(html, "video_url")
        assertEquals(listOf("https://cdn.exemplo.com/v.mp4&token=abc"), urls)
    }

    @Test
    fun `desfaz escapes duplos de url`() {
        val raw = """https:\\/\\/cdn.exemplo.com\\/v.mp4\\u0026token=abc"""
        assertEquals("https://cdn.exemplo.com/v.mp4&token=abc", Html.unescape(raw))
    }

    @Test
    fun `decodifica unicode em texto de legenda`() {
        val raw = """Oi \\u0040fulano, tudo bem?\u0020Teste"""
        assertEquals("Oi @fulano, tudo bem? Teste", Html.unescape(raw))
    }

    @Test
    fun `remove quebras de linha escapadas`() {
        assertEquals("linha um linha dois", Html.unescape("""linha um\nlinha dois"""))
    }

    @Test
    fun `extrai numero de json`() {
        val json = """{"video_duration":12.345,"outro":1}"""
        assertEquals(12.345, Html.jsonNumber(json, "video_duration")!!, 0.0001)
    }

    @Test
    fun `encontra urls mp4 escapadas`() {
        val html = """<script>{"photoUrl":"https:\/\/v.exemplo.com\/a\/b.mp4?tag=1"}</script>"""
        val urls = Html.mp4Urls(html)
        assertTrue(urls.any { it == "https://v.exemplo.com/a/b.mp4?tag=1" })
    }

    @Test
    fun `encontra video em tag source`() {
        val html = """<video controls><source src="https://v.exemplo.com/clipe.mp4" type="video/mp4"></video>"""
        assertTrue(Html.videoTagUrls(html).contains("https://v.exemplo.com/clipe.mp4"))
    }

    @Test
    fun `limpa titulo do instagram`() {
        assertEquals("Fulano (@fulano)", Html.cleanTitle("Fulano (@fulano) • Instagram photos and videos"))
        assertNull(Html.cleanTitle("Instagram"))
        assertEquals("fulano", Html.instagramHandle("Fulano (@fulano) • Instagram"))
    }

    @Test
    fun `identifica urls de video`() {
        assertTrue(Html.isVideoUrl("https://x.com/a.mp4"))
        assertTrue(Html.isVideoUrl("https://x.com/a.m3u8?token=1"))
        assertTrue(!Html.isVideoUrl("https://x.com/a.jpg"))
        assertTrue(!Html.isVideoUrl("ftp://x.com/a.mp4"))
    }
}
