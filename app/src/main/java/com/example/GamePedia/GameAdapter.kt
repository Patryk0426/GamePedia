package com.example.GamePedia

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class GameAdapter(
    private val gry: List<Game>,
    private val onAddClick: ((Game) -> Unit)? = null
) : RecyclerView.Adapter<GameAdapter.WidokPosiadaczaGry>() {

    class WidokPosiadaczaGry(widok: View) : RecyclerView.ViewHolder(widok) {
        val tytul: TextView = widok.findViewById(R.id.gameTitle)
        val data: TextView = widok.findViewById(R.id.gameDate)
        val okladka: ImageView = widok.findViewById(R.id.gameCover)
        val btnAdd: MaterialButton = widok.findViewById(R.id.btnAddToList)
    }

    override fun onCreateViewHolder(rodzic: ViewGroup, typWidoku: Int): WidokPosiadaczaGry {
        val widok = LayoutInflater.from(rodzic.context)
            .inflate(R.layout.item_game, rodzic, false)
        return WidokPosiadaczaGry(widok)
    }

    override fun onBindViewHolder(posiadacz: WidokPosiadaczaGry, pozycja: Int) {
        val gra = gry[pozycja]
        posiadacz.tytul.text = gra.nazwa
        posiadacz.data.text = gra.dataWydania?.let {
            val data = Date(it * 1000)
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(data)
        } ?: "Brak daty"

        if (gra.urlOkladki != null) {
            Glide.with(posiadacz.itemView).load(gra.urlOkladki).into(posiadacz.okladka)
        } else {
            posiadacz.okladka.setImageResource(android.R.color.darker_gray)
        }

        posiadacz.btnAdd.setOnClickListener {
            onAddClick?.invoke(gra)
        }

        posiadacz.itemView.setOnClickListener {
            val context = posiadacz.itemView.context
            val intent = Intent(context, GameDetailsActivity::class.java).apply {
                putExtra("GAME_EXTRA", gra)
            }

            val options = androidx.core.app.ActivityOptionsCompat.makeCustomAnimation(
                context,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            context.startActivity(intent, options.toBundle())
        }
    }

    override fun getItemCount() = gry.size
}
