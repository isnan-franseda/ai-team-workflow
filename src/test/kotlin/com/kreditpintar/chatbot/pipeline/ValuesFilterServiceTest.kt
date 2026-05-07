package com.kreditpintar.chatbot.pipeline

import com.kreditpintar.chatbot.config.MiniMaxClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class ValuesFilterServiceTest {

    private lateinit var miniMaxClient: MiniMaxClient
    private lateinit var valuesFilterService: ValuesFilterService

    @BeforeEach
    fun setUp() {
        miniMaxClient = mock()
        valuesFilterService = ValuesFilterService(miniMaxClient)
    }

    @Test
    fun `filter passes aligned web content`() {
        val webResults = listOf(
            WebSearchEntry(
                title = "OJK Regulation",
                content = "Regulasi OJK tentang pinjaman online yang sah.",
                url = "https://ojk.go.id/regulasi"
            )
        )

        whenever(miniMaxClient.validateOutput(org.mockito.kotlin.any(), org.mockito.kotlin.any()))
            .thenReturn("PASS\nAlasan: Konten informatif dan sejalan dengan nilai KP")

        val results = valuesFilterService.filter(webResults, "session-1")

        assertEquals(1, results.size)
        assertTrue(results[0].passed)
        assertEquals(null, results[0].filterReason)
    }

    @Test
    fun `filter rejects competitor promotion content`() {
        val webResults = listOf(
            WebSearchEntry(
                title = "Competitor Promo",
                content = "Pinjaman cepat cair, lebih murah dari Kredit Pintar!",
                url = "https://competitor.com/promo"
            )
        )

        whenever(miniMaxClient.validateOutput(org.mockito.kotlin.any(), org.mockito.kotlin.any()))
            .thenReturn("FAIL\nAlasan: Membandingkan dengan kompetitor secara negatif")

        val results = valuesFilterService.filter(webResults, "session-2")

        assertEquals(1, results.size)
        assertFalse(results[0].passed)
        assertEquals("Membandingkan dengan kompetitor secara negatif", results[0].filterReason)
    }

    @Test
    fun `filter handles mixed results`() {
        val webResults = listOf(
            WebSearchEntry(
                title = "Good content",
                content = "Informasi edukasi keuangan.",
                url = "https://example.com/good"
            ),
            WebSearchEntry(
                title = "Bad content",
                content = "Pinjaman darurat tanpa syarat!",
                url = "https://example.com/bad"
            )
        )

        whenever(miniMaxClient.validateOutput(org.mockito.kotlin.any(), org.mockito.kotlin.any()))
            .thenReturn("PASS\nAlasan: Konten edukatif")
            .thenReturn("FAIL\nAlasan: Taktik urgensi")

        val results = valuesFilterService.filter(webResults, "session-3")

        assertEquals(2, results.size)
        assertTrue(results[0].passed)
        assertFalse(results[1].passed)
    }

    @Test
    fun `filter marks all as FAIL when MiniMax call fails`() {
        val webResults = listOf(
            WebSearchEntry(
                title = "Any content",
                content = "Some content",
                url = "https://example.com"
            )
        )

        whenever(miniMaxClient.validateOutput(org.mockito.kotlin.any(), org.mockito.kotlin.any()))
            .thenThrow(RuntimeException("API error"))

        val results = valuesFilterService.filter(webResults, "session-4")

        assertEquals(1, results.size)
        assertFalse(results[0].passed)
    }

    @Test
    fun `filter returns empty for empty input`() {
        val results = valuesFilterService.filter(emptyList(), "session-5")
        assertTrue(results.isEmpty())
    }
}
