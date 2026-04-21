package com.protelion.hexserver.domain.usecase

import org.junit.Test
import org.junit.Assert.*

class GenerateHexUseCaseTest {
    private val useCase = GenerateHexUseCase()

    @Test
    fun `generated string has length 24`() {
        val hex = useCase()
        assertEquals(24, hex.length)
    }

    @Test
    fun `generated string contains only hex characters`() {
        val hex = useCase()
        val hexChars = "0123456789ABCDEF"
        assertTrue("Generated string contains invalid characters: $hex", hex.all { it in hexChars })
    }

    @Test
    fun `multiple calls generate different results`() {
        val hex1 = useCase()
        val hex2 = useCase()
        assertNotEquals("Successive calls produced identical HEX codes", hex1, hex2)
    }

    @Test
    fun `generated string is not always the same`() {
        val results = List(100) { useCase() }
        assertEquals("Generated 100 codes, expected unique count to be 100", 100, results.distinct().size)
    }
}
