package com.example.GamePedia

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.GamePedia.database.DatabaseHelper
import com.example.GamePedia.database.UserGame
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import java.util.*

class MainActivity : AppCompatActivity() {
    private val klientId = "yzdpgs364zmpawfrhfdlqjzo2rpgvk"
    private val sekretKlienta = "thcy8wbm2kfzfrscjhlep5wpz2i90b"

    private var tokenDostepu: String? = null
    private val klient = OkHttpClient()
    private lateinit var dbHelper: DatabaseHelper
    
    private var currentUserId: Int = -1
    private var currentUsername: String? = null

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var listTitle: TextView
    private lateinit var listaGier: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DatabaseHelper(this)
        
        currentUserId = intent.getIntExtra("USER_ID", -1)
        currentUsername = intent.getStringExtra("USERNAME")

        drawerLayout = findViewById(R.id.drawerLayout)
        val btnMenu: ImageButton = findViewById(R.id.btnMenu)
        val navigationView: NavigationView = findViewById(R.id.navigationView)
        listTitle = findViewById(R.id.listTitle)
        listaGier = findViewById(R.id.gamesRecycler)
        val poleWyszukiwania: com.google.android.material.textfield.TextInputEditText = findViewById(R.id.searchInput)
        val przyciskWyszukaj: com.google.android.material.button.MaterialButton = findViewById(R.id.searchButton)

        listaGier.layoutManager = LinearLayoutManager(this)

        val headerView = navigationView.getHeaderView(0)
        headerView.findViewById<TextView>(R.id.nav_username).text = currentUsername ?: "Gość"

        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navigationView.setNavigationListener()

        przyciskWyszukaj.setOnClickListener {
            val zapytanie = poleWyszukiwania.text.toString().trim()
            lifecycleScope.launch {
                ensureToken()
                if (zapytanie.isNotEmpty()) {
                    listTitle.text = "Wyniki dla: $zapytanie"
                    val gry = pobierzGry(zapytanie, tokenDostepu!!)
                    updateAdapter(gry)
                }
            }
        }

