package com.apsmkimo.zenwoodsudoku.domain.model

enum class Difficulty(val code: Int) {
    EASY(1),
    MEDIUM(2),
    MASTER(3),
    ;

    companion object {
        fun fromCode(code: Int): Difficulty = entries.firstOrNull { it.code == code } ?: EASY
    }
}
