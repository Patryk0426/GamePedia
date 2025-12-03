package com.example.GamePedia

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Game(
    val id: Long,
    val nazwa: String,
    val dataWydania: Long?,
    val okladka: Long?,
    val opis: String?,
    val urlOkladki: String?
) : Parcelable