        lifecycleScope.launch {
            ensureToken()
            loadUpcoming()
        }
    }

    private fun NavigationView.setNavigationListener() {
        setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_upcoming -> loadUpcoming()
                R.id.nav_latest -> loadLatest()
                R.id.nav_best_all_time -> loadBestAllTime()
                R.id.nav_my_list -> loadMyList()
                R.id.nav_profile -> {
                    val intent = Intent(this@MainActivity, ProfileActivity::class.java).apply {
                        putExtra("USER_ID", currentUserId)
                        putExtra("USERNAME", currentUsername)
                    }
                    startActivity(intent)
                }
                R.id.nav_logout -> {
                    startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                    finish()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun loadUpcoming() {
        listTitle.text = "Nadchodzące premiery"
        lifecycleScope.launch {
            ensureToken()
            val gry = pobierzNadchodzaceGry(tokenDostepu!!)
            updateAdapter(gry)
        }
    }

    private fun loadLatest() {
        listTitle.text = "Najnowsze gry"
        lifecycleScope.launch {
            ensureToken()
            val gry = pobierzNajnowszeGry(tokenDostepu!!)
            updateAdapter(gry)
        }
    }

    private fun loadBestAllTime() {
        listTitle.text = "Najlepsze gry wszech czasów"
        lifecycleScope.launch {
            ensureToken()
            val gry = pobierzBestAllTime(tokenDostepu!!)
            updateAdapter(gry)
        }
    }

    private fun loadMyList() {
        listTitle.text = "Twoja lista"
        lifecycleScope.launch {
            val userGames = withContext(Dispatchers.IO) {
                dbHelper.getUserGames(currentUserId)
            }
            val gry = userGames.map { 
                Game(it.gameId, it.title, it.releaseDate, null, it.summary, it.coverUrl)
            }
            updateAdapter(gry)
        }
    }

    private fun updateAdapter(gry: List<Game>) {
        listaGier.adapter = GameAdapter(gry) { gra ->
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    val userGame = UserGame(
                        gameId = gra.id,
                        userId = currentUserId,
                        title = gra.nazwa,
                        releaseDate = gra.dataWydania,
                        coverUrl = gra.urlOkladki,
                        summary = gra.opis
                    )
                    dbHelper.insertGame(userGame)
                }
                Toast.makeText(this@MainActivity, "Dodano do listy!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun ensureToken() {
        if (tokenDostepu == null) {
            tokenDostepu = pobierzTokenDostepu()
        }
    }

    private suspend fun pobierzTokenDostepu(): String = withContext(Dispatchers.IO) {
        val adresUrl = "https://id.twitch.tv/oauth2/token"
        val trescZapytania = "client_id=$klientId&client_secret=$sekretKlienta&grant_type=client_credentials"
        val zapytanieHttp = Request.Builder()
            .url(adresUrl)
            .post(trescZapytania.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
            .build()

        val odpowiedz = klient.newCall(zapytanieHttp).execute()
        val json = org.json.JSONObject(odpowiedz.body?.string() ?: "{}")
        json.getString("access_token")
    }

    // Używamy cover.image_id aby pobrać okładkę od razu w jednym zapytaniu
    private suspend fun pobierzNadchodzaceGry(token: String): List<Game> = withContext(Dispatchers.IO) {
        val teraz = System.currentTimeMillis() / 1000
        val body = "fields name, first_release_date, cover.image_id, summary, id; " +
                "where first_release_date > $teraz & cover != null; " +
                "sort first_release_date asc; limit 20;"
        val request = Request.Builder()
            .url("https://api.igdb.com/v4/games")
            .addHeader("Client-ID", klientId)
            .addHeader("Authorization", "Bearer $token")
            .post(body.toRequestBody("text/plain".toMediaType()))
            .build()
        wykonajZapytanieIGDB(request, token)
    }

    private suspend fun pobierzNajnowszeGry(token: String): List<Game> = withContext(Dispatchers.IO) {
        val teraz = System.currentTimeMillis() / 1000
        val body = "fields name, first_release_date, cover.image_id, summary, id; " +
                "where first_release_date < $teraz & cover != null; " +
                "sort first_release_date desc; limit 20;"
        val request = Request.Builder()
            .url("https://api.igdb.com/v4/games")
            .addHeader("Client-ID", klientId)
            .addHeader("Authorization", "Bearer $token")
            .post(body.toRequestBody("text/plain".toMediaType()))
            .build()
        wykonajZapytanieIGDB(request, token)
    }

    private suspend fun pobierzBestAllTime(token: String): List<Game> = withContext(Dispatchers.IO) {
        val body = "fields name, first_release_date, cover.image_id, summary, total_rating, id; " +
                "where total_rating != null & total_rating_count > 100 & cover != null; " +
                "sort total_rating desc; limit 20;"
        val request = Request.Builder()
            .url("https://api.igdb.com/v4/games")
            .addHeader("Client-ID", klientId)
            .addHeader("Authorization", "Bearer $token")
            .post(body.toRequestBody("text/plain".toMediaType()))
            .build()
        wykonajZapytanieIGDB(request, token)
    }

    private suspend fun pobierzGry(zapytanie: String, token: String): List<Game> = withContext(Dispatchers.IO) {
        val body = "search \"$zapytanie\"; fields name, first_release_date, cover.image_id, summary, id; limit 15;"
        val request = Request.Builder()
            .url("https://api.igdb.com/v4/games")
            .addHeader("Client-ID", klientId)
            .addHeader("Authorization", "Bearer $token")
            .post(body.toRequestBody("text/plain".toMediaType()))
            .build()
        wykonajZapytanieIGDB(request, token)
    }

    private suspend fun wykonajZapytanieIGDB(request: Request, token: String): List<Game> = withContext(Dispatchers.IO) {
        val odpowiedz = klient.newCall(request).execute()
        val tekst = odpowiedz.body?.string() ?: "[]"
        val tablicaGier = JSONArray(tekst)
        val gry = mutableListOf<Game>()

        for (i in 0 until tablicaGier.length()) {
            val obiekt = tablicaGier.getJSONObject(i)
            
            var urlOkladki: String? = null
            var okladkaId: Long? = null
            
            if (obiekt.has("cover")) {
                val coverObj = obiekt.getJSONObject("cover")
                okladkaId = coverObj.getLong("id")
                if (coverObj.has("image_id")) {
                    val imageId = coverObj.getString("image_id")
                    urlOkladki = "https://images.igdb.com/igdb/image/upload/t_cover_big/$imageId.jpg"
                }
            }

            val gra = Game(
                id = obiekt.getLong("id"),
                nazwa = obiekt.getString("name"),
                dataWydania = if (obiekt.has("first_release_date")) obiekt.getLong("first_release_date") else null,
                okladka = okladkaId,
                opis = if (obiekt.has("summary")) obiekt.getString("summary") else null,
                urlOkladki = urlOkladki
            )
            gry.add(gra)
        }
        return@withContext gry
    }
}
