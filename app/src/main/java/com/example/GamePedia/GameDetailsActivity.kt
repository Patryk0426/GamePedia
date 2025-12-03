package com.example.GamePedia

import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class GameDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_details)

        // Referencje do widoków
        val gameCover: ImageView = findViewById(R.id.gameCoverDetails)
        val gameTitle: TextView = findViewById(R.id.gameTitleDetails)
        val gameDate: TextView = findViewById(R.id.gameDateDetails)
        val gameSummary: TextView = findViewById(R.id.gameSummary)
        val backButton: Button = findViewById(R.id.backButton)


        val gra: Game? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("GAME_EXTRA", Game::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("GAME_EXTRA")
        }

        // Ustawienie danych w widokach
        gra?.let {
            gameTitle.text = it.nazwa
            gameDate.text = it.dataWydania?.let { timestamp ->
                val date = Date(timestamp * 1000)
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
            } ?: "Brak daty"
            gameSummary.text = it.opis ?: "Brak opisu."

            if (it.urlOkladki != null) {
                Glide.with(this).load(it.urlOkladki).into(gameCover)
            } else {
                gameCover.setImageResource(android.R.color.darker_gray)
            }
        }

        backButton.setOnClickListener {
            finish()
        }
    }
}
