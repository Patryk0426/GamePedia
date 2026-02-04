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


        val btnUpcoming: Button = findViewById(R.id.btnUpcoming)
        val btnBestRecent: Button = findViewById(R.id.btnBestRecent)
        val listTitle: android.widget.TextView = findViewById(R.id.listTitle)
        val listaGier: RecyclerView = findViewById(R.id.gamesRecycler)
        val poleWyszukiwania: com.google.android.material.textfield.TextInputEditText = findViewById(R.id.searchInput)
        val przyciskWyszukaj: com.google.android.material.button.MaterialButton = findViewById(R.id.searchButton)


        listaGier.layoutManager = LinearLayoutManager(this)


        btnBestRecent.setOnClickListener {
            listTitle.text = "Najlepsze gry (ostatnie 3 miesiące)"
            lifecycleScope.launchWhenStarted {
                if (tokenDostepu == null) tokenDostepu = pobierzTokenDostepu()
                val gry = pobierzNajlepszeGry(tokenDostepu!!)
                listaGier.adapter = GameAdapter(gry)
            }
        }


        btnUpcoming.setOnClickListener {
            listTitle.text = "Nadchodzące premiery"
            lifecycleScope.launchWhenStarted {
                if (tokenDostepu == null) tokenDostepu = pobierzTokenDostepu()
                val gry = pobierzNadchodzaceGry(tokenDostepu!!)
                listaGier.adapter = GameAdapter(gry)
            }
        }


        przyciskWyszukaj.setOnClickListener {
            val zapytanie = poleWyszukiwania.text.toString().trim()
            lifecycleScope.launchWhenStarted {
                if (tokenDostepu == null) tokenDostepu = pobierzTokenDostepu()

                if (zapytanie.isNotEmpty()) {
                    listTitle.text = "Wyniki wyszukiwania dla: $zapytanie"
                    val gry = pobierzGry(zapytanie, tokenDostepu!!)
                    listaGier.adapter = GameAdapter(gry)
                } else {
                    listTitle.text = "Nadchodzące premiery"
                    val nadchodzace = pobierzNadchodzaceGry(tokenDostepu!!)
                    listaGier.adapter = GameAdapter(nadchodzace)
                }
            }
        }


        lifecycleScope.launchWhenStarted {
            if (tokenDostepu == null) {
                tokenDostepu = pobierzTokenDostepu()
            }
            val nadchodzace = pobierzNadchodzaceGry(tokenDostepu!!)
            listaGier.adapter = GameAdapter(nadchodzace)
        }
    }
    private suspend fun pobierzNadchodzaceGry(token: String): List<Game> = withContext(Dispatchers.IO) {
        val teraz = System.currentTimeMillis() / 1000
        val zaTrzyMiesiace = teraz + (90 * 24 * 60 * 60)

        // Gry, które wyjdą w ciągu najbliższych 90 dni, posortowane po popularności (hypes)
        val body = "fields name, first_release_date, cover, summary, id; " +
                "where first_release_date > $teraz & first_release_date < $zaTrzyMiesiace & cover != null; " +
                "sort hypes desc; limit 20;"

        val request = Request.Builder()
            .url("https://api.igdb.com/v4/games")
            .addHeader("Client-ID", klientId)
            .addHeader("Authorization", "Bearer $token")
            .post(body.toRequestBody("text/plain".toMediaType()))
            .build()

        return@withContext wykonajZapytanieIGDB(request, token)
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

    private suspend fun pobierzNajlepszeGry(token: String): List<Game> = withContext(Dispatchers.IO) {
        val trzyMiesiaceTemu = (System.currentTimeMillis() / 1000) - (90 * 24 * 60 * 60)
        val teraz = System.currentTimeMillis() / 1000

        // Szukamy gier z wysokim ratingiem, które wyszły niedawno
        val body = "fields name, first_release_date, cover, summary, total_rating, id; " +
                "where first_release_date > $trzyMiesiaceTemu & first_release_date < $teraz & total_rating != null & cover != null; " +
                "sort total_rating desc; limit 20;"

        val request = Request.Builder()
            .url("https://api.igdb.com/v4/games")
            .addHeader("Client-ID", klientId)
            .addHeader("Authorization", "Bearer $token")
            .post(body.toRequestBody("text/plain".toMediaType()))
            .build()

        return@withContext wykonajZapytanieIGDB(request, token)
    }

    private suspend fun pobierzGry(zapytanie: String, token: String): List<Game> = withContext(Dispatchers.IO) {
        val body = "search \"$zapytanie\"; fields name, first_release_date, cover, summary, id; limit 15;"
        val request = Request.Builder()
            .url("https://api.igdb.com/v4/games")
            .addHeader("Client-ID", klientId)
            .addHeader("Authorization", "Bearer $token")
            .post(body.toRequestBody("text/plain".toMediaType()))
            .build()

        return@withContext wykonajZapytanieIGDB(request, token)
    }


    private suspend fun wykonajZapytanieIGDB(request: Request, token: String): List<Game> = withContext(Dispatchers.IO) {
        val odpowiedz = klient.newCall(request).execute()
        val tekst = odpowiedz.body?.string() ?: "[]"
        val tablicaGier = JSONArray(tekst)

        val gry = mutableListOf<Game>()
        val idOkładek = mutableListOf<Long>()

        for (i in 0 until tablicaGier.length()) {
            val obiekt = tablicaGier.getJSONObject(i)
            val okladkaId = if (obiekt.has("cover")) obiekt.getLong("cover") else null

            val gra = Game(
                id = obiekt.getLong("id"),
                nazwa = obiekt.getString("name"),
                dataWydania = if (obiekt.has("first_release_date")) obiekt.getLong("first_release_date") else null,
                okladka = okladkaId,
                opis = if (obiekt.has("summary")) obiekt.getString("summary") else null,
                urlOkladki = null
            )
            gry.add(gra)
            okladkaId?.let { idOkładek.add(it) }
        }

        if (idOkładek.isNotEmpty()) {
            val warunekWhere = "where id = (${idOkładek.joinToString(",")}); fields game, image_id;"
            val zapytanieOkładki = Request.Builder()
                .url("https://api.igdb.com/v4/covers")
                .addHeader("Client-ID", klientId)
                .addHeader("Authorization", "Bearer $token")
                .post(warunekWhere.toRequestBody("text/plain".toMediaType()))
                .build()

            val odpOkladki = klient.newCall(zapytanieOkładki).execute()
            val tekstOkładek = odpOkladki.body?.string() ?: "[]"
            val tablicaOkładek = JSONArray(tekstOkładek)
            val mapaOkładek = mutableMapOf<Long, String>()

            for (i in 0 until tablicaOkładek.length()) {
                val obiekt = tablicaOkładek.getJSONObject(i)
                val idGry = obiekt.getLong("game")
                val idObrazka = obiekt.getString("image_id")
                mapaOkładek[idGry] = "https://images.igdb.com/igdb/image/upload/t_cover_big/$idObrazka.jpg"
            }

            return@withContext gry.map { it.copy(urlOkladki = mapaOkładek[it.id]) }
        }
        return@withContext gry
    }

}

