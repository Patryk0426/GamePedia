package com.example.GamePedia.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, getDatabasePath(context), null, 1) {

    companion object {
        private const val DATABASE_NAME = "gamepedia_database.db"

        // Funkcja ustalająca, gdzie fizycznie leży plik (w folderze widocznym przez USB)
        private fun getDatabasePath(context: Context): String {
            val folder = context.getExternalFilesDir(null)
            return File(folder, DATABASE_NAME).absolutePath
        }
    }

    // Tworzy tabele przy pierwszym uruchomieniu
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, password TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS user_games (gameId INTEGER PRIMARY KEY, userId INTEGER, title TEXT, releaseDate INTEGER, coverUrl TEXT, summary TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Puste - dla uproszczenia
    }

    // Rejestracja użytkownika
    fun insertUser(user: User): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("username", user.username)
            put("password", user.passwordHash)
        }
        return db.insert("users", null, values)
    }

    // Logowanie użytkownika
    fun getUserByUsername(username: String): User? {
        val db = this.readableDatabase
        val cursor = db.query("users", null, "username=?", arrayOf(username), null, null, null)

        return if (cursor != null && cursor.moveToFirst()) {
            val u = User(id = cursor.getInt(0), username = cursor.getString(1), email = "", passwordHash = cursor.getString(2))
            cursor.close()
            u
        } else null
    }

    // Dodawanie gry do listy
    fun insertGame(game: UserGame): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("gameId", game.gameId)
            put("userId", game.userId)
            put("title", game.title)
            put("releaseDate", game.releaseDate)
            put("coverUrl", game.coverUrl)
            put("summary", game.summary)
        }
        return db.insertWithOnConflict("user_games", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // Pobieranie listy gier
    fun getUserGames(userId: Int): List<UserGame> {
        val games = mutableListOf<UserGame>()
        val db = this.readableDatabase
        val cursor = db.query("user_games", null, "userId=?", arrayOf(userId.toString()), null, null, null)

        if (cursor != null && cursor.moveToFirst()) {
            do {
                games.add(UserGame(cursor.getLong(0), cursor.getInt(1), cursor.getString(2), cursor.getLong(3), cursor.getString(4), cursor.getString(5)))
            } while (cursor.moveToNext())
            cursor.close()
        }
        return games
    }
}