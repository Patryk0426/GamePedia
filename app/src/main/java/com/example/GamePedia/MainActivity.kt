package com.example.GamePedia

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import java.util.*

class MainActivity : AppCompatActivity() {
    // Uzupełnij swoimi danymi z Twitch Developer Console
    private val klientId = "yzdpgs364zmpawfrhfdlqjzo2rpgvk"
    private val sekretKlienta = "thcy8wbm2kfzfrscjhlep5wpz2i90b"

    private var tokenDostepu: String? = null
    private val klient = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val listaGier: RecyclerView = findViewById(R.id.gamesRecycler)
        listaGier.layoutManager = LinearLayoutManager(this)

        val poleWyszukiwania: EditText = findViewById(R.id.searchInput)
        val przyciskWyszukaj: Button = findViewById(R.id.searchButton)

        przyciskWyszukaj.setOnClickListener {
            val zapytanie = poleWyszukiwania.text.toString().trim()
            if (zapytanie.isNotEmpty()) {
                lifecycleScope.launchWhenStarted {
                    if (tokenDostepu == null) {
                        tokenDostepu = pobierzTokenDostepu()
                    }
                    val gry = pobierzGry(zapytanie, tokenDostepu!!)
                    listaGier.adapter = GameAdapter(gry)
                }
            }
        }
    }

    private suspend fun pobierzTokenDostepu(): String = withContext(Dispatchers.IO) {
        val adresUrl = "https://id.twitch.tv/oauth2/token"
        val trescZapytania = "client_id=$klientId&client_secret=$sekretKlienta&grant_type=client_credentials"
        val typMediow = "application/x-www-form-urlencoded".toMediaType()
        val zapytanieHttp = Request.Builder()
            .url(adresUrl)
            .post(trescZapytania.toRequestBody(typMediow))
            .build()

        val odpowiedz = klient.newCall(zapytanieHttp).execute()
        val tekst = odpowiedz.body?.string() ?: throw Exception("Empty response")
        val json = org.json.JSONObject(tekst)
        json.getString("access_token")
    }

    private suspend fun pobierzGry(zapytanie: String, token: String): List<Game> = withContext(Dispatchers.IO) {

        val trescGier = "search \"$zapytanie\"; fields name, first_release_date, cover, summary, id; limit 10;"
        val zapytanieGry = Request.Builder()
            .url("https://api.igdb.com/v4/games")
            .addHeader("Client-ID", klientId)
            .addHeader("Authorization", "Bearer $token")
            .post(trescGier.toRequestBody("text/plain".toMediaType()))
            .build()

        val odpowiedzGry = klient.newCall(zapytanieGry).execute()
        val tekstGier = odpowiedzGry.body?.string() ?: "[]"
        val tablicaGier = JSONArray(tekstGier)

        val gry = mutableListOf<Game>()
        val idOkładek = mutableListOf<Long>()
        for (i in 0 until tablicaGier.length()) {
            val obiekt = tablicaGier.getJSONObject(i)
            val id = obiekt.getLong("id")
            val nazwa = obiekt.getString("name")
            val dataWydania = if (obiekt.has("first_release_date")) obiekt.getLong("first_release_date") else null
            val okladkaId = if (obiekt.has("cover")) obiekt.getLong("cover") else null
            val opis = if (obiekt.has("summary")) obiekt.getString("summary") else null


            gry.add(Game(id, nazwa, dataWydania, okladkaId, opis, null))
            okladkaId?.let { idOkładek.add(it) }
        }


        val mapaOkładek = mutableMapOf<Long, String>()
        if (idOkładek.isNotEmpty()) {

            val warunekWhere = "where id = (${idOkładek.joinToString(",")}); fields game, image_id;"
            val zapytanieOkładki = Request.Builder()
                .url("https://api.igdb.com/v4/covers")
                .addHeader("Client-ID", klientId)
                .addHeader("Authorization", "Bearer $token")
                .post(warunekWhere.toRequestBody("text/plain".toMediaType()))
                .build()

            val odpowiedzOkładki = klient.newCall(zapytanieOkładki).execute()
            val tekstOkładek = odpowiedzOkładki.body?.string() ?: "[]"
            val tablicaOkładek = JSONArray(tekstOkładek)
            for (i in 0 until tablicaOkładek.length()) {
                val obiekt = tablicaOkładek.getJSONObject(i)
                val idGry = obiekt.getLong("game")
                val idObrazka = obiekt.getString("image_id")
                val adresUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/$idObrazka.jpg"
                mapaOkładek[idGry] = adresUrl
            }
        }

        // 3) Mapowanie do finalnej listy Game z urlOkladki (POPRAWIONE)
        return@withContext gry.map { gra ->
            // Znajdź URL okładki używając `gra.okladka` jako klucza, ale musimy dopasować `idGry`
            // Musimy zmapować id gry do URL-a okładki, co już robimy w mapaOkładek.
            gra.copy(urlOkladki = mapaOkładek[gra.id])
        }
    }
}
