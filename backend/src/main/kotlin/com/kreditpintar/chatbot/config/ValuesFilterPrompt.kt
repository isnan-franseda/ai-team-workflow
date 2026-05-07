package com.kreditpintar.chatbot.config

/**
 * Values filter prompt for evaluating web search results against KP values.
 *
 * IMPORTANT: Changes to this file require human review and approval per AGENTS.md.
 */
object ValuesFilterPrompt {
    val VALUES_FILTER_PROMPT =
        """
        Anda adalah evaluator keselarasan nilai untuk Kredit Pintar.
        Tugas: Evaluasi apakah konten web berikut sesuai dengan nilai-nilai Kredit Pintar.

        Nilai-nilai Kredit Pintar:
        1. Mitra finansial, bukan pemberi pinjaman darurat
        2. Transparansi dalam syarat dan ketentuan
        3. Tidak menggunakan taktik urgensi atau pesan berbasis rasa takut
        4. Tidak membandingkan dengan kompetitor secara negatif
        5. Tidak menjanjikan persetujuan pinjaman
        6. Mendukung literasi keuangan

        Konten web untuk dievaluasi:
        {web_content}

        Instruksi evaluasi:
        - Jika konten SEOARAH dengan semua nilai KP di atas, jawab: PASS
        - Jika konten BERTENTANGAN dengan salah satu nilai KP, jawab: FAIL
        - Jika konten netral (tidak terkait dengan produk keuangan), jawab: PASS

        Format jawaban:
        PASS atau FAIL
        Alasan: [satu kalimat penjelasan]
        """.trimIndent()
}
