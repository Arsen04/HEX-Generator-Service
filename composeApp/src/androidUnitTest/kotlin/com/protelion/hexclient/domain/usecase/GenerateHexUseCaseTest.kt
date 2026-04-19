package com.protelion.hexclient.domain.usecase

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
        assertTrue(hex.all { it in hexChars })
    }

    @Test
    fun `multiple calls generate different results`() {
        val hex1 = useCase()
        val hex2 = useCase()
        assertNotEquals(hex1, hex2)
    }
}
