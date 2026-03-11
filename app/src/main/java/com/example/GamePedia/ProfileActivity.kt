package com.example.GamePedia

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.GamePedia.database.DatabaseHelper
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        dbHelper = DatabaseHelper(this)

        val usernameText: TextView = findViewById(R.id.profileUsername)
        val emailText: TextView = findViewById(R.id.profileEmail)
        val btnLogout: MaterialButton = findViewById(R.id.btnLogout)
        val btnBack: MaterialButton = findViewById(R.id.btnBack)

        val username = intent.getStringExtra("USERNAME")
        usernameText.text = username ?: "Nieznany użytkownik"

        if (username != null) {
            lifecycleScope.launch {
                val user = withContext(Dispatchers.IO) {
                    dbHelper.getUserByUsername(username)
                }
                user?.let {
                    emailText.text = it.email.ifEmpty { "Brak przypisanego adresu email" }
                }
            }
        }

        btnLogout.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }
}
