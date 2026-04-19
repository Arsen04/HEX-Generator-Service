package com.protelion.hexclient.domain.usecase

class GenerateHexUseCase {
    private val chars = "0123456789ABCDEF".toCharArray()
    operator fun invoke(): String = (1..24).map { chars.random() }.joinToString("")
}
