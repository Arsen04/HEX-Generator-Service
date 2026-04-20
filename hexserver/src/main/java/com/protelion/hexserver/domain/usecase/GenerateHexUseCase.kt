package com.protelion.hexserver.domain.usecase

import kotlin.random.Random

class GenerateHexUseCase {
    operator fun invoke(): String {
        val chars = "0123456789ABCDEF"
        return (1..24)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }
}
