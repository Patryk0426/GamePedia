package com.example.GamePedia.database

data class User(
    val id: Int = 0,
    val username: String,
    val email: String,
    val passwordHash: String
)

data class UserGame(
    val gameId: Long,
    val userId: Int,
    val title: String,
    val releaseDate: Long?,
    val coverUrl: String?,
    val summary: String?
)
